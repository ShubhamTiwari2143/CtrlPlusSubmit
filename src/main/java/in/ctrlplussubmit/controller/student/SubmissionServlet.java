package in.ctrlplussubmit.controller.student;

import in.ctrlplussubmit.dao.NotificationDAO;
import in.ctrlplussubmit.dao.SubmissionDAO;
import in.ctrlplussubmit.dao.TaskDAO;
import in.ctrlplussubmit.model.Submission;
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

/**
 * SubmissionServlet — Handles student file submissions and resubmissions
 *
 * Access: STUDENT only
 *
 * GET  /student/submit?taskId=N  → returns current submission state for a task
 *                                  (used to decide whether to show Submit or Resubmit)
 * POST /student/submit           → first submission (file + comments)
 * POST /student/submit?action=resubmit → replace existing file before deadline
 *
 * POST body (multipart/form-data):
 *   taskId      : int
 *   submissionFile : file (PDF / ZIP / Image / Code)
 *   comments    : text (optional student note)
 */
@WebServlet("/student/submit")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,
    maxFileSize       = 20 * 1024 * 1024L,
    maxRequestSize    = 25 * 1024 * 1024L
)
public class SubmissionServlet extends HttpServlet {

    private final SubmissionDAO   submissionDAO   = new SubmissionDAO();
    private final TaskDAO         taskDAO         = new TaskDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    // -------------------------------------------------------
    //  GET — current submission state for a task
    // -------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "STUDENT")) return;

        User student  = SessionUtil.getLoggedInUser(request);
        int  taskId   = parseId(request.getParameter("taskId"));

        if (taskId <= 0) { JsonUtil.sendBadRequest(response, "Invalid task ID."); return; }

        Submission sub = submissionDAO.findByTaskAndStudent(taskId, student.getId());
        if (sub == null) {
            JsonUtil.sendForbidden(response, "You are not assigned this task.");
            return;
        }

        JsonUtil.sendSuccess(response, "Submission loaded.", sub);
    }

    // -------------------------------------------------------
    //  POST — submit or resubmit
    // -------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "STUDENT")) return;
        request.setCharacterEncoding("UTF-8");

        User   student  = SessionUtil.getLoggedInUser(request);
        String action   = request.getParameter("action"); // null = first submit, "resubmit" = replace
        int    taskId   = parseId(request.getParameter("taskId"));
        String comments = request.getParameter("comments");

        if (taskId <= 0) { JsonUtil.sendBadRequest(response, "Invalid task ID."); return; }

        // Fetch task (validates student is assigned)
        Task task = null;
        var assigned = taskDAO.findAssignedToStudent(student.getId());
        for (Task t : assigned) { if (t.getId() == taskId) { task = t; break; } }

        if (task == null) {
            JsonUtil.sendForbidden(response, "You are not assigned this task."); return;
        }

        // Fetch existing submission row
        Submission existing = submissionDAO.findByTaskAndStudent(taskId, student.getId());
        if (existing == null) {
            JsonUtil.sendServerError(response, "Submission record not found. Contact admin."); return;
        }

        // For resubmit: verify it was previously submitted (not just PENDING)
        boolean isResubmit = "resubmit".equalsIgnoreCase(action);
        if (isResubmit && existing.isPending()) {
            JsonUtil.sendBadRequest(response,
                "No prior submission to resubmit. Please submit first."); return;
        }

        // Process file upload
        Part filePart = request.getPart("submissionFile");
        if (filePart == null || filePart.getSize() == 0) {
            JsonUtil.sendBadRequest(response, "Please select a file to upload."); return;
        }

        String uploadDir = getServletContext().getInitParameter("UPLOAD_DIR");
        FileUploadUtil.UploadResult result =
            FileUploadUtil.upload(filePart, uploadDir, FileUploadUtil.CONTEXT_SUBMISSIONS);

        if (!result.isSuccess()) {
            JsonUtil.sendBadRequest(response, result.getErrorMessage()); return;
        }

        // Build updated submission object
        existing.setFilePath(result.getStoredPath());
        existing.setFileName(result.getOriginalName());
        
        // ---> FIX: Calculate and set file size in KB <---
        int fileSizeInKB = (int) (filePart.getSize() / 1024);
        existing.setFileSizeKB(fileSizeInKB);
        
        existing.setComments(comments != null && !comments.isBlank() ? comments.trim() : null);

        boolean saved;
        if (isResubmit) {
            saved = submissionDAO.resubmit(existing, task.getDeadline());
        } else {
            saved = submissionDAO.submit(existing, task.getDeadline());
        }

        if (saved) {
            // Notify faculty — create a system notification for the faculty owner
            String notifTitle = isResubmit
                ? "Resubmission: " + task.getTitle()
                : "New submission: " + task.getTitle();
            String notifMsg = student.getFullName() +
                (isResubmit ? " resubmitted" : " submitted") +
                " their work for \"" + task.getTitle() + "\".";
            notificationDAO.notify(task.getFacultyId(),
                notifTitle, notifMsg, "TASK_ASSIGNED", taskId);

            String successMsg = isResubmit
                ? "Resubmission successful! Your file has been updated."
                : "Submission successful! Your assignment has been sent to the faculty.";
            JsonUtil.sendSuccess(response, successMsg);
        } else {
            JsonUtil.sendServerError(response,
                "Submission failed. Please try again."); return;
        }
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}