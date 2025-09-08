package app;

public class Nurse extends Staff {
    public Nurse(String staffId, String name, Gender gender, String username, String password) {
        super(staffId, name, gender, username, password, Role.NURSE);
    }

    @Override
    public void displayInfo() {
        System.out.println("Nurse: " + name + " (ID: " + staffId + ")");
    }
}
