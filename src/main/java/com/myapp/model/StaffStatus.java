package com.myapp.model;

public enum StaffStatus {
    ACTIVE("Active"),
    OFF_SHIFT("Off Shift"),
    BREAK("Break"),
    DISABLED("Disabled");

    private final String value;

    StaffStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StaffStatus fromString(String value) {
        if (value == null) return null;
        for (StaffStatus status : StaffStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}