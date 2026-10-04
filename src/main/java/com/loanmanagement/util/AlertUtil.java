package com.loanmanagement.util;

import javafx.scene.control.Alert;

/** Consistent, readable application dialogs for all LoanFlow screens. */
public final class AlertUtil {
    private AlertUtil() { }

    public static void style(Alert alert) {
        alert.setHeaderText(null);
        if (alert.getDialogPane().getScene() != null) {
            alert.getDialogPane().getScene().getStylesheets().add(
                    AlertUtil.class.getResource("/css/dashboard.css").toExternalForm());
        }
        alert.getDialogPane().getStyleClass().add("loanflow-dialog");
    }

    public static void show(Alert alert) {
        style(alert);
        alert.showAndWait();
    }
}
