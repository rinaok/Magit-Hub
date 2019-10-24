package engine.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
This class is thread safe in the manner of adding\fetching new chat lines, but not in the manner of getting the size of the list
if the use of getVersion is to be incorporated with other methods here - it should be synchronized from the user code
 */
public class MagitMsgManager {

    private final Map<String, List<SingleMessageEntry>> magitDataList; // username to list of messages
    public MagitMsgManager() {
        magitDataList = new HashMap<>();
    }

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
}