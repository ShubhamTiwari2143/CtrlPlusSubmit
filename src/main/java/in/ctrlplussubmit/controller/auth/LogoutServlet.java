package in.ctrlplussubmit.controller.auth;

import in.ctrlplussubmit.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * LogoutServlet — Handles user logout
 *
 * URL : GET  /auth/logout
 *       POST /auth/logout  (both supported)
 *
 * Flow:
 *   1. Invalidate the HttpSession (clears all stored user data)
 *   2. Redirect to login page with a ?logout=true query param
 *      (login.html reads this param and shows a "Logged out" toast)
 *
 * Called from the logout button in all dashboards:
 *   <a href="/TaskFlow/auth/logout">Logout</a>
 *   or via Fetch: fetch('/TaskFlow/auth/logout', { method: 'POST' })
 */
@WebServlet("/auth/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    // -------------------------------------------------------
    //  Core logout logic
    // -------------------------------------------------------

    private void processLogout(HttpServletRequest request,
                                HttpServletResponse response) throws IOException {

        // Invalidate the session (removes User object + all session data)
        SessionUtil.logout(request);

        // Prevent browser from caching the logged-in pages after logout
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        // Redirect to login page — ?logout=true tells login.html to show success toast
        response.sendRedirect(request.getContextPath() +
                              "/views/auth/login.html?logout=true");
    }
}
