package dev.cpvp.module;

public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), UTILITY("Utility"),
    PLAYER("Player"), WORLD("World"),
    SETTINGS("Settings"); // not a column - opened from the gear button
    public final String label;
    Category(String label) { this.label = label; }
}
