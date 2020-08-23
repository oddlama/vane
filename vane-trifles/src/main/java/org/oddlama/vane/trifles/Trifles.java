package org.oddlama.vane.trifles;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.module.Module;

@VaneModule("trifles")
public class Trifles extends Module<Trifles> {
	// Configuration
	@ConfigVersion(1)
	public long config_version;

	// Language
	@LangVersion(1)
	public long lang_version;

	public Trifles() {
		var fast_walking_group = new FastWalkingGroup(this);
		new FastWalkingListener(fast_walking_group);
		new DoubleDoorListener(this);
	}
}
