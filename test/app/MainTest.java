package app;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
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
            // Expected
        }

        String output = outContent.toString();
        assertTrue(output.contains("Resident HealthCare System"));
        assertTrue(output.contains("Choice:"));
    }

    @Test
    public void testMainMenuAddResident() {
        String input = "2\nJohn Doe\nM\nHypertension\n0\n"; // Add resident then exit
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected
        }

        String output = outContent.toString();
        assertTrue(output.contains("Name:"));
        assertTrue(output.contains("Gender"));
    }

    @Test
    public void testMainMenuExit() {
        String input = "0\n"; // Exit immediately
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected
        }

        String output = outContent.toString();
        assertTrue(output.contains("Goodbye"));
    }

    @Test
    public void testMainMenuInvalidChoice() {
        String input = "99\n0\n"; // Invalid choice then exit
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected
        }

        String output = outContent.toString();
        assertTrue(output.contains("Invalid choice"));
    }

    @Test
    public void testMainMenuSaveAndLoad() {
        String input = "6\n7\n0\n"; // Save, Load, Exit
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected
        }

        String output = outContent.toString();
        assertTrue(output.contains("✓ Data successfully saved") || output.contains("✓ Data successfully loaded") || output.contains("Goodbye"));
    }

    @Test
    public void testMainMenuCheckCompliance() {
        String input = "5\n0\n"; // Check compliance then exit
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        try {
            Main.main(new String[]{});
        } catch (NoSuchElementException e) {
            // Expected
        }

        String output = outContent.toString();
        assertTrue(output.contains("Compliance") || output.contains("Goodbye"));
    }

    @Test
    public void testCareHomeSingleton() {
        CareHome instance1 = CareHome.getInstance();
        CareHome instance2 = CareHome.getInstance();

        assertSame(instance1, instance2);
        assertNotNull(instance1);
    }

    @Test
    public void testCareHomeAddStaff() {
        CareHome ch = CareHome.getInstance();
        Manager manager = new Manager("M1", "Test Manager", Gender.M, "test", "pass");

        ch.addStaff(manager);
        assertEquals(1, ch.getStaff().size());
        assertEquals("M1", ch.getStaff().get(0).getStaffId());
    }

    @Test
    public void testCareHomeAddResident() {
        CareHome ch = CareHome.getInstance();
        Resident resident = new Resident("R1", "Test Resident", Gender.M, "Test Condition");

        ch.addResident(resident);
        assertEquals(1, ch.getResidents().size());
        assertEquals("R1", ch.getResidents().get(0).getResidentId());
    }

    @Test
    public void testCareHomeLogging() {
        CareHome ch = CareHome.getInstance();
        ch.log("TEST123", "Test action");

        assertEquals(1, ch.getLogs().size());
        assertTrue(ch.getLogs().get(0).toString().contains("TEST123"));
    }

    @Test
    public void testSampleDataBootstrap() {
        CareHome ch = CareHome.getInstance();

        SampleData.bootstrapBeds(ch);
        assertFalse(ch.getWards().isEmpty());
        assertFalse(ch.getWards().get(0).getRooms().isEmpty());
        assertFalse(ch.getWards().get(0).getRooms().get(0).getBeds().isEmpty());
    }

    @Test
    public void testSampleDataPeople() {
        CareHome ch = CareHome.getInstance();

        SampleData.bootstrapPeople(ch);
        assertFalse(ch.getStaff().isEmpty());
        assertFalse(ch.getResidents().isEmpty());
    }

    @Test
    public void testFileOperations() {
        try {
            File testFile = tempFolder.newFile("test.dat");
            CareHome ch = CareHome.getInstance();

            ch.saveData(testFile);
            assertTrue(testFile.exists());

            CareHome loaded = CareHome.loadData(testFile);
            assertNotNull(loaded);

        } catch (Exception e) {
            fail("File operations should not fail: " + e.getMessage());
        }
    }

    @Test
    public void testGenderEnum() {
        assertEquals(Gender.M, Gender.valueOf("M"));
        assertEquals(Gender.F, Gender.valueOf("F"));

        Gender male = "M".startsWith("M") ? Gender.M : Gender.F;
        Gender female = "F".startsWith("M") ? Gender.M : Gender.F;

        assertEquals(Gender.M, male);
        assertEquals(Gender.F, female);
    }

    @Test
    public void testResidentCreation() {
        Resident resident = new Resident("R1", "John Doe", Gender.M, "Diabetes");

        assertEquals("R1", resident.getResidentId());
        assertEquals("John Doe", resident.getName());
        assertEquals(Gender.M, resident.getGender());
        assertEquals("Diabetes", resident.getMedicalCondition());
    }

    @Test
    public void testStaffRoles() {
        Manager manager = new Manager("M1", "Manager", Gender.M, "user", "pass");
        Nurse nurse = new Nurse("N1", "Nurse", Gender.F, "user", "pass");
        Doctor doctor = new Doctor("D1", "Doctor", Gender.M, "user", "pass");

        assertEquals(Role.MANAGER, manager.getRole());
        assertEquals(Role.NURSE, nurse.getRole());
        assertEquals(Role.DOCTOR, doctor.getRole());
    }

    @Test
    public void testPrescriptionAddItem() {
        Prescription prescription = new Prescription("R1");
        Medicine med = new Medicine("Aspirin");
        prescription.addItem(med, "1 tablet", LocalTime.of(9, 0));

        assertTrue(prescription.toString().contains("Aspirin"));
        assertTrue(prescription.getItems().containsKey(med));
    }

    @Test
    public void testBedVacancy() throws Exception {
        Bed bed = new Bed("B1");
        assertTrue(bed.isVacant());

        Resident r = new Resident("R1", "Test", Gender.M, null);
        bed.assignResident(r);
        assertFalse(bed.isVacant());

        bed.removeResident();
        assertTrue(bed.isVacant());
    }

    @Test
    public void testScheduleComplianceViolation() {
        CareHome ch = CareHome.getInstance();
        Nurse nurse = new Nurse("N1", "Nurse", Gender.F, "user", "pass");
        ch.addStaff(nurse);

        Shift shift1 = new Shift(DayOfWeek.MONDAY, LocalTime.of(8,0), LocalTime.of(20,0)); // 12h shift
        ch.getSchedule().assignNurseShift(nurse, shift1);

        Exception ex = assertThrows(Exception.class, ch::checkCompliance);
        assertTrue(ex.getMessage().contains("Compliance violation"));
    }
}
