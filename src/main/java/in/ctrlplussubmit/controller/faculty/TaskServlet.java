package in.ctrlplussubmit.controller.faculty;

import in.ctrlplussubmit.dao.BatchDAO;
import in.ctrlplussubmit.dao.TaskAssignmentDAO;
import in.ctrlplussubmit.dao.TaskDAO;
import in.ctrlplussubmit.dao.UserDAO;
import in.ctrlplussubmit.model.Batch;
import in.ctrlplussubmit.model.Task;
import in.ctrlplussubmit.model.User;
import in.ctrlplussubmit.util.FileUploadUtil;
import in.ctrlplussubmit.util.JsonUtil;
import in.ctrlplussubmit.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TaskServlet — Faculty task management
 *
 * assignType values are INDIVIDUAL | BATCH everywhere:
 *   - JS radio buttons send value="INDIVIDUAL" and value="BATCH"
 *   - task_assignments.assignment_type enum('BATCH','INDIVIDUAL')
 *   - TaskAssignmentDAO uses 'INDIVIDUAL' in SQL INSERT strings
 *
 * All three layers are now consistent.
 *
 * GET  /faculty/tasks                                    → all tasks for this faculty
 * GET  /faculty/tasks?taskId=N                           → single task + assignments
 * GET  /faculty/tasks?action=batches                     → faculty's batch dropdown list
 * GET  /faculty/tasks?action=studentsByBatch&batchId=N → students in a batch
 * GET  /faculty/tasks?action=students                    → all students (fallback)
 * POST /faculty/tasks                                    → create task
 * POST /faculty/tasks?action=update                      → update task
 * POST /faculty/tasks?action=delete                      → soft delete
 * POST /faculty/tasks?action=assign                      → assign to INDIVIDUAL students or BATCH
 */
@WebServlet("/faculty/tasks")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,
    maxFileSize       = 20 * 1024 * 1024L,
    maxRequestSize    = 25 * 1024 * 1024L
)
public class TaskServlet extends HttpServlet {

    private final TaskDAO             taskDAO   = new TaskDAO();
    private final TaskAssignmentDAO assignDAO = new TaskAssignmentDAO();
    private final BatchDAO            batchDAO  = new BatchDAO();
    private final UserDAO             userDAO   = new UserDAO();

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // =========================================================
    //  GET
    // =========================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "FACULTY")) return;

        User   faculty = SessionUtil.getLoggedInUser(request);
        String action  = request.getParameter("action");
        String taskIdP = request.getParameter("taskId");

        // ── Students in a specific batch (called by JS after batch dropdown change) ──
        if ("studentsByBatch".equals(action)) {
            int batchId = parseId(request.getParameter("batchId"));
            if (batchId <= 0) {
                JsonUtil.sendBadRequest(response, "Invalid batch ID."); return;
            }
            // Security: batch must belong to this faculty
            var batch = batchDAO.findById(batchId);
            if (batch == null || batch.getFacultyId() != faculty.getId()) {
                JsonUtil.sendForbidden(response, "You do not have access to this batch."); return;
            }
            var students = batchDAO.getStudentsInBatch(batchId);
            JsonUtil.sendSuccess(response, "Students loaded.", students);
            return;
        }

        // ── Batch dropdown list (populate batch <select> in modal) ──────────────
        if ("batches".equals(action)) {
            var batches = batchDAO.findByFaculty(faculty.getId())
                    .stream()
                    .filter(Batch::isActive) // 🔒 Active batches only (using boolean isActive)
                    .map(b -> Map.of(
                            "id",          (Object) b.getId(),
                            "batchName",    b.getBatchName(),
                            "studentCount", b.getStudentCount()))
                    .collect(Collectors.toList());
            JsonUtil.sendSuccess(response, "Batches loaded.", batches);
            return;
        }

        // ── All students (fallback list — not used by the enhanced modal) ────────
        if ("students".equals(action)) {
            var students = userDAO.findByRole("STUDENT")
                    .stream()
                    .map(u -> Map.of(
                            "id",       (Object) u.getId(),
                            "fullName", u.getFullName(),
                            "email",    u.getEmail()))
                    .collect(Collectors.toList());
            JsonUtil.sendSuccess(response, "Students loaded.", students);
            return;
        }

        // ── Single task detail ───────────────────────────────────────────────────
        if (taskIdP != null) {
            int taskId = parseId(taskIdP);
            Task task  = taskDAO.findById(taskId, faculty.getId());
            if (task == null) {
                JsonUtil.sendNotFound(response, "Task not found."); return;
            }
            var assignments = assignDAO.getAssignmentsForTask(taskId);
            Map<String, Object> data = new HashMap<>();
            data.put("task",        task);
            data.put("assignments", assignments);
            JsonUtil.sendSuccess(response, "Task loaded.", data);
            return;
        }

        // ── All tasks for this faculty ───────────────────────────────────────────
        JsonUtil.sendSuccess(response, "Tasks loaded.",
                taskDAO.findByFaculty(faculty.getId()));
    }

    // =========================================================
    //  POST
    // =========================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "FACULTY")) return;
        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        if (action == null) {
            handleCreate(request, response);
        } else {
            switch (action) {
                case "update" -> handleUpdate(request, response);
                case "delete" -> handleDelete(request, response);
                case "assign" -> handleAssign(request, response);
                default       -> JsonUtil.sendBadRequest(response, "Unknown action: " + action);
            }
        }
    }

    // =========================================================
    //  CREATE
    // =========================================================
    private void handleCreate(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        User   faculty = SessionUtil.getLoggedInUser(request);
        String title   = request.getParameter("title");
        String desc    = request.getParameter("description");
        String dlStr   = request.getParameter("deadline");
        String maxMStr = request.getParameter("maxMarks");

        if (isBlank(title)) { JsonUtil.sendBadRequest(response, "Task title is required."); return; }
        if (isBlank(dlStr)) { JsonUtil.sendBadRequest(response, "Deadline is required.");   return; }

        LocalDateTime deadline = parseDeadline(dlStr);
        if (deadline == null) {
            JsonUtil.sendBadRequest(response, "Invalid deadline format. Use YYYY-MM-DDTHH:mm."); return;
        }
        if (deadline.isBefore(LocalDateTime.now())) {
            JsonUtil.sendBadRequest(response, "Deadline cannot be in the past."); return;
        }

        int maxMarks = 100;
        try { maxMarks = Integer.parseInt(maxMStr); } catch (Exception ignored) {}
        if (maxMarks < 1 || maxMarks > 1000) {
            JsonUtil.sendBadRequest(response, "Max marks must be between 1 and 1000."); return;
        }

        Task task = new Task(
                title.trim(),
                isBlank(desc) ? null : desc.trim(),
                faculty.getId(),
                deadline,
                maxMarks
        );

        // Optional file attachment
        Part filePart = request.getPart("attachFile");
        if (filePart != null && filePart.getSize() > 0) {
            String uploadDir = getServletContext().getInitParameter("UPLOAD_DIR");
            FileUploadUtil.UploadResult result =
                    FileUploadUtil.upload(filePart, uploadDir, FileUploadUtil.CONTEXT_TASKS);
            if (!result.isSuccess()) {
                JsonUtil.sendBadRequest(response, "File upload failed: " + result.getErrorMessage());
                return;
            }
            task.setAttachFilePath(result.getStoredPath());
            task.setAttachFileName(result.getOriginalName());
        }

        if (taskDAO.create(task)) {
            JsonUtil.sendSuccess(response,
                    "Task '" + task.getTitle() + "' created successfully.", task);
        } else {
            JsonUtil.sendServerError(response, "Failed to create task. Please try again.");
        }
    }

    // =========================================================
    //  UPDATE
    // =========================================================
    private void handleUpdate(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        User faculty = SessionUtil.getLoggedInUser(request);
        int  taskId  = parseId(request.getParameter("taskId"));
        if (taskId <= 0) { JsonUtil.sendBadRequest(response, "Invalid task ID."); return; }

        Task existing = taskDAO.findById(taskId, faculty.getId());
        if (existing == null) {
            JsonUtil.sendNotFound(response, "Task not found or access denied."); return;
        }

        String title   = request.getParameter("title");
        String desc    = request.getParameter("description");
        String dlStr   = request.getParameter("deadline");
        String maxMStr = request.getParameter("maxMarks");

        if (isBlank(title)) { JsonUtil.sendBadRequest(response, "Task title is required."); return; }
        if (isBlank(dlStr)) { JsonUtil.sendBadRequest(response, "Deadline is required.");   return; }

        LocalDateTime deadline = parseDeadline(dlStr);
        if (deadline == null) {
            JsonUtil.sendBadRequest(response, "Invalid deadline format. Use YYYY-MM-DDTHH:mm."); return;
        }

        int maxMarks = existing.getMaxMarks();
        try { maxMarks = Integer.parseInt(maxMStr); } catch (Exception ignored) {}

        existing.setTitle(title.trim());
        existing.setDescription(isBlank(desc) ? null : desc.trim());
        existing.setDeadline(deadline);
        existing.setMaxMarks(maxMarks);
        existing.setAttachFilePath(null);
        existing.setAttachFileName(null);

        Part filePart = request.getPart("attachFile");
        if (filePart != null && filePart.getSize() > 0) {
            String uploadDir = getServletContext().getInitParameter("UPLOAD_DIR");
            FileUploadUtil.UploadResult result =
                    FileUploadUtil.upload(filePart, uploadDir, FileUploadUtil.CONTEXT_TASKS);
            if (!result.isSuccess()) {
                JsonUtil.sendBadRequest(response, "File upload failed: " + result.getErrorMessage());
                return;
            }
            existing.setAttachFilePath(result.getStoredPath());
            existing.setAttachFileName(result.getOriginalName());
        }

        if (taskDAO.update(existing)) {
            JsonUtil.sendSuccess(response, "Task updated successfully.", existing);
        } else {
            JsonUtil.sendServerError(response, "Update failed. Please try again.");
        }
    }

    // =========================================================
    //  SOFT DELETE
    // =========================================================
    private void handleDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        User faculty = SessionUtil.getLoggedInUser(request);
        int  taskId  = parseId(request.getParameter("taskId"));
        if (taskId <= 0) { JsonUtil.sendBadRequest(response, "Invalid task ID."); return; }

        if (taskDAO.softDelete(taskId, faculty.getId())) {
            JsonUtil.sendSuccess(response, "Task deleted successfully.");
        } else {
            JsonUtil.sendServerError(response, "Delete failed or task not found.");
        }
    }

    // =========================================================
    //  ASSIGN
    // =========================================================
    private void handleAssign(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        User   faculty    = SessionUtil.getLoggedInUser(request);
        int    taskId     = parseId(request.getParameter("taskId"));
        String assignType = request.getParameter("assignType");   // "INDIVIDUAL" | "BATCH"

        if (taskId <= 0)        { JsonUtil.sendBadRequest(response, "Invalid task ID."); return; }
        if (isBlank(assignType)) { JsonUtil.sendBadRequest(response, "Assign type is required."); return; }

        assignType = assignType.trim().toUpperCase();

        Task task = taskDAO.findById(taskId, faculty.getId());
        if (task == null) {
            JsonUtil.sendNotFound(response, "Task not found or access denied."); return;
        }

        // ── BATCH ────────────────────────────────────────────────────────────
        if ("BATCH".equals(assignType)) {

            int batchId = parseId(request.getParameter("targetId"));
            if (batchId <= 0) {
                JsonUtil.sendBadRequest(response, "Invalid batch ID."); return;
            }

            var batch = batchDAO.findById(batchId);
            if (batch == null) {
                JsonUtil.sendNotFound(response, "Batch not found."); return;
            }
            if (batch.getFacultyId() != faculty.getId()) {
                JsonUtil.sendForbidden(response, "You do not own this batch."); return;
            }

            // 🔒 Prevent assigning to an INACTIVE batch
            if (!batch.isActive()) {
                JsonUtil.sendBadRequest(response, "Cannot assign tasks to an inactive batch.");
                return;
            }

            int count = assignDAO.assignToBatch(taskId, batchId);
            if (count >= 0) {
                JsonUtil.sendSuccess(response,
                        "Task assigned to batch '" + batch.getBatchName() +
                        "'. " + count + " student(s) can now see it.");
            } else {
                JsonUtil.sendServerError(response, "Batch assignment failed. Please try again.");
            }
            return;
        }

        // ── INDIVIDUAL (multi-select checkboxes) ─────────────────────────────
        if ("INDIVIDUAL".equals(assignType)) {

            String studentIdsParam = request.getParameter("studentIds");
            if (isBlank(studentIdsParam)) {
                JsonUtil.sendBadRequest(response,
                        "Please select at least one student."); return;
            }

            List<Integer> studentIds = new ArrayList<>();
            for (String part : studentIdsParam.split(",")) {
                int id = parseId(part.trim());
                if (id > 0) studentIds.add(id);
            }

            if (studentIds.isEmpty()) {
                JsonUtil.sendBadRequest(response,
                        "No valid student IDs provided."); return;
            }

            int          successCount = 0;
            List<String> skipped      = new ArrayList<>();

            for (int studentId : studentIds) {
                User student = userDAO.findById(studentId);

                if (student == null || !student.isStudent()) {
                    skipped.add("ID " + studentId + " is not a valid student");
                    continue;
                }

                if (assignDAO.isAlreadyAssigned(taskId, studentId)) {
                    skipped.add(student.getFullName() + " (already assigned)");
                    continue;
                }

                if (assignDAO.assignToStudent(taskId, studentId)) {
                    successCount++;
                } else {
                    skipped.add(student.getFullName() + " (assignment failed)");
                }
            }

            String msg = successCount + " student(s) assigned successfully.";
            if (!skipped.isEmpty()) {
                msg += " Skipped: " + String.join(", ", skipped) + ".";
            }
            JsonUtil.sendSuccess(response, msg);
            return;
        }

        JsonUtil.sendBadRequest(response,
                "Invalid assignType '" + assignType + "'. Must be 'INDIVIDUAL' or 'BATCH'.");
    }

    // =========================================================
    //  HELPERS
    // =========================================================

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return -1; }
    }

    private LocalDateTime parseDeadline(String s) {
        try { return LocalDateTime.parse(s.trim(), DT_FMT); }
        catch (DateTimeParseException e) { return null; }
    }
}