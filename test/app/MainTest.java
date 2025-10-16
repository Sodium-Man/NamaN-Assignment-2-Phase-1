package app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    private static final File DATA_FILE = new File("data/carehome_test.dat");
    private CareHome ch;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up any existing test file
        if (DATA_FILE.exists()) {
            DATA_FILE.delete();
        }
        DATA_FILE.getParentFile().mkdirs();

        // Initialize CareHome instance
        ch = CareHome.getInstance();

        // Bootstrap minimal data
        SampleData.bootstrapBeds(ch);
        SampleData.bootstrapPeople(ch);
        SampleData.bootstrapSchedule(ch);

        // Mock DBManager to avoid actual database access during tests
        try {
            DBManager.initializeDatabase(); // Ensure tables exist
        } catch (Exception e) {
            // Swallow exception if database setup fails (test isolation)
            System.err.println("Warning: Database setup failed, tests will skip DB-related operations: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        if (DATA_FILE.exists()) {
            DATA_FILE.delete();
        }
        // Clean up any temporary database if needed
        File dbFile = new File("carehome.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    void testAddResident() {
        Resident resident = new Resident("R3", "Test Resident", Gender.M, "Healthy");
        ch.addResident(resident);
        assertTrue(ch.getResidents().contains(resident), "Resident should be added to CareHome");
        assertEquals(LocalDate.now(), resident.getAdmissionDate(), "Admission date should be set to today");
    }

    @Test
    void testAssignResidentToBed() throws Exception {
        Resident resident = ch.getResidentById("R1");
        Bed bed = ch.findBed("W1-R1-B1");
        assertNotNull(bed, "Bed should exist after bootstrap");
        ch.assignResidentToBed("M1", resident, bed.getBedId());
        assertFalse(bed.isVacant(), "Bed should be occupied after assignment");
        assertEquals(resident, bed.getResident(), "Bed should contain the assigned resident");
    }

    @Test
    void testMoveResident() throws Exception {
        Resident resident = ch.getResidentById("R1");
        ch.assignResidentToBed("M1", resident, "W1-R1-B1");
        ch.moveResident("M1", "W1-R1-B1", "W1-R1-B2");
        Bed fromBed = ch.findBed("W1-R1-B1");
        Bed toBed = ch.findBed("W1-R1-B2");
        assertTrue(fromBed.isVacant(), "Source bed should be vacant after move");
        assertFalse(toBed.isVacant(), "Target bed should be occupied after move");
        assertEquals(resident, toBed.getResident(), "Target bed should contain the moved resident");
    }

    @Test
    void testAssignNurseShift() throws Exception {
        Nurse nurse = (Nurse) ch.getStaffById("N1");
        Shift shift = new Shift(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0));
        ch.getSchedule().assignNurseShift(nurse, shift);
        List<Shift> shifts = ch.getSchedule().getShiftsForStaff(nurse);
        assertTrue(shifts.contains(shift), "Nurse shift should be assigned");
    }

    @Test
    void testCheckCompliance() throws Exception {
        Nurse nurse = (Nurse) ch.getStaffById("N1");
        ch.getSchedule().assignNurseShift(nurse, new Shift(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0)));
        ch.getSchedule().checkCompliance(); // Should not throw exception
        ch.getSchedule().assignNurseShift(nurse, new Shift(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0)));
        assertThrows(Exception.class, () -> ch.getSchedule().checkCompliance(), "Should throw exception for shift > 8 hours");
    }

    @Test
    void testSaveAndLoadData() throws Exception {
        Resident resident = new Resident("R3", "Test Resident", Gender.M, "Healthy");
        ch.addResident(resident);
        ch.saveData(DATA_FILE);
        CareHome loadedCh = CareHome.loadData(DATA_FILE);
        assertEquals(ch.getResidents().size(), loadedCh.getResidents().size(), "Loaded data should match saved data");
        assertTrue(loadedCh.getResidents().contains(resident), "Loaded data should contain the added resident");
    }

    @Test
    void testDischargeResident() throws Exception {
        Resident resident = ch.getResidentById("R1");
        ch.assignResidentToBed("M1", resident, "W1-R1-B1");
        ch.dischargeResident("M1", resident.getResidentId());
        assertFalse(ch.getResidents().contains(resident), "Resident should be removed after discharge");
        Bed bed = ch.findBed("W1-R1-B1");
        assertTrue(bed.isVacant(), "Bed should be vacant after discharge");
    }

    @Test
    void testAddPrescription() throws Exception {
        Doctor doctor = (Doctor) ch.getStaffById("D1");
        Resident resident = ch.getResidentById("R1");
        Medicine medicine = new Medicine("Paracetamol");
        ch.addPrescription(doctor.getStaffId(), resident.getResidentId(), medicine, "500mg", LocalTime.of(8, 0));
        Prescription prescription = ch.getPrescription(resident.getResidentId());
        assertNotNull(prescription, "Prescription should be added");
        assertEquals(medicine.getName(), prescription.getMedicine().getName(), "Medicine name should match");
    }

    @Test
    void testAdministerMedication() throws Exception {
        Doctor doctor = (Doctor) ch.getStaffById("D1");
        Resident resident = ch.getResidentById("R1");
        Medicine medicine = new Medicine("Paracetamol");
        ch.addPrescription(doctor.getStaffId(), resident.getResidentId(), medicine, "500mg", LocalTime.of(8, 0));
        Nurse nurse = (Nurse) ch.getStaffById("N1");
        ch.recordAdministration(nurse.getStaffId(), resident.getResidentId(), medicine);
        Prescription prescription = ch.getPrescription(resident.getResidentId());
        assertFalse(prescription.getAllAdministrations().isEmpty(), "Administration should be recorded");
    }
}