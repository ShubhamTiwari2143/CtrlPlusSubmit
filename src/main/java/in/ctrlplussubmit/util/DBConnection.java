package in.ctrlplussubmit.util;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {

    public static Connection getConnection() {
        try {
            return DataSourceProvider.getDataSource().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get DB connection", e);
        }
    }
}