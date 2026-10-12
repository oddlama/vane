package org.oddlama.vane.proxycore.commands

/**
 * Represents a source capable of receiving command feedback.
 */
interface ProxyCommandSender {
    /**
     * Sends a chat/system message to this sender.
     *
     * @param message formatted message content.
     */
    fun sendMessage(message: String?)

    /**
     * Sends [message], which copies [copy] to the clipboard when clicked and shows [hover] when
     * hovered, where the platform supports it.
     *
     * @param message formatted message content.
     * @param copy text copied on click.
     * @param hover formatted hover text.
     */
    fun sendCopyable(message: String, copy: String, hover: String) = sendMessage(message)
}
