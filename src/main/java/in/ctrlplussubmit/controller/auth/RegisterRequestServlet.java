package in.ctrlplussubmit.controller.auth;

import in.ctrlplussubmit.dao.AccountRequestDAO;
import in.ctrlplussubmit.dao.UserDAO;
import in.ctrlplussubmit.model.AccountRequest;
import in.ctrlplussubmit.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * AccountRequestServlet — Handles new account requests from the public form
 *
 * URL  : POST /auth/request
 *        GET  /auth/request  → serves the request-account.html page
 *
 * This is a PUBLIC endpoint — no session required.
 * Anyone can submit a request; Admin reviews and approves it.
 *
 * Validation:
 *   - All required fields present
 *   - Valid role (FACULTY or STUDENT only — ADMIN cannot be self-requested)
 *   - Email not already registered as a user
 *   - No pending request already exists for this email
 *
 * Response (success):
 *   { "success": true, "message": "Request submitted. Admin will review it soon.", "data": null }
 *
 * Response (failure):
 *   { "success": false, "message": "...", "data": null }
 */

@WebServlet("/auth/request")
public class RegisterRequestServlet extends HttpServlet {

    private final AccountRequestDAO requestDAO = new AccountRequestDAO();
    private final UserDAO           userDAO    = new UserDAO();

    // -------------------------------------------------------
    //  GET — serve the account request page
    // -------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() +
                              "/views/auth/request-account.html");
    }

    // -------------------------------------------------------
    //  POST — process the account request form submission
    // -------------------------------------------------------
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // --- 1. Read parameters ---
        String fullName      = request.getParameter("fullName");
        String email         = request.getParameter("email");
        String requestedRole = request.getParameter("requestedRole");
        String message       = request.getParameter("message"); // optional

        // --- 2. Required field validation ---
        if (fullName == null || fullName.isBlank()) {
            JsonUtil.sendBadRequest(response, "Full name is required.");
            return;
        }
        if (email == null || email.isBlank()) {
            JsonUtil.sendBadRequest(response, "Email address is required.");
            return;
        }
        if (requestedRole == null || requestedRole.isBlank()) {
            JsonUtil.sendBadRequest(response, "Please select a role (Faculty or Student).");
            return;
        }

        // Normalize inputs
        fullName      = fullName.trim();
        email         = email.trim().toLowerCase();
        requestedRole = requestedRole.trim().toUpperCase();

        // --- 3. Role validation — ADMIN accounts cannot be self-requested ---
        if (!requestedRole.equals("FACULTY") && !requestedRole.equals("STUDENT")) {
            JsonUtil.sendBadRequest(response,
                "Invalid role selected. Only Faculty or Student accounts can be requested.");
            return;
        }

        // --- 4. Email format validation (basic) ---
        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            JsonUtil.sendBadRequest(response, "Please enter a valid email address.");
            return;
        }

        // --- 5. Check if email is already a registered user ---
        if (userDAO.emailExists(email)) {
            JsonUtil.sendBadRequest(response,
                "This email is already registered. Please use the login page.");
            return;
        }

        // --- 6. Check if a pending request already exists for this email ---
        if (requestDAO.pendingExistsForEmail(email)) {
            JsonUtil.sendBadRequest(response,
                "A pending request already exists for this email. " +
                "Please wait for the Admin to review it.");
            return;
        }

        // --- 7. Save the request ---
        AccountRequest req = new AccountRequest(
            fullName,
            email,
            requestedRole,
            (message != null && !message.isBlank()) ? message.trim() : null
        );

        boolean saved = requestDAO.save(req);

        if (saved) {
            JsonUtil.sendSuccess(response,
                "Your request has been submitted successfully! " +
                "The administrator will review it and create your account.");
        } else {
            JsonUtil.sendServerError(response,
                "Something went wrong while submitting your request. Please try again.");
        }
    }
}

