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
        Manager manager1 = new Manager("M1", "Rhea", Gender.F, "rhea", "pass");
        Manager manager2 = new Manager("M2", "Josh", Gender.M, "josh", "pass");

        Nurse nurse1 = new Nurse("N1", "Cathy", Gender.M, "cathy", "pass");
        Nurse nurse2 = new Nurse("N2", "Bella", Gender.M, "bella", "pass");
        Nurse nurse3 = new Nurse("N3", "Helene", Gender.M, "helene", "pass");
        Nurse nurse4 = new Nurse("N4", "Dane", Gender.M, "dane", "pass");



        Doctor doctor1 = new Doctor("D1", "Jax", Gender.M, "jax", "pass");
        Doctor doctor2 = new Doctor("D2", "Vaik", Gender.M, "vaik", "pass");
        Doctor doctor3 = new Doctor("D3", "Khan", Gender.M, "khan", "pass");
        Doctor doctor4 = new Doctor("D4", "Gary", Gender.M, "gary", "pass");
        Doctor doctor5 = new Doctor("D5", "Henry", Gender.M, "henry", "pass");
        Doctor doctor6 = new Doctor("D6", "Jay", Gender.M, "jay", "pass");
        Doctor doctor7 = new Doctor("D7", "Neil", Gender.M, "neil", "pass");


        careHome.addStaff(manager1);
        careHome.addStaff(manager2);

        careHome.addStaff(nurse1);
        careHome.addStaff(nurse2);
        careHome.addStaff(nurse3);
        careHome.addStaff(nurse4);



        careHome.addStaff(doctor1);
        careHome.addStaff(doctor2);
        careHome.addStaff(doctor3);
        careHome.addStaff(doctor4);
        careHome.addStaff(doctor5);
        careHome.addStaff(doctor6);
        careHome.addStaff(doctor7);


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
