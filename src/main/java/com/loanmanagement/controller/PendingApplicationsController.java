package com.loanmanagement.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PendingApplicationsController {

    // ============================================================
    // TABLE
    // ============================================================

    @FXML
    private TableView<LoanApplication> applicationTable;

    @FXML
    private TableColumn<LoanApplication, Integer> applicationIdColumn;

    @FXML
    private TableColumn<LoanApplication, String> customerColumn;

    @FXML
    private TableColumn<LoanApplication, String> loanTypeColumn;

    @FXML
    private TableColumn<LoanApplication, Double> amountColumn;

    @FXML
    private TableColumn<LoanApplication, String> purposeColumn;

    @FXML
    private TableColumn<LoanApplication, Integer> tenureColumn;

    @FXML
    private TableColumn<LoanApplication, Double> interestColumn;

    @FXML
    private TableColumn<LoanApplication, Double> emiColumn;

    @FXML
    private TableColumn<LoanApplication, String> dateColumn;

    @FXML
    private TableColumn<LoanApplication, String> statusColumn;


    // ============================================================
    // DETAILS
    // ============================================================

    @FXML
    private Label applicationIdLabel;

    @FXML
    private Label customerLabel;

    @FXML
    private Label loanTypeLabel;

    @FXML
    private Label amountLabel;

    @FXML
    private Label purposeLabel;

    @FXML
    private Label tenureLabel;

    @FXML
    private Label interestLabel;

    @FXML
    private Label emiLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label statusLabel;


    // ============================================================
    // BUTTONS
    // ============================================================

    @FXML
    private Button approveButton;

    @FXML
    private Button rejectButton;


    // ============================================================
    // DATA
    // ============================================================

    private final ObservableList<LoanApplication> applications =
            FXCollections.observableArrayList();


    // ============================================================
    // INITIALIZE
    // ============================================================

    @FXML
    public void initialize() {

        setupTable();

        disableReviewButtons();

        loadPendingApplications();

        applicationTable.setRowFactory(tableView -> {

            TableRow<LoanApplication> row =
                    new TableRow<>();

            row.itemProperty().addListener(
                    (observable, oldValue, newValue) -> {

                        if (newValue == null) {
                            row.setStyle("");
                        } else {
                            row.setStyle("");
                        }
                    }
            );

            return row;
        });

        applicationTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                showApplicationDetails(newValue)
                );
    }


    // ============================================================
    // SETUP TABLE
    // ============================================================

    private void setupTable() {

        applicationIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("applicationId")
        );

        customerColumn.setCellValueFactory(
                new PropertyValueFactory<>("customerName")
        );

        loanTypeColumn.setCellValueFactory(
                new PropertyValueFactory<>("loanType")
        );

        amountColumn.setCellValueFactory(
                new PropertyValueFactory<>("loanAmount")
        );

        purposeColumn.setCellValueFactory(
                new PropertyValueFactory<>("loanPurpose")
        );

        tenureColumn.setCellValueFactory(
                new PropertyValueFactory<>("tenureMonths")
        );

        interestColumn.setCellValueFactory(
                new PropertyValueFactory<>("interestRate")
        );

        emiColumn.setCellValueFactory(
                new PropertyValueFactory<>("emiAmount")
        );

        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("applicationDate")
        );

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );


        // Amount formatting
        amountColumn.setCellFactory(column ->
                new TableCell<LoanApplication, Double>() {

                    @Override
                    protected void updateItem(
                            Double amount,
                            boolean empty
                    ) {

                        super.updateItem(amount, empty);

                        if (empty || amount == null) {
                            setText(null);
                        } else {
                            setText(
                                    String.format(
                                            "₹%,.2f",
                                            amount
                                    )
                            );
                        }
                    }
                }
        );


        // Interest formatting
        interestColumn.setCellFactory(column ->
                new TableCell<LoanApplication, Double>() {

                    @Override
                    protected void updateItem(
                            Double interest,
                            boolean empty
                    ) {

                        super.updateItem(interest, empty);

                        if (empty || interest == null) {
                            setText(null);
                        } else {
                            setText(
                                    String.format(
                                            "%.2f%%",
                                            interest
                                    )
                            );
                        }
                    }
                }
        );


        // EMI formatting
        emiColumn.setCellFactory(column ->
                new TableCell<LoanApplication, Double>() {

                    @Override
                    protected void updateItem(
                            Double emi,
                            boolean empty
                    ) {

                        super.updateItem(emi, empty);

                        if (empty || emi == null) {
                            setText(null);
                        } else {
                            setText(
                                    String.format(
                                            "₹%,.2f",
                                            emi
                                    )
                            );
                        }
                    }
                }
        );
    }


    // ============================================================
    // LOAD PENDING APPLICATIONS
    // ============================================================

    private void loadPendingApplications() {

        applications.clear();

        String query =
                "SELECT " +
                "    la.APPLICATION_ID, " +
                "    lc.FULL_NAME, " +
                "    la.LOAN_TYPE, " +
                "    la.LOAN_AMOUNT, " +
                "    la.LOAN_PURPOSE, " +
                "    la.TENURE_MONTHS, " +
                "    la.INTEREST_RATE, " +
                "    la.EMI_AMOUNT, " +
                "    TO_CHAR(la.APPLICATION_DATE, 'DD-MON-YY') AS APPLICATION_DATE, " +
                "    la.STATUS " +
                "FROM LOAN_APPLICATION la " +
                "JOIN LMS_CUSTOMER lc " +
                "ON la.CUSTOMER_ID = lc.CUSTOMER_ID " +
                "WHERE UPPER(la.STATUS) = 'PENDING' " +
                "ORDER BY la.APPLICATION_ID";


        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(query);
             ResultSet resultSet =
                     statement.executeQuery()) {


            while (resultSet.next()) {

                LoanApplication application =
                        new LoanApplication(

                                resultSet.getInt(
                                        "APPLICATION_ID"
                                ),

                                resultSet.getString(
                                        "FULL_NAME"
                                ),

                                resultSet.getString(
                                        "LOAN_TYPE"
                                ),

                                resultSet.getDouble(
                                        "LOAN_AMOUNT"
                                ),

                                resultSet.getString(
                                        "LOAN_PURPOSE"
                                ),

                                resultSet.getInt(
                                        "TENURE_MONTHS"
                                ),

                                resultSet.getDouble(
                                        "INTEREST_RATE"
                                ),

                                resultSet.getDouble(
                                        "EMI_AMOUNT"
                                ),

                                resultSet.getString(
                                        "APPLICATION_DATE"
                                ),

                                resultSet.getString(
                                        "STATUS"
                                )
                        );

                applications.add(application);
            }


            applicationTable.setItems(applications);


            if (applications.isEmpty()) {

                clearApplicationDetails();

            }

        } catch (SQLException e) {

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to load pending applications.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // SHOW SELECTED APPLICATION
    // ============================================================

    private void showApplicationDetails(
            LoanApplication application
    ) {

        if (application == null) {

            clearApplicationDetails();

            return;
        }


        applicationIdLabel.setText(
                String.valueOf(
                        application.getApplicationId()
                )
        );

        customerLabel.setText(
                application.getCustomerName()
        );

        loanTypeLabel.setText(
                application.getLoanType()
        );

        amountLabel.setText(
                String.format(
                        "₹%,.2f",
                        application.getLoanAmount()
                )
        );

        purposeLabel.setText(
                application.getLoanPurpose()
        );

        tenureLabel.setText(
                application.getTenureMonths()
                        + " months"
        );

        interestLabel.setText(
                String.format(
                        "%.2f%%",
                        application.getInterestRate()
                )
        );

        emiLabel.setText(
                String.format(
                        "₹%,.2f",
                        application.getEmiAmount()
                )
        );

        dateLabel.setText(
                application.getApplicationDate()
        );

        statusLabel.setText(
                application.getStatus()
        );


        approveButton.setDisable(false);

        rejectButton.setDisable(false);
    }


    // ============================================================
    // CLEAR DETAILS
    // ============================================================

    private void clearApplicationDetails() {

        applicationIdLabel.setText("-");
        customerLabel.setText("-");
        loanTypeLabel.setText("-");
        amountLabel.setText("-");
        purposeLabel.setText("-");
        tenureLabel.setText("-");
        interestLabel.setText("-");
        emiLabel.setText("-");
        dateLabel.setText("-");
        statusLabel.setText("-");

        disableReviewButtons();
    }


    // ============================================================
    // DISABLE BUTTONS
    // ============================================================

    private void disableReviewButtons() {

        if (approveButton != null) {
            approveButton.setDisable(true);
        }

        if (rejectButton != null) {
            rejectButton.setDisable(true);
        }
    }


    // ============================================================
    // APPROVE
    // ============================================================

    @FXML
    private void approveApplication() {

        LoanApplication selectedApplication =
                applicationTable
                        .getSelectionModel()
                        .getSelectedItem();


        if (selectedApplication == null) {

            showWarning(
                    "No Application Selected",
                    "Please select a pending application first."
            );

            return;
        }


        boolean updated =
                updateApplicationStatus(
                        selectedApplication.getApplicationId(),
                        "APPROVED"
                );


        if (updated) {

            showInformation(
                    "Application Approved",
                    "Application #"
                            + selectedApplication.getApplicationId()
                            + " has been approved successfully."
            );

            loadPendingApplications();
        }
    }


    // ============================================================
    // REJECT
    // ============================================================

    @FXML
    private void rejectApplication() {

        LoanApplication selectedApplication =
                applicationTable
                        .getSelectionModel()
                        .getSelectedItem();


        if (selectedApplication == null) {

            showWarning(
                    "No Application Selected",
                    "Please select a pending application first."
            );

            return;
        }


        boolean updated =
                updateApplicationStatus(
                        selectedApplication.getApplicationId(),
                        "REJECTED"
                );


        if (updated) {

            showInformation(
                    "Application Rejected",
                    "Application #"
                            + selectedApplication.getApplicationId()
                            + " has been rejected."
            );

            loadPendingApplications();
        }
    }


    // ============================================================
    // UPDATE STATUS
    // ============================================================

    private boolean updateApplicationStatus(
            int applicationId,
            String newStatus
    ) {

        String query =
                "UPDATE LOAN_APPLICATION "
                        + "SET STATUS = ? "
                        + "WHERE APPLICATION_ID = ? "
                        + "AND UPPER(STATUS) = 'PENDING'";


        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(query)) {


            statement.setString(
                    1,
                    newStatus
            );

            statement.setInt(
                    2,
                    applicationId
            );


            int rowsUpdated =
                    statement.executeUpdate();


            if (rowsUpdated == 1) {

                connection.commit();

                return true;
            }


            showWarning(
                    "Application Not Updated",
                    "This application is no longer pending."
            );

            return false;


        } catch (SQLException e) {

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to update application status.\n\n"
                            + e.getMessage()
            );

            return false;
        }
    }


    // ============================================================
    // BACK TO DASHBOARD
    // ============================================================

    @FXML
    private void backToDashboard() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/dashboard.fxml"
                            )
                    );

            Parent root = loader.load();

            Stage currentStage =
                    (Stage) applicationTable
                            .getScene()
                            .getWindow();


            Scene scene =
                    new Scene(
                            root,
                            1400,
                            850
                    );


            currentStage.setScene(scene);

            currentStage.setTitle(
                    "LoanFlow - Dashboard"
            );

            currentStage.setMaximized(true);


        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Navigation Error",
                    "Unable to return to dashboard.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // DATABASE CONNECTION
    // ============================================================

    private Connection getConnection()
            throws SQLException {

        String password =
                System.getenv("LMS_DB_PASSWORD");


        if (password == null || password.isBlank()) {

            throw new SQLException(
                    "Database password is not configured.\n"
                            + "Please set the LMS_DB_PASSWORD "
                            + "environment variable."
            );
        }


        return DriverManager.getConnection(
                "jdbc:oracle:thin:@localhost:1521:xe",
                "system",
                password
        );
    }


    // ============================================================
    // ALERTS
    // ============================================================

    private void showInformation(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }


    private void showWarning(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.WARNING
                );

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }


    private void showError(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }


    // ============================================================
    // APPLICATION MODEL
    // ============================================================

    public static class LoanApplication {

        private final int applicationId;
        private final String customerName;
        private final String loanType;
        private final double loanAmount;
        private final String loanPurpose;
        private final int tenureMonths;
        private final double interestRate;
        private final double emiAmount;
        private final String applicationDate;
        private final String status;


        public LoanApplication(
                int applicationId,
                String customerName,
                String loanType,
                double loanAmount,
                String loanPurpose,
                int tenureMonths,
                double interestRate,
                double emiAmount,
                String applicationDate,
                String status
        ) {

            this.applicationId = applicationId;
            this.customerName = customerName;
            this.loanType = loanType;
            this.loanAmount = loanAmount;
            this.loanPurpose = loanPurpose;
            this.tenureMonths = tenureMonths;
            this.interestRate = interestRate;
            this.emiAmount = emiAmount;
            this.applicationDate = applicationDate;
            this.status = status;
        }


        public int getApplicationId() {
            return applicationId;
        }


        public String getCustomerName() {
            return customerName;
        }


        public String getLoanType() {
            return loanType;
        }


        public double getLoanAmount() {
            return loanAmount;
        }


        public String getLoanPurpose() {
            return loanPurpose;
        }


        public int getTenureMonths() {
            return tenureMonths;
        }


        public double getInterestRate() {
            return interestRate;
        }


        public double getEmiAmount() {
            return emiAmount;
        }


        public String getApplicationDate() {
            return applicationDate;
        }


        public String getStatus() {
            return status;
        }
    }
}