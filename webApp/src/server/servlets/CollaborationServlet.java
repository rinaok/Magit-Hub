package server.servlets;

import com.google.gson.Gson;
import engine.manager.PRManager;
import engine.manager.PullRequest;
import engine.ui.UIManager;
import server.utils.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "CollaborationServlet", urlPatterns = {"/pages/users/collaboration", "/pages/repository/collaboration"})
public class CollaborationServlet extends HttpServlet {
    private static final String PULL = "0";
    private static final String PUSH = "1";
    private static final String PR = "2";
    private UIManager uiManager;
    private PRManager prManager;

    private void addPR(HttpServletRequest request){
        prManager = ServletUtils.getPRManager(getServletContext());
        String targetBranch = request.getParameter("target");
        targetBranch = targetBranch.replace("/","\\");
        String baseBranch= request.getParameter("base");
        baseBranch = baseBranch.replace("/","\\");
        String msg = request.getParameter("msg");
        if(targetBranch != null && baseBranch != null) {
            try {
                String owner = uiManager.getPullRequestUser();
                prManager.addPR(new PullRequest(targetBranch, baseBranch, msg), owner);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String reqType = request.getParameter("reqType");
            uiManager = ServletUtils.getUIManager(getServletContext());
            String toJSON = "";
            switch (reqType){
                case PULL:
                    try {
                        boolean openChanges = uiManager.doPull();
                        toJSON = openChanges ? "Pull was done successfully" : "Pull was not performed since there are open changes";
                    }
                    catch (Exception e) {
                        toJSON = e.toString();
                    }
                    finally {
                        toJSON = new Gson().toJson(toJSON);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(toJSON);
                    }
                    break;
                case PUSH:
                    try{
                        uiManager.doPushToRR();
                        toJSON = "Push of head branch was performed successfully!";
                    }
                    catch (Exception e){
                        toJSON = e.toString();
                    }
                    finally {
                        toJSON = new Gson().toJson(toJSON);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(toJSON);
                    }
                    break;
                case PR:
                    addPR(request);
                    break;
            }
        }
        catch (Exception e){
            System.out.println("Error in GET Collaboration");
        }
    }
}
