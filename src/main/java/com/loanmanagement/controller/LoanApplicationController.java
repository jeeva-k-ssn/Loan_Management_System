package com.loanmanagement.controller;

import com.loanmanagement.model.User;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class LoanApplicationController {

    // ============================================================
    // FXML COMPONENTS
    // ============================================================

    @FXML
    private TextField customerNameField;

    @FXML
    private ComboBox<String> loanTypeComboBox;

    @FXML
    private TextField loanAmountField;

    @FXML
    private TextArea loanPurposeField;

    @FXML
    private ComboBox<Integer> tenureComboBox;

    @FXML
    private TextField interestRateField;

    @FXML
    private TextField emiField;

    @FXML
    private Label validationLabel;

    @FXML
    private Button submitButton;


    // ============================================================
    // CURRENT USER
    // ============================================================

    private User currentUser;


    // ============================================================
    // INITIALIZE
    // ============================================================

    @FXML
    public void initialize() {

        // Loan types
        loanTypeComboBox.setItems(
                FXCollections.observableArrayList(
                        "Personal Loan",
                        "Education Loan",
                        "Home Loan",
                        "Vehicle Loan",
                        "Business Loan"
                )
        );

        // Loan tenure
        tenureComboBox.setItems(
                FXCollections.observableArrayList(
                        12,
                        24,
                        36,
                        48,
                        60,
                        72,
                        84,
                        120
                )
        );

        // Default interest rate
        interestRateField.setText("10.5");

        /*
         * Change interest rate according to loan type.
         */
        loanTypeComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> {

                    if (newValue == null) {
                        return;
                    }

                    switch (newValue) {

                        case "Education Loan":
                            interestRateField.setText("8.5");
                            break;

                        case "Home Loan":
                            interestRateField.setText("8.0");
                            break;

                        case "Vehicle Loan":
                            interestRateField.setText("9.0");
                            break;

                        case "Business Loan":
                            interestRateField.setText("11.5");
                            break;

                        case "Personal Loan":
                        default:
                            interestRateField.setText("10.5");
                            break;
                    }

                    clearCalculatedEMI();
                }
        );

        /*
         * If amount changes, previous EMI is no longer valid.
         */
        loanAmountField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        clearCalculatedEMI()
        );

        /*
         * If tenure changes, previous EMI is no longer valid.
         */
        tenureComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        clearCalculatedEMI()
        );
    }


    // ============================================================
    // SET CURRENT USER
    // ============================================================

    public void setCurrentUser(User user) {

        this.currentUser = user;

        if (user == null) {
            return;
        }

        customerNameField.setText(
                user.getFullName()
        );
    }


    // ============================================================
    // CLEAR EMI
    // ============================================================

    private void clearCalculatedEMI() {

        if (emiField != null) {
            emiField.clear();
        }

        if (submitButton != null) {
            submitButton.setDisable(true);
        }

        if (validationLabel != null) {
            validationLabel.setText("");
        }
    }


    // ============================================================
    // CALCULATE EMI
    // ============================================================

    @FXML
    private void calculateEMI() {

        clearValidation();

        // Loan type
        if (loanTypeComboBox.getValue() == null) {

            showValidation(
                    "Please select a loan type."
            );

            return;
        }

        // Loan amount
        String amountText =
                loanAmountField.getText().trim();

        if (amountText.isEmpty()) {

            showValidation(
                    "Please enter the loan amount."
            );

            return;
        }

        // Tenure
        if (tenureComboBox.getValue() == null) {

            showValidation(
                    "Please select the loan tenure."
            );

            return;
        }

        // Interest rate
        String rateText =
                interestRateField.getText().trim();

        if (rateText.isEmpty()) {

            showValidation(
                    "Please enter the interest rate."
            );

            return;
        }

        try {

            double principal =
                    Double.parseDouble(amountText);

            double annualRate =
                    Double.parseDouble(rateText);

            int months =
                    tenureComboBox.getValue();


            // Validate amount

            if (principal <= 0) {

                showValidation(
                        "Loan amount must be greater than zero."
                );

                return;
            }


            // Validate interest

            if (annualRate < 0) {

                showValidation(
                        "Interest rate cannot be negative."
                );

                return;
            }


            // Validate tenure

            if (months <= 0) {

                showValidation(
                        "Please select a valid tenure."
                );

                return;
            }


            // Monthly interest rate

            double monthlyRate =
                    annualRate / 12 / 100;


            double emi;


            // Zero-interest calculation

            if (monthlyRate == 0) {

                emi =
                        principal / months;

            } else {

                emi =
                        principal
                                * monthlyRate
                                * Math.pow(
                                        1 + monthlyRate,
                                        months
                                )
                                /
                                (
                                        Math.pow(
                                                1 + monthlyRate,
                                                months
                                        ) - 1
                                );
            }


            String emiText =
                    String.format(
                            Locale.US,
                            "%.2f",
                            emi
                    );


            emiField.setText(
                    "₹ " + emiText
            );


            showSuccessValidation(
                    "EMI calculated successfully. You can now submit your application."
            );


            /*
             * Enable submit only after successful EMI calculation.
             */
            submitButton.setDisable(false);


        } catch (NumberFormatException e) {

            showValidation(
                    "Please enter valid numeric values."
            );
        }
    }


    // ============================================================
    // SUBMIT APPLICATION
    // ============================================================

    @FXML
    private void submitApplication() {

        clearValidation();


        // Check logged-in user

        if (currentUser == null) {

            showValidation(
                    "Unable to identify the logged-in customer."
            );

            return;
        }


        // Loan type

        if (loanTypeComboBox.getValue() == null) {

            showValidation(
                    "Please select a loan type."
            );

            return;
        }


        // Loan amount

        if (loanAmountField.getText().trim().isEmpty()) {

            showValidation(
                    "Please enter the loan amount."
            );

            return;
        }


        // Loan purpose

        if (loanPurposeField.getText().trim().isEmpty()) {

            showValidation(
                    "Please enter the purpose of the loan."
            );

            return;
        }


        // Tenure

        if (tenureComboBox.getValue() == null) {

            showValidation(
                    "Please select the loan tenure."
            );

            return;
        }


        // EMI

        if (emiField.getText() == null
                || emiField.getText().isBlank()) {

            showValidation(
                    "Please calculate the EMI before submitting."
            );

            return;
        }


        double loanAmount;

        double interestRate;

        double emi;

        int tenure;


        try {

            loanAmount =
                    Double.parseDouble(
                            loanAmountField
                                    .getText()
                                    .trim()
                    );


            interestRate =
                    Double.parseDouble(
                            interestRateField
                                    .getText()
                                    .trim()
                    );


            tenure =
                    tenureComboBox.getValue();


            String emiText =
                    emiField
                            .getText()
                            .replace("₹", "")
                            .replace(",", "")
                            .trim();


            emi =
                    Double.parseDouble(
                            emiText
                    );


            if (loanAmount <= 0) {

                showValidation(
                        "Loan amount must be greater than zero."
                );

                return;
            }


        } catch (NumberFormatException e) {

            showValidation(
                    "Please check your loan amount, interest rate and EMI."
            );

            return;
        }


        // ========================================================
        // DATABASE
        // ========================================================

        try (Connection connection =
                     getConnection()) {


            // Find customer

            int customerId =
                    findCustomerId(
                            connection,
                            currentUser.getUserId()
                    );


            if (customerId == -1) {

                showError(
                        "Customer Not Found",
                        "Your customer profile could not be found."
                );

                return;
            }


            /*
             * Insert new application.
             *
             * APPLICATION_ID is generated by the sequence.
             *
             * STATUS is automatically PENDING.
             */

            String sql =
                    "INSERT INTO LOAN_APPLICATION "
                            + "(APPLICATION_ID, CUSTOMER_ID, "
                            + "LOAN_AMOUNT, LOAN_PURPOSE, "
                            + "APPLICATION_DATE, STATUS, "
                            + "LOAN_TYPE, TENURE_MONTHS, "
                            + "INTEREST_RATE, EMI_AMOUNT) "
                            + "VALUES "
                            + "(LOAN_APPLICATION_SEQ.NEXTVAL, "
                            + "?, ?, ?, SYSDATE, 'PENDING', "
                            + "?, ?, ?, ?)";


            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {


                statement.setInt(
                        1,
                        customerId
                );


                statement.setDouble(
                        2,
                        loanAmount
                );


                statement.setString(
                        3,
                        loanPurposeField
                                .getText()
                                .trim()
                );


                statement.setString(
                        4,
                        loanTypeComboBox
                                .getValue()
                );


                statement.setInt(
                        5,
                        tenure
                );


                statement.setDouble(
                        6,
                        interestRate
                );


                statement.setDouble(
                        7,
                        emi
                );


                int rows =
                        statement.executeUpdate();


                if (rows == 0) {

                    showError(
                            "Submission Failed",
                            "The application could not be submitted."
                    );

                    return;
                }
            }


            // ====================================================
            // SUCCESS
            // ====================================================

            showInformation(
                    "Application Submitted",
                    "Your loan application has been submitted successfully.\n\n"
                            + "Status: PENDING\n\n"
                            + "A loan officer will review your application."
            );


            handleBack();


        } catch (SQLException e) {

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to submit the loan application.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // FIND CUSTOMER ID
    // ============================================================

    private int findCustomerId(
            Connection connection,
            int userId
    ) {

        /*
         * First try LMS_CUSTOMER.
         */

        String query =
                "SELECT CUSTOMER_ID "
                        + "FROM LMS_CUSTOMER "
                        + "WHERE USER_ID = ?";


        try (PreparedStatement statement =
                     connection.prepareStatement(query)) {


            statement.setInt(
                    1,
                    userId
            );


            try (ResultSet resultSet =
                         statement.executeQuery()) {


                if (resultSet.next()) {

                    return resultSet.getInt(
                            "CUSTOMER_ID"
                    );
                }
            }


        } catch (SQLException e) {

            System.out.println(
                    "LMS_CUSTOMER lookup failed: "
                            + e.getMessage()
            );
        }


        /*
         * Fallback to old CUSTOMER table.
         */

        query =
                "SELECT CUST_ID "
                        + "FROM CUSTOMER "
                        + "WHERE CUST_ID = ?";


        try (PreparedStatement statement =
                     connection.prepareStatement(query)) {


            statement.setString(
                    1,
                    String.valueOf(userId)
            );


            try (ResultSet resultSet =
                         statement.executeQuery()) {


                if (resultSet.next()) {

                    try {

                        return Integer.parseInt(
                                resultSet.getString(
                                        "CUST_ID"
                                )
                        );

                    } catch (NumberFormatException ignored) {

                        // Ignore invalid CUST_ID
                    }
                }
            }


        } catch (SQLException e) {

            System.out.println(
                    "CUSTOMER lookup failed: "
                            + e.getMessage()
            );
        }


        return -1;
    }


    // ============================================================
    // BACK
    // ============================================================

    @FXML
    private void handleBack() {

        if (customerNameField == null
                || customerNameField.getScene() == null) {

            return;
        }

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/dashboard.fxml"
                            )
                    );

            Parent root = loader.load();

            DashboardController controller =
                    loader.getController();

            if (currentUser != null) {

                controller.setCurrentUser(
                        currentUser
                );
            }

            Stage stage =
                    (Stage)
                            customerNameField
                                    .getScene()
                                    .getWindow();

            Scene scene =
                    new Scene(
                            root,
                            1400,
                            850
                    );

            stage.setScene(scene);

            stage.setTitle(
                    "LoanFlow - Dashboard"
            );

            stage.setMaximized(true);

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


        /*
         * Password is taken from the environment variable.
         *
         * LMS_DB_PASSWORD
         */

        String password =
                System.getenv(
                        "LMS_DB_PASSWORD"
                );


        if (password == null
                || password.isBlank()) {


            throw new SQLException(
                    "Database password is not configured.\n\n"
                            + "Please set the LMS_DB_PASSWORD "
                            + "environment variable and restart VS Code."
            );
        }


        return DriverManager.getConnection(
                "jdbc:oracle:thin:@localhost:1521:xe",
                "system",
                password
        );
    }


    // ============================================================
    // VALIDATION
    // ============================================================

    private void showValidation(
            String message
    ) {

        validationLabel.setText(
                message
        );


        validationLabel.setStyle(
                "-fx-text-fill: #DC2626;"
        );
    }


    // ============================================================
    // SUCCESS VALIDATION
    // ============================================================

    private void showSuccessValidation(
            String message
    ) {

        validationLabel.setText(
                message
        );


        validationLabel.setStyle(
                "-fx-text-fill: #059669;"
        );
    }


    // ============================================================
    // CLEAR VALIDATION
    // ============================================================

    private void clearValidation() {

        if (validationLabel != null) {

            validationLabel.setText("");

            validationLabel.setStyle(
                    "-fx-text-fill: #DC2626;"
            );
        }
    }


    // ============================================================
    // INFORMATION ALERT
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


    // ============================================================
    // ERROR ALERT
    // ============================================================

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
}
