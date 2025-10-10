package app;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.LocalDateTime; // Added missing import
import java.util.HashMap;
import java.util.Map;

public class Prescription implements Serializable {
    private final String residentId;
    private final Map<Medicine, PrescriptionItem> items; // Medicine to dose/time mapping

    public Prescription(String residentId) {
        this.residentId = residentId;
        this.items = new HashMap<>();
    }

    public void addItem(Medicine medicine, String dose, LocalTime time) {
        items.put(medicine, new PrescriptionItem(dose, time));
    }

    public Medicine getMedicine() {
        // Return the first medicine if multiple are possible, or null if none
        return items.keySet().stream().findFirst().orElse(null);
    }

    public String getDose() {
        // Return the dose for the first medicine, or null if none
        return items.values().stream().findFirst().map(PrescriptionItem::getDose).orElse(null);
    }

    public LocalTime getTime() {
        // Return the time for the first medicine, or null if none
        return items.values().stream().findFirst().map(PrescriptionItem::getTime).orElse(null);
    }

    public void recordAdministration(Medicine medicine, LocalDateTime adminTime) {
        // Placeholder implementation; could store administration history
        // Example: items.get(medicine).setAdminTime(adminTime); (requires modification)
    }

    @Override
    public String toString() {
        return "Prescription{" +
                "residentId='" + residentId + '\'' +
                ", items=" + items +
                '}';
    }

    private static class PrescriptionItem implements Serializable {
        private final String dose;
        private final LocalTime time;

        public PrescriptionItem(String dose, LocalTime time) {
            this.dose = dose;
            this.time = time;
        }

        public String getDose() {
            return dose;
        }

        public LocalTime getTime() {
            return time;
        }
    }
}