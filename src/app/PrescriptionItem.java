package app;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionItem implements Serializable {
    private final String dose;
    private final LocalTime scheduledTime;
    private final List<LocalDateTime> administeredTimes = new ArrayList<>();

    public PrescriptionItem(String dose, LocalTime scheduledTime) {
        this.dose = dose;
        this.scheduledTime = scheduledTime;
    }

    public String getDose() { return dose; }
    public LocalTime getScheduledTime() { return scheduledTime; }
    public List<LocalDateTime> getAdministeredTimes() { return administeredTimes; }

    public void addAdministration(LocalDateTime time) {
        administeredTimes.add(time);
    }

    @Override
    public String toString() {
        return dose + " @ " + scheduledTime + " (administered: " + administeredTimes.size() + " times)";
    }
}