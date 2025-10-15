package app;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Administration implements Serializable {
    private final Medicine medicine;
    private final LocalDateTime adminTime; // Changed from LocalTime to LocalDateTime

    public Administration(Medicine medicine, LocalDateTime adminTime) {
        this.medicine = medicine;
        this.adminTime = adminTime;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public LocalDateTime getAdminTime() {
        return adminTime;
    }

    @Override
    public String toString() {
        return "Administration{" +
                "medicine=" + medicine.getName() +
                ", adminTime=" + adminTime +
                '}';
    }
}