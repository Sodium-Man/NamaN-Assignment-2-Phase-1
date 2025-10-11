package app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

public class App extends Application {
    private CareHome ch = CareHome.getInstance();
    private Staff currentStaff;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Auto-save on exit (PDF requirement)
        primaryStage.setOnCloseRequest(e -> {
            try {
                ch.saveData(new File("carehome.ser"));
            } catch (Exception ex) {
                showAlert("Save failed: " + ex.toString());
            }
        });

        if (ch.getWards().isEmpty()) {
            try {
                SampleData.bootstrapBeds(ch);
                SampleData.bootstrapPeople(ch);
                SampleData.bootstrapSchedule(ch);
            } catch (Exception e) {
                showAlert("Bootstrap error: " + e.toString());
            }
        }

        primaryStage.setScene(createLoginScene());
        primaryStage.setTitle("Resident HealthCare System");
        primaryStage.show();
    }

    private Scene createLoginScene() {
        VBox loginBox = new VBox(10);
        loginBox.setAlignment(Pos.CENTER);
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Button loginButton = new Button("Login");
        loginBox.getChildren().addAll(usernameField, passwordField, loginButton);

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            Optional<Staff> staffOpt = ch.getStaff().stream()
                    .filter(s -> s.getUsername().equals(username) && s.getPassword().equals(password))
                    .findFirst();
            if (staffOpt.isPresent()) {
                currentStaff = staffOpt.get();
                primaryStage.setScene(createMainScene());
            } else {
                showAlert("Invalid credentials");
            }
        });

        return new Scene(loginBox, 300, 200);
    }

    private Scene createMainScene() {
        TabPane tabPane = new TabPane();
        for (Ward ward : ch.getWards()) {
            GridPane grid = new GridPane();
            grid.setPadding(new Insets(10));
            grid.setHgap(10);
            grid.setVgap(10);
            int row = 0, col = 0;
            for (Room room : ward.getRooms()) {
                Label roomLabel = new Label(room.getRoomId());
                grid.add(roomLabel, col, row);
                row++;
                for (Bed bed : room.getBeds()) {
                    Button bedButton = new Button(bed.getBedId() + "\n" + (bed.isVacant() ? "VACANT" : bed.getResident().getName()));
                    if (!bed.isVacant()) {
                        Gender g = bed.getResident().getGender();
                        bedButton.setStyle(g == Gender.M ? "-fx-background-color: blue;" : "-fx-background-color: red;");
                    } else {
                        bedButton.setStyle("-fx-background-color: white;");
                    }
                    bedButton.setMinSize(100, 50);
                    bedButton.setOnAction(e -> handleBedClick(bed));
                    grid.add(bedButton, col, row);
                    row++;
                }
                col++;
                row = 0;
            }
            Tab tab = new Tab(ward.getWardName(), grid);
            tabPane.getTabs().add(tab);
        }

        // Menu bar for actions
        MenuBar menuBar = new MenuBar();
        Menu managerMenu = new Menu("Manager");
        if (currentStaff.getRole() == Role.MANAGER) {
            MenuItem addResident = new MenuItem("Add Resident");
            addResident.setOnAction(e -> addResidentGUI());
            MenuItem addStaff = new MenuItem("Add Staff");
            addStaff.setOnAction(e -> addStaffGUI());
            MenuItem modifyStaff = new MenuItem("Modify Staff Password");
            modifyStaff.setOnAction(e -> modifyStaffGUI());
            MenuItem assignShift = new MenuItem("Assign Nurse Shift");
            assignShift.setOnAction(e -> assignShiftGUI());
            managerMenu.getItems().addAll(addResident, addStaff, modifyStaff, assignShift);
        }

        Menu viewMenu = new Menu("View");
        MenuItem viewLogs = new MenuItem("View Logs");
        viewLogs.setOnAction(e -> viewLogsGUI());
        MenuItem viewSchedule = new MenuItem("View Schedule");
        viewSchedule.setOnAction(e -> viewScheduleGUI());
        MenuItem viewArchives = new MenuItem("View Archived Residents");
        viewArchives.setOnAction(e -> viewArchivesGUI());
        viewMenu.getItems().addAll(viewLogs, viewSchedule, viewArchives);

        Menu accountMenu = new Menu("Account");
        MenuItem logout = new MenuItem("Logout");
        logout.setOnAction(e -> {
            currentStaff = null;
            primaryStage.setScene(createLoginScene());
        });
        accountMenu.getItems().add(logout);

        menuBar.getMenus().addAll(managerMenu, viewMenu, accountMenu);

        VBox root = new VBox(menuBar, tabPane);
        return new Scene(root, 800, 600);
    }

    private void handleBedClick(Bed bed) {
        if (bed.isVacant()) {
            if (currentStaff.getRole() == Role.MANAGER || currentStaff.getRole() == Role.NURSE) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Assign Resident");
                dialog.setContentText("Resident ID:");
                dialog.showAndWait().ifPresent(resId -> {
                    try {
                        Resident r = ch.getResidentById(resId);
                        ch.assignResidentToBed(currentStaff.getStaffId(), r, bed.getBedId());
                        refreshGUI();
                    } catch (Exception ex) {
                        showAlert("Assignment failed: " + ex.toString());
                    }
                });
            }
        } else {
            Resident r = bed.getResident();
            Prescription p = ch.getPrescription(r.getResidentId());
            String presText = p != null ? p.getFormattedDetails() : "None";
            Alert details = new Alert(Alert.AlertType.INFORMATION);
            details.setTitle("Resident Details");
            details.setContentText(r.toString() + "\nPrescription: " + presText);
            details.show();

            if (currentStaff.getRole() == Role.DOCTOR) {
                Dialog<Void> presDialog = new Dialog<>();
                presDialog.setTitle("Add Prescription");
                GridPane grid = new GridPane();
                TextField medName = new TextField();
                TextField dose = new TextField();
                TextField time = new TextField();
                grid.add(new Label("Med Name:"), 0, 0);
                grid.add(medName, 1, 0);
                grid.add(new Label("Dose:"), 0, 1);
                grid.add(dose, 1, 1);
                grid.add(new Label("Time (HH:MM):"), 0, 2);
                grid.add(time, 1, 2);
                Button addBtn = new Button("Add");
                addBtn.setOnAction(ev -> {
                    try {
                        Medicine med = new Medicine(medName.getText());
                        LocalTime t = LocalTime.parse(time.getText());
                        ch.addPrescription(currentStaff.getStaffId(), r.getResidentId(), med, dose.getText(), t);
                        presDialog.getDialogPane().getScene().getWindow().hide();
                        refreshGUI();
                    } catch (Exception ex) {
                        showAlert("Prescription failed: " + ex.toString());
                    }
                });
                grid.add(addBtn, 1, 3);
                presDialog.getDialogPane().setContent(grid);
                presDialog.show();
            } else if (currentStaff.getRole() == Role.NURSE) {
                ChoiceDialog<String> choice = new ChoiceDialog<>("Administer Med", "Move Resident", "Discharge");
                choice.showAndWait().ifPresent(act -> {
                    try {
                        if (act.equals("Administer Med")) {
                            TextInputDialog medDialog = new TextInputDialog();
                            medDialog.setContentText("Med Name:");
                            medDialog.showAndWait().ifPresent(medName -> {
                                try {
                                    Medicine med = new Medicine(medName);
                                    ch.recordAdministration(currentStaff.getStaffId(), r.getResidentId(), med);
                                    refreshGUI();
                                } catch (Exception ex) {
                                    showAlert("Administration failed: " + ex.toString());
                                }
                            });
                        } else if (act.equals("Move Resident")) {
                            TextInputDialog toBed = new TextInputDialog();
                            toBed.setContentText("To Bed ID:");
                            toBed.showAndWait().ifPresent(to -> {
                                try {
                                    ch.moveResident(currentStaff.getStaffId(), bed.getBedId(), to);
                                    refreshGUI();
                                } catch (Exception ex) {
                                    showAlert("Move failed: " + ex.toString());
                                }
                            });
                        } else if (act.equals("Discharge")) {
                            ch.dischargeResident(currentStaff.getStaffId(), r.getResidentId());
                            refreshGUI();
                        }
                    } catch (Exception ex) {
                        showAlert("Action failed: " + ex.toString());
                    }
                });
            }
        }
    }

    private void addResidentGUI() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Resident");
        GridPane grid = new GridPane();
        TextField name = new TextField();
        ChoiceBox<Gender> gender = new ChoiceBox<>();
        gender.getItems().addAll(Gender.values());
        TextField condition = new TextField();
        grid.add(new Label("Name:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Gender:"), 0, 1);
        grid.add(gender, 1, 1);
        grid.add(new Label("Condition:"), 0, 2);
        grid.add(condition, 1, 2);
        Button add = new Button("Add");
        add.setOnAction(e -> {
            try {
                String id = "R" + java.util.UUID.randomUUID().toString().substring(0, 4);
                Resident r = new Resident(id, name.getText(), gender.getValue(), condition.getText());
                ch.addResident(r);
                dialog.getDialogPane().getScene().getWindow().hide();
            } catch (Exception ex) {
                showAlert("Add resident failed: " + ex.toString());
            }
        });
        grid.add(add, 1, 3);
        dialog.getDialogPane().setContent(grid);
        dialog.show();
    }

    private void addStaffGUI() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Staff");
        GridPane grid = new GridPane();
        ChoiceBox<Role> role = new ChoiceBox<>();
        role.getItems().addAll(Role.values());
        TextField name = new TextField();
        ChoiceBox<Gender> gender = new ChoiceBox<>();
        gender.getItems().addAll(Gender.values());
        TextField username = new TextField();
        TextField password = new TextField();
        grid.add(new Label("Role:"), 0, 0);
        grid.add(role, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(name, 1, 1);
        grid.add(new Label("Gender:"), 0, 2);
        grid.add(gender, 1, 2);
        grid.add(new Label("Username:"), 0, 3);
        grid.add(username, 1, 3);
        grid.add(new Label("Password:"), 0, 4);
        grid.add(password, 1, 4);
        Button add = new Button("Add");
        add.setOnAction(e -> {
            try {
                String id = role.getValue().name().charAt(0) + java.util.UUID.randomUUID().toString().substring(0, 4);
                Staff s;
                switch (role.getValue()) {
                    case MANAGER -> s = new Manager(id, name.getText(), gender.getValue(), username.getText(), password.getText());
                    case DOCTOR -> s = new Doctor(id, name.getText(), gender.getValue(), username.getText(), password.getText());
                    case NURSE -> s = new Nurse(id, name.getText(), gender.getValue(), username.getText(), password.getText());
                    default -> { return; }
                }
                ch.addStaff(s);
                dialog.getDialogPane().getScene().getWindow().hide();
            } catch (Exception ex) {
                showAlert("Add staff failed: " + ex.toString());
            }
        });
        grid.add(add, 1, 5);
        dialog.getDialogPane().setContent(grid);
        dialog.show();
    }

    private void modifyStaffGUI() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Modify Password");
        dialog.setContentText("Staff ID:");
        dialog.showAndWait().ifPresent(id -> {
            TextInputDialog passDialog = new TextInputDialog();
            passDialog.setContentText("New Password:");
            passDialog.showAndWait().ifPresent(pass -> {
                try {
                    ch.modifyStaffPassword(id, pass);
                } catch (Exception ex) {
                    showAlert("Modify password failed: " + ex.toString());
                }
            });
        });
    }

    private void assignShiftGUI() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Assign Nurse Shift");
        GridPane grid = new GridPane();
        TextField nurseId = new TextField();
        ChoiceBox<DayOfWeek> day = new ChoiceBox<>();
        day.getItems().addAll(DayOfWeek.values());
        TextField start = new TextField();
        TextField end = new TextField();
        grid.add(new Label("Nurse ID:"), 0, 0);
        grid.add(nurseId, 1, 0);
        grid.add(new Label("Day:"), 0, 1);
        grid.add(day, 1, 1);
        grid.add(new Label("Start (HH:MM):"), 0, 2);
        grid.add(start, 1, 2);
        grid.add(new Label("End (HH:MM):"), 0, 3);
        grid.add(end, 1, 3);
        Button assign = new Button("Assign");
        assign.setOnAction(e -> {
            try {
                Nurse n = (Nurse) ch.getStaffById(nurseId.getText());
                LocalTime sTime = LocalTime.parse(start.getText());
                LocalTime eTime = LocalTime.parse(end.getText());
                ch.getSchedule().assignNurseShift(n, new Shift(day.getValue(), sTime, eTime));
                dialog.getDialogPane().getScene().getWindow().hide();
                refreshGUI();
            } catch (Exception ex) {
                showAlert("Assign shift failed: " + ex.toString());
            }
        });
        grid.add(assign, 1, 4);
        dialog.getDialogPane().setContent(grid);
        dialog.show();
    }

    private void viewLogsGUI() {
        TextArea ta = new TextArea();
        ch.getLogs().forEach(log -> ta.appendText(log + "\n"));
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.getDialogPane().setContent(ta);
        alert.setTitle("Logs");
        alert.show();
    }

    private void viewScheduleGUI() {
        TextArea ta = new TextArea();
        ch.getSchedule().getAllShifts().forEach(s -> ta.appendText(s + "\n"));
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.getDialogPane().setContent(ta);
        alert.setTitle("Schedule");
        alert.show();
    }

    private void viewArchivesGUI() {
        TextArea ta = new TextArea();
        try {
            try (Connection conn = DriverManager.getConnection(DBManager.DB_URL);
                 var stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id FROM residents")) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    Map<String, String> details = DBManager.getResidentDetails(id);
                    ta.appendText("Resident ID: " + id + "\n");
                    details.forEach((k, v) -> ta.appendText(k + ": " + v + "\n"));
                    ta.appendText("----\n");
                }
            }
        } catch (Exception ex) {
            ta.appendText("Error loading archives: " + ex.toString());
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.getDialogPane().setContent(ta);
        alert.setTitle("Archived Residents");
        alert.show();
    }

    private void refreshGUI() {
        primaryStage.setScene(createMainScene());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}