package org.oddlama.imex.core;

import org.oddlama.imex.annotation.ConfigVersion;
import org.oddlama.imex.annotation.ImexModule;
import org.oddlama.imex.annotation.LangVersion;

@ImexModule
public class Core extends Module {
	@ConfigVersion(1)
	public long config_version;

	@LangVersion(1)
	public long lang_version;
}
