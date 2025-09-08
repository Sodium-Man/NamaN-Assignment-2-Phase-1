package app;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public class SampleData {
    public static void bootstrapBeds(CareHome ch) {
        // Two wards, each with 6 rooms and 1-4 beds
        for (int w = 1; w <= 2; w++) {
            Ward ward = new Ward("W" + w, "Ward-" + w); // Added ward name parameter
            for (int r = 1; r <= 6; r++) {
                Room room = new Room("W" + w + "R" + r);
                int beds = 1 + (r % 4);
                for (int b = 1; b <= beds; b++) {
                    room.getBeds().add(new Bed(room.getRoomId() + "B" + b));
                }
                ward.addRoom(room); // Use addRoom method instead of getRooms().add()
            }
            ch.addWard(ward); // Use addWard method instead of getWards().add()
        }
    }

    public static void bootstrapPeople(CareHome ch) {
        // Manager
        ch.addStaff(new Manager("M1", "Manager Mary", Gender.F, "mary", "pass"));
        // Nurses
        ch.addStaff(new Nurse("N1", "Nurse Nina", Gender.F, "nina", "pass"));
        ch.addStaff(new Nurse("N2", "Nurse Noel", Gender.M, "noel", "pass"));
        // Doctor
        ch.addStaff(new Doctor("D1", "Doctor Dan", Gender.M, "dan", "pass"));

        // Residents
        ch.addResident(new Resident("R1", "Alice", Gender.F, "Diabetes"));
        ch.addResident(new Resident("R2", "Bob", Gender.M, "Asthma"));
    }

    public static void bootstrapSchedule(CareHome ch) {
        Schedule sched = ch.getSchedule();
        List<Staff> staffList = ch.getStaff();

        Nurse n1 = (Nurse) staffList.stream()
                .filter(s -> s instanceof Nurse && s.getStaffId().equals("N1"))
                .findFirst()
                .orElse(null);

        Nurse n2 = (Nurse) staffList.stream()
                .filter(s -> s instanceof Nurse && s.getStaffId().equals("N2"))
                .findFirst()
                .orElse(null);

        for (DayOfWeek day : DayOfWeek.values()) {
            if (n1 != null) {
                sched.assignNurseShift(n1, new Shift(day, LocalTime.of(8, 0), LocalTime.of(16, 0)));
            }
            if (n2 != null) {
                sched.assignNurseShift(n2, new Shift(day, LocalTime.of(14, 0), LocalTime.of(22, 0)));
            }
            sched.setDoctorPresent(day, true);
        }
    }
}