package app;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static final File DATA_FILE = new File("data/carehome.dat");

    public static void main(String[] args) {
        DATA_FILE.getParentFile().mkdirs();

        CareHome ch = CareHome.getInstance();
        if (ch.getWards().isEmpty()) {
            try {
                SampleData.bootstrapBeds(ch);
                SampleData.bootstrapPeople(ch);
                SampleData.bootstrapSchedule(ch);
            } catch (Exception e) {
                System.out.println("ERROR during bootstrap: " + e.getMessage());
            }
        }

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== Resident HealthCare System (Console) ===");
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
            System.out.println("12. List all people");
            System.out.println("13. Add staff");
            System.out.println("14. Modify staff password");
            System.out.println("15. Add prescription");
            System.out.println("16. Administer medication");
            System.out.println("17. Discharge resident");
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
                    case "12" -> listAllPeople(ch);
                    case "13" -> addStaff(ch, sc);
                    case "14" -> modifyStaffPassword(ch, sc);
                    case "15" -> addPrescription(ch, sc);
                    case "16" -> administerMedication(ch, sc);
                    case "17" -> dischargeResident(ch, sc);
                    case "0" -> {
                        System.out.println("Exiting...");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void listBeds(CareHome ch) {
        System.out.println("\n=== Beds ===");
        ch.getWards().forEach(ward -> {
            System.out.println(ward);
            ward.getRooms().forEach(room -> System.out.println("  " + room));
        });
    }

    private static void addResident(CareHome ch, Scanner sc) {
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Gender (M/F): ");
        Gender gender = Gender.valueOf(sc.nextLine().trim().toUpperCase());
        System.out.print("Medical Condition: ");
        String condition = sc.nextLine().trim();
        String id = "R" + UUID.randomUUID().toString().substring(0, 4);
        Resident r = new Resident(id, name, gender, condition);
        ch.addResident(r);
        System.out.println("✓ Resident added: " + r);
    }

    private static void assignResident(CareHome ch, Scanner sc) throws Exception {
        System.out.print("Staff ID: ");
        String staffId = sc.nextLine().trim();
        System.out.print("Resident ID: ");
        String resId = sc.nextLine().trim();
        System.out.print("Bed ID: ");
        String bedId = sc.nextLine().trim();
        Resident r = ch.getResidentById(resId);
        ch.assignResidentToBed(staffId, r, bedId);
        System.out.println("✓ Assigned!");
    }

    private static void moveResident(CareHome ch, Scanner sc) throws Exception {
        System.out.print("Staff ID: ");
        String staffId = sc.nextLine().trim();
        System.out.print("From Bed ID: ");
        String from = sc.nextLine().trim();
        System.out.print("To Bed ID: ");
        String to = sc.nextLine().trim();
        ch.moveResident(staffId, from, to);
        System.out.println("✓ Moved!");
    }

    private static void checkCompliance(CareHome ch) throws Exception {
        ch.checkCompliance();
        System.out.println("✓ Compliance check passed!");
    }

    private static void saveData(CareHome ch) throws Exception {
        ch.saveData(DATA_FILE);
        System.out.println("✓ Data saved!");
    }

    private static CareHome loadData() throws Exception {
        System.out.println("✓ Data loaded!");
        return CareHome.loadData(DATA_FILE);
    }

    private static void viewSchedule(CareHome ch) {
        System.out.println("\n=== Schedule ===");
        ch.getSchedule().getAllShifts().forEach(s -> System.out.println(s));
    }

    private static void assignNurseShift(CareHome ch, Scanner sc) throws Exception {
        System.out.print("Nurse ID: ");
        String nurseId = sc.nextLine().trim();
        Nurse nurse = (Nurse) ch.getStaffById(nurseId);
        System.out.print("Day (e.g., MONDAY): ");
        DayOfWeek day = DayOfWeek.valueOf(sc.nextLine().trim().toUpperCase());
        System.out.print("Start time (HH:MM): ");
        LocalTime start = LocalTime.parse(sc.nextLine().trim());
        System.out.print("End time (HH:MM): ");
        LocalTime end = LocalTime.parse(sc.nextLine().trim());
        ch.getSchedule().assignNurseShift(nurse, new Shift(day, start, end));
        System.out.println("✓ Shift assigned!");
    }

    private static void setDoctorAvailability(CareHome ch, Scanner sc) {
        System.out.print("Day (e.g., MONDAY): ");
        DayOfWeek day = DayOfWeek.valueOf(sc.nextLine().trim().toUpperCase());
        System.out.print("Available (Y/N): ");
        boolean available = sc.nextLine().trim().toUpperCase().startsWith("Y");
        ch.getSchedule().setDoctorPresent(day, available);
        System.out.println("✓ Availability set!");
    }

    private static void viewLogs(CareHome ch) {
        System.out.println("\n=== Action Logs ===");
        if (ch.getLogs().isEmpty()) {
            System.out.println("No logs.");
            return;
        }
        ch.getLogs().forEach(log -> System.out.println(log));
    }

    private static void listAllPeople(CareHome ch) {
        System.out.println("\n=== All People ===");
        System.out.println("\nResidents:");
        ch.getResidents().forEach(r -> System.out.println("  " + r.getResidentId() + " - " + r.getName()));
        System.out.println("\nManagers:");
        ch.getStaff().stream().filter(s -> s instanceof Manager).forEach(s -> System.out.println("  " + s.getStaffId() + " - " + s.getName()));
        System.out.println("\nNurses:");
        ch.getStaff().stream().filter(s -> s instanceof Nurse).forEach(s -> System.out.println("  " + s.getStaffId() + " - " + s.getName()));
        System.out.println("\nDoctors:");
        ch.getStaff().stream().filter(s -> s instanceof Doctor).forEach(s -> System.out.println("  " + s.getStaffId() + " - " + s.getName()));
    }

    // New methods
    private static void addStaff(CareHome ch, Scanner sc) {
        System.out.print("Role (MANAGER/DOCTOR/NURSE): ");
        Role role = Role.valueOf(sc.nextLine().trim().toUpperCase());
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Gender (M/F): ");
        Gender gender = Gender.valueOf(sc.nextLine().trim().toUpperCase());
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();
        String id = role.name().charAt(0) + UUID.randomUUID().toString().substring(0, 4);
        Staff s;
        switch (role) {
            case MANAGER -> s = new Manager(id, name, gender, username, password);
            case DOCTOR -> s = new Doctor(id, name, gender, username, password);
            case NURSE -> s = new Nurse(id, name, gender, username, password);
            default -> { System.out.println("Invalid role."); return; }
        }
        ch.addStaff(s);
        System.out.println("✓ Staff added: " + s);
    }

    private static void modifyStaffPassword(CareHome ch, Scanner sc) throws Exception {
        System.out.print("Staff ID: ");
        String id = sc.nextLine().trim();
        System.out.print("New Password: ");
        String pass = sc.nextLine().trim();
        ch.modifyStaffPassword(id, pass);
        System.out.println("✓ Password updated!");
    }

    private static void addPrescription(CareHome ch, Scanner sc) throws Exception {
        System.out.print("Doctor ID: ");
        String docId = sc.nextLine().trim();
        System.out.print("Resident ID: ");
        String resId = sc.nextLine().trim();
        System.out.print("Medicine Name: ");
        String medName = sc.nextLine().trim();
        Medicine med = new Medicine(medName);
        System.out.print("Dose: ");
        String dose = sc.nextLine().trim();
        System.out.print("Scheduled Time (HH:MM): ");
        LocalTime time = LocalTime.parse(sc.nextLine().trim());
        ch.addPrescription(docId, resId, med, dose, time);
        System.out.println("✓ Prescription added!");
    }

    private static void administerMedication(CareHome ch, Scanner sc) throws Exception {
        System.out.print("Nurse ID: ");
        String nurseId = sc.nextLine().trim();
        System.out.print("Resident ID: ");
        String resId = sc.nextLine().trim();
        System.out.print("Medicine Name: ");
        String medName = sc.nextLine().trim();
        Medicine med = new Medicine(medName);
        ch.recordAdministration(nurseId, resId, med);
        System.out.println("✓ Medication administered!");
    }

    private static void dischargeResident(CareHome ch, Scanner sc) throws Exception {
        System.out.print("Staff ID: ");
        String staffId = sc.nextLine().trim();
        System.out.print("Resident ID: ");
        String resId = sc.nextLine().trim();
        ch.dischargeResident(staffId, resId);
        System.out.println("✓ Resident discharged and archived!");
    }
}