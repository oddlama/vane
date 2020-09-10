package org.oddlama.vane.portals;

import java.util.UUID;

public class PortalBlock {
	private UUID portal_id;
	private Type type;

	public static enum Type {
		ORIGIN,
		CONSOLE,
		BOUNDARY,
		PORTAL;
	}

	public UUID portal_id() {
		return portal_id;
	}

	public Type type() {
		return Type.ORIGIN;
	}
}
