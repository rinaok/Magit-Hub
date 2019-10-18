package engine.users;

import java.util.HashSet;
import java.util.Set;

/*
Adding and retrieving users is synchronized and in that manner - these actions are thread safe
Note that asking if a user exists (isUserExists) does not participate in the synchronization and it is the responsibility
of the user of this class to handle the synchronization of isUserExists with other methods here on it's own
 */
public class UserManager {

    private final Set<String> onlineUsers;
    private final Set<String> allUsers;
    public UserManager() {
        onlineUsers = new HashSet<>();
        allUsers = new HashSet<>();
    }

    public synchronized void addUser(String username) {
        onlineUsers.add(username);
        if(!allUsers.contains(username)){
            allUsers.add(username);
        }
    }

    public synchronized void removeUser(String username) {
        onlineUsers.remove(username);
    }

    public synchronized Set<String> getOnlineUsers() {
        return onlineUsers;
    }

    public synchronized Set<String> getAllUsers() {
        return allUsers;
    }

    public boolean isUserExists(String username) {
        return onlineUsers.contains(username);
    }
}
