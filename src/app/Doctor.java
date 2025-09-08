package app;

public class Doctor extends Staff {
    public Doctor(String staffId, String name, Gender gender, String username, String password) {
        super(staffId, name, gender, username, password, Role.DOCTOR);
    }

    @Override
    public void displayInfo() {
        System.out.println("Doctor: " + name + " (ID: " + staffId + ")");
    }
}
