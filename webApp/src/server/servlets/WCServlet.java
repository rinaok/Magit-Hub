package server.servlets;
import engine.ui.UIManager;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import server.utils.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "WCServlet", urlPatterns = {"/pages/users/wc", "/pages/repository/wc"})
public class WCServlet extends HttpServlet {
    private static final String EDIT_FILE = "4";
    private static final String DELETE_FILE = "5";
    private static final String NEW_FILE = "6";
    private UIManager uiManager;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        uiManager = ServletUtils.getUIManager(getServletContext());
        String requestType = request.getParameter("reqType");
        String filePath = request.getParameter("filePath");
        switch (requestType) {
            case EDIT_FILE:
                String newContent = request.getParameter("content").replaceAll("\\n", "\r\n");
                //String path = uiManager.findFilePath(filePath);
                try {
                    uiManager.editFileInServer(filePath, newContent);
                } catch (FailedToCreateRepositoryException e) {
                    e.printStackTrace();
                }
                break;
            case DELETE_FILE:
                uiManager.deleteFile(filePath);
                break;
            case NEW_FILE:
                String content = request.getParameter("fileContent").replaceAll("\\n", "\r\n");
                String fileName = request.getParameter("fileName");
                try {
                    uiManager.addNewFile(filePath, fileName, content);
                } catch (FailedToCreateRepositoryException e) {
                    e.printStackTrace();
                }
                break;
        }
    }


}
