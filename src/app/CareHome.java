package app;

import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

public class CareHome implements Serializable {
    private static CareHome instance;

    private final List<Staff> staff = new ArrayList<>();
    private final List<Resident> residents = new ArrayList<>();
    private final List<Ward> wards = new ArrayList<>();
    private final List<ActionLog> logs = new ArrayList<>();
    private final Schedule schedule = new Schedule();

    private CareHome() {}

    public static CareHome getInstance() {
        if (instance == null) instance = new CareHome();
        return instance;
    }

    public void addStaff(Staff s) { staff.add(s); }
    public void addResident(Resident r) { residents.add(r); }
    public void addWard(Ward w) { wards.add(w); }

    public List<Staff> getStaff() { return staff; }
    public List<Resident> getResidents() { return residents; }
    public List<Ward> getWards() { return wards; }
    public List<ActionLog> getLogs() { return logs; }
    public Schedule getSchedule() { return schedule; }

    public void log(String staffId, String action) {
        logs.add(new ActionLog(staffId, action));
    }

    public Bed findBed(String bedId) {
        for (Ward w : wards) for (Room r : w.getRooms()) for (Bed b : r.getBeds())
            if (b.getBedId().equals(bedId)) return b;
        return null;
    }

    public void assignResidentToBed(String staffId, Resident resident, String bedId) throws Exception {
        Staff staffMember = staff.stream().filter(s -> s.getStaffId().equals(staffId)).findFirst().orElse(null);
        if (staffMember == null || staffMember.getRole() == Role.DOCTOR) {
            throw new Exception("Only Manager or Nurse can assign residents to beds.");
        }
        Bed bed = findBed(bedId);
        if (bed == null) throw new Exception("Bed not found: " + bedId);
        bed.assignResident(resident);
        log(staffId, "Assigned resident " + resident.getResidentId() + " to bed " + bedId);
    }

    public void moveResident(String staffId, String fromBedId, String toBedId) throws Exception {
        Staff staffMember = staff.stream().filter(s -> s.getStaffId().equals(staffId)).findFirst().orElse(null);
        if (staffMember == null || staffMember.getRole() == Role.DOCTOR) {
            throw new Exception("Only Manager or Nurse can move residents.");
        }
        Bed from = findBed(fromBedId);
        Bed to = findBed(toBedId);
        if (from == null || to == null) throw new Exception("Invalid bed id.");
        if (to.getResident() != null) throw new Exception("Destination bed occupied.");
        Resident r = from.getResident();
        if (r == null) throw new Exception("Source bed is vacant.");
        from.removeResident();
        to.assignResident(r);
        log(staffId, "Moved resident " + r.getResidentId() + " from " + fromBedId + " to " + toBedId);
    }

    public void checkCompliance() throws Exception {
        for (Map.Entry<String, List<Shift>> e : schedule.getAllNurseShifts().entrySet()) {
            Map<DayOfWeek, Integer> hoursPerDay = new EnumMap<>(DayOfWeek.class);
            for (Shift s : e.getValue()) {
                hoursPerDay.merge(s.getDay(), s.getHours(), Integer::sum);
            }
            for (Map.Entry<DayOfWeek, Integer> d : hoursPerDay.entrySet()) {
                if (d.getValue() > 8) {
                    throw new Exception("Compliance violation: Nurse " + e.getKey() + " exceeds 8 hours on " + d.getKey());
                }
            }
        }

        for (DayOfWeek day : DayOfWeek.values()) {
            boolean hasMorning = false, hasEvening = false;
            for (List<Shift> list : schedule.getAllNurseShifts().values()) {
                for (Shift s : list) {
                    if (s.getDay() == day && s.getStart().equals(LocalTime.of(8,0)) && s.getEnd().equals(LocalTime.of(16,0))) {
                        hasMorning = true;
                    }
                    if (s.getDay() == day && s.getStart().equals(LocalTime.of(14,0)) && s.getEnd().equals(LocalTime.of(22,0))) {
                        hasEvening = true;
                    }
                }
            }
            if (!(hasMorning && hasEvening)) {
                throw new Exception("Compliance violation: Nurse shift coverage missing on " + day);
            }
            if (!schedule.isDoctorPresent(day)) {
                throw new Exception("Compliance violation: No doctor assigned on " + day);
            }
        }
    }

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
}
