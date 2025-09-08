package app;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static final File DATA_FILE = new File("C:\\Users\\bhala\\Desktop\\NamaN Assignment 2 Phase 1\\data\\carehome.dat");

    public static void main(String[] args) {
        // Create data directory if it doesn't exist
        DATA_FILE.getParentFile().mkdirs();

        CareHome ch = CareHome.getInstance();
        if (ch.getWards().isEmpty()) {
            SampleData.bootstrapBeds(ch);
            SampleData.bootstrapPeople(ch);
            SampleData.bootstrapSchedule(ch);
        }

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== Resident HealthCare System (Phase 1) ===");
            System.out.println("1. List beds");
            System.out.println("2. Add resident");
            System.out.println("3. Assign resident to bed");
            System.out.println("4. Move resident");
            System.out.println("5. Check compliance");
            System.out.println("6. Save data");
            System.out.println("7. Load data");
            System.out.println("8. View schedule");
            System.out.println("9. Assign nurse shift");
            System.out.println("10. Set doctor availability");
            System.out.println("11. View action logs");
            System.out.println("0. Exit");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> listBeds(ch);
                    case "2" -> addResident(ch, sc);
                    case "3" -> assignResident(ch, sc);
                    case "4" -> moveResident(ch, sc);
                    case "5" -> checkCompliance(ch);
                    case "6" -> saveData(ch);
                    case "7" -> ch = loadData();
                    case "8" -> viewSchedule(ch);
                    case "9" -> assignNurseShift(ch, sc);
                    case "10" -> setDoctorAvailability(ch, sc);
                    case "11" -> viewLogs(ch);
                    case "0" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice. Please enter 0-11.");
                }
            } catch (Exception ex) {
                System.out.println("ERROR: " + ex.getMessage());
            }
        }
    }

    private static void listBeds(CareHome ch) {
        ch.getWards().forEach(w -> {
            System.out.println("[" + w.getWardName() + "]");
            w.getRooms().forEach(r -> {
                System.out.print("  " + r.getRoomId() + ": ");
                r.getBeds().forEach(b -> System.out.print(b + " | "));
                System.out.println();
            });
        });
    }

    private static void addResident(CareHome ch, Scanner sc) {
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Gender (M/F): ");
        String g = sc.nextLine().trim().toUpperCase();
        Gender gender = g.startsWith("M") ? Gender.M : Gender.F;
        System.out.print("Medical condition (optional): ");
        String cond = sc.nextLine().trim();
        Resident r = new Resident("R" + UUID.randomUUID().toString().substring(0, 6), name, gender, cond.isEmpty() ? null : cond);
        ch.addResident(r);
        System.out.println("Added: " + r);
    }

    private static void assignResident(CareHome ch, Scanner sc) throws Exception {
        System.out.print("Staff ID (Manager or Nurse): ");
        String staffId = sc.nextLine().trim();
        System.out.print("Resident ID: ");
        String residentId = sc.nextLine().trim();
        System.out.print("Bed ID: ");
        String bedId = sc.nextLine().trim();
        Resident r = ch.getResidents().stream().filter(x -> x.getResidentId().equals(residentId)).findFirst().orElse(null);
        if (r == null) throw new Exception("Resident not found");
        ch.assignResidentToBed(staffId, r, bedId);
        System.out.println("Assigned.");
    }

    private static void moveResident(CareHome ch, Scanner sc) throws Exception {
        System.out.print("Staff ID (Manager or Nurse): ");
        String staffId = sc.nextLine().trim();
        System.out.print("From Bed ID: ");
        String fromBed = sc.nextLine().trim();
        System.out.print("To Bed ID: ");
        String toBed = sc.nextLine().trim();
        ch.moveResident(staffId, fromBed, toBed);
        System.out.println("Moved.");
    }

    private static void checkCompliance(CareHome ch) {
        try {
            ch.checkCompliance();
            System.out.println("✓ Compliance check passed. All regulations are satisfied.");
        } catch (Exception e) {
            System.out.println("✗ COMPLIANCE VIOLATION: " + e.getMessage());
        }
    }

    private static void saveData(CareHome ch) {
        try {
            ch.saveData(DATA_FILE);
            System.out.println("✓ Data successfully saved to: " + DATA_FILE.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("ERROR: Failed to save data: " + e.getMessage());
        }
    }

    private static CareHome loadData() {
        try {
            if (!DATA_FILE.exists()) {
                System.out.println("No saved data found. Starting with fresh instance.");
                return CareHome.getInstance();
            }

            CareHome loaded = CareHome.loadData(DATA_FILE);
            System.out.println("✓ Data successfully loaded from: " + DATA_FILE.getAbsolutePath());
            return loaded;
        } catch (Exception e) {
            System.out.println("ERROR: Failed to load data: " + e.getMessage());
            return CareHome.getInstance();
        }
    }

    private static void viewSchedule(CareHome ch) {
        Schedule schedule = ch.getSchedule();
        System.out.println("\n=== Current Schedule ===");

        // Display nurse shifts
        System.out.println("Nurse Shifts:");
        if (schedule.getAllNurseShifts().isEmpty()) {
            System.out.println("  No nurse shifts assigned.");
        } else {
            schedule.getAllNurseShifts().forEach((nurseId, shifts) -> {
                System.out.println("Nurse " + nurseId + ":");
                shifts.forEach(shift -> System.out.println("  " + shift));
            });
        }

        // Display doctor availability
        System.out.println("\nDoctor Availability:");
        for (DayOfWeek day : DayOfWeek.values()) {
            System.out.println(day + ": " + (schedule.isDoctorPresent(day) ? "PRESENT" : "NOT PRESENT"));
        }
    }

    private static void assignNurseShift(CareHome ch, Scanner sc) throws Exception {
        System.out.println("\n=== Assign Nurse Shift ===");

        // List available nurses
        System.out.println("Available Nurses:");
        ch.getStaff().stream()
                .filter(s -> s instanceof Nurse)
                .forEach(nurse -> System.out.println(nurse.getStaffId() + ": " + nurse.getName()));

        System.out.print("Enter Nurse ID: ");
        String nurseId = sc.nextLine().trim();

        Nurse nurse = (Nurse) ch.getStaff().stream()
                .filter(s -> s.getStaffId().equals(nurseId) && s instanceof Nurse)
                .findFirst()
                .orElseThrow(() -> new Exception("Nurse not found: " + nurseId));

        // Get day of week
        System.out.println("Days: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY");
        System.out.print("Enter day: ");
        String dayInput = sc.nextLine().trim().toUpperCase();
        DayOfWeek day = DayOfWeek.valueOf(dayInput);

        // Get shift times
        System.out.print("Enter start time (HH:MM): ");
        String startTime = sc.nextLine().trim();
        System.out.print("Enter end time (HH:MM): ");
        String endTime = sc.nextLine().trim();

        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        // Create and assign shift
        Shift shift = new Shift(day, start, end);
        ch.getSchedule().assignNurseShift(nurse, shift);

        ch.log("SYSTEM", "Assigned shift to nurse " + nurseId + ": " + shift);
        System.out.println("✓ Shift assigned successfully!");
    }

    private static void setDoctorAvailability(CareHome ch, Scanner sc) {
        System.out.println("\n=== Set Doctor Availability ===");

        // List available doctors
        System.out.println("Available Doctors:");
        ch.getStaff().stream()
                .filter(s -> s instanceof Doctor)
                .forEach(doctor -> System.out.println(doctor.getStaffId() + ": " + doctor.getName()));

        System.out.println("Days: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY");
        System.out.print("Enter day: ");
        String dayInput = sc.nextLine().trim().toUpperCase();
        DayOfWeek day = DayOfWeek.valueOf(dayInput);

        System.out.print("Is doctor available? (Y/N): ");
        String available = sc.nextLine().trim().toUpperCase();

        boolean isPresent = available.startsWith("Y");
        ch.getSchedule().setDoctorPresent(day, isPresent);

        ch.log("SYSTEM", "Set doctor availability for " + day + ": " + (isPresent ? "PRESENT" : "NOT PRESENT"));
        System.out.println("✓ Doctor availability updated!");
    }

    private static void viewLogs(CareHome ch) {
        System.out.println("\n=== Action Logs ===");
        if (ch.getLogs().isEmpty()) {
            System.out.println("No action logs available.");
            return;
        }

        ch.getLogs().forEach(log -> System.out.println(log));
    }
}