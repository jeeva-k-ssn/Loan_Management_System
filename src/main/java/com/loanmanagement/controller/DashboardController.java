package com.loanmanagement.controller;

import com.loanmanagement.database.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardController {

    @FXML
    private Label customerCountLabel;

    @FXML
    private Label activeLoanCountLabel;

    @FXML
    private Label pendingApplicationCountLabel;

    @FXML
    private Label totalPaymentLabel;

    @FXML
    private VBox recentLoanContainer;


    @FXML
    public void initialize() {

        loadDashboardData();
        loadRecentLoans();

    }


    // =========================================================
    // LOAD DASHBOARD DATA
    // =========================================================

    private void loadDashboardData() {

        loadCustomerCount();
        loadActiveLoanCount();
        loadPendingApplicationCount();
        loadTotalPayments();

    }


    // =========================================================
    // CUSTOMER COUNT
    // =========================================================

    private void loadCustomerCount() {

        String sql = "SELECT COUNT(*) FROM LMS_CUSTOMER";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            if (result.next()) {

                customerCountLabel.setText(
                        String.valueOf(result.getInt(1))
                );

            }

        } catch (Exception e) {

            e.printStackTrace();

        }
    }


    // =========================================================
    // ACTIVE LOAN COUNT
    // =========================================================

    private void loadActiveLoanCount() {

        String sql =
                "SELECT COUNT(*) " +
                "FROM LOAN " +
                "WHERE STATUS = 'ACTIVE'";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            if (result.next()) {

                activeLoanCountLabel.setText(
                        String.valueOf(result.getInt(1))
                );

            }

        } catch (Exception e) {

            e.printStackTrace();

        }
    }


    // =========================================================
    // PENDING APPLICATION COUNT
    // =========================================================

    private void loadPendingApplicationCount() {

        String sql =
                "SELECT COUNT(*) " +
                "FROM LOAN_APPLICATION " +
                "WHERE STATUS = 'PENDING'";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            if (result.next()) {

                pendingApplicationCountLabel.setText(
                        String.valueOf(result.getInt(1))
                );

            }

        } catch (Exception e) {

            e.printStackTrace();

        }
    }


    // =========================================================
    // TOTAL PAYMENTS
    // =========================================================

    private void loadTotalPayments() {

        String sql =
                "SELECT NVL(SUM(AMOUNT), 0) " +
                "FROM PAYMENT " +
                "WHERE PAYMENT_STATUS = 'PAID'";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            if (result.next()) {

                double total = result.getDouble(1);

                totalPaymentLabel.setText(
                        "₹ " + String.format("%.2f", total)
                );

            }

        } catch (Exception e) {

            e.printStackTrace();

        }
    }


    // =========================================================
    // RECENT LOAN ACTIVITY
    // =========================================================

    private void loadRecentLoans() {

        String sql =
                "SELECT LOAN_ID, CUSTOMER_ID, LOAN_AMOUNT, STATUS " +
                "FROM LOAN " +
                "ORDER BY LOAN_ID DESC " +
                "FETCH FIRST 5 ROWS ONLY";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            recentLoanContainer.getChildren().clear();

            boolean hasLoans = false;

            while (result.next()) {

                hasLoans = true;

                int loanId = result.getInt("LOAN_ID");

                int customerId =
                        result.getInt("CUSTOMER_ID");

                double amount =
                        result.getDouble("LOAN_AMOUNT");

                String status =
                        result.getString("STATUS");


                HBox loanRow = new HBox(10);

                loanRow.getStyleClass().add("loan-row");


                Label loanIdLabel =
                        new Label(String.valueOf(loanId));

                Label customerLabel =
                        new Label(String.valueOf(customerId));

                Label amountLabel =
                        new Label(
                                "₹ " +
                                String.format("%.2f", amount)
                        );

                Label statusLabel =
                        new Label(status);


                loanIdLabel.setPrefWidth(100);
                customerLabel.setPrefWidth(150);
                amountLabel.setPrefWidth(150);
                statusLabel.setPrefWidth(100);


                loanRow.getChildren().addAll(
                        loanIdLabel,
                        customerLabel,
                        amountLabel,
                        statusLabel
                );


                recentLoanContainer
                        .getChildren()
                        .add(loanRow);
            }


            if (!hasLoans) {

                Label emptyLabel =
                        new Label("No recent loan activity");

                emptyLabel.getStyleClass()
                        .add("empty-message");

                recentLoanContainer
                        .getChildren()
                        .add(emptyLabel);
            }

        } catch (Exception e) {

            e.printStackTrace();

        }
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @FXML
    public void logout(ActionEvent event) {

        try {

            Parent root =
                    FXMLLoader.load(
                            getClass().getResource(
                                    "/fxml/login.fxml"
                            )
                    );

            Stage stage =
                    (Stage)
                    ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            stage.setScene(
                    new Scene(root)
            );

            stage.setTitle("Login");

        } catch (Exception e) {

            e.printStackTrace();

        }
    }

}