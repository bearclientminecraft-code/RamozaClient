package dev.ramoza.client.module;

public enum Category {
    COMBAT("Combat"),
    RENDER("Render"),
    MISC("Misc"),
    POT_CHEATS("Pot Cheats"),
    PLAYER("Player");

    private final String title;

    Category(String title) { this.title = title; }

    public String title() { return title; }
}
