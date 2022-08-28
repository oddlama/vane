package org.oddlama.vane.portals.menu;

import java.util.UUID;

public class PortalMenuTag {

	private final UUID portal_id;

	public PortalMenuTag(final UUID portal_id) {
		this.portal_id = portal_id;
	}

	public UUID portal_id() {
		return portal_id;
	}
}
