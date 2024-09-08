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
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import java.util.Arrays;

public class WrapperPlayServerChat extends AbstractPacket {

	public static final PacketType TYPE = PacketType.Play.Server.CHAT;

	public WrapperPlayServerChat() {
		super(new PacketContainer(TYPE), TYPE);
		handle.getModifier().writeDefaults();
	}

	public WrapperPlayServerChat(PacketContainer packet) {
		super(packet, TYPE);
	}

	/**
	 * Retrieve the chat message.
	 * <p>
	 * Limited to 32.767 bytes
	 *
	 * @return The current message
	 */
	public WrappedChatComponent getMessage() {
		return handle.getChatComponents().read(0);
	}

	/**
	 * Set the message.
	 *
	 * @param value - new value.
	 */
	public void setMessage(WrappedChatComponent value) {
		handle.getChatComponents().write(0, value);
	}

	public ChatType getChatType() {
		return handle.getChatTypes().read(0);
	}

	public void setChatType(ChatType type) {
		handle.getChatTypes().write(0, type);
	}

	/**
	 * Retrieve Position.
	 * <p>
	 * Notes: 0 - Chat (chat box), 1 - System Message (chat box), 2 - Above
	 * action bar
	 *
	 * @return The current Position
	 * @deprecated Magic values replaced by enum
	 */
	@Deprecated
	public byte getPosition() {
		Byte position = handle.getBytes().readSafely(0);
		if (position != null) {
			return position;
		} else {
			return getChatType().getId();
		}
	}

	/**
	 * Set Position.
	 *
	 * @param value - new value.
	 * @deprecated Magic values replaced by enum
	 */
	@Deprecated
	public void setPosition(byte value) {
		handle.getBytes().writeSafely(0, value);

		if (EnumWrappers.getChatTypeClass() != null) {
			Arrays
				.stream(ChatType.values())
				.filter(t -> t.getId() == value)
				.findAny()
				.ifPresent(t -> handle.getChatTypes().writeSafely(0, t));
		}
	}
}
