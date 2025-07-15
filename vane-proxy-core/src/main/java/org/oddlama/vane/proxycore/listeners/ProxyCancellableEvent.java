package org.oddlama.vane.proxycore.listeners;

public interface ProxyCancellableEvent {
    void cancel();

    void cancel(String reason);
}
