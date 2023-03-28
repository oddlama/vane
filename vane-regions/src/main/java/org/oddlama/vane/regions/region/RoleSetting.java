package org.oddlama.vane.regions.region;

import org.oddlama.vane.regions.Regions;

public enum RoleSetting {
	ADMIN(false, true),
	BUILD(false, true),
	USE(true, true),
	CONTAINER(false, true),
	PORTAL(false, true);

	private boolean def;
	private boolean def_admin;

	private RoleSetting(final boolean def, final boolean def_admin) {
		this.def = def;
		this.def_admin = def_admin;
	}

	public boolean default_value(final boolean admin) {
		if (admin) {
			return def_admin;
		}
		return def;
	}

	public boolean has_override() {
		return get_override() != 0;
	}

	public int get_override() {
		return Regions.role_overrides.get_override(this);
	}
}
