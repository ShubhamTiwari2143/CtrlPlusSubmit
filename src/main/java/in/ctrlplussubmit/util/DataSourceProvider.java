package in.ctrlplussubmit.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceProvider {

    private static HikariDataSource dataSource;

    public static void init(String url, String username, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10000);
            config.setIdleTimeout(30000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(5000);
            config.setInitializationFailTimeout(-1);
            config.setConnectionTestQuery("SELECT 1");

            dataSource = new HikariDataSource(config);

        } catch (Exception e) {
            throw new RuntimeException("Hikari init failed", e);
        }
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            throw new RuntimeException("DataSource not initialized");
        }
        return dataSource;
    }

    public static void shutdown() {
        if (dataSource != null) dataSource.close();
    }
}