package org.oddlama.vane.regions.region;

public enum RoleSetting {
	ADMIN(false, true),
	BUILD(false, true),
	USE(false, true),
	CONTAINER(false, true),
	;

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
}
