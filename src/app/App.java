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
                SampleData.bootstrapBeds(ch); // Ensure 2 wards, 6 rooms each, 1-4 beds
                SampleData.bootstrapPeople(ch);
                SampleData.bootstrapSchedule(ch);
            } catch (Exception e) {
                showAlert("Bootstrap error: " + e.toString());
            }
        }

        primaryStage.setScene(createLoginScene());
        primaryStage.setTitle("Resident HealthCare System");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    private Scene createLoginScene() {
        VBox loginBox = new VBox(15);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(20));
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Button loginButton = new Button("Login");
        loginButton.setStyle("-fx-font-size: 14px; -fx-padding: 5px 20px;");
        loginBox.getChildren().addAll(new Label("Login"), usernameField, passwordField, loginButton);

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

        return new Scene(loginBox, 300, 250);
    }

    private Scene createMainScene() {
        BorderPane borderPane = new BorderPane();
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Ensure exactly 2 wards with 6 rooms each
        if (ch.getWards().size() != 2) {
            showAlert("Error: Expected 2 wards, found " + ch.getWards().size());
            return createLoginScene();
        }
        for (Ward ward : ch.getWards()) {
            if (ward.getRooms().size() != 6) {
                showAlert("Error: Ward " + ward.getWardName() + " should have 6 rooms, found " + ward.getRooms().size());
                return createLoginScene();
            }
            GridPane grid = new GridPane();
            grid.setPadding(new Insets(15));
            grid.setHgap(15);
            grid.setVgap(15);
            int row = 0, col = 0;
            for (Room room : ward.getRooms()) {
                Label roomLabel = new Label(room.getRoomId());
                roomLabel.setStyle("-fx-font-size: 14px;");
                grid.add(roomLabel, col, row);
                row++;
                for (Bed bed : room.getBeds()) {
                    ToggleButton bedButton = new ToggleButton(bed.getBedId() + "\n" + (bed.isVacant() ? "VACANT" : bed.getResident().getName()));
                    bedButton.setMinSize(120, 60);
                    bedButton.setStyle("-fx-font-size: 12px; -fx-alignment: center;");
                    if (!bed.isVacant()) {
                        Gender g = bed.getResident().getGender();
                        bedButton.setStyle(bedButton.getStyle() + (g == Gender.M ? "-fx-background-color: #ADD8E6;" : "-fx-background-color: #F08080;"));
                        bedButton.setTooltip(new Tooltip("Name: " + bed.getResident().getName() + "\nCondition: " + bed.getResident().getCondition()));
                    } else {
                        bedButton.setStyle(bedButton.getStyle() + "-fx-background-color: #F0F0F0;");
                    }
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

        // Status bar
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px; -fx-padding: 5px;");
        updateStatus(statusLabel);
        borderPane.setTop(menuBar);
        borderPane.setCenter(tabPane);
        borderPane.setBottom(statusLabel);

        return new Scene(borderPane, 900, 700);
    }

    private void updateStatus(Label statusLabel) {
        if (currentStaff != null) {
            statusLabel.setText("Logged in as: " + currentStaff.getName() + " (Role: " + currentStaff.getRole() + ") | Time: " + java.time.LocalTime.now());
        } else {
            statusLabel.setText("Not logged in | Time: " + java.time.LocalTime.now());
        }
    }

    private void handleBedClick(Bed bed) {
        if (bed.isVacant()) {
            if (currentStaff.getRole() == Role.MANAGER || currentStaff.getRole() == Role.NURSE) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Assign Resident");
                dialog.setHeaderText("Enter Resident ID to assign");
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
            details.setHeaderText("Details for " + r.getName());
            details.setContentText(r.toString() + "\nPrescription: " + presText);
            details.showAndWait();

            if (currentStaff.getRole() == Role.DOCTOR) {
                Dialog<Void> presDialog = new Dialog<>();
                presDialog.setTitle("Add Prescription");
                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(10));
                TextField medName = new TextField();
                medName.setPromptText("e.g., Paracetamol");
                TextField dose = new TextField();
                dose.setPromptText("e.g., 500mg");
                TextField time = new TextField();
                time.setPromptText("e.g., 08:00");
                grid.add(new Label("Medicine:"), 0, 0);
                grid.add(medName, 1, 0);
                grid.add(new Label("Dose:"), 0, 1);
                grid.add(dose, 1, 1);
                grid.add(new Label("Time (HH:MM):"), 0, 2);
                grid.add(time, 1, 2);
                Button addBtn = new Button("Add Prescription");
                addBtn.setOnAction(ev -> {
                    if (isValidTime(time.getText())) {
                        try {
                            Medicine med = new Medicine(medName.getText());
                            LocalTime t = LocalTime.parse(time.getText());
                            ch.addPrescription(currentStaff.getStaffId(), r.getResidentId(), med, dose.getText(), t);
                            presDialog.getDialogPane().getScene().getWindow().hide();
                            refreshGUI();
                        } catch (Exception ex) {
                            showAlert("Prescription failed: " + ex.toString());
                        }
                    } else {
                        showAlert("Invalid time format. Use HH:MM (e.g., 08:00)");
                    }
                });
                grid.add(addBtn, 1, 3);
                presDialog.getDialogPane().setContent(grid);
                presDialog.showAndWait();
            } else if (currentStaff.getRole() == Role.NURSE) {
                ChoiceDialog<String> choice = new ChoiceDialog<>("Administer Med", "Move Resident", "Discharge");
                choice.setTitle("Nurse Actions");
                choice.showAndWait().ifPresent(act -> {
                    try {
                        if (act.equals("Administer Med")) {
                            TextInputDialog medDialog = new TextInputDialog();
                            medDialog.setTitle("Administer Medicine");
                            medDialog.setHeaderText("Select medicine to administer");
                            medDialog.setContentText("Medicine Name:");
                            medDialog.showAndWait().ifPresent(medName -> {
                                if (!medName.isEmpty()) {
                                    try {
                                        Medicine med = new Medicine(medName);
                                        ch.recordAdministration(currentStaff.getStaffId(), r.getResidentId(), med);
                                        refreshGUI();
                                    } catch (Exception ex) {
                                        showAlert("Administration failed: " + ex.toString());
                                    }
                                } else {
                                    showAlert("Medicine name cannot be empty");
                                }
                            });
                        } else if (act.equals("Move Resident")) {
                            TextInputDialog toBed = new TextInputDialog();
                            toBed.setTitle("Move Resident");
                            toBed.setHeaderText("Enter target bed ID");
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
        dialog.setHeaderText("Enter new resident details");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        TextField name = new TextField();
        name.setPromptText("e.g., John Doe");
        ChoiceBox<Gender> gender = new ChoiceBox<>();
        gender.getItems().addAll(Gender.values());
        TextField condition = new TextField();
        condition.setPromptText("e.g., Healthy");
        grid.add(new Label("Name:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Gender:"), 0, 1);
        grid.add(gender, 1, 1);
        grid.add(new Label("Condition:"), 0, 2);
        grid.add(condition, 1, 2);
        Button add = new Button("Add Resident");
        add.setOnAction(e -> {
            if (!name.getText().isEmpty() && gender.getValue() != null && !condition.getText().isEmpty()) {
                try {
                    String id = "R" + java.util.UUID.randomUUID().toString().substring(0, 4);
                    Resident r = new Resident(id, name.getText(), gender.getValue(), condition.getText());
                    ch.addResident(r);
                    dialog.getDialogPane().getScene().getWindow().hide();
                } catch (Exception ex) {
                    showAlert("Add resident failed: " + ex.toString());
                }
            } else {
                showAlert("All fields are required");
            }
        });
        grid.add(add, 1, 3);
        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private void addStaffGUI() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Staff");
        dialog.setHeaderText("Enter new staff details");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        ChoiceBox<Role> role = new ChoiceBox<>();
        role.getItems().addAll(Role.values());
        TextField name = new TextField();
        name.setPromptText("e.g., Jane Smith");
        ChoiceBox<Gender> gender = new ChoiceBox<>();
        gender.getItems().addAll(Gender.values());
        TextField username = new TextField();
        username.setPromptText("e.g., jsmith");
        TextField password = new TextField();
        password.setPromptText("e.g., pass123");
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
        Button add = new Button("Add Staff");
        add.setOnAction(e -> {
            if (!name.getText().isEmpty() && role.getValue() != null && gender.getValue() != null &&
                    !username.getText().isEmpty() && !password.getText().isEmpty()) {
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
            } else {
                showAlert("All fields are required");
            }
        });
        grid.add(add, 1, 5);
        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private void modifyStaffGUI() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Modify Password");
        dialog.setHeaderText("Enter staff ID to modify password");
        dialog.setContentText("Staff ID:");
        dialog.showAndWait().ifPresent(id -> {
            TextInputDialog passDialog = new TextInputDialog();
            passDialog.setHeaderText("Enter new password");
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
        dialog.setHeaderText("Assign a shift to a nurse");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        TextField nurseId = new TextField();
        nurseId.setPromptText("e.g., N1234");
        ChoiceBox<DayOfWeek> day = new ChoiceBox<>();
        day.getItems().addAll(DayOfWeek.values());
        TextField start = new TextField();
        start.setPromptText("e.g., 08:00");
        TextField end = new TextField();
        end.setPromptText("e.g., 16:00");
        grid.add(new Label("Nurse ID:"), 0, 0);
        grid.add(nurseId, 1, 0);
        grid.add(new Label("Day:"), 0, 1);
        grid.add(day, 1, 1);
        grid.add(new Label("Start (HH:MM):"), 0, 2);
        grid.add(start, 1, 2);
        grid.add(new Label("End (HH:MM):"), 0, 3);
        grid.add(end, 1, 3);
        Button assign = new Button("Assign Shift");
        assign.setOnAction(e -> {
            if (isValidTime(start.getText()) && isValidTime(end.getText())) {
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
            } else {
                showAlert("Invalid time format. Use HH:MM (e.g., 08:00)");
            }
        });
        grid.add(assign, 1, 4);
        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private void viewLogsGUI() {
        TextArea ta = new TextArea();
        ta.setStyle("-fx-font-size: 12px;");
        ch.getLogs().forEach(log -> ta.appendText(log + "\n"));
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Logs");
        alert.setHeaderText("System Action Logs");
        alert.getDialogPane().setContent(ta);
        alert.showAndWait();
    }

    private void viewScheduleGUI() {
        TextArea ta = new TextArea();
        ta.setStyle("-fx-font-size: 12px;");
        ch.getSchedule().getAllShifts().forEach(s -> ta.appendText(s + "\n"));
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Schedule");
        alert.setHeaderText("Staff Shift Schedule");
        alert.getDialogPane().setContent(ta);
        alert.showAndWait();
    }

    private void viewArchivesGUI() {
        TextArea ta = new TextArea();
        ta.setStyle("-fx-font-size: 12px;");
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
        alert.setTitle("Archived Residents");
        alert.setHeaderText("Discharged Resident Records");
        alert.getDialogPane().setContent(ta);
        alert.showAndWait();
    }

    private void refreshGUI() {
        primaryStage.setScene(createMainScene());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private boolean isValidTime(String timeStr) {
        try {
            LocalTime.parse(timeStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}