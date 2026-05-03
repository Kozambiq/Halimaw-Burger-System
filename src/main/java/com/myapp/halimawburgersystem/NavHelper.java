package com.myapp.halimawburgersystem;

import javafx.scene.control.Button;

public class NavHelper {

    public static void setActiveNav(
            Button[] buttons, String navName) {

        clearAllHighlights(buttons);

        if (buttons == null || navName == null) return;

        String target = navName;
        int index = getNavIndex(target);
        if (index >= 0 && index < buttons.length && buttons[index] != null) {
            buttons[index].getStyleClass().add("nav-item-active");
        }
    }

    public static void clearAllHighlights(Button[] buttons) {
        if (buttons == null) return;
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-item-active");
            }
        }
    }

    private static int getNavIndex(String navName) {
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