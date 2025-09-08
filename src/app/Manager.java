package app;

public class Manager extends Staff {
    public Manager(String staffId, String name, Gender gender, String username, String password) {
        super(staffId, name, gender, username, password, Role.MANAGER);
    }

    @Override
    public void displayInfo() {
        System.out.println("Manager: " + name + " (ID: " + staffId + ")");
    }
}
