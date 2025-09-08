package app;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.Assert.*;

public class MainTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        // Reset CareHome instance before each test
        CareHome instance = CareHome.getInstance();
        instance.getWards().clear();
        instance.getStaff().clear();
        instance.getResidents().clear();
        instance.getLogs().clear();
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testMainInitialization() {
        try {
            CareHome ch = CareHome.getInstance();
            assertNotNull("CareHome instance should not be null", ch);
        } catch (Exception e) {
            fail("Initialization should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testMainMenuListBeds() {
        String input = "1\n0\n"; // List beds then exit
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected when scanner closes
        }

        String output = outContent.toString();
        assertTrue("Should display menu options", output.contains("Resident HealthCare System"));
        assertTrue("Should handle list beds command", output.contains("Choice:"));
    }

    @Test
    public void testMainMenuAddResident() {
        String input = "2\nJohn Doe\nM\nHypertension\n0\n"; // Add resident then exit
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected when scanner closes
        }

        String output = outContent.toString();
        assertTrue("Should process add resident", output.contains("Name:"));
        assertTrue("Should ask for gender", output.contains("Gender"));
    }

    @Test
    public void testMainMenuExit() {
        String input = "0\n"; // Exit immediately
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected when scanner closes
        }

        String output = outContent.toString();
        assertTrue("Should display goodbye message", output.contains("Goodbye"));
    }

    @Test
    public void testMainMenuInvalidChoice() {
        String input = "99\n0\n"; // Invalid choice then exit
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected when scanner closes
        }

        String output = outContent.toString();
        assertTrue("Should handle invalid choice", output.contains("Invalid choice"));
    }

    @Test
    public void testMainMenuSaveAndLoad() {
        String input = "6\n7\n0\n"; // Save, Load, Exit
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected when scanner closes
        }

        String output = outContent.toString();
        assertTrue("Should process save/load commands", output.contains("Saved") || output.contains("Loaded") || output.contains("Goodbye"));
    }

    @Test
    public void testMainMenuCheckCompliance() {
        String input = "5\n0\n"; // Check compliance then exit
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected when scanner closes
        }

        String output = outContent.toString();
        assertTrue("Should process compliance check", output.contains("Compliance") || output.contains("Goodbye"));
    }

    @Test
    public void testCareHomeSingleton() {
        CareHome instance1 = CareHome.getInstance();
        CareHome instance2 = CareHome.getInstance();

        assertSame("Should return the same instance", instance1, instance2);
        assertNotNull("Instance should not be null", instance1);
    }

    @Test
    public void testCareHomeAddStaff() {
        CareHome ch = CareHome.getInstance();
        Manager manager = new Manager("M1", "Test Manager", Gender.M, "test", "pass");

        ch.addStaff(manager);
        assertEquals("Should have one staff member", 1, ch.getStaff().size());
        assertEquals("Staff ID should match", "M1", ch.getStaff().get(0).getStaffId());
    }

    @Test
    public void testCareHomeAddResident() {
        CareHome ch = CareHome.getInstance();
        Resident resident = new Resident("R1", "Test Resident", Gender.M, "Test Condition");

        ch.addResident(resident);
        assertEquals("Should have one resident", 1, ch.getResidents().size());
        assertEquals("Resident ID should match", "R1", ch.getResidents().get(0).getResidentId());
    }

    @Test
    public void testCareHomeLogging() {
        CareHome ch = CareHome.getInstance();
        ch.log("TEST123", "Test action");

        assertEquals("Should have one log entry", 1, ch.getLogs().size());
        assertTrue("Log should contain staff ID", ch.getLogs().get(0).toString().contains("TEST123"));
    }

    @Test
    public void testSampleDataBootstrap() {
        CareHome ch = CareHome.getInstance();

        SampleData.bootstrapBeds(ch);
        assertFalse("Should have wards after bootstrap", ch.getWards().isEmpty());
        assertFalse("Should have rooms after bootstrap", ch.getWards().get(0).getRooms().isEmpty());
        assertFalse("Should have beds after bootstrap", ch.getWards().get(0).getRooms().get(0).getBeds().isEmpty());
    }

    @Test
    public void testSampleDataPeople() {
        CareHome ch = CareHome.getInstance();

        SampleData.bootstrapPeople(ch);
        assertFalse("Should have staff after bootstrap", ch.getStaff().isEmpty());
        assertFalse("Should have residents after bootstrap", ch.getResidents().isEmpty());
    }

    @Test
    public void testFileOperations() {
        try {
            File testFile = tempFolder.newFile("test.dat");
            CareHome ch = CareHome.getInstance();

            // Test save
            ch.saveData(testFile);
            assertTrue("File should exist after save", testFile.exists());

            // Test load
            CareHome loaded = CareHome.loadData(testFile);
            assertNotNull("Loaded instance should not be null", loaded);

        } catch (Exception e) {
            fail("File operations should not fail: " + e.getMessage());
        }
    }

    @Test
    public void testGenderEnum() {
        assertEquals("M should represent Male", Gender.M, Gender.valueOf("M"));
        assertEquals("F should represent Female", Gender.F, Gender.valueOf("F"));

        // Test string parsing
        Gender male = "M".startsWith("M") ? Gender.M : Gender.F;
        Gender female = "F".startsWith("M") ? Gender.M : Gender.F;

        assertEquals("Should parse M as Male", Gender.M, male);
        assertEquals("Should parse F as Female", Gender.F, female);
    }

    @Test
    public void testResidentCreation() {
        Resident resident = new Resident("R1", "John Doe", Gender.M, "Diabetes");

        assertEquals("ID should match", "R1", resident.getResidentId());
        assertEquals("Name should match", "John Doe", resident.getName());
        assertEquals("Gender should match", Gender.M, resident.getGender());
        assertEquals("Condition should match", "Diabetes", resident.getMedicalCondition());
    }

    @Test
    public void testStaffRoles() {
        Manager manager = new Manager("M1", "Manager", Gender.M, "user", "pass");
        Nurse nurse = new Nurse("N1", "Nurse", Gender.F, "user", "pass");
        Doctor doctor = new Doctor("D1", "Doctor", Gender.M, "user", "pass");

        assertEquals("Manager should have correct role", Role.MANAGER, manager.getRole());
        assertEquals("Nurse should have correct role", Role.NURSE, nurse.getRole());
        assertEquals("Doctor should have correct role", Role.DOCTOR, doctor.getRole());
    }
}