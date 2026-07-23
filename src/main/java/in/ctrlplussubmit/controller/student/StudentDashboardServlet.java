package in.ctrlplussubmit.controller.student;

import in.ctrlplussubmit.dao.NotificationDAO;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set; // Added this import
import java.util.stream.Collectors;

/**
 * StudentDashboardServlet — Student dashboard data
 *
 * GET /student/dashboard
 *   Returns JSON:
 *     - stats: total tasks, pending, submitted, reviewed
 *     - upcomingTasks: tasks with deadline in next 7 days
 *     - recentSubmissions: last 5 submission updates
 *     - unreadNotifications: count for badge
 *
 * Access: STUDENT only
 */
@WebServlet("/student/dashboard")
public class StudentDashboardServlet extends HttpServlet {

    private final TaskDAO         taskDAO         = new TaskDAO();
    private final SubmissionDAO   submissionDAO   = new SubmissionDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "STUDENT")) return;

        User student   = SessionUtil.getLoggedInUser(request);
        int  studentId = student.getId();

        // All submissions for this student
        List<Submission> allSubs = submissionDAO.findByStudent(studentId);

        // Compute status counts
        long totalTasks  = allSubs.size();
        long pending     = allSubs.stream().filter(s -> "PENDING".equals(s.getStatus())).count();
        long submitted   = allSubs.stream().filter(s ->
            "SUBMITTED".equals(s.getStatus()) || "LATE".equals(s.getStatus())).count();
        long reviewed    = allSubs.stream().filter(Submission::isReviewed).count();

        // Upcoming deadlines — tasks not yet submitted, deadline within 7 days
        LocalDateTime now     = LocalDateTime.now();
        LocalDateTime in7Days = now.plusDays(7);

        // --- NEW FIX: Get IDs of tasks that are already submitted or reviewed ---
        Set<Integer> completedTaskIds = allSubs.stream()
            .filter(s -> !"PENDING".equals(s.getStatus()))
            .map(Submission::getTaskId)
            .collect(Collectors.toSet());

        List<Task> assigned = taskDAO.findAssignedToStudent(studentId);
        List<Map<String, Object>> upcoming = assigned.stream()
            .filter(t -> t.getDeadline() != null
                      && t.getDeadline().isAfter(now)
                      && t.getDeadline().isBefore(in7Days))
            // --- NEW FIX: Filter out tasks if they are in the completed list ---
            .filter(t -> !completedTaskIds.contains(t.getId()))
            .sorted((a, b) -> a.getDeadline().compareTo(b.getDeadline()))
            .limit(5)
            .map(t -> {
                Map<String, Object> m = new HashMap<>();
                m.put("taskId",    t.getId());
                m.put("title",     t.getTitle());
                m.put("deadline",  t.getDeadline().format(FMT));
                m.put("maxMarks",  t.getMaxMarks());
                m.put("facultyName", t.getFacultyName());
                return m;
            })
            .collect(Collectors.toList());

        // Recent submissions (last 5 with updates)
        List<Map<String, Object>> recentSubs = allSubs.stream()
            .filter(Submission::isSubmitted)
            .limit(5)
            .map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("taskTitle",   s.getTaskTitle());
                m.put("status",      s.getStatus());
                m.put("marks",       s.getMarks());
                m.put("maxMarks",    s.getMaxMarks());
                m.put("submittedAt", s.getSubmittedAt() != null
                                     ? s.getSubmittedAt().format(FMT) : null);
                return m;
            })
            .collect(Collectors.toList());

        // Unread notification count
        int unreadCount = notificationDAO.countUnread(studentId);

        // Build final payload
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTasks", totalTasks);
        stats.put("pending",    pending);
        stats.put("submitted",  submitted);
        stats.put("reviewed",   reviewed);
        data.put("stats",              stats);
        data.put("upcomingDeadlines",  upcoming);
        data.put("recentSubmissions",  recentSubs);
        data.put("unreadNotifications", unreadCount);

        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("fullName", student.getFullName());
        studentInfo.put("id",       studentId);
        data.put("student", studentInfo);

        JsonUtil.sendSuccess(response, "Dashboard loaded.", data);
    }
}