package engine.manager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PRManager {
    Map<String, List<PullRequest>> userToPR;
    Map<Integer, PullRequest> idToPR;

    public PRManager(){
        userToPR = new HashMap<>();
        idToPR = new HashMap<>();
    }

    public List<PullRequest> getPullRequests(String user){
        if(userToPR.containsKey(user)){
            return userToPR.get(user);
        }
        else return null;
    }

    public void addPR(PullRequest newPR, String user){
        if(!userToPR.containsKey(user)){
            userToPR.put(user, new ArrayList<>());
        }
        userToPR.get(user).add(newPR);
        int id = idToPR.size();
        newPR.setPrID(id);
        idToPR.put(id, newPR);
    }

    public PullRequest getPRByID(int id){
        if(idToPR.containsKey(id))
            return idToPR.get(id);
        return null;
    }


}
