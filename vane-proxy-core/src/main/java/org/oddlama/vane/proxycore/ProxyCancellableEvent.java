package org.oddlama.vane.proxycore;

public interface ProxyCancellableEvent {

	void cancel();

	void cancel(String reason);

}
