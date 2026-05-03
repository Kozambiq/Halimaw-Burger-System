package com.myapp.halimawburgersystem;

import javafx.scene.control.Button;
import java.util.Arrays;

public abstract class BaseController {

    protected abstract Button[] getNavButtons();

    protected void setActiveNav(String navName) {
        clearAllHighlights();
        if (navName == null || getNavButtons() == null) return;

        Button[] buttons = getNavButtons();
        int index = getNavIndex(navName);
        if (index >= 0 && index < buttons.length && buttons[index] != null) {
            buttons[index].getStyleClass().add("nav-item-active");
        }
    }

    protected void clearAllHighlights() {
        Button[] buttons = getNavButtons();
        if (buttons == null) return;
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-item-active");
            }
        }
    }

    private int getNavIndex(String navName) {
        switch(navName) {
            case "Dashboard": return 0;
            case "Orders": return 1;
            case "Kitchen Queue": return 2;
            case "Menu Items": return 3;
            case "Combos & Promos": return 4;
            case "Inventory": return 5;
            case "Sales Reports": return 6;
            case "Staff": return 7;
            default: return -1;
        }
    }
}