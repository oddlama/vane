package org.oddlama.vane.portals.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class PortalEvent extends Event implements Cancellable {

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
