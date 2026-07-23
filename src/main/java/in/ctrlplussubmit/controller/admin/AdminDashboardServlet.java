package in.ctrlplussubmit.controller.admin;

import in.ctrlplussubmit.dao.AccountRequestDAO;
import in.ctrlplussubmit.dao.BatchDAO;
import in.ctrlplussubmit.dao.UserDAO;
import in.ctrlplussubmit.util.JsonUtil;
import in.ctrlplussubmit.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * AdminDashboardServlet — Serves Admin dashboard statistics
 *
 * GET /admin/dashboard
 *   → Returns JSON stats: total users by role, batches, pending requests
 *   → Used by dashboard.html to populate the stat cards via Fetch
 *
 * Access: ADMIN only
 */
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private final UserDAO           userDAO    = new UserDAO();
    private final BatchDAO          batchDAO   = new BatchDAO();
    private final AccountRequestDAO requestDAO = new AccountRequestDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Guard: ADMIN only
        if (!SessionUtil.requireRole(request, response, "ADMIN")) return;

        // Gather stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents",      userDAO.countByRole("STUDENT"));
        stats.put("totalFaculty",       userDAO.countByRole("FACULTY"));
        stats.put("totalAdmins",        userDAO.countByRole("ADMIN"));
        stats.put("totalUsers",         userDAO.countByRole("STUDENT")
                                      + userDAO.countByRole("FACULTY")
                                      + userDAO.countByRole("ADMIN"));
        stats.put("totalBatches",       batchDAO.countAll());
        stats.put("activeBatches",      batchDAO.countActive());
        stats.put("pendingRequests",    requestDAO.countPending());

        // Logged-in admin info
        Map<String, Object> adminInfo = new HashMap<>();
        adminInfo.put("fullName", SessionUtil.getLoggedInUser(request).getFullName());
        stats.put("admin", adminInfo);

        JsonUtil.sendSuccess(response, "Dashboard stats loaded.", stats);
    }
}
