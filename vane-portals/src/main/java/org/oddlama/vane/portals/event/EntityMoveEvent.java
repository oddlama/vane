package org.oddlama.vane.portals.event;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EntityMoveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Entity entity;
    private Location from;
    private Location to;

    public EntityMoveEvent(final Entity entity, final Location from, final Location to) {
        this.entity = entity;
        this.from = from;
        this.to = to;
    }

    public Entity getEntity() {
        return entity;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
