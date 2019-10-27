package server.servlets;

import com.google.gson.Gson;
import engine.manager.PRManager;
import engine.manager.PullRequest;
import engine.ui.UIManager;
import logic.manager.Exceptions.FailedToCreateBranchException;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Repository;
import logic.manager.WCFileNode;
import logic.modules.Branch;
import logic.modules.Commit;
import server.utils.*;

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
    private static final String GET_OPEN_CHANGES = "4";
    private static final String GET_ACTIVE_USER = "5";
    private static final String CHECKOUT = "6";
    private static final String CHECKOUT_RTB = "7";

    private UIManager uiManager;
    private Repository currRepo;
    private PRManager prManager;

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
        try {
            String reqType = request.getParameter("reqType");
            switch (reqType) {
                case GET_REPOSITORY_PAGE_DATA:
                    String username = SessionUtils.getUsername(request);
                    String json = getRepositoryPageData(username);
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
                case GET_OPEN_CHANGES:
                    String toJSON = new Gson().toJson(getOpenChanges());
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(toJSON);
                    break;
                case GET_ACTIVE_USER:
                    String user = SessionUtils.getUsername(request);
                    user = new Gson().toJson(user);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(user);
                    break;
                case CHECKOUT:
                    String branch = request.getParameter("name");
                    if(uiManager.isRemoteBranch(branch))
                        returnError(response, "Cannot checkout remote branch, would you like to create a RTB instead?");
                    else {
                        uiManager.checkout(branch, false);
                    }
                    break;
                case CHECKOUT_RTB:
                    String branchName = request.getParameter("name");
                    uiManager.createCheckoutOnRTB(branchName);
            }
        }
        catch (Exception e){
            System.out.println("Error in GET repository");
        }
    }

    private void returnError(HttpServletResponse response, String message) throws IOException {
        try{
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(message);
            response.flushBuffer();
        }
        catch (Exception e){
            System.out.println("returnError Error : Branch not exists");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String[] data = br.readLine().split("&");
        String sha1 = data[0];
        String branchName = data[1];
        try {
            uiManager.addNewBranch(branchName, sha1);
            List<Branch> branches = uiManager.getBranches();
            String json = new Gson().toJson(branches);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(json);
        } catch (FailedToCreateBranchException e) {
            e.printStackTrace();
        } catch (FailedToCreateRepositoryException e) {
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String[] data = br.readLine().split("&");
        String branchName = data[0];
        try {
            uiManager.deleteBranch(branchName);
            List<Branch> branches = uiManager.getBranches();
            String json = new Gson().toJson(branches);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(json);
        }
        catch (Exception e){
            e.printStackTrace();
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
        try{
            String username = SessionUtils.getUsername(request);
            uiManager = ServletUtils.getUIManager(getServletContext());

            BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String[] data = br.readLine().split("&");
            String user = data[1];
            String repoName = data[0];
            String path = ServletUtils.REPOSITORY_DIR + "\\" + user +"\\"+ repoName;
            currRepo = uiManager.getRepositoryByPath(path);
            try {
                uiManager.changeActiveRepository(currRepo.getPath(), currRepo.getName());
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (FailedToCreateRepositoryException e) {
                e.printStackTrace();
            }
            catch (Exception e){
                e.printStackTrace();
            }

            if(!username.equals(currRepo.getUsername())){
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("not current user's repository");
                response.flushBuffer();
            }
        }
        catch (Exception e){
            System.out.println("Error in POST repository");
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private String getRepositoryPageData(String username){
        try{
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
            catch (Exception e) {
                e.printStackTrace();
            }
            List<Commit> commits = new ArrayList<>(uiManager.getCommitsMap().values());
            prManager = ServletUtils.getPRManager(getServletContext());
            List<PullRequest> pullRequests = prManager.getPullRequests(username, uiManager.getRepositoryName());
            RepoMagitFile magitFile = new RepoMagitFile(branches, commits, wcFiles, uiManager.isForked(), pullRequests);
            return new Gson().toJson(magitFile);
        }
        catch (Exception e){
            System.out.println("Error in get Repository Page Data");
            return "";
        }
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
            return commitFiles;
        } catch (FailedToCreateRepositoryException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private OpenChanges getOpenChanges() throws IOException {
        OpenChanges openChanges = new OpenChanges(
                uiManager.getNewFiles(),
                uiManager.getDeletedFiles(),
                uiManager.getModifieddFiles());
        return openChanges;
    }
}
