package app;

import java.io.Serializable;
import java.util.Objects;

public class Resident implements Serializable {
    private final String residentId;
    private final String name;
    private final Gender gender;
    private final String medicalCondition;

    public Resident(String residentId, String name, Gender gender, String medicalCondition) {
        this.residentId = Objects.requireNonNull(residentId);
        this.name = Objects.requireNonNull(name);
        this.gender = Objects.requireNonNull(gender);
        this.medicalCondition = medicalCondition;
    }

    public String getResidentId() { return residentId; }
    public String getName() { return name; }
    public Gender getGender() { return gender; }
    public String getMedicalCondition() { return medicalCondition; }

    @Override
    public String toString() {
        return name + " (" + gender + ") id=" + residentId + (medicalCondition != null ? " [" + medicalCondition + "]" : "");
    }
}
