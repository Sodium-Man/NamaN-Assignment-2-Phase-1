package app;

import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class CareHome implements Serializable {
    private static CareHome instance; // Singleton for single system instance (SOLID: Single Responsibility)
    private static final long serialVersionUID = 1L; // For serialization compatibility

    private final List<Ward> wards;
    private final List<Staff> staff;
    private final List<Resident> residents;
    private final List<LogEntry> logs; // Unified logging with LogEntry
    private final Schedule schedule;
    private final Map<String, Prescription> prescriptions; // residentId -> Prescription

    // Private constructor for Singleton pattern
    private CareHome() {
        wards = new ArrayList<>();
        staff = new ArrayList<>();
        residents = new ArrayList<>();
        logs = new ArrayList<>();
        schedule = new Schedule();
        prescriptions = new HashMap<>();
    }

    // Singleton access method
    public static CareHome getInstance() {
        if (instance == null) {
            instance = new CareHome();
        }
        return instance;
    }

    // Ward management
    public void addWard(Ward ward) {
        wards.add(ward);
        log("SYSTEM", "Added ward " + ward.getWardId());
    }

    public List<Ward> getWards() {
        return new ArrayList<>(wards); // Defensive copy for encapsulation
    }

    // Staff management
    public void addStaff(Staff s) {
        staff.add(s);
        log("SYSTEM", "Added staff " + s.getStaffId());
    }

    public List<Staff> getStaff() {
        return new ArrayList<>(staff); // Defensive copy
    }

    public void modifyStaffPassword(String staffId, String newPassword) throws Exception {
        Staff s = getStaffById(staffId);
        s.setPassword(newPassword);
        log(staffId, "Modified password for staff " + staffId);
    }

    // Resident management
    public void addResident(Resident r) {
        r.setAdmissionDate(LocalDate.now());
        residents.add(r);
        log("SYSTEM", "Added resident " + r.getResidentId());
    }

    public List<Resident> getResidents() {
        return new ArrayList<>(residents); // Defensive copy
    }

    // Logging
    public List<LogEntry> getLogs() {
        return new ArrayList<>(logs); // Defensive copy
    }

    public void log(String staffId, String action) {
        logs.add(new LogEntry(staffId, action, LocalDateTime.now()));
    }

    // Schedule access
    public Schedule getSchedule() {
        return schedule;
    }

    // Bed assignment
    public void assignResidentToBed(String staffId, Resident resident, String bedId) throws Exception {
        Staff s = getStaffById(staffId);
        checkAuthorization(s, Role.MANAGER, Role.NURSE);
        checkOnDuty(s);

        Bed bed = findBed(bedId);
        if (bed == null) {
            throw new Exception("Bed " + bedId + " not found.");
        }
        if (!bed.isVacant()) {
            throw new Exception("Bed " + bedId + " is already occupied.");
        }
        // TODO: Add gender/condition/isolation checks per PDF
        bed.assignResident(resident);
        log(staffId, "Assigned resident " + resident.getResidentId() + " to bed " + bedId);
    }

    // Move resident
    public void moveResident(String staffId, String fromBedId, String toBedId) throws Exception {
        Staff s = getStaffById(staffId);
        checkAuthorization(s, Role.MANAGER, Role.NURSE);
        checkOnDuty(s);

        Bed fromBed = findBed(fromBedId);
        Bed toBed = findBed(toBedId);
        if (fromBed == null || toBed == null) {
            throw new Exception("Bed not found: " + (fromBed == null ? fromBedId : toBedId));
        }
        if (fromBed.isVacant()) {
            throw new Exception("No resident in bed " + fromBedId);
        }
        if (!toBed.isVacant()) {
            throw new Exception("Target bed " + toBedId + " is already occupied.");
        }
        Resident r = fromBed.getResident();
        fromBed.removeResident();
        // TODO: Add gender/condition/isolation checks per PDF
        toBed.assignResident(r);
        log(staffId, "Moved resident " + r.getResidentId() + " from bed " + fromBedId + " to " + toBedId);
    }

    // Discharge resident
    public void dischargeResident(String staffId, String residentId) throws Exception {
        Staff s = getStaffById(staffId);
        checkAuthorization(s, Role.MANAGER, Role.NURSE);
        checkOnDuty(s);
        Resident r = getResidentById(residentId);
        r.setDischargeDate(LocalDate.now());
        Prescription p = prescriptions.get(residentId);
        // Remove from bed
        Bed bed = findBedByResident(residentId);
        if (bed != null) bed.removeResident();
        residents.remove(r);
        prescriptions.remove(residentId);
        // Archive to DB
        DBManager.archiveResident(r);
        if (p != null) {
            DBManager.archivePrescription(residentId, p);
            for (Administration a : p.getAllAdministrations()) {
                DBManager.archiveAdministration(residentId, a);
            }
        }
        log(staffId, "Discharged resident " + residentId + " and archived to DB");
    }

    // Prescription management
    public void addPrescription(String doctorId, String residentId, Medicine medicine, String dose, LocalTime time) throws Exception {
        Staff s = getStaffById(doctorId);
        checkAuthorization(s, Role.DOCTOR);
        checkOnDuty(s);
        Resident r = getResidentById(residentId);
        Prescription p = prescriptions.computeIfAbsent(residentId, k -> new Prescription(residentId));
        p.addItem(medicine, dose, time);
        log(doctorId, "Added prescription for resident " + residentId);
    }

    public Prescription getPrescription(String residentId) {
        return prescriptions.get(residentId); // Consider defensive copy if modified
    }

    // Medication administration
    public void recordAdministration(String nurseId, String residentId, Medicine medicine) throws Exception {
        Staff s = getStaffById(nurseId);
        checkAuthorization(s, Role.NURSE);
        checkOnDuty(s);
        Prescription p = prescriptions.get(residentId);
        if (p == null) {
            throw new Exception("No prescription exists for resident " + residentId);
        }
        p.recordAdministration(medicine, LocalDateTime.now());
        log(nurseId, "Administered medicine " + medicine.getName() + " to resident " + residentId);
    }

    // Compliance check
    public void checkCompliance() throws Exception {
        schedule.checkCompliance(); // Delegates to Schedule for nurse/doctor rules
    }

    // Find bed by ID
    public Bed findBed(String bedId) {
        for (Ward w : wards) {
            for (Room r : w.getRooms()) {
                for (Bed b : r.getBeds()) {
                    if (b.getBedId().equals(bedId)) {
                        return b;
                    }
                }
            }
        }
        return null;
    }

    // Find bed by resident
    public Bed findBedByResident(String residentId) {
        for (Ward w : wards) {
            for (Room r : w.getRooms()) {
                for (Bed b : r.getBeds()) {
                    if (b.getResident() != null && b.getResident().getResidentId().equals(residentId)) {
                        return b;
                    }
                }
            }
        }
        return null;
    }

    // Authorization check
    private void checkAuthorization(Staff s, Role... allowedRoles) throws UnauthorizedActionException {
        for (Role role : allowedRoles) {
            if (s.getRole() == role) {
                return;
            }
        }
        throw new UnauthorizedActionException("Staff " + s.getStaffId() + " not authorized for this action");
    }

    // Duty check
    private void checkOnDuty(Staff s) throws NotOnDutyException {
        if (s.getRole() == Role.MANAGER) {
            return; // Managers are always on duty (fixes M1 error)
        }
        if (!schedule.isOnDuty(s, LocalDate.now().getDayOfWeek(), LocalTime.now())) {
            throw new NotOnDutyException("Staff " + s.getStaffId() + " is not on duty at this time");
        }
    }

    // Serialization methods
    public void saveData(File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
        }
    }

    public static CareHome loadData(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            instance = (CareHome) ois.readObject();
            return instance;
        }
    }

    // Public access to staff and resident by ID for GUI
    public Staff getStaffById(String staffId) throws Exception {
        return staff.stream()
                .filter(s -> s.getStaffId().equals(staffId))
                .findFirst()
                .orElseThrow(() -> new Exception("Staff " + staffId + " not found"));
    }

    public Resident getResidentById(String residentId) throws Exception {
        return residents.stream()
                .filter(r -> r.getResidentId().equals(residentId))
                .findFirst()
                .orElseThrow(() -> new Exception("Resident " + residentId + " not found"));
    }

    // LogEntry inner class
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