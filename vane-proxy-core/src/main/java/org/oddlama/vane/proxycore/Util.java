package org.oddlama.vane.proxycore;

import java.util.UUID;

public class Util {

	public static UUID add_uuid(UUID uuid, long i) {
		var msb = uuid.getMostSignificantBits();
		var lsb = uuid.getLeastSignificantBits();

		lsb += i;
		if (lsb < uuid.getLeastSignificantBits()) {
			++msb;
		}

		return new UUID(msb, lsb);
	}

}
