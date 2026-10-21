package org.oddlama.vane.proxycore.listeners

/** Contract for events that can be cancelled. */
interface ProxyCancellableEvent {
    /** Cancels the event with an implementation-defined default reason. */
    fun cancel()

    /**
     * Cancels the event with a custom [reason].
     *
     * @param reason cancellation reason shown to the user, if applicable.
     */
    fun cancel(reason: String?)
}
