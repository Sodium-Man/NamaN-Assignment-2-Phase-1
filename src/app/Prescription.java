package app;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class Prescription implements Serializable {
    private final String residentId;
    private final Map<Medicine, String> items = new LinkedHashMap<>();

    public Prescription(String residentId) {
        this.residentId = residentId;
    }

    public void addItem(Medicine med, String dose, LocalTime time) {
        items.put(med, dose + " @ " + time);
    }

    public Map<Medicine, String> getItems() { return items; }

    @Override
    public String toString() {
        return "Prescription for " + residentId + ": " + items;
    }
}
