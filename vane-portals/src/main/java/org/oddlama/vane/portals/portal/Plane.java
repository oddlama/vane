package org.oddlama.vane.portals.portal;

public enum Plane {
    XY(true, true, false),
    YZ(false, true, true),
    XZ(true, false, true);

    private boolean x;
    private boolean y;
    private boolean z;

    private Plane(boolean x, boolean y, boolean z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean x() {
        return x;
    }

    public boolean y() {
        return y;
    }

    public boolean z() {
        return z;
    }
}
