package org.oddlama.vane.proxycore;

public interface ProxyEvent {

	ProxyPendingConnection get_connection();

	void fire();

}
