package in.ctrlplussubmit.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import in.ctrlplussubmit.model.Batch;
import in.ctrlplussubmit.model.User;
import in.ctrlplussubmit.util.DBConnection;

public class BatchDAO {

    // ================= CREATE =================
    public boolean create(Batch batch) {
        String sql = "INSERT INTO batches (batch_name, description, faculty_id) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, batch.getBatchName());
            ps.setString(2, batch.getDescription());
            ps.setInt(3, batch.getFacultyId());

            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) batch.setId(keys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[BatchDAO] create error: " + e.getMessage());
        }
        return false;
    }

    // ================= UPDATE =================
    public boolean update(Batch batch) {
        String sql = "UPDATE batches SET batch_name = ?, description = ?, faculty_id = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, batch.getBatchName());
            ps.setString(2, batch.getDescription());
            ps.setInt(3, batch.getFacultyId());
            ps.setInt(4, batch.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BatchDAO] update error: " + e.getMessage());
        }
        return false;
    }

    
    // ================= SET ACTIVE =================
    public boolean setActive(int batchId, boolean isActive) {
        String sql = "UPDATE batches SET is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isActive);
            ps.setInt(2, batchId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BatchDAO] setActive error: " + e.getMessage());
        }
        return false;
    }
    
    public List<User> getStudentsInBatch(int batchId) {

        List<User> students = new ArrayList<>();

        String sql = """
            SELECT u.*
            FROM batch_students bs
            JOIN users u ON bs.student_id = u.id
            WHERE bs.batch_id = ?
              AND u.role = 'STUDENT'
              AND u.is_active = 1
            ORDER BY u.full_name ASC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, batchId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    User user = new User();

                    user.setId(rs.getInt("id"));
                    user.setFullName(rs.getString("full_name"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setActive(rs.getBoolean("is_active"));
                    user.setProfilePic(rs.getString("profile_picture"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        user.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        user.setUpdatedAt(updatedAt.toLocalDateTime());
                    }

                    students.add(user);
                }
            }

        } catch (SQLException e) {
            System.err.println("[BatchDAO] getStudentsInBatch error: " + e.getMessage());
        }

        return students;
    }
    
 // ================= COUNT ALL =================
    public int countAll() {

        String sql = "SELECT COUNT(*) FROM batches";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.err.println("[BatchDAO] countAll error: " + e.getMessage());
        }

        return 0;
    }

    // ================= COUNT ACTIVE =================
    public int countActive() {

        String sql = "SELECT COUNT(*) FROM batches WHERE is_active = 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.err.println("[BatchDAO] countActive error: " + e.getMessage());
        }

        return 0;
    }

    // ================= FIND ALL =================
    public List<Batch> findAll() {
        List<Batch> list = new ArrayList<>();

        String sql = """
            SELECT b.*, u.full_name AS faculty_name,
                   COUNT(DISTINCT bs.student_id) AS student_count
            FROM batches b
            LEFT JOIN users u ON b.faculty_id = u.id
            LEFT JOIN batch_students bs ON b.id = bs.batch_id
            GROUP BY b.id, u.full_name
            ORDER BY b.created_at DESC
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("[BatchDAO] findAll error: " + e.getMessage());
        }

        return list;
    }

    // ================= FIND BY ID =================
    public Batch findById(int id) {

        String sql = """
            SELECT b.*, u.full_name AS faculty_name,
                   COUNT(DISTINCT bs.student_id) AS student_count
            FROM batches b
            LEFT JOIN users u ON b.faculty_id = u.id
            LEFT JOIN batch_students bs ON b.id = bs.batch_id
            WHERE b.id = ?
            GROUP BY b.id, u.full_name
            LIMIT 1
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }

        } catch (SQLException e) {
            System.err.println("[BatchDAO] findById error: " + e.getMessage());
        }

        return null;
    }

    // ================= FIND BY FACULTY =================
    public List<Batch> findByFaculty(int facultyId) {

        List<Batch> list = new ArrayList<>();

        String sql = """
            SELECT b.*, u.full_name AS faculty_name,
                   COUNT(DISTINCT bs.student_id) AS student_count
            FROM batches b
            LEFT JOIN users u ON b.faculty_id = u.id
            LEFT JOIN batch_students bs ON b.id = bs.batch_id
            WHERE b.faculty_id = ?
            GROUP BY b.id, u.full_name
            ORDER BY b.created_at DESC
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, facultyId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[BatchDAO] findByFaculty error: " + e.getMessage());
        }

        return list;
    }

    // ================= FIND BY FACULTY WITH STATS =================
    public List<Batch> findByFacultyWithStats(int facultyId) {

        List<Batch> list = new ArrayList<>();

        String sql = """
            SELECT b.id, b.batch_name, b.description, b.faculty_id,
                   b.is_active, b.created_at,
                   u.full_name AS faculty_name,
                   COUNT(DISTINCT bs.student_id) AS student_count,

                   COUNT(DISTINCT CASE
                        WHEN t.is_active = 1 THEN ta.id END) AS active_tasks,

                   COUNT(DISTINCT CASE
                        WHEN t.is_active = 1
                         AND t.deadline >= NOW()
                         AND t.deadline <= DATE_ADD(NOW(), INTERVAL 7 DAY)
                        THEN t.id END) AS upcoming_count

            FROM batches b
            LEFT JOIN users u ON b.faculty_id = u.id
            LEFT JOIN batch_students bs ON bs.batch_id = b.id
            LEFT JOIN task_assignments ta
                   ON ta.batch_id = b.id AND ta.assignment_type = 'BATCH'
            LEFT JOIN tasks t ON ta.task_id = t.id

            WHERE b.faculty_id = ?

            GROUP BY b.id, b.batch_name, b.description,
                     b.faculty_id, b.is_active, b.created_at, u.full_name

            ORDER BY b.is_active DESC, b.created_at DESC
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, facultyId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Batch b = mapRow(rs);
                    b.setActiveTasks(rs.getInt("active_tasks"));
                    b.setUpcomingCount(rs.getInt("upcoming_count"));
                    list.add(b);
                }
            }

        } catch (SQLException e) {
            System.err.println("[BatchDAO] findByFacultyWithStats error: " + e.getMessage());
        }

        return list;
    }

    // ================= STUDENTS =================
    public boolean addStudent(int batchId, int studentId) {

        String sql = "INSERT IGNORE INTO batch_students (batch_id, student_id) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, batchId);
            ps.setInt(2, studentId);

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("[BatchDAO] addStudent error: " + e.getMessage());
        }

        return false;
    }

    public boolean removeStudent(int batchId, int studentId) {

        String sql = "DELETE FROM batch_students WHERE batch_id = ? AND student_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, batchId);
            ps.setInt(2, studentId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BatchDAO] removeStudent error: " + e.getMessage());
        }

        return false;
    }

    public List<Integer> getStudentIds(int batchId) {

        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT student_id FROM batch_students WHERE batch_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, batchId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("student_id"));
            }

        } catch (SQLException e) {
            System.err.println("[BatchDAO] getStudentIds error: " + e.getMessage());
        }

        return ids;
    }

    // ================= MAPPER =================
    private Batch mapRow(ResultSet rs) throws SQLException {

        Batch b = new Batch();

        b.setId(rs.getInt("id"));
        b.setBatchName(rs.getString("batch_name"));
        b.setDescription(rs.getString("description"));
        b.setFacultyId(rs.getInt("faculty_id"));
        b.setActive(rs.getBoolean("is_active"));

        try { b.setFacultyName(rs.getString("faculty_name")); } catch (Exception ignored) {}
        try { b.setStudentCount(rs.getInt("student_count")); } catch (Exception ignored) {}

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) b.setCreatedAt(createdAt.toLocalDateTime());

        return b;
    }
    
    public boolean isStudentInBatch(int batchId, int studentId) {

        String sql = """
            SELECT COUNT(*)
            FROM batch_students
            WHERE batch_id = ? AND student_id = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, batchId);
            ps.setInt(2, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("[BatchDAO] isStudentInBatch error: " + e.getMessage());
        }

        return false;
    }
}