package app;

import java.io.Serializable;
import java.time.LocalTime;

public class Administration implements Serializable {
    private final Medicine medicine;
    private final LocalTime adminTime;

    public Administration(Medicine medicine, LocalTime adminTime) {
        this.medicine = medicine;
        this.adminTime = adminTime;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public LocalTime getAdminTime() {
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