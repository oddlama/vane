package org.oddlama.vane.portals.menu;

import java.util.UUID;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.Portals;

public class PortalMenuTag {
	private UUID portal_id = null;

	public PortalMenuTag(final UUID portal_id) {
		this.portal_id = portal_id;
	}

	public UUID portal_id() {
		return portal_id;
	}
}
