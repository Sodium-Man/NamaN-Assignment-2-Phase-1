package app;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public class SampleData {

    public static void bootstrapBeds(CareHome careHome) {
        // Ward 1
        Ward ward1 = new Ward("W1", "General Ward 1");
        addRoomsToWard(ward1);
        careHome.addWard(ward1);

        // Ward 2
        Ward ward2 = new Ward("W2", "General Ward 2");
        addRoomsToWard(ward2);
        careHome.addWard(ward2);
    }

    private static void addRoomsToWard(Ward ward) {
        Room r1 = new Room("R1"); for (int i = 1; i <= 1; i++) r1.addBed(new Bed("B" + i));
        Room r2 = new Room("R2"); for (int i = 2; i <= 3; i++) r2.addBed(new Bed("B" + i));
        Room r3 = new Room("R3"); for (int i = 4; i <= 6; i++) r3.addBed(new Bed("B" + i));
        Room r4 = new Room("R4"); for (int i = 7; i <= 10; i++) r4.addBed(new Bed("B" + i));
        Room r5 = new Room("R5"); for (int i = 11; i <= 13; i++) r5.addBed(new Bed("B" + i));
        Room r6 = new Room("R6"); for (int i = 14; i <= 15; i++) r6.addBed(new Bed("B" + i));
        ward.addRoom(r1); ward.addRoom(r2); ward.addRoom(r3); ward.addRoom(r4); ward.addRoom(r5); ward.addRoom(r6);
    }

    public static void bootstrapPeople(CareHome careHome) {
        // Managers
        careHome.addStaff(new Manager("M1", "Rhea", Gender.F, "rhea", "pass"));
        careHome.addStaff(new Manager("M2", "Josh", Gender.M, "josh", "pass"));

        // Nurses (more for coverage)
        for (int i = 1; i <= 10; i++) {
            careHome.addStaff(new Nurse("N" + i, "Nurse" + i, Gender.F, "nurse" + i, "pass"));
        }

        // Doctors (more for coverage)
        for (int i = 1; i <= 7; i++) {
            careHome.addStaff(new Doctor("D" + i, "Doctor" + i, Gender.M, "doctor" + i, "pass"));
        }

        // Residents
        careHome.addResident(new Resident("R1", "Peter Patel", Gender.M, "Hypertension"));
        careHome.addResident(new Resident("R2", "Naman Patel", Gender.F, "Diabetes"));
    }

    public static void bootstrapSchedule(CareHome careHome) throws Exception {
        Schedule sched = careHome.getSchedule();

        // Fixed nurse shifts: 8-4 and 2-10 daily
        LocalTime morningStart = LocalTime.of(8, 0);
        LocalTime morningEnd = LocalTime.of(16, 0);
        LocalTime afternoonStart = LocalTime.of(14, 0);
        LocalTime afternoonEnd = LocalTime.of(22, 0);

        // Assign nurses to shifts (example: cycle through nurses for coverage)
        List<Nurse> nurses = careHome.getStaff().stream().filter(s -> s instanceof Nurse).map(Nurse.class::cast).toList();
        for (DayOfWeek day : DayOfWeek.values()) {
            sched.assignNurseShift(nurses.get(0), new Shift(day, morningStart, morningEnd));
            sched.assignNurseShift(nurses.get(1), new Shift(day, afternoonStart, afternoonEnd));
            // Add more if needed for redundancy
        }

        // Doctor shifts: 1 hour per day (9-10)
        List<Doctor> doctors = careHome.getStaff().stream().filter(s -> s instanceof Doctor).map(Doctor.class::cast).toList();
        LocalTime doctorStart = LocalTime.of(9, 0);
        LocalTime doctorEnd = LocalTime.of(10, 0);
        int docIndex = 0;
        for (DayOfWeek day : DayOfWeek.values()) {
            sched.assignDoctorShift(doctors.get(docIndex % doctors.size()), new Shift(day, doctorStart, doctorEnd));
            sched.setDoctorPresent(day, true);
            docIndex++;
        }
    }
}