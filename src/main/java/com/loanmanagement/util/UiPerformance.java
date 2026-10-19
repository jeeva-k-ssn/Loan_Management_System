package com.loanmanagement.util;

import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;

/** Shared rendering hints for LoanFlow's card-based scrollable workspaces. */
public final class UiPerformance {
    private UiPerformance() { }

    public static void optimizeAncestor(Node node) {
        Parent parent = node == null ? null : node.getParent();
        while (parent != null && !(parent instanceof ScrollPane)) parent = parent.getParent();
        if (parent instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            scrollPane.getContent().setCache(true);
            scrollPane.getContent().setCacheHint(CacheHint.SPEED);
        }
    }
}
