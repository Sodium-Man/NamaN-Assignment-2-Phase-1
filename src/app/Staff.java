package app;

import java.io.Serializable;
import java.util.Objects;

public abstract class Staff implements Serializable {
    protected final String staffId;
    protected String name;
    protected String username;
    protected String password;
    protected Gender gender;
    protected final Role role;

    protected Staff(String staffId, String name, Gender gender, String username, String password, Role role) {
        this.staffId = Objects.requireNonNull(staffId);
        this.name = Objects.requireNonNull(name);
        this.username = Objects.requireNonNull(username);
        this.password = Objects.requireNonNull(password);
        this.gender = Objects.requireNonNull(gender);  // FIXED: changed this.gender to gender
        this.role = Objects.requireNonNull(role);
    }

    public String getStaffId() { return staffId; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public Gender getGender() { return gender; }
    public Role getRole() { return role; }

    public void setPassword(String password) { this.password = password; }
    public abstract void displayInfo();

    @Override
    public String toString() {
        return role + " " + name + " (ID: " + staffId + ")";
    }
}