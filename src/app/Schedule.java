package app;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

public class Schedule implements Serializable {
    private final Map<String, List<Shift>> shifts; // staffId -> list of shifts
    private final Map<DayOfWeek, Boolean> doctorAvailability; // day -> is doctor present

    public Schedule() {
        this.shifts = new HashMap<>();
        this.doctorAvailability = new HashMap<>();
        // Default all days to false (no doctor)
        for (DayOfWeek day : DayOfWeek.values()) {
            doctorAvailability.put(day, false);
        }
    }

    public void assignShift(Staff staff, Shift shift) {
        shifts.computeIfAbsent(staff.getStaffId(), k -> new ArrayList<>()).add(shift);
    }

    public void assignNurseShift(Nurse nurse, Shift shift) {
        assignShift(nurse, shift);
    }

    public void assignDoctorShift(Doctor doctor, Shift shift) {
        assignShift(doctor, shift);
    }

    public List<Shift> getShiftsForStaff(Staff staff) {
        return shifts.getOrDefault(staff.getStaffId(), new ArrayList<>());
    }

    /**
     * Check if a staff member is currently on duty at the given day+time.
     */
    public boolean isOnDuty(Staff staff, DayOfWeek day, LocalTime time) {
        List<Shift> staffShifts = getShiftsForStaff(staff);
        for (Shift s : staffShifts) {
            if (s.getDay() == day &&
                    !time.isBefore(s.getStart()) &&
                    !time.isAfter(s.getEnd())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enforce compliance rules (e.g., maximum shift length).
     * Throws Exception if rules are violated.
     */
    public void checkCompliance() throws Exception {
        for (Map.Entry<String, List<Shift>> entry : shifts.entrySet()) {
            for (Shift s : entry.getValue()) {
                if (s.getHours() > 8) {
                    throw new Exception("Compliance violation: shift longer than 8 hours for staff " + entry.getKey());
                }
            }
        }
    }

    /**
     * Return all shifts assigned to nurses
     */
    public List<Shift> getAllNurseShifts() {
        List<Shift> nurseShifts = new ArrayList<>();
        for (Map.Entry<String, List<Shift>> entry : shifts.entrySet()) {
            for (Shift s : entry.getValue()) {
                // Find nurse by ID and type
                // Assuming staff type info is maintained elsewhere; here we just return all shifts
                nurseShifts.add(s);
            }
        }
        return nurseShifts;
    }

    /**
     * Set doctor availability for a specific day
     */
    public void setDoctorPresent(DayOfWeek day, boolean present) {
        doctorAvailability.put(day, present);
    }

    /**
     * Check if doctor is available for a specific day
     */
    public boolean isDoctorPresent(DayOfWeek day) {
        return doctorAvailability.getOrDefault(day, false);
    }

    /**
     * Optional helper: return all shifts
     */
    public List<Shift> getAllShifts() {
        List<Shift> all = new ArrayList<>();
        for (List<Shift> list : shifts.values()) {
            all.addAll(list);
        }
        return all;
    }
}
