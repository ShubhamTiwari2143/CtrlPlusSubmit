package in.ctrlplussubmit.dao;

import in.ctrlplussubmit.model.AccountRequest;
import in.ctrlplussubmit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountRequestDAO {

    // -------------------------------------------------------
    // CREATE
    // -------------------------------------------------------

    public boolean save(AccountRequest req) {
        String sql = "INSERT INTO account_requests " +
                "(full_name, email, requested_role, message, status) " +
                "VALUES (?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, req.getFullName());
            ps.setString(2, req.getEmail());
            ps.setString(3, req.getRequestedRole());
            ps.setString(4, req.getMessage());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) req.setId(keys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[AccountRequestDAO] save error: " + e.getMessage());
        }

        return false;
    }

    // -------------------------------------------------------
    // READ
    // -------------------------------------------------------

    public List<AccountRequest> getPending() {
        return findByStatus("PENDING");
    }

    public List<AccountRequest> findAll() {
        List<AccountRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM account_requests ORDER BY requested_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("[AccountRequestDAO] findAll error: " + e.getMessage());
        }

        return list;
    }

    public AccountRequest findById(int id) {
        String sql = "SELECT * FROM account_requests WHERE id = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }

        } catch (SQLException e) {
            System.err.println("[AccountRequestDAO] findById error: " + e.getMessage());
        }

        return null;
    }

    // 🔥 Updated (as you wanted)
    public AccountRequest findByEmail(String email) {
        String sql = "SELECT * FROM account_requests WHERE email = ? ORDER BY requested_at DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }

        } catch (SQLException e) {
            System.err.println("[AccountRequestDAO] findByEmail error: " + e.getMessage());
        }

        return null;
    }

    public boolean pendingExistsForEmail(String email) {
        String sql = "SELECT COUNT(*) FROM account_requests WHERE email = ? AND status = 'PENDING'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("[AccountRequestDAO] pendingExistsForEmail error: " + e.getMessage());
        }

        return false;
    }

    // -------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------

    public boolean approve(int requestId) {
        return updateStatus(requestId, "APPROVED");
    }

    public boolean reject(int requestId) {
        return updateStatus(requestId, "REJECTED");
    }

    public int countPending() {
        String sql = "SELECT COUNT(*) FROM account_requests WHERE status = 'PENDING'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.err.println("[AccountRequestDAO] countPending error: " + e.getMessage());
        }

        return 0;
    }

    // -------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------

    private List<AccountRequest> findByStatus(String status) {
        List<AccountRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM account_requests WHERE status = ? ORDER BY requested_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AccountRequestDAO] findByStatus error: " + e.getMessage());
        }

        return list;
    }

    private boolean updateStatus(int requestId, String status) {
        String sql = "UPDATE account_requests SET status = ?, reviewed_at = NOW() WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, requestId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AccountRequestDAO] updateStatus error: " + e.getMessage());
        }

        return false;
    }

    private AccountRequest mapRow(ResultSet rs) throws SQLException {
        AccountRequest req = new AccountRequest();

        req.setId(rs.getInt("id"));
        req.setFullName(rs.getString("full_name"));
        req.setEmail(rs.getString("email"));
        req.setRequestedRole(rs.getString("requested_role"));
        req.setMessage(rs.getString("message"));
        req.setStatus(rs.getString("status"));

        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        if (reviewedAt != null) req.setReviewedAt(reviewedAt.toLocalDateTime());

        Timestamp requestedAt = rs.getTimestamp("requested_at");
        if (requestedAt != null) req.setRequestedAt(requestedAt.toLocalDateTime());

        return req;
    }
}