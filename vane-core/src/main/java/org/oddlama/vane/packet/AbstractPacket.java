/**
 * PacketWrapper - ProtocolLib wrappers for Minecraft packets Copyright (C) dmulloy2
 * <http://dmulloy2.net> Copyright (C) Kristian S. Strangeland
 *
 * <p>This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package org.oddlama.vane.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Objects;
import org.bukkit.entity.Player;

public abstract class AbstractPacket {

    // The packet we will be modifying
    protected PacketContainer handle;

    /**
     * Constructs a new strongly typed wrapper for the given packet.
     *
     * @param handle - handle to the raw packet data.
     * @param type - the packet type.
     */
    protected AbstractPacket(PacketContainer handle, PacketType type) {
        // Make sure we're given a valid packet
        if (handle == null) throw new IllegalArgumentException("Packet handle cannot be NULL.");
        if (!Objects.equal(handle.getType(), type)) throw new IllegalArgumentException(
            handle.getHandle() + " is not a packet of type " + type
        );

        this.handle = handle;
    }

    /**
     * Retrieve a handle to the raw packet data.
     *
     * @return Raw packet data.
     */
    public PacketContainer getHandle() {
        return handle;
    }

    /**
     * Send the current packet to the given receiver.
     *
     * @param receiver - the receiver.
     * @throws RuntimeException If the packet cannot be sent.
     */
    public void sendPacket(Player receiver) {
        ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, getHandle());
    }

    /** Send the current packet to all online players. */
    public void broadcastPacket() {
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(getHandle());
    }
}
