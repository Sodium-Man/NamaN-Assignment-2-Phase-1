package app;

import java.io.Serializable;
import java.time.LocalDate;

public class Resident implements Serializable {
    private final String residentId;
    private String name;
    private Gender gender;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private String condition; // Added condition field

    public Resident(String residentId, String name, Gender gender, String condition) {
        this.residentId = residentId;
        this.name = name;
        this.gender = gender;
        this.condition = condition;
        this.admissionDate = LocalDate.now(); // Default to now, can be overridden
        this.dischargeDate = null;
    }

    public String getResidentId() {
        return residentId;
    }

    public String getName() {
        return name;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public LocalDate getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(LocalDate dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setAdmissionDate(LocalDate admissionDate) { // Added method
        this.admissionDate = admissionDate;
    }

    public String getCurrentBedId() {
        return null; // Placeholder; implement if needed
    }

    @Override
    public String toString() {
        return "Resident{" +
                "residentId='" + residentId + '\'' +
                ", name='" + name + '\'' +
                ", gender=" + gender +
                ", admissionDate=" + admissionDate +
                ", dischargeDate=" + dischargeDate +
                ", condition='" + condition + '\'' +
                '}';
    }
}