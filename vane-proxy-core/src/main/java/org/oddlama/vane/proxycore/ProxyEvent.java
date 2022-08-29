package org.oddlama.vane.proxycore;

public interface ProxyEvent {

	void cancel();

	void cancel(String reason);

	ProxyPendingConnection get_connection();

}
