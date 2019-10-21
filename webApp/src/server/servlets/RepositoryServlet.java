package server.servlets;

import com.google.gson.Gson;
import engine.ui.UIManager;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Repository;
import logic.manager.WCFileNode;
import logic.modules.Branch;
import logic.modules.Commit;
import server.utils.CommitFile;
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
import java.util.Map;

@WebServlet(name = "RepositoryServlet", urlPatterns = {"/pages/users/repo", "/pages/repository/repo"})
public class RepositoryServlet extends HttpServlet {

    private static final String REFRESH_WC = "0";
    private static final String GET_REPOSITORY_PAGE_DATA = "1";
    private static final String GET_REPOSITORY_PAGE_COMMIT_FILES = "2";
    private static final String GET_FILE_CONTENT = "3";
    private UIManager uiManager;
    private Repository currRepo;

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
        String reqType=request.getParameter("reqType");
            switch (reqType) {
            case GET_REPOSITORY_PAGE_DATA:
                String json = getRepositoryPageData();
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(json);
                break;
            case GET_REPOSITORY_PAGE_COMMIT_FILES:
                String commitSha1 = request.getParameter("commitSha1");
                String filesJson = new Gson().toJson(commitFilesDetails(commitSha1));
                if (filesJson != null) {
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(filesJson);
                }
                break;
            case GET_FILE_CONTENT:
                String filePath = request.getParameter("filePath");
                String contentJson = new Gson().toJson(uiManager.getFileContent(filePath));
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(contentJson);
                break;
            case REFRESH_WC:
                try {
                    List<WCFileNode> wcFiles = getWC();
                    String wcJSON = new Gson().toJson(wcFiles);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(wcJSON);
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (FailedToCreateRepositoryException e) {
                    e.printStackTrace();
                }
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

    private String getRepositoryPageData(){
        List<Branch> branches = uiManager.getBranches();
        List<WCFileNode> wcFiles = new ArrayList<>();
        try {
            wcFiles = getWC();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (FailedToCreateRepositoryException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Commit> commits = new ArrayList<>(uiManager.getCommitsMap().values());
        RepoMagitFile magitFile = new RepoMagitFile(branches, commits, wcFiles);
        return new Gson().toJson(magitFile);
    }

    private List<WCFileNode> getWC() throws ParserConfigurationException, IOException, FailedToCreateRepositoryException {
        return uiManager.createFilesTree();
    }

    private List<CommitFile> commitFilesDetails(String commitSha1){
        try {
            List<Map<String, String>> commitFilesDetails = uiManager.commitFilesDetails(commitSha1);
            List<CommitFile> commitFiles = new ArrayList<>();
            for(Map<String, String> file : commitFilesDetails){
                commitFiles.add(new CommitFile(file));
            }
            //return new Gson().toJson(commitFiles);
            return commitFiles;
        } catch (FailedToCreateRepositoryException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
