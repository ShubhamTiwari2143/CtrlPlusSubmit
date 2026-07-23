package in.ctrlplussubmit.util;

import in.ctrlplussubmit.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * SessionUtil — HTTP Session Manager & Role Guard
 *
 * Centralizes all session operations:
 *   - Store and retrieve the logged-in User
 *   - Check authentication status
 *   - Enforce role-based access control (RBAC)
 *   - Redirect unauthorized users cleanly
 *
 * Usage in any Servlet's doGet / doPost:
 *
 *   // Guard: only ADMIN can proceed
 *   if (!SessionUtil.requireRole(request, response, "ADMIN")) return;
 *
 *   // Get the logged-in user object
 *   User user = SessionUtil.getLoggedInUser(request);
 */
public class SessionUtil {

    // Session attribute key — the logged-in User object is stored under this key
    public static final String SESSION_USER_KEY = "loggedInUser";

    // How long a session lives without any activity (seconds)
    // -1 = session lives until browser closes / explicit logout
    public static final int SESSION_MAX_INACTIVE = -1;

    // Private constructor — static utility class
    private SessionUtil() {}

    // -------------------------------------------------------
    //  STORE — called after successful login
    // -------------------------------------------------------

    /**
     * Saves the authenticated User into the session.
     * Creates a new session (invalidates any existing one) to prevent
     * session fixation attacks.
     *
     * @param request   HttpServletRequest
     * @param user      the fully populated User object from the DB
     */
    public static void setLoggedInUser(HttpServletRequest request, User user) {
        // Invalidate old session to prevent session fixation
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        // Create a fresh session
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(SESSION_MAX_INACTIVE);
        session.setAttribute(SESSION_USER_KEY, user);
    }

    // -------------------------------------------------------
    //  RETRIEVE — use anywhere after login
    // -------------------------------------------------------

    /**
     * Returns the logged-in User from the session, or null if not logged in.
     *
     * @param request   HttpServletRequest
     * @return          User object, or null
     */
    public static User getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        return (User) session.getAttribute(SESSION_USER_KEY);
    }

    /**
     * Returns true if there is an active session with a logged-in user.
     *
     * @param request   HttpServletRequest
     * @return          true if authenticated
     */
    public static boolean isLoggedIn(HttpServletRequest request) {
        return getLoggedInUser(request) != null;
    }

    // -------------------------------------------------------
    //  ROLE CHECKS — convenience helpers
    // -------------------------------------------------------

    /**
     * Returns the role string of the logged-in user ("ADMIN", "FACULTY", "STUDENT"),
     * or null if no session exists.
     */
    public static String getRole(HttpServletRequest request) {
        User user = getLoggedInUser(request);
        return (user != null) ? user.getRole() : null;
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return "ADMIN".equals(getRole(request));
    }

    public static boolean isFaculty(HttpServletRequest request) {
        return "FACULTY".equals(getRole(request));
    }

    public static boolean isStudent(HttpServletRequest request) {
        return "STUDENT".equals(getRole(request));
    }

    // -------------------------------------------------------
    //  GUARDS — place at the top of every servlet handler
    // -------------------------------------------------------

    /**
     * Ensures the user is logged in. If not, redirects to login page.
     *
     * @return true if authenticated; false if redirect was issued (caller must return)
     */
    public static boolean requireLogin(HttpServletRequest request,
                                       HttpServletResponse response) throws IOException {
        if (!isLoggedIn(request)) {
            response.sendRedirect(request.getContextPath() + "/views/auth/login.html");
            return false;
        }
        return true;
    }

    /**
     * Ensures the user is logged in AND has the required role.
     * Redirects to login if not authenticated, or to 403 page if wrong role.
     *
     * @param requiredRole  "ADMIN", "FACULTY", or "STUDENT"
     * @return true if access is granted; false if redirect was issued
     */
    public static boolean requireRole(HttpServletRequest request,
                                      HttpServletResponse response,
                                      String requiredRole) throws IOException {
        if (!isLoggedIn(request)) {
            response.sendRedirect(request.getContextPath() + "/views/auth/login.html");
            return false;
        }

        if (!requiredRole.equals(getRole(request))) {
            response.sendRedirect(request.getContextPath() + "/error/403.html");
            return false;
        }

        return true;
    }

    /**
     * Ensures the user has ANY of the allowed roles (OR logic).
     * Useful for pages accessible by both ADMIN and FACULTY.
     *
     * @param allowedRoles  varargs of allowed roles e.g. "ADMIN", "FACULTY"
     * @return true if access is granted
     */
    public static boolean requireAnyRole(HttpServletRequest request,
                                         HttpServletResponse response,
                                         String... allowedRoles) throws IOException {
        if (!isLoggedIn(request)) {
            response.sendRedirect(request.getContextPath() + "/views/auth/login.html");
            return false;
        }

        String userRole = getRole(request);
        for (String role : allowedRoles) {
            if (role.equals(userRole)) return true;
        }

        response.sendRedirect(request.getContextPath() + "/error/403.html");
        return false;
    }

    // -------------------------------------------------------
    //  LOGOUT — call from LogoutServlet
    // -------------------------------------------------------

    /**
     * Invalidates the session completely, clearing all stored data.
     */
    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}

