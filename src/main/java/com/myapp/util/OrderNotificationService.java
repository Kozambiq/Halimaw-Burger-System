package com.myapp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Shared service to handle real-time notifications between different modules.
 * This ensures UI updates (like the Kitchen Queue) happen instantly without waiting for timers.
 */
public class OrderNotificationService {
    private static final List<Runnable> orderListeners = new ArrayList<>();

    public static synchronized void subscribeToNewOrders(Runnable listener) {
        orderListeners.add(listener);
    }

    public static synchronized void unsubscribeFromNewOrders(Runnable listener) {
        orderListeners.remove(listener);
    }

    public static synchronized void notifyNewOrder() {
        for (Runnable listener : orderListeners) {
            javafx.application.Platform.runLater(listener);
        }
    }
}