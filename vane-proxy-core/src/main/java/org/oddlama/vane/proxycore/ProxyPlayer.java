package org.oddlama.vane.proxycore;

import java.util.UUID;
import org.oddlama.vane.proxycore.commands.ProxyCommandSender;

public interface ProxyPlayer extends ProxyCommandSender {
    void disconnect(String message);

    UUID get_unique_id();

    long get_ping();
}
