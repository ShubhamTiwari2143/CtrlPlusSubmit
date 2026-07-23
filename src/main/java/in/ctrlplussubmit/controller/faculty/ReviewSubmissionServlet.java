package in.ctrlplussubmit.controller.faculty;

import in.ctrlplussubmit.dao.SubmissionDAO;
import in.ctrlplussubmit.dao.TaskDAO;
import in.ctrlplussubmit.model.Submission;
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

/**
 * SubmissionReviewServlet — Faculty views and reviews student submissions
 *
 * Access: FACULTY only
 *
 * GET  /faculty/review                      → all submissions for this faculty
 * GET  /faculty/review?taskId=N             → submissions for a specific task
 * GET  /faculty/review?status=SUBMITTED     → filter by status
 * POST /faculty/review                      → submit a grade/review
 *
 * POST body params (review):
 *   submissionId, marks, remarks, newStatus (APPROVED|NEEDS_IMPROVEMENT|REJECTED|REVIEWED)
 */
@WebServlet("/faculty/review")
public class ReviewSubmissionServlet extends HttpServlet {

    private final SubmissionDAO submissionDAO = new SubmissionDAO();
    private final TaskDAO       taskDAO       = new TaskDAO();

    // -------------------------------------------------------
    //  GET — list submissions
    // -------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "FACULTY")) return;

        User   faculty      = SessionUtil.getLoggedInUser(request);
        String taskIdParam  = request.getParameter("taskId");
        String statusFilter = request.getParameter("status");

        List<Submission> submissions;

        if (taskIdParam != null) {
            int taskId = parseId(taskIdParam);
            if (taskId <= 0) { JsonUtil.sendBadRequest(response, "Invalid task ID."); return; }

            // Security: ensure this task belongs to the logged-in faculty
            if (taskDAO.findById(taskId, faculty.getId()) == null) {
                JsonUtil.sendForbidden(response, "Access denied to this task's submissions."); return;
            }
            submissions = submissionDAO.findByTask(taskId);
        } else {
            submissions = submissionDAO.findByFaculty(faculty.getId(), statusFilter);
        }

        JsonUtil.sendSuccess(response, "Submissions loaded.", submissions);
    }

    // -------------------------------------------------------
    //  POST — submit review
    // -------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "FACULTY")) return;
        request.setCharacterEncoding("UTF-8");

        User faculty       = SessionUtil.getLoggedInUser(request);
        int  submissionId  = parseId(request.getParameter("submissionId"));
        String newStatus   = request.getParameter("newStatus");
        String remarksStr  = request.getParameter("remarks");
        String marksStr    = request.getParameter("marks");

        if (submissionId <= 0) { JsonUtil.sendBadRequest(response, "Invalid submission ID."); return; }
        if (isBlank(newStatus)) { JsonUtil.sendBadRequest(response, "Review status is required."); return; }

        // Validate newStatus
        List<String> valid = List.of("REVIEWED","APPROVED","NEEDS_IMPROVEMENT","REJECTED");
        if (!valid.contains(newStatus)) {
            JsonUtil.sendBadRequest(response,
                "Invalid status. Use: APPROVED, NEEDS_IMPROVEMENT, REJECTED, or REVIEWED.");
            return;
        }

        // Parse marks (nullable)
        Integer marks = null;
        if (!isBlank(marksStr)) {
            try {
                marks = Integer.parseInt(marksStr.trim());
                if (marks < 0) { JsonUtil.sendBadRequest(response, "Marks cannot be negative."); return; }
            } catch (NumberFormatException e) {
                JsonUtil.sendBadRequest(response, "Marks must be a valid number."); return;
            }
        }

        String remarks = isBlank(remarksStr) ? null : remarksStr.trim();

        if (submissionDAO.review(submissionId, marks, remarks, newStatus, faculty.getId())) {
            JsonUtil.sendSuccess(response, "Submission reviewed successfully. Status: " + newStatus);
        } else {
            JsonUtil.sendServerError(response, "Review failed. Please try again.");
        }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}
