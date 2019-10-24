package server.servlets;

//taken from: http://www.servletworld.com/servlet-tutorials/servlet3/multipartconfig-file-upload-example.html
// and http://docs.oracle.com/javaee/6/tutorial/doc/glraq.html

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import engine.ui.UIManager;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Exceptions.XmlParseException;
import server.utils.ServletUtils;
import server.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutionException;

@WebServlet(name = "FileUploadServlet", urlPatterns = {"/pages/users/upload"})
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 5 * 5)
public class FileUploadServlet extends HttpServlet {
    UIManager uiManager;
    private final Map<String, List<String>> repositories;

    public FileUploadServlet() {
        repositories = new HashMap<>();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        printRepositoryDetails(response, SessionUtils.getUsername(request));
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String name = br.readLine();
        List<String> repository = repositories.get(name);
        String json = new Gson().toJson(repository);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Collection<Part> parts = request.getParts();
        StringBuilder fileContent = new StringBuilder();
        for (Part part : parts) {
            fileContent.append(readFromInputStream(part.getInputStream()));
        }
        String xmlString = fileContent.toString();
        uiManager = ServletUtils.getUIManager(getServletContext());
        String username = SessionUtils.getUsername(request);
        uiManager.changeUserName(username);
        try {
            uiManager.loadXML(xmlString);
            printRepositoryDetails(response, username);
        } catch (XmlParseException e) {
            returnError(response, e.getMessage());
        } catch (FailedToCreateRepositoryException e) {
            returnError(response, e.getMessage());
        } catch (ParserConfigurationException e) {
            returnError(response, e.getMessage());
        } catch (ExecutionException e) {
            returnError(response, e.getMessage());
        } catch (InterruptedException e) {
            returnError(response, e.getMessage());
        }catch (Exception e) {
            returnError(response, e.getMessage());
        }

    }

    private void returnError(HttpServletResponse response, String message) throws IOException {
        try{
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(message);
            response.flushBuffer();
        }
        catch (Exception e){
            System.out.println("Error : Repository not exists");
        }
    }

    private void printRepositoryDetails(HttpServletResponse response, String username) throws IOException {
        List repositoriesList = new LinkedList();
        JsonObject repositoryDetails = new JsonObject();
        repositoryDetails.addProperty("Name", uiManager.getRepositoryName());
        repositoryDetails.addProperty("ActiveBranch", uiManager.getHeadBranch());
        repositoryDetails.addProperty("BranchesAmount", uiManager.getBranches().size());
        repositoryDetails.addProperty("CommitDate", uiManager.getHeadCommit().getCreationDate());
        repositoryDetails.addProperty("CommitMessage", uiManager.getHeadCommit().getMessage());
        repositoryDetails.addProperty("Forked", uiManager.isForked());

        String json = new Gson().toJson(repositoryDetails);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if(repositories.containsKey(username)){
            repositoriesList = repositories.get(username);
        }
        repositoriesList.add(json);
        repositories.put(username, repositoriesList);
        response.getWriter().write(json);
    }

    private String readFromInputStream(InputStream inputStream) {
        return new Scanner(inputStream).useDelimiter("\\Z").next();
    }
}