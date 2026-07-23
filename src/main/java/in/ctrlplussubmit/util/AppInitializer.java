package in.ctrlplussubmit.util;

import jakarta.servlet.*;

public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {


        ServletContext sc = sce.getServletContext();

        String url = "jdbc:mysql://localhost:3306/ctrl_plus_submit";
        String username = "root";
        String password = "Mystudents";
        
        System.out.println("DB URL = " + url);
        System.out.println("DB USER = " + username);

        DataSourceProvider.init(url, username, password);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DataSourceProvider.shutdown();
    }
}