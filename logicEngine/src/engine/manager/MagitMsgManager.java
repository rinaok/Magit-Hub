package engine.manager;

import java.util.ArrayList;
import java.util.List;

/*
This class is thread safe in the manner of adding\fetching new chat lines, but not in the manner of getting the size of the list
if the use of getVersion is to be incorporated with other methods here - it should be synchronized from the user code
 */
public class MagitMsgManager {

    private final List<SingleMessageEntry> magitDataList;
    public MagitMsgManager() {
        magitDataList = new ArrayList<>();
    }

    public synchronized void addMsgString(String message, String timestamp) {
        magitDataList.add(new SingleMessageEntry(message, timestamp));
    }

    public synchronized List<SingleMessageEntry> getMessagesEntries(int fromIndex){
        if (fromIndex < 0 || fromIndex > magitDataList.size()) {
            fromIndex = 0;
        }
        return magitDataList.subList(fromIndex, magitDataList.size());
    }

    public int getVersion() {
        return magitDataList.size();
    }
}