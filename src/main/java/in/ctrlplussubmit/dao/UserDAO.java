package in.ctrlplussubmit.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import in.ctrlplussubmit.model.User;
import in.ctrlplussubmit.util.DBConnection;

public class UserDAO {

    // ================= FIND BY EMAIL =================
    public User findByEmail(String email) {

        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }

        } catch (SQLException e) {
        	e.printStackTrace();
            System.err.println("[UserDAO] findByEmail error: " + e.getMessage());
            
        }

        return null;
    }

    // ================= FIND BY ID =================
    public User findById(int id) {

        String sql = "SELECT * FROM users WHERE id = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }

        } catch (SQLException e) {
        	e.printStackTrace();
            System.err.println("[UserDAO] findById error: " + e.getMessage());
        }

        return null;
    }

    // ================= FIND ALL =================
    public List<User> findAll() {

        List<User> users = new ArrayList<>();

        String sql = "SELECT * FROM users ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) users.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("[UserDAO] findAll error: " + e.getMessage());
        }

        return users;
    }

    // ================= FIND BY ROLE =================
    public List<User> findByRole(String role) {

        List<User> users = new ArrayList<>();

        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] findByRole error: " + e.getMessage());
        }

        return users;
    }

    // ================= SEARCH =================
    public List<User> search(String keyword) {

        List<User> users = new ArrayList<>();

        String sql = """
            SELECT * FROM users
            WHERE full_name LIKE ? OR email LIKE ?
            ORDER BY full_name ASC
            """;

        String pattern = "%" + keyword + "%";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] search error: " + e.getMessage());
        }

        return users;
    }

    // ================= EMAIL EXISTS =================
    public boolean emailExists(String email) {

        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] emailExists error: " + e.getMessage());
        }

        return false;
    }

    // ================= SAVE =================
    public boolean save(User user) {

        String sql = """
            INSERT INTO users (full_name, email, password, role, is_active)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.setBoolean(5, user.isActive());

            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) user.setId(keys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] save error: " + e.getMessage());
        }

        return false;
    }

    // ================= UPDATE =================
    public boolean update(User user) {

        String sql = """
            UPDATE users
            SET full_name = ?, email = ?, profile_picture = ?
            WHERE id = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getProfilePic());
            ps.setInt(4, user.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] update error: " + e.getMessage());
        }

        return false;
    }

    // ================= UPDATE PASSWORD =================
    public boolean updatePassword(int userId, String newPasswordHash) {

        String sql = "UPDATE users SET password = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] updatePassword error: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ================= UPDATE ROLE =================
    public boolean updateRole(int userId, String newRole) {

        String sql = "UPDATE users SET role = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newRole);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] updateRole error: " + e.getMessage());
        }

        return false;
    }

    // ================= SET ACTIVE =================
    public boolean setActiveStatus(int userId, boolean isActive) {

        String sql = "UPDATE users SET is_active = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isActive);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] setActiveStatus error: " + e.getMessage());
        }

        return false;
    }

    // ================= COUNT =================
    public int countByRole(String role) {

        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] countByRole error: " + e.getMessage());
        }

        return 0;
    }

    // ================= MAPPER =================
    private User mapRow(ResultSet rs) throws SQLException {

        User user = new User();

        user.setId(rs.getInt("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));
        user.setProfilePic(rs.getString("profile_picture"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) user.setUpdatedAt(updatedAt.toLocalDateTime());

        return user;
    }
}