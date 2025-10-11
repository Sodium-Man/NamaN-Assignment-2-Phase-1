package app;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

public class SampleData {

    public static void bootstrapBeds(CareHome careHome) {
        // Create exactly 2 wards
        Ward ward1 = new Ward("W1", "General Ward 1");
        Ward ward2 = new Ward("W2", "General Ward 2");
        addRoomsToWard(ward1, "W1");
        addRoomsToWard(ward2, "W2");
        careHome.addWard(ward1);
        careHome.addWard(ward2);
    }

    private static void addRoomsToWard(Ward ward, String wardPrefix) {
        Random random = new Random();
        for (int i = 1; i <= 6; i++) { // Exactly 6 rooms per ward
            Room room = new Room(wardPrefix + "-R" + i);
            int bedCount = random.nextInt(4) + 1; // Random 1-4 beds
            for (int j = 1; j <= bedCount; j++) {
                room.addBed(new Bed(wardPrefix + "-R" + i + "-B" + j));
            }
            ward.addRoom(room);
        }
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
            if (nurses.size() > 0) {
                sched.assignNurseShift(nurses.get(0 % nurses.size()), new Shift(day, morningStart, morningEnd));
            }
            if (nurses.size() > 1) {
                sched.assignNurseShift(nurses.get(1 % nurses.size()), new Shift(day, afternoonStart, afternoonEnd));
            }
            // Add more if needed for redundancy (adjust based on nurse count)
        }

        // Doctor shifts: 1 hour per day (9-10)
        List<Doctor> doctors = careHome.getStaff().stream().filter(s -> s instanceof Doctor).map(Doctor.class::cast).toList();
        LocalTime doctorStart = LocalTime.of(9, 0);
        LocalTime doctorEnd = LocalTime.of(10, 0);
        int docIndex = 0;
        for (DayOfWeek day : DayOfWeek.values()) {
            if (docIndex < doctors.size()) {
                sched.assignDoctorShift(doctors.get(docIndex), new Shift(day, doctorStart, doctorEnd));
                sched.setDoctorPresent(day, true);
                docIndex++;
            }
        }
    }
}