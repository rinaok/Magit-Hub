package server.servlets;

import com.google.gson.Gson;
import engine.ui.UIManager;
import engine.users.UserManager;
import server.utils.ServletUtils;
import server.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

@WebServlet(name = "UserListServlet", urlPatterns = {"/pages/users/users", "/pages/repository/users"})
public class UsersListServlet extends HttpServlet {
    private final static String GET_ACTIVE_USER = "1";
    private final static String GET_USERLIST = "2";
    private final static String CLONE = "3";
    private UIManager uiManager;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //returning JSON objects, not HTML
        response.setContentType("application/json");
        try (PrintWriter out = response.getWriter()) {
            Gson gson = new Gson();
            UserManager userManager = ServletUtils.getUserManager(getServletContext());
            Set<String> usersList = userManager.getAllUsers();
            String json = gson.toJson(usersList);
            out.println(json);
            out.flush();
        }
    }

    private void doClone(HttpServletRequest request, HttpServletResponse response) throws IOException {
        uiManager = ServletUtils.getUIManager(getServletContext());
        String owner = request.getParameter("owner");
        String repoName = request.getParameter("repoName");
        String loggedInUser = SessionUtils.getUsername(request);
        String remotePath = ServletUtils.REPOSITORY_DIR + "\\" + owner + "\\" + repoName;
        String localPath = ServletUtils.REPOSITORY_DIR + "\\" + loggedInUser + "\\" + repoName;
        try {
            uiManager.doClone(remotePath, localPath, repoName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String reqType = request.getParameter("reqType");
        switch (reqType) {
            case GET_USERLIST:
                processRequest(request, response);
                break;
            case GET_ACTIVE_USER:
                String activeUser = SessionUtils.getUsername(request);
                String user = new Gson().toJson(activeUser);
                if (user != null) {
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(user);
                }
                break;
            case CLONE:
                doClone(request, response);
                break;
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
