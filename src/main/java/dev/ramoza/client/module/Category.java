package dev.ramoza.client.module;

public enum Category {
    COMBAT("Combat"),
    RENDER("Render"),
    MISC("Misc"),
    POT_CHESTS("Pot Chests"),
    PLAYER("Player");

    private final String title;

    Category(String title) { this.title = title; }

    public String title() { return title; }
}
