package in.ctrlplussubmit.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import in.ctrlplussubmit.model.Task;
import in.ctrlplussubmit.util.DBConnection;

public class TaskDAO {

    // ================= CREATE =================
    public boolean create(Task task) {

        String sql = """
            INSERT INTO tasks
                (title, description, faculty_id, deadline,
                 attach_file_path, attach_file_name,
                 max_marks, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, 1)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setInt(3, task.getFacultyId());
            ps.setTimestamp(4, Timestamp.valueOf(task.getDeadline()));
            ps.setString(5, task.getAttachFilePath());
            ps.setString(6, task.getAttachFileName());
            ps.setInt(7, task.getMaxMarks());

            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) task.setId(keys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[TaskDAO] create error: " + e.getMessage());
        }

        return false;
    }

    // ================= FIND BY FACULTY =================
    public List<Task> findByFaculty(int facultyId) {

        List<Task> list = new ArrayList<>();

        String sql = """
            SELECT
                t.*,
                u.full_name AS faculty_name,

                COUNT(DISTINCT
                    CASE
                        WHEN ta.assignment_type = 'INDIVIDUAL' THEN ta.student_id
                        WHEN ta.assignment_type = 'BATCH' THEN bs.student_id
                    END
                ) AS total_assigned,

                COUNT(DISTINCT
                    CASE
                        WHEN s.status IN ('SUBMITTED','LATE','APPROVED','NEEDS_IMPROVEMENT','REJECTED')
                        THEN s.student_id
                    END
                ) AS total_submitted,

                COUNT(DISTINCT
                    CASE
                        WHEN s.status IN ('SUBMITTED','LATE')
                        THEN s.student_id
                    END
                ) AS pending_review

            FROM tasks t
            LEFT JOIN users u ON t.faculty_id = u.id
            LEFT JOIN task_assignments ta ON ta.task_id = t.id
            LEFT JOIN batch_students bs
                   ON ta.assignment_type = 'BATCH'
                  AND ta.batch_id = bs.batch_id
            LEFT JOIN submissions s
                   ON s.task_id = t.id
                  AND (
                        s.student_id = ta.student_id
                        OR s.student_id = bs.student_id
                  )

            WHERE t.faculty_id = ?
              AND t.is_active = 1

            GROUP BY t.id, u.full_name
            ORDER BY t.deadline ASC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, facultyId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs, true));
            }

        } catch (SQLException e) {
            System.err.println("[TaskDAO] findByFaculty error: " + e.getMessage());
        }

        return list;
    }

    // ================= FIND BY ID =================
    public Task findById(int taskId, int facultyId) {

        String sql = """
            SELECT
                t.*,
                u.full_name AS faculty_name,

                COUNT(DISTINCT
                    CASE
                        WHEN ta.assignment_type = 'INDIVIDUAL' THEN ta.student_id
                        WHEN ta.assignment_type = 'BATCH' THEN bs.student_id
                    END
                ) AS total_assigned,

                COUNT(DISTINCT
                    CASE
                        WHEN s.status IN ('SUBMITTED','LATE','APPROVED','NEEDS_IMPROVEMENT','REJECTED')
                        THEN s.student_id
                    END
                ) AS total_submitted,

                COUNT(DISTINCT
                    CASE
                        WHEN s.status IN ('SUBMITTED','LATE')
                        THEN s.student_id
                    END
                ) AS pending_review

            FROM tasks t
            LEFT JOIN users u ON t.faculty_id = u.id
            LEFT JOIN task_assignments ta ON ta.task_id = t.id
            LEFT JOIN batch_students bs
                   ON ta.assignment_type = 'BATCH'
                  AND ta.batch_id = bs.batch_id
            LEFT JOIN submissions s
                   ON s.task_id = t.id
                  AND (
                        s.student_id = ta.student_id
                        OR s.student_id = bs.student_id
                  )
            WHERE t.id = ? AND t.faculty_id = ?
            GROUP BY t.id, u.full_name
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setInt(2, facultyId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs, true);
            }

        } catch (SQLException e) {
            System.err.println("[TaskDAO] findById error: " + e.getMessage());
        }

        return null;
    }

    // ================= STUDENT VIEW =================
    public List<Task> findAssignedToStudent(int studentId) {

        List<Task> list = new ArrayList<>();

        String sql = """
            SELECT DISTINCT t.*,
                   u.full_name AS faculty_name,
                   s.status AS submission_status
            FROM tasks t
            JOIN users u ON t.faculty_id = u.id
            LEFT JOIN submissions s ON s.task_id = t.id AND s.student_id = ?
            WHERE t.is_active = 1
              AND (
                    EXISTS (
                        SELECT 1 FROM task_assignments ta
                        WHERE ta.task_id = t.id
                          AND ta.assignment_type = 'INDIVIDUAL'
                          AND ta.student_id = ?
                    )
                    OR
                    EXISTS (
                        SELECT 1 FROM task_assignments ta
                        JOIN batch_students bs ON bs.batch_id = ta.batch_id
                        WHERE ta.task_id = t.id
                          AND ta.assignment_type = 'BATCH'
                          AND bs.student_id = ?
                    )
              )
            ORDER BY t.deadline ASC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ps.setInt(3, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs, false));
            }

        } catch (SQLException e) {
            System.err.println("[TaskDAO] findAssignedToStudent error: " + e.getMessage());
        }

        return list;
    }

    // ================= UPDATE =================
    public boolean update(Task task) {

        String sql = """
            UPDATE tasks
            SET title = ?, description = ?, deadline = ?, max_marks = ?,
                attach_file_path = COALESCE(?, attach_file_path),
                attach_file_name = COALESCE(?, attach_file_name)
            WHERE id = ? AND faculty_id = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(task.getDeadline()));
            ps.setInt(4, task.getMaxMarks());
            ps.setString(5, task.getAttachFilePath());
            ps.setString(6, task.getAttachFileName());
            ps.setInt(7, task.getId());
            ps.setInt(8, task.getFacultyId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[TaskDAO] update error: " + e.getMessage());
        }

        return false;
    }

    // ================= DELETE =================
    public boolean softDelete(int taskId, int facultyId) {

        String sql = "UPDATE tasks SET is_active = 0 WHERE id = ? AND faculty_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setInt(2, facultyId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[TaskDAO] softDelete error: " + e.getMessage());
        }

        return false;
    }

    // ================= STATS =================
    public int countByFaculty(int facultyId) {

        String sql = "SELECT COUNT(*) FROM tasks WHERE faculty_id = ? AND is_active = 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, facultyId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("[TaskDAO] countByFaculty error: " + e.getMessage());
        }

        return 0;
    }

    public int countPendingReview(int facultyId) {

        String sql = """
            SELECT COUNT(*) FROM submissions s
            JOIN tasks t ON s.task_id = t.id
            WHERE t.faculty_id = ? AND s.status IN ('SUBMITTED','LATE')
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, facultyId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("[TaskDAO] countPendingReview error: " + e.getMessage());
        }

        return 0;
    }

    public int countReviewed(int facultyId) {

        String sql = """
            SELECT COUNT(*) FROM submissions s
            JOIN tasks t ON s.task_id = t.id
            WHERE t.faculty_id = ?
            AND s.status IN ('REVIEWED','APPROVED','NEEDS_IMPROVEMENT','REJECTED')
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, facultyId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("[TaskDAO] countReviewed error: " + e.getMessage());
        }

        return 0;
    }

    // ================= MAPPER =================
    private Task mapRow(ResultSet rs, boolean includeStats) throws SQLException {

        Task t = new Task();

        t.setId(rs.getInt("id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setFacultyId(rs.getInt("faculty_id"));
        t.setMaxMarks(rs.getInt("max_marks"));
        t.setActive(rs.getBoolean("is_active"));
        t.setAttachFilePath(rs.getString("attach_file_path"));
        t.setAttachFileName(rs.getString("attach_file_name"));

        Timestamp deadline = rs.getTimestamp("deadline");
        if (deadline != null) t.setDeadline(deadline.toLocalDateTime());

        // FIX: Extract faculty_name securely!
        try {
            t.setFacultyName(rs.getString("faculty_name"));
        } catch (SQLException ignored) {
            // Column might not exist in some simple queries, so we ignore safely
        }

        if (includeStats) {
            try { t.setTotalAssigned(rs.getInt("total_assigned")); } catch (Exception ignored) {}
            try { t.setTotalSubmitted(rs.getInt("total_submitted")); } catch (Exception ignored) {}
            try { t.setPendingReview(rs.getInt("pending_review")); } catch (Exception ignored) {}
        }

        return t;
    }
}