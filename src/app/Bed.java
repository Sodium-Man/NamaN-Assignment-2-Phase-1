package app;

import java.io.Serializable;

public class Bed implements Serializable {
    private final String bedId;
    private Resident resident;

    public Bed(String bedId) { this.bedId = bedId; }

    public String getBedId() { return bedId; }
    public Resident getResident() { return resident; }
    public boolean isVacant() { return resident == null; }

    public void assignResident(Resident r) throws Exception {
        if (!isVacant()) throw new Exception("Bed " + bedId + " is already occupied!");
        this.resident = r;
    }

    public void removeResident() {
        this.resident = null;
    }

    @Override
    public String toString() {
        return "Bed{" + bedId + ", " + (resident == null ? "VACANT" : resident.toString()) + "}";
    }
}
