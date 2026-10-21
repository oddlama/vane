package org.oddlama.vane.admin

import org.oddlama.vane.admin.commands.*
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.core.module.Module

/** Root module for administrative gameplay utilities and commands. */
@VaneModule(name = "admin", bstats = 8638, configVersion = 2, langVersion = 2, storageVersion = 1)
class Admin : Module<Admin?>() {
    /**
     * Automatic shutdown scheduling. Public so integrations such as vane-busybar can read and abort
     * it.
     */
    val autostopGroup: AutostopGroup

    init {
        Gamemode(this)
        SlimeChunk(this)
        Time(this)
        Weather(this)

        autostopGroup = AutostopGroup(this)
        AutostopListener(autostopGroup)
        Autostop(autostopGroup)

        SpawnProtection(this)
        WorldProtection(this)
        HazardProtection(this)
        ChatMessageFormatter(this)
    }
}
