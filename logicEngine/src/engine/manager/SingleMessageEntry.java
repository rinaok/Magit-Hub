package engine.manager;

public class SingleMessageEntry {
    private final String message;
    private final String timestamp;

    public SingleMessageEntry(String message, String timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return timestamp;
    }

    @Override
    public String toString() {
        return (message != null ? timestamp + ": " : "") + message;
    }
}
