package org.oddlama.vane.core.command.enums;

public enum WeatherValue {
    clear(false, false),
    sun(false, false),
    rain(true, false),
    thunder(true, true);

    private boolean is_storm;
    private boolean is_thunder;

    private WeatherValue(boolean is_storm, boolean is_thunder) {
        this.is_storm = is_storm;
        this.is_thunder = is_thunder;
    }

    public boolean storm() {
        return is_storm;
    }

    public boolean thunder() {
        return is_thunder;
    }
}
