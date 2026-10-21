package org.oddlama.vane.core.command.params

import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.Param

/**
 * Base implementation for command parameters.
 *
 * @param command the owning command.
 */
abstract class BaseParam(override val command: Command<*>?) : Param {
    /** Child parameter nodes. */
    override val params: MutableList<Param?> = mutableListOf()
}
