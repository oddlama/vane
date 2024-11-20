package org.oddlama.vane.admin;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "admin", bstats = 8638, config_version = 2, lang_version = 2, storage_version = 1)
public class Admin extends Module<Admin> {

    public Admin() {
        // Create components
        new org.oddlama.vane.admin.commands.Gamemode(this);
        new org.oddlama.vane.admin.commands.SlimeChunk(this);
        new org.oddlama.vane.admin.commands.Time(this);
        new org.oddlama.vane.admin.commands.Weather(this);

        var autostop_group = new AutostopGroup(this);
        new AutostopListener(autostop_group);
        new org.oddlama.vane.admin.commands.Autostop(autostop_group);

        new SpawnProtection(this);
        new WorldProtection(this);
        new HazardProtection(this);
        new ChatMessageFormatter(this);
    }
}
