package in.ctrlplussubmit.util;

import jakarta.servlet.*;

public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {


        ServletContext sc = sce.getServletContext();

        String url = "jdbc:mysql://sakura.proxy.rlwy.net:38693/railway";
        String username = "root";
        String password = "uNqqJdooljjJmMRQPVEnPMctYdFsVnwf";
        
        System.out.println("DB URL = " + url);
        System.out.println("DB USER = " + username);

        DataSourceProvider.init(url, username, password);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DataSourceProvider.shutdown();
    }
}