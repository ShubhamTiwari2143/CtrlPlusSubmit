package in.ctrlplussubmit.controller.admin;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserManagementServlet — Full CRUD for user management
 *
 * Access: ADMIN only
 *
 * GET  /admin/users              → list all users (JSON)
 * GET  /admin/users?role=STUDENT → filter by role
 * GET  /admin/users?search=name  → search by name/email
 * POST /admin/users              → create a new user
 *
 * POST /admin/users?action=update    → update profile fields
 * POST /admin/users?action=toggle    → activate / deactivate
 * POST /admin/users?action=resetPwd  → reset password
 * POST /admin/users?action=changeRole→ change role
 */
@WebServlet("/admin/users")
public class UserManagementServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    // -------------------------------------------------------
    //  GET — list / search / filter users
    // -------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "ADMIN")) return;

        String roleFilter = request.getParameter("role");
        String search     = request.getParameter("search");

        List<User> users;

        if (search != null && !search.isBlank()) {
            users = userDAO.search(search.trim());
        } else if (roleFilter != null && !roleFilter.isBlank()) {
            users = userDAO.findByRole(roleFilter.toUpperCase());
        } else {
            users = userDAO.findAll();
        }

        // Strip password hashes before sending to frontend
        List<Map<String, Object>> safeUsers = users.stream()
            .map(this::toSafeMap)
            .collect(Collectors.toList());

        JsonUtil.sendSuccess(response, "Users loaded.", safeUsers);
    }

    // -------------------------------------------------------
    //  POST — create user or perform action
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
                case "update"     -> handleUpdate(request, response);
                case "toggle"     -> handleToggleActive(request, response);
                case "resetPwd"   -> handleResetPassword(request, response);
                case "changeRole" -> handleChangeRole(request, response);
                default           -> JsonUtil.sendBadRequest(response, "Unknown action: " + action);
            }
        }
    }

    // -------------------------------------------------------
    //  CREATE — Admin creates a new user directly
    // -------------------------------------------------------
    private void handleCreate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String fullName = request.getParameter("fullName");
        String email    = request.getParameter("email");
        String password = request.getParameter("password");
        String role     = request.getParameter("role");

        // Validation
        if (isBlank(fullName)) { JsonUtil.sendBadRequest(response, "Full name is required."); return; }
        if (isBlank(email))    { JsonUtil.sendBadRequest(response, "Email is required."); return; }
        if (isBlank(password)) { JsonUtil.sendBadRequest(response, "Password is required."); return; }
        if (isBlank(role))     { JsonUtil.sendBadRequest(response, "Role is required."); return; }

        role = role.toUpperCase();
        if (!role.equals("ADMIN") && !role.equals("FACULTY") && !role.equals("STUDENT")) {
            JsonUtil.sendBadRequest(response, "Invalid role. Use ADMIN, FACULTY, or STUDENT.");
            return;
        }

        email = email.trim().toLowerCase();
        if (userDAO.emailExists(email)) {
            JsonUtil.sendBadRequest(response, "This email is already registered.");
            return;
        }

        User user = new User(fullName.trim(), email,
                             PasswordUtil.hash(password), role);
        if (userDAO.save(user)) {
            JsonUtil.sendSuccess(response, "User '" + user.getFullName() + "' created successfully.",
                                 toSafeMap(user));
        } else {
            JsonUtil.sendServerError(response, "Failed to create user. Please try again.");
        }
    }

    // -------------------------------------------------------
    //  UPDATE — Edit name and email
    // -------------------------------------------------------
    private void handleUpdate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int    userId   = parseId(request.getParameter("userId"));
        String fullName = request.getParameter("fullName");
        String email    = request.getParameter("email");

        if (userId <= 0)       { JsonUtil.sendBadRequest(response, "Invalid user ID."); return; }
        if (isBlank(fullName)) { JsonUtil.sendBadRequest(response, "Full name is required."); return; }
        if (isBlank(email))    { JsonUtil.sendBadRequest(response, "Email is required."); return; }

        User user = userDAO.findById(userId);
        if (user == null) { JsonUtil.sendNotFound(response, "User not found."); return; }

        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());

        if (userDAO.update(user)) {
            JsonUtil.sendSuccess(response, "User updated successfully.", toSafeMap(user));
        } else {
            JsonUtil.sendServerError(response, "Update failed.");
        }
    }

    // -------------------------------------------------------
    //  TOGGLE — Activate or deactivate a user
    // -------------------------------------------------------
    private void handleToggleActive(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int    userId   = parseId(request.getParameter("userId"));
        String isActive = request.getParameter("isActive"); // "true" or "false"

        if (userId <= 0) { JsonUtil.sendBadRequest(response, "Invalid user ID."); return; }

        // Prevent admin from deactivating themselves
        int loggedInId = SessionUtil.getLoggedInUser(request).getId();
        if (userId == loggedInId) {
            JsonUtil.sendBadRequest(response, "You cannot deactivate your own account.");
            return;
        }

        boolean activate = Boolean.parseBoolean(isActive);
        if (userDAO.setActiveStatus(userId, activate)) {
            String msg = activate ? "User activated." : "User deactivated.";
            JsonUtil.sendSuccess(response, msg);
        } else {
            JsonUtil.sendServerError(response, "Status update failed.");
        }
    }

    // -------------------------------------------------------
    //  RESET PASSWORD — Admin sets a new temporary password
    // -------------------------------------------------------
    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int    userId      = parseId(request.getParameter("userId"));
        String newPassword = request.getParameter("newPassword");

        if (userId <= 0)        { JsonUtil.sendBadRequest(response, "Invalid user ID."); return; }
        if (isBlank(newPassword)) { JsonUtil.sendBadRequest(response, "New password is required."); return; }
        if (newPassword.length() < 6) {
            JsonUtil.sendBadRequest(response, "Password must be at least 6 characters."); return;
        }

        String hash = PasswordUtil.hash(newPassword);
        if (userDAO.updatePassword(userId, hash)) {
            JsonUtil.sendSuccess(response, "Password reset successfully.");
        } else {
            JsonUtil.sendServerError(response, "Password reset failed.");
        }
    }

    // -------------------------------------------------------
    //  CHANGE ROLE — Admin reassigns user role
    // -------------------------------------------------------
    private void handleChangeRole(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int    userId  = parseId(request.getParameter("userId"));
        String newRole = request.getParameter("newRole");

        if (userId <= 0)     { JsonUtil.sendBadRequest(response, "Invalid user ID."); return; }
        if (isBlank(newRole)){ JsonUtil.sendBadRequest(response, "New role is required."); return; }

        newRole = newRole.toUpperCase();
        if (!newRole.equals("ADMIN") && !newRole.equals("FACULTY") && !newRole.equals("STUDENT")) {
            JsonUtil.sendBadRequest(response, "Invalid role."); return;
        }

        if (userDAO.updateRole(userId, newRole)) {
            JsonUtil.sendSuccess(response, "Role changed to " + newRole + ".");
        } else {
            JsonUtil.sendServerError(response, "Role change failed.");
        }
    }

    // -------------------------------------------------------
    //  HELPERS
    // -------------------------------------------------------

    /** Converts a User to a Map safe for JSON (no passwordHash). */
    private Map<String, Object> toSafeMap(User u) {
        return Map.of(
            "id",        u.getId(),
            "fullName",  u.getFullName(),
            "email",     u.getEmail(),
            "role",      u.getRole(),
            "isActive",  u.isActive(),
            "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        );
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private int parseId(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return -1; }
    }
}
