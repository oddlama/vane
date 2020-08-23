/**
 * PacketWrapper - ProtocolLib wrappers for Minecraft packets
 * Copyright (C) dmulloy2 <http://dmulloy2.net>
 * Copyright (C) Kristian S. Strangeland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.oddlama.vane.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.mojang.brigadier.suggestion.Suggestions;

public class WrapperPlayServerTabComplete extends AbstractPacket {
	public static final PacketType TYPE = PacketType.Play.Server.TAB_COMPLETE;

	public WrapperPlayServerTabComplete() {
		super(new PacketContainer(TYPE), TYPE);
		handle.getModifier().writeDefaults();
	}

	public WrapperPlayServerTabComplete(PacketContainer packet) {
		super(packet, TYPE);
	}

	public int getTransactionId() {
		return handle.getIntegers().read(0);
	}

	public void setTransactionId(int value) {
		handle.getIntegers().write(0, value);
	}

	public Suggestions getSuggestions() {
		return handle.getSpecificModifier(Suggestions.class).read(0);
	}

	public void setSuggestions(Suggestions value) {
		handle.getSpecificModifier(Suggestions.class).write(0, value);
	}

}
