package org.oddlama.vane.trifles;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "trifles", bstats = 8644, config_version = 1, lang_version = 1, storage_version = 1)
public class Trifles extends Module<Trifles> {
	public Trifles() {
		var fast_walking_group = new FastWalkingGroup(this);
		new FastWalkingListener(fast_walking_group);
		new DoubleDoorListener(this);
	}
}
