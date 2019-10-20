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

    private UIManager uiManager;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        uiManager = ServletUtils.getUIManager(getServletContext());
        String fileSha1 = request.getParameter("fileSha1");
        String newContent = request.getParameter("content");
        String path = uiManager.findFilePath(fileSha1);
        try {
            uiManager.editFileInServer(path, newContent);
        } catch (FailedToCreateRepositoryException e) {
            e.printStackTrace();
        }
    }


}
