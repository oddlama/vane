package org.oddlama.vane.core;

import org.oddlama.vane.annotation.ConfigVersion;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.LangVersion;

@VaneModule
public class Core extends Module {
	@ConfigVersion(1)
	public long config_version;

	@LangVersion(1)
	public long lang_version;

	@Override
	protected void on_enable() {
	}

	@Override
	protected void on_disable() {
	}

	@Override
	protected void on_config_change() {
	}
}
