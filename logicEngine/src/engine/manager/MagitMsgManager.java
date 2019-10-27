package engine.manager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MagitMsgManager {

    private final Map<String, List<SingleMessageEntry>> magitDataList; // username to list of messages
    public MagitMsgManager() {
        magitDataList = new HashMap<>();
    }
    private boolean isFirst = true;

    public synchronized void addMsgString(String message, String timestamp, String user) {
        if(!magitDataList.containsKey(user))
            magitDataList.put(user, new ArrayList<>());
        magitDataList.get(user).add(new SingleMessageEntry(message, timestamp));
    }

    public synchronized List<SingleMessageEntry> getMessagesEntries(int fromIndex, String user){
        if(magitDataList.containsKey(user)) {
            if (fromIndex < 0 || fromIndex > magitDataList.get(user).size()) {
                fromIndex = 0;
            }
            return magitDataList.get(user).subList(fromIndex, magitDataList.get(user).size());
        }
        return null;
    }

    public int getVersion(String user) {
        if(magitDataList.containsKey(user))
            return magitDataList.get(user).size();
        else
            return 0;
    }

    public boolean isEmpty(){
        if(isFirst) {
            isFirst = false;
            return true;
        }
        else return false;
    }
}