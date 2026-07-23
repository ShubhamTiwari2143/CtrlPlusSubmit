package in.ctrlplussubmit.controller.student;


import in.ctrlplussubmit.dao.SubmissionDAO;
import in.ctrlplussubmit.dao.TaskDAO;
import in.ctrlplussubmit.model.Submission;
import in.ctrlplussubmit.model.Task;
import in.ctrlplussubmit.model.User;
import in.ctrlplussubmit.util.JsonUtil;
import in.ctrlplussubmit.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TaskViewServlet — Returns tasks assigned to the logged-in student
 *
 * Access: STUDENT only
 *
 * GET /student/tasks              → all assigned tasks with submission status
 * GET /student/tasks?taskId=N     → single task detail + student's own submission
 * GET /student/tasks?status=PENDING → filter tasks by submission status
 */
@WebServlet("/student/tasks")
public class TaskViewServlet extends HttpServlet {

    private final TaskDAO       taskDAO       = new TaskDAO();
    private final SubmissionDAO submissionDAO = new SubmissionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "STUDENT")) return;

        User student   = SessionUtil.getLoggedInUser(request);
        int  studentId = student.getId();

        String taskIdParam    = request.getParameter("taskId");
        String statusFilter   = request.getParameter("status");

        if (taskIdParam != null) {
            // ---- Single task detail ----
            int taskId = parseId(taskIdParam);
            if (taskId <= 0) { JsonUtil.sendBadRequest(response, "Invalid task ID."); return; }

            Task task = null;
            // Find the task from the student's assigned list (security: only see assigned tasks)
            List<Task> assigned = taskDAO.findAssignedToStudent(studentId);
            for (Task t : assigned) {
                if (t.getId() == taskId) { task = t; break; }
            }

            if (task == null) {
                JsonUtil.sendForbidden(response,
                    "You are not assigned this task or it does not exist.");
                return;
            }

            // Fetch this student's submission for the task
            Submission submission = submissionDAO.findByTaskAndStudent(taskId, studentId);

            Map<String, Object> data = new HashMap<>();
            data.put("task",       task);
            data.put("submission", submission);

            JsonUtil.sendSuccess(response, "Task detail loaded.", data);

        } else {
            // ---- All assigned tasks ----
            List<Task> tasks = taskDAO.findAssignedToStudent(studentId);

            // Optional status filter — match against the student's submission status
            if (statusFilter != null && !statusFilter.isBlank()) {
                final String filter = statusFilter.toUpperCase();
                // Fetch all submissions to cross-reference
                List<Submission> subs = submissionDAO.findByStudent(studentId);
                Map<Integer, String> statusMap = new HashMap<>();
                subs.forEach(s -> statusMap.put(s.getTaskId(), s.getStatus()));

                tasks = tasks.stream()
                    .filter(t -> filter.equals(statusMap.getOrDefault(t.getId(), "PENDING")))
                    .collect(java.util.stream.Collectors.toList());
            }

            JsonUtil.sendSuccess(response, "Tasks loaded.", tasks);
        }
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}

