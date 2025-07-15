package org.oddlama.vane.core;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

public class Listener<T extends Module<T>> extends ModuleComponent<T> implements org.bukkit.event.Listener {

    public Listener(Context<T> context) {
        super(context);
    }

    @Override
    protected void on_enable() {
        get_module().register_listener(this);
    }

    @Override
    protected void on_disable() {
        get_module().unregister_listener(this);
    }
}
