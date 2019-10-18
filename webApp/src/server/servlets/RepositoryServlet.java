package server.servlets;

import com.google.gson.Gson;
import engine.ui.UIManager;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Repository;
import logic.modules.Branch;
import logic.modules.Commit;
import server.utils.RepoMagitFile;
import server.utils.ServletUtils;
import server.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "RepositoryServlet", urlPatterns = {"/pages/users/repo", "/pages/repository/repo"})
public class RepositoryServlet extends HttpServlet {

    UIManager uiManager;
    Repository currRepo;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */

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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Branch> branches = uiManager.getBranches();
        List<Commit> commits = new ArrayList<>(uiManager.getCommitsMap().values());
        RepoMagitFile magitFile = new RepoMagitFile(branches, commits);
        String json = new Gson().toJson(magitFile);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = SessionUtils.getUsername(request);
        uiManager = ServletUtils.getUIManager(getServletContext());

        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String repositoryName = br.readLine();
        currRepo = uiManager.getRepositoryByName(repositoryName);
        try {
            uiManager.changeActiveRepository(currRepo.getPath(), currRepo.getName());
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (FailedToCreateRepositoryException e) {
            e.printStackTrace();
        }

        if(username != currRepo.getUsername()){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("not current user's repository");
            response.flushBuffer();
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
