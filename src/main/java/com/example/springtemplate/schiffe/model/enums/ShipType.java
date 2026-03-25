package com.example.springtemplate.schiffe.model.enums;

public enum ShipType {
    SCHLACHTSCHIFF(4, "Schlachtschiff"),
    KREUZER(3, "Kreuzer"),
    ZERSTOERER(2, "Zerstörer"),
    UBOOT(1, "U-Boot");

    private final int size;
    private final String displayName;

    ShipType(int size, String displayName) {
        this.size = size;
        this.displayName = displayName;
    }

    public int getSize() {
        return size;
    }

    public String getDisplayName() {
        return displayName;
    }
}