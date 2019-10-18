package server.servlets;

import org.apache.commons.io.FileUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;

public final class ContextListenerServlet implements ServletContextListener {

    private String repositoryDir = "C:\\magit-ex3";
    private File file;
    public ContextListenerServlet() {
    }

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        try {
            file = new File(repositoryDir);
            file.mkdir();
        } catch (Exception e) {
            System.out.println("Error creating directory: " + e.getMessage());
        }
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            System.out.println("Error deleting directory: " + e.getMessage());
        }
    }
}