package in.ctrlplussubmit.dao;

import in.ctrlplussubmit.model.Submission;
import in.ctrlplussubmit.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SubmissionDAO {

    // =====================================================
    // READ — Faculty views
    // =====================================================

    public List<Submission> findByTask(int taskId) {

        List<Submission> list = new ArrayList<>();

        String sql = """
            SELECT s.*,
                   u.full_name  AS student_name,
                   u.email      AS student_email,
                   t.title      AS task_title,
                   t.deadline   AS task_deadline,
                   t.max_marks  AS max_marks
            FROM submissions s
            JOIN users u ON s.student_id = u.id
            JOIN tasks t ON s.task_id    = t.id
            WHERE s.task_id = ?
            ORDER BY s.submitted_at DESC, u.full_name ASC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] findByTask error: " + e.getMessage());
        }

        return list;
    }

    public List<Submission> findByFaculty(int facultyId, String statusFilter) {

        List<Submission> list = new ArrayList<>();

        String sql = """
            SELECT s.*,
                   u.full_name  AS student_name,
                   u.email      AS student_email,
                   t.title      AS task_title,
                   t.deadline   AS task_deadline,
                   t.max_marks  AS max_marks
            FROM submissions s
            JOIN users u ON s.student_id = u.id
            JOIN tasks t ON s.task_id    = t.id
            WHERE t.faculty_id = ?
            """ + (statusFilter != null ? " AND s.status = ?" : "") + """
            ORDER BY s.submitted_at DESC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, facultyId);

            if (statusFilter != null) {
                ps.setString(2, statusFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] findByFaculty error: " + e.getMessage());
        }

        return list;
    }

    // =====================================================
    // READ — Student views
    // =====================================================

    public List<Submission> findByStudent(int studentId) {

        List<Submission> list = new ArrayList<>();

        String sql = """
            SELECT s.*,
                   u.full_name AS student_name,
                   u.email     AS student_email,
                   t.title     AS task_title,
                   t.deadline  AS task_deadline,
                   t.max_marks AS max_marks
            FROM submissions s
            JOIN users u ON s.student_id = u.id
            JOIN tasks t ON s.task_id    = t.id
            WHERE s.student_id = ?
            ORDER BY s.updated_at DESC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] findByStudent error: " + e.getMessage());
        }

        return list;
    }

    public Submission findByTaskAndStudent(int taskId, int studentId) {

        String sql = """
            SELECT s.*,
                   u.full_name AS student_name,
                   u.email     AS student_email,
                   t.title     AS task_title,
                   t.deadline  AS task_deadline,
                   t.max_marks AS max_marks
            FROM submissions s
            JOIN users u ON s.student_id = u.id
            JOIN tasks t ON s.task_id    = t.id
            WHERE s.task_id = ? AND s.student_id = ?
            LIMIT 1
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setInt(2, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }

        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] findByTaskAndStudent error: " + e.getMessage());
        }

        return null;
    }

    // =====================================================
    // SUBMIT / RESUBMIT
    // =====================================================

    public boolean submit(Submission submission, LocalDateTime taskDeadline) {

        boolean isLate = LocalDateTime.now().isAfter(taskDeadline);
        String status = isLate ? "LATE" : "SUBMITTED";

        String sql = """
            UPDATE submissions
            SET file_path       = ?,
                file_name       = ?,
                file_size_kb    = ?,
                comments        = ?,
                status          = ?,
                submitted_at    = NOW()
            WHERE task_id = ? AND student_id = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, submission.getFilePath());
            ps.setString(2, submission.getFileName());
            ps.setInt(3, submission.getFileSizeKB());
            ps.setString(4, submission.getComments());
            ps.setString(5, status);
            ps.setInt(6, submission.getTaskId());
            ps.setInt(7, submission.getStudentId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] submit error: " + e.getMessage());
        }

        return false;
    }

    public boolean resubmit(Submission submission, LocalDateTime taskDeadline) {

        boolean isLate = LocalDateTime.now().isAfter(taskDeadline);
        String status = isLate ? "LATE" : "SUBMITTED";

        String sql = """
            UPDATE submissions
            SET file_path       = ?,
                file_name       = ?,
                file_size_kb    = ?,
                comments        = ?,
                status          = ?,
                submitted_at    = NOW(),
                resubmit_count  = resubmit_count + 1,
                marks           = NULL,
                faculty_remarks = NULL,
                reviewed_at     = NULL
            WHERE task_id = ? AND student_id = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, submission.getFilePath());
            ps.setString(2, submission.getFileName());
            ps.setInt(3, submission.getFileSizeKB());
            ps.setString(4, submission.getComments());
            ps.setString(5, status);
            ps.setInt(6, submission.getTaskId());
            ps.setInt(7, submission.getStudentId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] resubmit error: " + e.getMessage());
        }

        return false;
    }

    // =====================================================
    // REVIEW
    // =====================================================

    public boolean review(int submissionId, Integer marks, String remarks,
                          String newStatus, int reviewerId) {

        List<String> validStatuses = List.of(
                "REVIEWED", "APPROVED", "NEEDS_IMPROVEMENT", "REJECTED");

        if (!validStatuses.contains(newStatus)) {
            System.err.println("[SubmissionDAO] Invalid status: " + newStatus);
            return false;
        }

        String sql = """
            UPDATE submissions
            SET marks           = ?,
                faculty_remarks = ?,
                status          = ?,
                reviewed_at     = NOW()
            WHERE id = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (marks != null) ps.setInt(1, marks);
            else ps.setNull(1, Types.INTEGER);

            ps.setString(2, remarks);
            ps.setString(3, newStatus);
            ps.setInt(4, submissionId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] review error: " + e.getMessage());
        }

        return false;
    }

    // =====================================================
    // MAPPER
    // =====================================================

    private Submission mapRow(ResultSet rs) throws SQLException {

        Submission s = new Submission();

        s.setId(rs.getInt("id"));
        s.setTaskId(rs.getInt("task_id"));
        s.setStudentId(rs.getInt("student_id"));
        s.setFilePath(rs.getString("file_path"));
        s.setFileName(rs.getString("file_name"));
        s.setComments(rs.getString("comments"));
        s.setStatus(rs.getString("status"));
        s.setResubmitCount(rs.getInt("resubmit_count"));

        // FIX: Extract file size KB
        try { s.setFileSizeKB(rs.getInt("file_size_kb")); } catch (Exception ignored) {}

        int marks = rs.getInt("marks");
        if (!rs.wasNull()) s.setMarks(marks);

        s.setFacultyRemarks(rs.getString("faculty_remarks"));

        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) s.setSubmittedAt(submittedAt.toLocalDateTime());

        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        if (reviewedAt != null) s.setReviewedAt(reviewedAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) s.setUpdatedAt(updatedAt.toLocalDateTime());

        try { s.setStudentName(rs.getString("student_name")); } catch (Exception ignored) {}
        try { s.setStudentEmail(rs.getString("student_email")); } catch (Exception ignored) {}
        try { s.setTaskTitle(rs.getString("task_title")); } catch (Exception ignored) {}
        try { s.setMaxMarks(rs.getInt("max_marks")); } catch (Exception ignored) {}

        try {
            Timestamp deadline = rs.getTimestamp("task_deadline");
            if (deadline != null) s.setTaskDeadline(deadline.toLocalDateTime());
        } catch (Exception ignored) {}

        return s;
    }
}