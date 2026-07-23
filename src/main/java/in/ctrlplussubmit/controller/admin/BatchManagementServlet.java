package in.ctrlplussubmit.controller.admin;


import in.ctrlplussubmit.dao.BatchDAO;
import in.ctrlplussubmit.dao.UserDAO;
import in.ctrlplussubmit.model.Batch;
import in.ctrlplussubmit.model.User;
import in.ctrlplussubmit.util.JsonUtil;
import in.ctrlplussubmit.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * BatchServlet — Batch/Course management for Admin
 *
 * Access: ADMIN only
 *
 * GET  /admin/batches                  → list all batches
 * GET  /admin/batches?action=faculty   → list all faculty users (for dropdown)
 * GET  /admin/batches?action=students&batchId=N → list students in batch
 * POST /admin/batches                  → create a batch
 * POST /admin/batches?action=update    → update batch details
 * POST /admin/batches?action=toggle    → activate/deactivate batch
 * POST /admin/batches?action=addStudent    → enroll student into batch
 * POST /admin/batches?action=removeStudent → remove student from batch
 */
@WebServlet("/admin/batches")
public class BatchManagementServlet extends HttpServlet {

    private final BatchDAO batchDAO = new BatchDAO();
    private final UserDAO  userDAO  = new UserDAO();

    // -------------------------------------------------------
    //  GET
    // -------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "ADMIN")) return;

        String action = request.getParameter("action");

        if ("faculty".equals(action)) {
            // Return faculty list for batch-create dropdown
            List<Map<String, Object>> faculty = userDAO.findByRole("FACULTY")
                .stream()
                .map(u -> Map.of("id", (Object) u.getId(), "fullName", u.getFullName()))
                .collect(Collectors.toList());
            JsonUtil.sendSuccess(response, "Faculty list loaded.", faculty);

        } else if ("students".equals(action)) {
            // Return students enrolled in a specific batch
            int batchId = parseId(request.getParameter("batchId"));
            if (batchId <= 0) { JsonUtil.sendBadRequest(response, "Invalid batch ID."); return; }

            List<Integer> ids = batchDAO.getStudentIds(batchId);
            JsonUtil.sendSuccess(response, "Enrolled students.", ids);

        } else {
            // Default: return all batches
            List<Batch> batches = batchDAO.findAll();
            JsonUtil.sendSuccess(response, "Batches loaded.", batches);
        }
    }

    // -------------------------------------------------------
    //  POST
    // -------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "ADMIN")) return;
        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        if (action == null) {
            handleCreate(request, response);
        } else {
            switch (action) {
                case "update"        -> handleUpdate(request, response);
                case "toggle"        -> handleToggle(request, response);
                case "addStudent"    -> handleAddStudent(request, response);
                case "removeStudent" -> handleRemoveStudent(request, response);
                default              -> JsonUtil.sendBadRequest(response, "Unknown action: " + action);
            }
        }
    }

    // -------------------------------------------------------
    //  CREATE
    // -------------------------------------------------------
    private void handleCreate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String batchName   = request.getParameter("batchName");
        String description = request.getParameter("description");
        int    facultyId   = parseId(request.getParameter("facultyId"));

        if (isBlank(batchName)) { JsonUtil.sendBadRequest(response, "Batch name is required."); return; }
        if (facultyId <= 0)     { JsonUtil.sendBadRequest(response, "Please select a faculty member."); return; }

        // Validate faculty exists and is actually a FACULTY
        User faculty = userDAO.findById(facultyId);
        if (faculty == null || !faculty.isFaculty()) {
            JsonUtil.sendBadRequest(response, "Selected user is not a valid faculty member."); return;
        }

        Batch batch = new Batch(batchName.trim(),
                                isBlank(description) ? null : description.trim(),
                                facultyId);
        if (batchDAO.create(batch)) {
            JsonUtil.sendSuccess(response, "Batch '" + batch.getBatchName() + "' created successfully.", batch);
        } else {
            JsonUtil.sendServerError(response, "Failed to create batch.");
        }
    }

    // -------------------------------------------------------
    //  UPDATE
    // -------------------------------------------------------
    private void handleUpdate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int    batchId     = parseId(request.getParameter("batchId"));
        String batchName   = request.getParameter("batchName");
        String description = request.getParameter("description");
        int    facultyId   = parseId(request.getParameter("facultyId"));

        if (batchId <= 0)       { JsonUtil.sendBadRequest(response, "Invalid batch ID."); return; }
        if (isBlank(batchName)) { JsonUtil.sendBadRequest(response, "Batch name is required."); return; }
        if (facultyId <= 0)     { JsonUtil.sendBadRequest(response, "Please select a faculty member."); return; }

        Batch batch = batchDAO.findById(batchId);
        if (batch == null) { JsonUtil.sendNotFound(response, "Batch not found."); return; }

        batch.setBatchName(batchName.trim());
        batch.setDescription(isBlank(description) ? null : description.trim());
        batch.setFacultyId(facultyId);

        if (batchDAO.update(batch)) {
            JsonUtil.sendSuccess(response, "Batch updated successfully.");
        } else {
            JsonUtil.sendServerError(response, "Update failed.");
        }
    }

    // -------------------------------------------------------
    //  TOGGLE ACTIVE
    // -------------------------------------------------------
    private void handleToggle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int     batchId  = parseId(request.getParameter("batchId"));
        boolean isActive = Boolean.parseBoolean(request.getParameter("isActive"));

        if (batchId <= 0) { JsonUtil.sendBadRequest(response, "Invalid batch ID."); return; }

        if (batchDAO.setActive(batchId, isActive)) {
            JsonUtil.sendSuccess(response, isActive ? "Batch activated." : "Batch deactivated.");
        } else {
            JsonUtil.sendServerError(response, "Status update failed.");
        }
    }

    // -------------------------------------------------------
    //  ENROLL STUDENT
    // -------------------------------------------------------
    private void handleAddStudent(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int batchId   = parseId(request.getParameter("batchId"));
        int studentId = parseId(request.getParameter("studentId"));

        if (batchId <= 0 || studentId <= 0) {
            JsonUtil.sendBadRequest(response, "Invalid batch or student ID."); return;
        }

        User student = userDAO.findById(studentId);
        if (student == null || !student.isStudent()) {
            JsonUtil.sendBadRequest(response, "Selected user is not a student."); return;
        }

        if (batchDAO.isStudentInBatch(batchId, studentId)) {
            JsonUtil.sendBadRequest(response, "Student is already enrolled in this batch."); return;
        }

        if (batchDAO.addStudent(batchId, studentId)) {
            JsonUtil.sendSuccess(response, student.getFullName() + " enrolled successfully.");
        } else {
            JsonUtil.sendServerError(response, "Enrollment failed.");
        }
    }

    // -------------------------------------------------------
    //  REMOVE STUDENT
    // -------------------------------------------------------
    private void handleRemoveStudent(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int batchId   = parseId(request.getParameter("batchId"));
        int studentId = parseId(request.getParameter("studentId"));

        if (batchId <= 0 || studentId <= 0) {
            JsonUtil.sendBadRequest(response, "Invalid batch or student ID."); return;
        }

        if (batchDAO.removeStudent(batchId, studentId)) {
            JsonUtil.sendSuccess(response, "Student removed from batch.");
        } else {
            JsonUtil.sendServerError(response, "Removal failed.");
        }
    }

    // -------------------------------------------------------
    //  HELPERS
    // -------------------------------------------------------
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
    
    
}
