package app;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a single action log in the care home system.
 */
public class LogEntry implements Serializable {
    private final String staffId;
    private final String action;
    private final LocalDateTime timestamp;

    public LogEntry(String staffId, String action, LocalDateTime timestamp) {
        this.staffId = staffId;
        this.action = action;
        this.timestamp = timestamp;
    }

    public String getStaffId() {
        return staffId;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] Staff " + staffId + ": " + action;
    }
}
