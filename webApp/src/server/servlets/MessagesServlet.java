package server.servlets;

import server.constants.Constants;
import server.utils.ServletUtils;
import server.utils.SessionUtils;
import com.google.gson.Gson;
import engine.manager.MagitMsgManager;
import engine.manager.SingleMessageEntry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "MessagesServlet", urlPatterns = {"/pages/users/messages", "/pages/repository/messages"})
public class MessagesServlet extends HttpServlet {

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        MagitMsgManager magitManager = ServletUtils.getMsgManager(getServletContext());
        String username = SessionUtils.getUsername(request);
        /*
        verify chat version given from the user is a valid number. if not it is considered an error and nothing is returned back
        Obviously the UI should be ready for such a case and handle it properly
         */
        int msgVersion = ServletUtils.getIntParameter(request, Constants.MSG_VERSION_PARAMETER);
        if (msgVersion == Constants.INT_PARAMETER_ERROR) {
            return;
        }

        /*
        Synchronizing as minimum as I can to fetch only the relevant information from the chat manager and then only processing and sending this information onward
        Note that the synchronization here is on the ServletContext, and the one that also synchronized on it is the chat servlet when adding new chat lines.
         */
        int msgManagerVersion = 0;
        List<SingleMessageEntry> msgEntries;
        synchronized (getServletContext()) {
            msgManagerVersion = magitManager.getVersion();
            msgEntries = magitManager.getMessagesEntries(msgVersion, username);
        }

        // log and create the response json string
        MessagesAndVersion mav = new MessagesAndVersion(msgEntries, msgManagerVersion);
        Gson gson = new Gson();
        String jsonResponse = gson.toJson(mav);
        logServerMessage("Server Messages version: " + msgManagerVersion + ", User '" + username + "' Message version: " + msgVersion);
        logServerMessage(jsonResponse);

        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse);
            out.flush();
        }

    }

    private void logServerMessage(String message){
        System.out.println(message);
    }
    
    private static class MessagesAndVersion {

        final private List<SingleMessageEntry> entries;
        final private int version;

        public MessagesAndVersion(List<SingleMessageEntry> entries, int version) {
            this.entries = entries;
            this.version = version;
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
        processRequest(request, response);
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
