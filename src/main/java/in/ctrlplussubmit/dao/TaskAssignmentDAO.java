package in.ctrlplussubmit.dao;

import in.ctrlplussubmit.model.TaskAssignment;
import in.ctrlplussubmit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskAssignmentDAO {

    // =====================================================
    // ASSIGN TO STUDENT
    // =====================================================

    public boolean assignToStudent(int taskId, int studentId) {

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                String assignSql = """
                    INSERT IGNORE INTO task_assignments
                        (task_id, assignment_type, student_id)
                    VALUES (?, 'INDIVIDUAL', ?)
                    """;

                try (PreparedStatement ps = conn.prepareStatement(assignSql)) {
                    ps.setInt(1, taskId);
                    ps.setInt(2, studentId);
                    ps.executeUpdate();
                }

                createSubmissionRow(conn, taskId, studentId);

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[TaskAssignmentDAO] assignToStudent error: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[TaskAssignmentDAO] connection error: " + e.getMessage());
        }

        return false;
    }

    // =====================================================
    // ASSIGN TO BATCH
    // =====================================================

    public int assignToBatch(int taskId, int batchId) {

        int count = 0;

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                String assignSql = """
                    INSERT IGNORE INTO task_assignments
                        (task_id, assignment_type, batch_id)
                    VALUES (?, 'BATCH', ?)
                    """;

                try (PreparedStatement ps = conn.prepareStatement(assignSql)) {
                    ps.setInt(1, taskId);
                    ps.setInt(2, batchId);
                    ps.executeUpdate();
                }

                List<Integer> studentIds = getStudentIdsInBatch(conn, batchId);

                for (int studentId : studentIds) {
                    createSubmissionRow(conn, taskId, studentId);
                    count++;
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[TaskAssignmentDAO] assignToBatch error: " + e.getMessage());
                return 0;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[TaskAssignmentDAO] connection error: " + e.getMessage());
        }

        return count;
    }

    // =====================================================
    // READ — ASSIGNMENTS
    // =====================================================

    public List<TaskAssignment> getAssignmentsForTask(int taskId) {

        List<TaskAssignment> list = new ArrayList<>();

        String sql = """
            SELECT ta.*,
                   u.full_name  AS student_name,
                   b.batch_name AS batch_name
            FROM task_assignments ta
            LEFT JOIN users u  ON ta.student_id = u.id
            LEFT JOIN batches b ON ta.batch_id  = b.id
            WHERE ta.task_id = ?
            ORDER BY ta.assigned_at DESC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    TaskAssignment ta = new TaskAssignment();

                    ta.setId(rs.getInt("id"));
                    ta.setTaskId(rs.getInt("task_id"));
                    ta.setAssignmentType(rs.getString("assignment_type"));
                    ta.setStudentId(rs.getInt("student_id"));
                    ta.setBatchId(rs.getInt("batch_id"));

                    try { ta.setStudentName(rs.getString("student_name")); } catch (Exception ignored) {}
                    try { ta.setBatchName(rs.getString("batch_name")); } catch (Exception ignored) {}

                    Timestamp ts = rs.getTimestamp("assigned_at");
                    if (ts != null) ta.setAssignedAt(ts.toLocalDateTime());

                    list.add(ta);
                }
            }

        } catch (SQLException e) {
            System.err.println("[TaskAssignmentDAO] getAssignmentsForTask error: " + e.getMessage());
        }

        return list;
    }

    // =====================================================
    // CHECK ASSIGNMENT
    // =====================================================

    public boolean isAlreadyAssigned(int taskId, int studentId) {

        String sql = """
            SELECT COUNT(*) FROM task_assignments ta
            WHERE ta.task_id = ?
              AND (
                (ta.assignment_type = 'INDIVIDUAL' AND ta.student_id = ?)
                OR
                (ta.assignment_type = 'BATCH' AND ta.batch_id IN (
                    SELECT batch_id FROM batch_students WHERE student_id = ?
                ))
              )
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setInt(2, studentId);
            ps.setInt(3, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("[TaskAssignmentDAO] isAlreadyAssigned error: " + e.getMessage());
        }

        return false;
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    private void createSubmissionRow(Connection conn, int taskId, int studentId) throws SQLException {

        String sql = """
            INSERT IGNORE INTO submissions
                (task_id, student_id, status)
            VALUES (?, ?, 'PENDING')
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, studentId);
            ps.executeUpdate();
        }
    }

    private List<Integer> getStudentIdsInBatch(Connection conn, int batchId) throws SQLException {

        List<Integer> ids = new ArrayList<>();

        String sql = "SELECT student_id FROM batch_students WHERE batch_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, batchId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("student_id"));
            }
        }

        return ids;
    }
}