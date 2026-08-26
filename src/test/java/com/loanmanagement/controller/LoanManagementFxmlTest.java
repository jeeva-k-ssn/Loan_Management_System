package com.loanmanagement.controller;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;

class LoanManagementFxmlTest {
    @Test
    void loanPortfolioFxmlLoadsWithItsController() throws Exception {
        CountDownLatch complete = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            try {
                for (String resource : new String[] {
                        "/fxml/login.fxml", "/fxml/register.fxml", "/fxml/dashboard.fxml",
                        "/fxml/apply-loan.fxml", "/fxml/pending-applications.fxml",
                        "/fxml/loans.fxml", "/fxml/admin.fxml" }) {
                    FXMLLoader.load(getClass().getResource(resource));
                }
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                complete.countDown();
            }
        });
        if (!complete.await(10, TimeUnit.SECONDS)) fail("FXML loading timed out");
        if (failure.get() != null) fail("Loan portfolio FXML failed to load", failure.get());
        Platform.exit();
    }
}
