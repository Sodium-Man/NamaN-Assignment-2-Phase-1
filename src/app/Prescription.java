package app;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        return items.keySet().stream().findFirst().orElse(null);
    }

    public String getDose() {
        return items.values().stream().findFirst().map(PrescriptionItem::getDose).orElse(null);
    }

    public LocalTime getTime() {
        return items.values().stream().findFirst().map(PrescriptionItem::getTime).orElse(null);
    }

    public Map<Medicine, PrescriptionItem> getItems() {
        return new HashMap<>(items); // Defensive copy for encapsulation
    }

    public void recordAdministration(Medicine medicine, LocalDateTime adminTime) throws Exception {
        PrescriptionItem item = items.get(medicine);
        if (item == null) {
            throw new Exception("No prescription for medicine: " + medicine.getName());
        }
        item.addAdministration(adminTime);
    }

    public List<Administration> getAllAdministrations() {
        List<Administration> all = new ArrayList<>();
        for (Map.Entry<Medicine, PrescriptionItem> entry : items.entrySet()) {
            for (LocalDateTime time : entry.getValue().getAdministeredTimes()) {
                all.add(new Administration(entry.getKey(), time));
            }
        }
        return all;
    }

    public String getFormattedDetails() {
        if (items.isEmpty()) {
            return "None";
        }
        StringBuilder sb = new StringBuilder();
        items.forEach((med, item) ->
                sb.append(med.getName()).append(": ").append(item.getDose())
                        .append(" @ ").append(item.getTime()).append("\n"));
        return sb.toString();
    }

    // Added for DBManager to archive prescription items
    public List<PrescriptionItemData> getPrescriptionItemsData() {
        List<PrescriptionItemData> data = new ArrayList<>();
        items.forEach((med, item) ->
                data.add(new PrescriptionItemData(med, item.getDose(), item.getTime())));
        return data;
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
        private final List<LocalDateTime> administeredTimes = new ArrayList<>();

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

        public List<LocalDateTime> getAdministeredTimes() {
            return new ArrayList<>(administeredTimes); // Defensive copy
        }

        public void addAdministration(LocalDateTime time) {
            administeredTimes.add(time);
        }

        @Override
        public String toString() {
            return dose + " @ " + time + " (administered: " + administeredTimes.size() + " times)";
        }
    }

    // Helper class for DBManager
    public static class PrescriptionItemData {
        private final Medicine medicine;
        private final String dose;
        private final LocalTime time;

        public PrescriptionItemData(Medicine medicine, String dose, LocalTime time) {
            this.medicine = medicine;
            this.dose = dose;
            this.time = time;
        }

        public Medicine getMedicine() {
            return medicine;
        }

        public String getDose() {
            return dose;
        }

        public LocalTime getTime() {
            return time;
        }
    }
}