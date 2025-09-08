package app;

import java.io.File;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static final File DATA_FILE = new File("C:\\Users\\bhala\\Desktop\\NamaN Assignment 2 Phase 1\\data\\carehome.dat");

    public static void main(String[] args) {
        CareHome ch = CareHome.getInstance();
        if (ch.getWards().isEmpty()) {
            SampleData.bootstrapBeds(ch);
            SampleData.bootstrapPeople(ch);
            SampleData.bootstrapSchedule(ch);
        }

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("=== Resident HealthCare System (Phase 1) ===");
            System.out.println("1. List beds");
            System.out.println("2. Add resident");
            System.out.println("3. Assign resident to bed");
            System.out.println("4. Move resident");
            System.out.println("5. Check compliance");
            System.out.println("6. Save");
            System.out.println("7. Load");
            System.out.println("8. View schedule");
            System.out.println("9. Assign nurse shift");
            System.out.println("10. Set doctor availability");
            System.out.println("0. Exit");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> listBeds(ch);
                    case "2" -> addResident(ch, sc);
                    case "3" -> assignResident(ch, sc);
                    case "4" -> moveResident(ch, sc);
                    case "5" -> {
                        ch.checkCompliance();
                        System.out.println("Compliance OK.");
                    }
                    case "6" -> {
                        ch.saveData(DATA_FILE);
                        System.out.println("Saved to " + DATA_FILE.getAbsolutePath());
                    }
                    case "7" -> {
                        CareHome.loadData(DATA_FILE);
                        System.out.println("Loaded from " + DATA_FILE.getAbsolutePath());
                    }
                    case "0" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
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
}
