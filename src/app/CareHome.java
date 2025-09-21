package app;

import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class CareHome implements Serializable {
    private static CareHome instance;

    private final List<Ward> wards;
    private final List<Staff> staff;
    private final List<Resident> residents;
    private final List<LogEntry> logs;
    private final Schedule schedule;
    private final Map<String, Prescription> prescriptions; // residentId -> prescription

    private CareHome() {
        wards = new ArrayList<>();
        staff = new ArrayList<>();
        residents = new ArrayList<>();
        logs = new ArrayList<>();
        schedule = new Schedule();
        prescriptions = new HashMap<>();
    }

    public static CareHome getInstance() {
        if (instance == null) {
            instance = new CareHome();
        }
        return instance;
    }

    // ---------- Basic Management ----------

    public void addWard(Ward ward) {
        wards.add(ward);
    }

    public List<Ward> getWards() {
        return wards;
    }

    public void addStaff(Staff s) {
        staff.add(s);
    }

    public List<Staff> getStaff() {
        return staff;
    }

    public void addResident(Resident r) {
        residents.add(r);
    }

    public List<Resident> getResidents() {
        return residents;
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void log(String staffId, String action) {
        logs.add(new LogEntry(staffId, action, LocalDateTime.now()));
    }

    // ---------- Resident & Bed Management ----------

    public void assignResidentToBed(String staffId, Resident resident, String bedId) throws Exception {
        Staff s = getStaffById(staffId);
        checkAuthorization(s, Role.MANAGER, Role.NURSE);

        Bed bed = findBed(bedId);
        if (!bed.isVacant()) {
            throw new Exception("Bed " + bedId + " is already occupied.");
        }
        bed.assignResident(resident);
        log(staffId, "Assigned resident " + resident.getResidentId() + " to bed " + bedId);
    }

    public void moveResident(String staffId, String fromBedId, String toBedId) throws Exception {
        Staff s = getStaffById(staffId);
        checkAuthorization(s, Role.MANAGER, Role.NURSE);

        Bed from = findBed(fromBedId);
        Bed to = findBed(toBedId);

        if (from.getResident() == null) {
            throw new Exception("No resident in bed " + fromBedId);
        }
        if (!to.isVacant()) {
            throw new Exception("Bed " + toBedId + " already occupied");
        }

        Resident r = from.getResident();
        from.removeResident();
        to.assignResident(r);

        log(staffId, "Moved resident " + r.getResidentId() + " from " + fromBedId + " to " + toBedId);
    }

    public Resident viewResidentDetails(String staffId, String bedId) throws Exception {
        Staff s = getStaffById(staffId);
        checkAuthorization(s, Role.DOCTOR, Role.NURSE, Role.MANAGER);

        Bed bed = findBed(bedId);
        if (bed.getResident() == null) {
            throw new Exception("No resident in bed " + bedId);
        }
        log(staffId, "Viewed resident details for bed " + bedId);
        return bed.getResident();
    }

    // ---------- Prescription Management ----------

    public void attachPrescription(String staffId, String bedId, Prescription prescription)
            throws UnauthorizedActionException, NotOnDutyException, Exception {
        Staff s = getStaffById(staffId);
        checkAuthorization(s, Role.DOCTOR);

        checkOnDuty(s);

        Bed bed = findBed(bedId);
        if (bed.getResident() == null) {
            throw new Exception("No resident in bed " + bedId);
        }

        prescriptions.put(bed.getResident().getResidentId(), prescription);
        log(staffId, "Attached prescription for resident " + bed.getResident().getResidentId());
    }

    public void updatePrescription(String staffId, String residentId, Medicine med, String dose, LocalTime time)
            throws UnauthorizedActionException, NotOnDutyException, Exception {
        Staff s = getStaffById(staffId);
        checkAuthorization(s, Role.DOCTOR);

        checkOnDuty(s);

        Prescription p = prescriptions.get(residentId);
        if (p == null) {
            throw new IllegalArgumentException("No prescription found for resident " + residentId);
        }
        p.addItem(med, dose, time);
        log(staffId, "Updated prescription for resident " + residentId + " with " + med.getName());
    }

    public void administerPrescription(String staffId, String residentId, Medicine med, String dose)
            throws UnauthorizedActionException, NotOnDutyException, Exception {
        Staff s = getStaffById(staffId);
        checkAuthorization(s, Role.NURSE, Role.DOCTOR);

        checkOnDuty(s);

        Prescription p = prescriptions.get(residentId);
        if (p == null) {
            throw new IllegalArgumentException("No prescription for resident " + residentId);
        }

        LocalTime now = LocalTime.now();
        log(staffId, "Administered " + dose + " of " + med.getName() + " to resident " + residentId + " at " + now);
    }

    // ---------- Helpers ----------

    private Staff getStaffById(String staffId) throws Exception {
        return staff.stream()
                .filter(s -> s.getStaffId().equals(staffId))
                .findFirst()
                .orElseThrow(() -> new Exception("Staff not found: " + staffId));
    }

    private Bed findBed(String bedId) throws Exception {
        for (Ward w : wards) {
            for (Room r : w.getRooms()) {
                for (Bed b : r.getBeds()) {
                    if (b.getBedId().equals(bedId)) return b;
                }
            }
        }
        throw new Exception("Bed not found: " + bedId);
    }

    private void checkAuthorization(Staff s, Role... allowed) throws UnauthorizedActionException {
        for (Role role : allowed) {
            if (s.getRole() == role) return;
        }
        throw new UnauthorizedActionException("Staff " + s.getStaffId() + " not authorized for this action.");
    }

    private void checkOnDuty(Staff s) throws NotOnDutyException {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek today = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();
        if (!schedule.isOnDuty(s, today, time)) {
            throw new NotOnDutyException("Staff " + s.getStaffId() + " is not on duty at this time.");
        }
    }

    // ---------- Compliance ----------

    public void checkCompliance() throws Exception {
        schedule.checkCompliance();
    }

    // ---------- Save/Load ----------

    public void saveData(File file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(this);
        }
    }

    public static CareHome loadData(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            instance = (CareHome) in.readObject();
            return instance;
        }
    }

    // ---------- LogEntry Inner Class ----------

    public static class LogEntry implements Serializable {
        private final String staffId;
        private final String action;
        private final LocalDateTime timestamp;

        public LogEntry(String staffId, String action, LocalDateTime timestamp) {
            this.staffId = staffId;
            this.action = action;
            this.timestamp = timestamp;
        }

        public String getStaffId() {
            return staffId;
        }

        public String getAction() {
            return action;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "[" + timestamp + "] Staff: " + staffId + " -> " + action;
        }
    }
}
