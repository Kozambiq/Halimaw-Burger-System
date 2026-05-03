package com.myapp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Shared service to handle real-time notifications between different modules.
 * This ensures UI updates (like the Kitchen Queue) happen instantly without waiting for timers.
 */
public class OrderNotificationService {
    private static final List<Runnable> listeners = new ArrayList<>();

    /**
     * Subscribe a module (like Kitchen, Inventory, or Dashboard) to system-wide updates.
     * @param listener The method to run when an update occurs.
     */
    public static synchronized void subscribe(Runnable listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unsubscribe a module from updates.
     */
    public static synchronized void unsubscribe(Runnable listener) {
        listeners.remove(listener);
    }

    /**
     * "Shout" to the entire system that data has changed (e.g., order placed, status updated, stock reserved).
     * All subscribed modules will instantly reload their database content.
     */
    public static synchronized void broadcastUpdate() {
        for (Runnable listener : listeners) {
            javafx.application.Platform.runLater(listener);
        }
    }
}