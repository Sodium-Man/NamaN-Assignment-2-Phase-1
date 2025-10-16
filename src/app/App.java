package app;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public class App extends Application {
    private final CareHome ch = CareHome.getInstance();
    private Staff currentStaff;
    private Stage primaryStage;
    private Label statusLabel = new Label();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Path embeddedCssFile;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            embeddedCssFile = writeEmbeddedCss();
        } catch (IOException e) {
            System.err.println("Failed to write embedded CSS: " + e.getMessage());
            embeddedCssFile = null;
        }

        primaryStage.setOnCloseRequest(e -> {
            try {
                ch.saveData(new File("carehome.ser"));
            } catch (Exception ex) {
                showAlert("Save failed: " + ex.toString());
            }
        });

        // Initialize DB if needed (silent)
        try {
            DBManager.initializeDatabase();
        } catch (Exception ignored) {}

        // Bootstrap sample data if none
        if (ch.getWards().isEmpty()) {
            try {
                SampleData.bootstrapBeds(ch); // Ensure 2 wards, 6 rooms each, 1-4 beds
                SampleData.bootstrapPeople(ch);
                SampleData.bootstrapSchedule(ch);
            } catch (Exception e) {
                showAlert("Bootstrap error: " + e.toString());
            }
        }

        Scene loginScene = createLoginScene();
        if (embeddedCssFile != null) loginScene.getStylesheets().add(embeddedCssFile.toUri().toString());

        primaryStage.setScene(loginScene);
        primaryStage.setTitle("CareHome Management System");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    private Scene createLoginScene() {
        // Container
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        Label title = new Label("CareHome Management System");
        title.getStyleClass().add("title");
        // small logo placeholder (you can replace with ImageView)
        Label logo = new Label("\u2764"); // heart symbol
        logo.setStyle("-fx-font-size:48px; -fx-text-fill: -accent;");
        header.getChildren().addAll(logo, title);
        BorderPane.setMargin(header, new Insets(30, 0, 10, 0));

        // Login card
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setMaxWidth(380);
        card.setAlignment(Pos.CENTER);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("input");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("input");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().addAll("btn", "btn-primary");
        loginButton.setDefaultButton(true);

        Label hint = new Label("Use demo accounts: rhea/pass, nurse1/pass, doctor1/pass");
        hint.getStyleClass().add("hint");

        card.getChildren().addAll(usernameField, passwordField, loginButton, hint);

        // center layout
        VBox center = new VBox(20, header, card);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(20));

        root.setCenter(center);

        // Login logic
        loginButton.setOnAction(e -> attemptLogin(usernameField, passwordField));
        // Enter key binds
        passwordField.setOnKeyPressed(k -> {
            if (k.getCode() == KeyCode.ENTER) loginButton.fire();
        });

        Scene scene = new Scene(root, 900, 700);
        return scene;
    }

    private void attemptLogin(TextField usernameField, PasswordField passwordField) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        Optional<Staff> staffOpt = ch.getStaff().stream()
                .filter(s -> s.getUsername().equals(username) && s.getPassword().equals(password))
                .findFirst();
        if (staffOpt.isPresent()) {
            currentStaff = staffOpt.get();
            Scene main = createMainScene();
            if (embeddedCssFile != null) main.getStylesheets().add(embeddedCssFile.toUri().toString());
            primaryStage.setScene(main);
        } else {
            showAlert("Invalid credentials");
        }
    }

    private Scene createMainScene() {
        BorderPane borderPane = new BorderPane();
        borderPane.getStyleClass().add("root");

        // Menu bar
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("top-menu");

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

        // TabPane (wards and other tabs)
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("main-tabs");

        // Wards tabs
        if (ch.getWards().size() != 2) {
            showAlert("Error: Expected 2 wards, found " + ch.getWards().size());
            return createLoginScene();
        }
        for (Ward ward : ch.getWards()) {
            if (ward.getRooms().size() != 6) {
                showAlert("Error: Ward " + ward.getWardName() + " should have 6 rooms, found " + ward.getRooms().size());
                return createLoginScene();
            }

            VBox wardBox = new VBox(12);
            wardBox.setPadding(new Insets(15));

            Label wardTitle = new Label(ward.getWardName());
            wardTitle.getStyleClass().add("ward-title");

            FlowPane roomsFlow = new FlowPane();
            roomsFlow.setHgap(16);
            roomsFlow.setVgap(16);
            roomsFlow.setPadding(new Insets(6));

            for (Room room : ward.getRooms()) {
                // Room card: VBox with room label + bed tiles vertically
                VBox roomCard = new VBox(8);
                roomCard.getStyleClass().add("room-card");
                roomCard.setPadding(new Insets(10));
                Label roomLabel = new Label(room.getRoomId());
                roomLabel.getStyleClass().add("room-label");
                roomCard.getChildren().add(roomLabel);

                for (Bed bed : room.getBeds()) {
                    ToggleButton bedButton = new ToggleButton();
                    bedButton.getStyleClass().add("bed-button");
                    bedButton.setMinSize(140, 64);
                    bedButton.setMaxWidth(Double.MAX_VALUE);
                    bedButton.setWrapText(true);

                    if (bed.isVacant()) {
                        bedButton.getStyleClass().add("bed-vacant");
                        bedButton.setText(bed.getBedId() + "\nVACANT");
                        bedButton.setTooltip(new Tooltip("Vacant bed: " + bed.getBedId()));
                    } else {
                        bedButton.getStyleClass().add("bed-occupied");
                        Resident res = bed.getResident();
                        bedButton.setText(bed.getBedId() + "\n" + res.getName());
                        Tooltip tip = new Tooltip("Name: " + res.getName() + "\nGender: " + res.getGender() + "\nCondition: " + res.getCondition());
                        bedButton.setTooltip(tip);
                    }

                    // Action on bed click
                    bedButton.setOnAction(e -> handleBedClick(bed));
                    roomCard.getChildren().add(bedButton);
                }

                roomsFlow.getChildren().add(roomCard);
            }

            wardBox.getChildren().addAll(wardTitle, roomsFlow);
            Tab tab = new Tab(ward.getWardName(), new ScrollPane(wardBox));
            tabPane.getTabs().add(tab);
        }

        // Additional tabs: Staff / Logs
        Tab staffTab = new Tab("Staff");
        staffTab.setContent(createStaffOverview());
        tabPane.getTabs().add(staffTab);

        Tab logsTab = new Tab("Logs");
        logsTab.setContent(createLogsOverview());
        tabPane.getTabs().add(logsTab);

        // Status bar
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(8));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        updateStatus();

        statusLabel.getStyleClass().add("status-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // quick action buttons on right
        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().addAll("btn", "btn-ghost");
        refreshBtn.setOnAction(e -> refreshGUI());

        statusBar.getChildren().addAll(statusLabel, spacer, refreshBtn);

        borderPane.setTop(menuBar);
        borderPane.setCenter(tabPane);
        borderPane.setBottom(statusBar);

        // Update clock every second
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), ev -> updateStatus()));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        Scene scene = new Scene(borderPane, 1100, 760);
        return scene;
    }

    private void updateStatus() {
        if (currentStaff != null) {
            statusLabel.setText("Logged in as: " + currentStaff.getName() + " (Role: " + currentStaff.getRole() + ")   |   Time: " + LocalTime.now().format(timeFmt));
        } else {
            statusLabel.setText("Not logged in   |   Time: " + LocalTime.now().format(timeFmt));
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
            } else {
                showAlert("Only managers or nurses can assign residents.");
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
                showAddPrescriptionDialog(r);
            } else if (currentStaff.getRole() == Role.NURSE) {
                showNurseActions(bed, r);
            }
        }
    }

    private void showAddPrescriptionDialog(Resident r) {
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
        addBtn.getStyleClass().addAll("btn","btn-primary");
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
    }

    private void showNurseActions(Bed bed, Resident r) {
        ChoiceDialog<String> choice = new ChoiceDialog<>("Administer Med", "Administer Med", "Move Resident", "Discharge");
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

    private VBox createStaffOverview() {
        VBox v = new VBox(10);
        v.setPadding(new Insets(12));
        Label lbl = new Label("Staff Overview");
        lbl.getStyleClass().add("section-title");

        ListView<String> listView = new ListView<>();
        ch.getStaff().forEach(s -> listView.getItems().add(s.toString() + " | username: " + s.getUsername()));

        v.getChildren().addAll(lbl, listView);
        return v;
    }

    private VBox createLogsOverview() {
        VBox v = new VBox(10);
        v.setPadding(new Insets(12));
        Label lbl = new Label("Action Logs");
        lbl.getStyleClass().add("section-title");

        TextArea ta = new TextArea();
        ta.setEditable(false);
        ch.getLogs().forEach(log -> ta.appendText(log + "\n"));

        v.getChildren().addAll(lbl, ta);
        return v;
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
        add.getStyleClass().addAll("btn","btn-primary");
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
        add.getStyleClass().addAll("btn","btn-primary");
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
        assign.getStyleClass().addAll("btn","btn-primary");
        assign.setOnAction(e -> {
            if (isValidTime(start.getText()) && isValidTime(end.getText())) {
                try {
                    Nurse n = (Nurse) ch.getStaffById(nurseId.getText());
                    java.time.LocalTime sTime = java.time.LocalTime.parse(start.getText());
                    java.time.LocalTime eTime = java.time.LocalTime.parse(end.getText());
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
        if (embeddedCssFile != null) primaryStage.getScene().getStylesheets().add(embeddedCssFile.toUri().toString());
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

    /**
     * Writes an embedded CSS string to a temporary stylesheet file and returns the Path.
     * The stylesheet uses a professional blue-white palette and modern styles.
     */
    private Path writeEmbeddedCss() throws IOException {
        String css = """
                /* Root and fonts */
                .root {
                    -fx-font-family: "Segoe UI", "Roboto", sans-serif;
                    -fx-background-color: linear-gradient(to bottom, #f7fbff, #ffffff);
                }
                .title {
                    -fx-font-size: 20px;
                    -fx-font-weight: bold;
                    -fx-text-fill: -accent;
                }
                /* Accent colors */
                :root {
                    -accent: #2b78d8;
                    -accent-dark: #1e5fb0;
                    -muted: #6b7280;
                    -card-bg: #ffffff;
                    -positive: #dff7e6;
                    -danger: #ffecec;
                }
                .card {
                    -fx-background-color: -card-bg;
                    -fx-background-radius: 10;
                    -fx-effect: dropshadow(gaussian, rgba(43,120,216,0.12), 8, 0, 0, 2);
                }
                .input {
                    -fx-background-radius: 8;
                    -fx-padding: 10 12 10 12;
                    -fx-border-radius: 8;
                    -fx-border-color: rgba(43,120,216,0.12);
                    -fx-font-size: 14px;
                }
                .btn {
                    -fx-background-radius: 8;
                    -fx-padding: 8 14 8 14;
                    -fx-font-size: 13px;
                    -fx-cursor: hand;
                }
                .btn-primary {
                    -fx-background-color: -accent;
                    -fx-text-fill: white;
                    -fx-font-weight: 600;
                }
                .btn-primary:hover {
                    -fx-background-color: -accent-dark;
                }
                .btn-ghost {
                    -fx-background-color: transparent;
                    -fx-border-color: rgba(43,120,216,0.18);
                    -fx-text-fill: -accent-dark;
                }
                .top-menu {
                    -fx-background-color: transparent;
                }
                .status-bar {
                    -fx-background-color: linear-gradient(to top, #ffffff, rgba(240,248,255,0.6));
                    -fx-border-color: rgba(0,0,0,0.04);
                    -fx-border-width: 1 0 0 0;
                }
                .status-label {
                    -fx-font-size: 13px;
                    -fx-text-fill: -muted;
                    -fx-padding: 0 6 0 6;
                }
                .ward-title {
                    -fx-font-size: 16px;
                    -fx-font-weight: 600;
                    -fx-text-fill: -accent-dark;
                }
                .room-card {
                    -fx-background-color: -card-bg;
                    -fx-background-radius: 8;
                    -fx-min-width: 180;
                    -fx-effect: dropshadow(gaussian, rgba(16,24,40,0.04), 6, 0, 0, 1);
                }
                .room-label {
                    -fx-font-size: 13px;
                    -fx-font-weight: 600;
                    -fx-padding: 0 0 6 0;
                }
                .bed-button {
                    -fx-background-radius: 6;
                    -fx-border-radius: 6;
                    -fx-font-size: 12px;
                    -fx-text-alignment: center;
                    -fx-alignment: center;
                }
                .bed-vacant {
                    -fx-background-color: linear-gradient(to bottom, #f0fbf6, #f0f8ff);
                    -fx-border-color: rgba(43,120,216,0.08);
                    -fx-text-fill: #0f5132;
                }
                .bed-occupied {
                    -fx-background-color: linear-gradient(to bottom, #fff4f4, #fffafa);
                    -fx-border-color: rgba(220,38,38,0.06);
                    -fx-text-fill: #7a1f1f;
                }
                .section-title {
                    -fx-font-size: 15px;
                    -fx-font-weight: 600;
                    -fx-text-fill: -accent-dark;
                }
                .hint {
                    -fx-font-size: 11px;
                    -fx-text-fill: -muted;
                }
                """;

        Path tmp = Files.createTempFile("carehome-style-", ".css");
        Files.writeString(tmp, css);
        tmp.toFile().deleteOnExit();
        return tmp;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
