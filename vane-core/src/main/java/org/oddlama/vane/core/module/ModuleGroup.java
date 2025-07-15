package org.oddlama.vane.core.module;

import org.oddlama.vane.annotation.config.ConfigBoolean;

/**
 * A ModuleGroup is a ModuleContext that automatically adds an enabled variable with description to
 * the context. If the group is disabled, on_enable() will not be called.
 */
public class ModuleGroup<T extends Module<T>> extends ModuleContext<T> {

    @ConfigBoolean(def = true, desc = "") // desc is set by #config_enabled_desc()
    public boolean config_enabled;

    public boolean config_enabled_def = true;
    private String config_enabled_desc;

    public boolean config_enabled_def() {
        return config_enabled_def;
    }

    public String config_enabled_desc() {
        return config_enabled_desc;
    }

    public ModuleGroup(Context<T> context, String group, String description) {
        this(context, group, description, true);
    }

    public ModuleGroup(Context<T> context, String group, String description, boolean compile_self) {
        super(context, group, null, ".", false);
        this.config_enabled_desc = description;

        if (compile_self) {
            compile_self();
        }
    }

    @Override
    public boolean enabled() {
        return config_enabled;
    }

    @Override
    public void enable() {
        if (config_enabled) {
            super.enable();
        }
    }

    @Override
    public void disable() {
        if (config_enabled) {
            super.disable();
        }
    }
}
