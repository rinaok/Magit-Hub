package engine.manager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PRManager {
    Map<String, List<PullRequest>> userToPR;

    public PRManager(){
        userToPR = new HashMap<>();
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
    }
}
