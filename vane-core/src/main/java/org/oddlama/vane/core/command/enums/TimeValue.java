package org.oddlama.vane.core.command.enums;

public enum TimeValue {
    dawn(23000),
    day(1000),
    noon(6000),
    afternoon(9000),
    dusk(13000),
    night(14000),
    midnight(18000);

    private int ticks;

    private TimeValue(int ticks) {
        this.ticks = ticks;
    }

    public int ticks() {
        return ticks;
    }
}
