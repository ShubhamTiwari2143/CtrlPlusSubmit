package in.ctrlplussubmit.controller.student;

import in.ctrlplussubmit.dao.NotificationDAO;
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

/**
 * NotificationServlet — Notification read/mark endpoints for the student
 *
 * Access: STUDENT only (Faculty also uses this for their own notifications)
 *
 * GET  /student/notifications              → all notifications for the student
 * GET  /student/notifications?unread=true  → unread only (count for badge)
 * POST /student/notifications?action=markRead&notifId=N   → mark one read
 * POST /student/notifications?action=markAllRead           → mark all read
 */
@WebServlet("/student/notifications")
public class NotificationServlet extends HttpServlet {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    // -------------------------------------------------------
    //  GET — list notifications
    // -------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "STUDENT")) return;

        User student     = SessionUtil.getLoggedInUser(request);
        String unreadOnly = request.getParameter("unread");

        if ("true".equalsIgnoreCase(unreadOnly)) {
            int count = notificationDAO.countUnread(student.getId());
            JsonUtil.sendSuccess(response, "Unread count.", Map.of("count", count));
        } else {
            var notifs = notificationDAO.getAll(student.getId());
            JsonUtil.sendSuccess(response, "Notifications loaded.", notifs);
        }
    }

    // -------------------------------------------------------
    //  POST — mark read
    // -------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "STUDENT")) return;
        request.setCharacterEncoding("UTF-8");

        User   student = SessionUtil.getLoggedInUser(request);
        String action  = request.getParameter("action");

        if ("markRead".equals(action)) {
            int notifId = parseId(request.getParameter("notifId"));
            if (notifId <= 0) { JsonUtil.sendBadRequest(response, "Invalid notification ID."); return; }
            if (notificationDAO.markRead(notifId, student.getId())) {
                JsonUtil.sendSuccess(response, "Notification marked as read.");
            } else {
                JsonUtil.sendServerError(response, "Could not mark as read.");
            }

        } else if ("markAllRead".equals(action)) {
            notificationDAO.markAllRead(student.getId());
            JsonUtil.sendSuccess(response, "All notifications marked as read.");

        } else {
            JsonUtil.sendBadRequest(response, "Unknown action. Use 'markRead' or 'markAllRead'.");
        }
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}
