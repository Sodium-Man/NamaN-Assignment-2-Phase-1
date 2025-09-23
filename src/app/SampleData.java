package app;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class SampleData {

    // Bootstrap wards, rooms, beds
    public static void bootstrapBeds(CareHome careHome) {
        Ward ward1 = new Ward("W1", "General Ward");

        Room room1 = new Room("R1");
        Room room2 = new Room("R2");

        // Add beds to rooms
        room1.getBeds().add(new Bed("B1"));
        room1.getBeds().add(new Bed("B2"));
        room2.getBeds().add(new Bed("B3"));
        room2.getBeds().add(new Bed("B4"));

        // Add rooms to ward
        ward1.getRooms().add(room1);
        ward1.getRooms().add(room2);

        careHome.addWard(ward1);
    }

    // Bootstrap sample staff and residents
    public static void bootstrapPeople(CareHome careHome) {
        // Staff
        Manager manager = new Manager("M1", "Rhea", Gender.F, "rhea", "pass");
        Nurse nurse = new Nurse("N1", "Cathy", Gender.M, "cathy", "pass");
        Doctor doctor = new Doctor("D1", "Jax", Gender.M, "jax", "pass");

        careHome.addStaff(manager);
        careHome.addStaff(nurse);
        careHome.addStaff(doctor);

        // Residents
        Resident res1 = new Resident("R1", "Peter Patel", Gender.M, "Hypertension");
        Resident res2 = new Resident("R2", "Naman Patel", Gender.F, "Diabetes");

        careHome.addResident(res1);
        careHome.addResident(res2);
    }

    // Bootstrap shifts for staff
    public static void bootstrapSchedule(CareHome careHome) throws Exception {
        Schedule sched = careHome.getSchedule();

        // Assign shifts for Nurse
        Nurse nurse = (Nurse) careHome.getStaff().stream()
                .filter(s -> s.getRole() == Role.NURSE)
                .findFirst()
                .orElseThrow(() -> new Exception("No nurse found"));
        sched.assignNurseShift(nurse, new Shift(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0)));
        sched.assignNurseShift(nurse, new Shift(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0)));

        // Assign shifts for Doctor
        Doctor doctor = (Doctor) careHome.getStaff().stream()
                .filter(s -> s.getRole() == Role.DOCTOR)
                .findFirst()
                .orElseThrow(() -> new Exception("No doctor found"));
        sched.assignDoctorShift(doctor, new Shift(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        sched.assignDoctorShift(doctor, new Shift(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));
    }
}
