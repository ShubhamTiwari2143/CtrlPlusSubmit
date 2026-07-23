package in.ctrlplussubmit.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import in.ctrlplussubmit.util.DBConnection;

public class SubmissionHistoryDAO {

    public static int count(String sql) {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("[SubmissionHistoryDAO] count error: " + e.getMessage());
        }

        return 0;
    }
}