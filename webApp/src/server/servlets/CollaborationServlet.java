package server.servlets;
import com.google.gson.Gson;
import engine.manager.MagitMsgManager;
import engine.manager.PRManager;
import engine.manager.PullRequest;
import engine.ui.UIManager;
import logic.manager.PRStatus;
import logic.manager.Utils;
import server.utils.ServletUtils;
import server.utils.SessionUtils;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@WebServlet(name = "CollaborationServlet", urlPatterns = {"/pages/users/collaboration", "/pages/repository/collaboration"})
public class CollaborationServlet extends HttpServlet {
    private static final String PULL = "0";
    private static final String PUSH = "1";
    private static final String PR = "2";
    private UIManager uiManager;
    private PRManager prManager;
    private MagitMsgManager msgManager;

    private void addPR(HttpServletRequest request) {
        prManager = ServletUtils.getPRManager(getServletContext());
        String targetBranch = request.getParameter("target");
        String baseBranch = request.getParameter("base");
        String msg = request.getParameter("msg");
        if (targetBranch != null && baseBranch != null) {
            try {
                String username = SessionUtils.getUsername(request);
                String repository = uiManager.getRepositoryName();
                PullRequest pr = new PullRequest(targetBranch, baseBranch, msg, PRStatus.OPEN, username, Utils.getTime(), repository);
                uiManager.deltaCommitPR(pr);
                prManager.addPR(pr, uiManager.getPullRequestUser());
                msgManager = ServletUtils.getMsgManager(getServletContext());
                String msgPR = "Pull Request #" + pr.getPrID() + " was sent by [" + username + "]\r\n" +
                        "Repository: " + pr.getRepository() + "\r\n" +
                        "Base Branch: " + pr.getBaseBranch() + "\r\n"
                        + "Target Branch: " + pr.getTargetBranch() + "\r\n"
                        + "PR Message: " + pr.getMsg();
                msgManager.addMsgString(msgPR, Utils.getTime(), uiManager.getPullRequestUser());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMsg(PullRequest PR, String username){
        msgManager = ServletUtils.getMsgManager(getServletContext());
        switch (PR.getStatus()){
            case CLOSED:
                String msgClosed = "Pull Request #" + PR.getPrID() + " was accepted by [" + username + "]\r\n" +
                        "Base Branch: " + PR.getBaseBranch() + "\r\n"
                        + "Target Branch: " + PR.getTargetBranch() + "\r\n"
                        + "Date of PR: " + PR.getDate();
                msgManager.addMsgString(msgClosed, Utils.getTime(), PR.getOwner());
                break;
            case REJECTED:
                String msgReject = "Pull Request #" + PR.getPrID() + " was rejected by [" + username + "]\r\n" +
                        "Reason: " + PR.getRejectedMsg() + "\r\n" +
                        "Base Branch: " + PR.getBaseBranch() + "\r\n"
                        + "Target Branch: " + PR.getTargetBranch() + "\r\n"
                        + "Date of PR: " + PR.getDate();
                msgManager.addMsgString(msgReject, Utils.getTime(), PR.getOwner());
                break;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String reqType = request.getParameter("reqType");
            uiManager = ServletUtils.getUIManager(getServletContext());
            String toJSON = "";
            switch (reqType) {
                case PULL:
                    try {
                        boolean openChanges = uiManager.doPull();
                        toJSON = openChanges ? "Pull was done successfully" : "Pull was not performed since there are open changes";
                    } catch (Exception e) {
                        toJSON = e.toString();
                    }
                    break;
                case PUSH:
                    try {
                        uiManager.doPushToRR();
                        toJSON = "Push of head branch was performed successfully!";
                    } catch (Exception e) {
                        toJSON = e.toString();
                    }
                    break;
                case PR:
                    addPR(request);
                    break;
            }

            toJSON = new Gson().toJson(toJSON);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(toJSON);
        } catch (Exception e) {
            System.out.println("Error in GET Collaboration");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String prID = br.readLine();
        prManager = ServletUtils.getPRManager(getServletContext());
        PullRequest pr = prManager.getPRByID(Integer.parseInt(prID));
        if (pr != null) {
            String toJSON = new Gson().toJson(pr.getDelta());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(toJSON);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String[] data = br.readLine().split("&");
        String prID = data[0];
        String status = data[1];
        prManager = ServletUtils.getPRManager(getServletContext());
        PullRequest pr = prManager.getPRByID(Integer.parseInt(prID));
        if(pr == null)
            return;
        PRStatus prStatus = PRStatus.OPEN;
        switch (status){
            case "Open":
                prStatus = PRStatus.OPEN;
                break;
            case "Closed":
                prStatus = PRStatus.CLOSED;
                uiManager = ServletUtils.getUIManager(getServletContext());
                uiManager.acceptPR(pr);
                sendMsg(pr, SessionUtils.getUsername(request));
                break;
            case "Rejected":
                prStatus = PRStatus.REJECTED;
                pr.setRejectedMsg(data[2]);
                sendMsg(pr, SessionUtils.getUsername(request));
                break;
        }
        pr.setPRStatus(prStatus);
    }
}
