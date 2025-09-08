package app;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.util.*;
import java.time.LocalTime;

public class Schedule implements Serializable {
    private final Map<String, List<Shift>> nurseShifts = new HashMap<>();
    private final EnumMap<DayOfWeek, Boolean> doctorDaily = new EnumMap<>(DayOfWeek.class);
    private final EnumMap<DayOfWeek, LocalTime> doctorHours = new EnumMap<>(DayOfWeek.class);

    public void assignNurseShift(Nurse nurse, Shift shift) {
        nurseShifts.computeIfAbsent(nurse.getStaffId(), k -> new ArrayList<>()).add(shift);
    }

    public void setDoctorHours(DayOfWeek day, LocalTime time) {
        doctorHours.put(day, time);
    }

    public List<Shift> getNurseShifts(Nurse nurse) {
        return nurseShifts.getOrDefault(nurse.getStaffId(), Collections.emptyList());
    }

    public void setDoctorPresent(DayOfWeek day, boolean present) {
        doctorDaily.put(day, present);
    }

    public boolean isDoctorPresent(DayOfWeek day) {
        return doctorDaily.getOrDefault(day, false);
    }

    public Map<String, List<Shift>> getAllNurseShifts() {
        return nurseShifts;
    }

    public Map<DayOfWeek, Boolean> getDoctorAvailability() {
        return new EnumMap<>(doctorDaily);
    }
}
