package in.ctrlplussubmit.controller.admin;


import in.ctrlplussubmit.dao.AccountRequestDAO;
import in.ctrlplussubmit.dao.UserDAO;
import in.ctrlplussubmit.model.AccountRequest;
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
 * AccountApprovalServlet — Admin reviews and approves account requests
 *
 * Access: ADMIN only
 *
 * GET  /admin/approve           → list all account requests (JSON)
 * GET  /admin/approve?status=PENDING → filter by status
 * POST /admin/approve?action=approve → approve a request (creates user)
 * POST /admin/approve?action=reject  → reject a request
 */
@WebServlet("/admin/approve")
public class AccountApprovalServlet extends HttpServlet {

    private final AccountRequestDAO requestDAO = new AccountRequestDAO();
    private final UserDAO           userDAO    = new UserDAO();

    // -------------------------------------------------------
    //  GET — list requests
    // -------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	//user not loggedin
        if (!SessionUtil.isLoggedIn(request)) {
        	JsonUtil.sendUnauthorized(response, "Login required!");
        	return;
        }
        //user is not admin
        if(!"ADMIN".equalsIgnoreCase(SessionUtil.getRole(request))) {
        	JsonUtil.sendForbidden(response, "You are not allowed to access this page!");
        	return;
        }

        String statusFilter = request.getParameter("status");
        List<AccountRequest> requests;

        if ("PENDING".equalsIgnoreCase(statusFilter)) {
            requests = requestDAO.getPending();
        } else {
            requests = requestDAO.findAll();
        }

        // Map to safe response objects
        List<Map<String, Object>> result = requests.stream()
            .map(req -> Map.of(
                "id",            (Object) req.getId(),
                "fullName",      req.getFullName(),
                "email",         req.getEmail(),
                "requestedRole", req.getRequestedRole(),
                "message",       req.getMessage() != null ? req.getMessage() : "",
                "status",        req.getStatus(),
                "requestedAt",   req.getRequestedAt() != null ? req.getRequestedAt().toString() : ""
            ))
            .collect(Collectors.toList());

        

        JsonUtil.sendSuccess(response, "Requests loaded.", result);
    }

    // -------------------------------------------------------
    //  POST — approve or reject
    // -------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "ADMIN")) return;
        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        if ("approve".equals(action)) {
            handleApprove(request, response);
        } else if ("reject".equals(action)) {
            handleReject(request, response);
        } else {
            JsonUtil.sendBadRequest(response, "Unknown action. Use 'approve' or 'reject'.");
        }
    }

    // -------------------------------------------------------
    //  APPROVE — create the user account, mark request approved
    // -------------------------------------------------------
    private void handleApprove(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int    requestId       = parseId(request.getParameter("requestId"));
        String tempPassword    = request.getParameter("tempPassword");

        if (requestId <= 0) { JsonUtil.sendBadRequest(response, "Invalid request ID."); return; }
        if (tempPassword == null || tempPassword.isBlank()) {
            JsonUtil.sendBadRequest(response, "A temporary password is required to approve."); return;
        }
        if (tempPassword.length() < 6) {
            JsonUtil.sendBadRequest(response, "Temporary password must be at least 6 characters."); return;
        }

        AccountRequest req = requestDAO.findById(requestId);
        if (req == null) { JsonUtil.sendNotFound(response, "Account request not found."); return; }

        if (!"PENDING".equals(req.getStatus())) {
            JsonUtil.sendBadRequest(response,
                "This request has already been " + req.getStatus().toLowerCase() + ".");
            return;
        }

        // Check email isn't already registered (edge case — concurrent approval)
        if (userDAO.emailExists(req.getEmail())) {
            JsonUtil.sendBadRequest(response,
                "A user with this email already exists. Request cannot be approved.");
            return;
        }

        // Create the user account
        User newUser = new User(
            req.getFullName(),
            req.getEmail(),
            PasswordUtil.hash(tempPassword),
            req.getRequestedRole()
        );

        int adminId = SessionUtil.getLoggedInUser(request).getId();

        if (userDAO.save(newUser)) {
            // Mark request as approved
            requestDAO.approve(requestId);
            JsonUtil.sendSuccess(response,
                "Account created for " + req.getFullName() + ". " +
                "They can now login with the temporary password.");
        } else {
            JsonUtil.sendServerError(response, "Failed to create user account. Please try again.");
        }
    }

    // -------------------------------------------------------
    //  REJECT — mark request as rejected
    // -------------------------------------------------------
    private void handleReject(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int requestId = parseId(request.getParameter("requestId"));

        if (requestId <= 0) { JsonUtil.sendBadRequest(response, "Invalid request ID."); return; }

        AccountRequest req = requestDAO.findById(requestId);
        if (req == null) { JsonUtil.sendNotFound(response, "Account request not found."); return; }

        if (!"PENDING".equals(req.getStatus())) {
            JsonUtil.sendBadRequest(response,
                "This request has already been " + req.getStatus().toLowerCase() + ".");
            return;
        }

        if (requestDAO.reject(requestId)) {
            JsonUtil.sendSuccess(response, "Request from " + req.getFullName() + " rejected.");
        } else {
            JsonUtil.sendServerError(response, "Rejection failed. Please try again.");
        }
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}

