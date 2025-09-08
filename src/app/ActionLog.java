package app;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ActionLog implements Serializable {
    private final LocalDateTime timestamp;
    private final String staffId;
    private final String action;

    public ActionLog(String staffId, String action) {
        this.timestamp = LocalDateTime.now();
        this.staffId = staffId;
        this.action = action;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] staff=" + staffId + " :: " + action;
    }
}
