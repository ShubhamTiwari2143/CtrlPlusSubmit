package in.ctrlplussubmit.controller.auth;


import in.ctrlplussubmit.dao.UserDAO;
import in.ctrlplussubmit.model.User;
import in.ctrlplussubmit.util.JsonUtil;
import in.ctrlplussubmit.util.PasswordUtil;
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
 * LoginServlet — Handles user authentication
 *
 * URL   : POST /auth/login
 * Body  : JSON  { "email": "...", "password": "..." }
 *
 * Flow:
 *   1. Read email + password from request body
 *   2. Look up user in DB by email
 *   3. Verify BCrypt password
 *   4. Check account is active
 *   5. Create HttpSession and store User object
 *   6. Return JSON with role + redirect URL
 *
 * GET /auth/login  → redirects to login.html (used for direct URL access)
 *
 * Response (success):
 *   { "success": true, "message": "Login successful",
 *     "data": { "role": "STUDENT", "redirectUrl": "/TaskFlow/student/dashboard" } }
 *
 * Response (failure):
 *   { "success": false, "message": "Invalid email or password.", "data": null }
 */
@WebServlet("/auth/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    // -------------------------------------------------------
    //  GET — redirect to login page if accessed via browser URL bar
    // -------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // If already logged in, redirect to their dashboard
        if (SessionUtil.isLoggedIn(request)) {
            redirectToDashboard(request, response, SessionUtil.getRole(request));
            return;
        }

        // Otherwise serve the login page
        response.sendRedirect(request.getContextPath() + "/views/auth/login.html");
    }

    // -------------------------------------------------------
    //  POST — process login credentials
    // -------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // --- 1. Read form parameters (sent as application/x-www-form-urlencoded by Fetch) ---
        String email    = request.getParameter("email");
        String password = request.getParameter("password");

        // --- 2. Basic input validation ---
        if (email == null || email.isBlank() ||
            password == null || password.isBlank()) {
            JsonUtil.sendBadRequest(response, "Email and password are required.");
            return;
        }

        email = email.trim().toLowerCase();

        // --- 3. Look up user by email ---
        User user = userDAO.findByEmail(email);

        if (user == null) {
            // Don't reveal whether email exists — generic message
            JsonUtil.sendUnauthorized(response, "Invalid email or password.");
            return;
        }

        // --- 4. Verify password using BCrypt ---
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            JsonUtil.sendUnauthorized(response, "Invalid email or password.");
            return;
        }

        // --- 5. Check if account is active ---
        if (!user.isActive()) {
            JsonUtil.sendUnauthorized(response,
                "Your account has been deactivated. Please contact the administrator.");
            return;
        }

        // --- 6. Create session ---
        SessionUtil.setLoggedInUser(request, user);

        // --- 7. Build redirect URL based on role ---
        String redirectUrl = getDashboardUrl(request.getContextPath(), user.getRole());

        // --- 8. Send success response ---
        Map<String, Object> data = new HashMap<>();
        data.put("role",        user.getRole());
        data.put("fullName",    user.getFullName());
        data.put("redirectUrl", redirectUrl);

        JsonUtil.sendSuccess(response, "Login successful. Welcome, " + user.getFullName() + "!", data);
    }

    // -------------------------------------------------------
    //  HELPERS
    // -------------------------------------------------------

    /**
     * Returns the dashboard URL for a given role.
     */
    private String getDashboardUrl(String contextPath, String role) {
        return switch (role) {
            case "ADMIN"   -> contextPath + "/views/admin/dashboard.html";
            case "FACULTY" -> contextPath + "/views/faculty/dashboard.html";
            case "STUDENT" -> contextPath + "/views/student/dashboard.html";
            default        -> contextPath + "/views/auth/login.html";
        };
    }

    /**
     * Redirects an already-logged-in user to their correct dashboard.
     */
    private void redirectToDashboard(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String role) throws IOException {
        response.sendRedirect(getDashboardUrl(request.getContextPath(), role));
    }
}
