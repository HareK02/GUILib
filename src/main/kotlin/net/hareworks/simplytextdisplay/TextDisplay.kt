package net.hareworks.simplytextdisplay

import org.bukkit.Location
import org.bukkit.entity.EntityType
import com.comphenix.protocol.wrappers.WrappedDataWatcher
import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketContainer
import java.util.UUID

class TextDisplay {
	var id: Int
	var text: String
	var location: Location
	var scale: Array<Float>
	var translation: Array<Float>
	var leftRotation: Array<Float>
	var rightRotation: Array<Float>

	var visibleDistance: Int = 0
	var updateInterval: Int = 0

	constructor(id: Int, text: String, location: Location, scale: Array<Float>, translation: Array<Float>, leftRotation: Array<Float>, rightRotation: Array<Float>, updateInterval: Int, visibleDistance: Int) {
		this.id = id
		this.text = text
		this.location = location
		this.scale = scale
		this.translation = translation
		this.leftRotation = leftRotation
		this.rightRotation = rightRotation
		this.visibleDistance = visibleDistance
		this.updateInterval = updateInterval
	}

	public fun update() {
		// for (player in location.world.getNearbyEntities(location, visibleDistance.toDouble(), visibleDistance.toDouble(), visibleDistance.toDouble())) {
		// 	send(spawnPacket(), player)
		// }
	}

	public fun spawnPacket(): PacketContainer {
		val packet = PacketContainer(PacketType.Play.Server.SPAWN_ENTITY)
		packet.getIntegers().write(0, 100);
    packet.getUUIDs().write(0, UUID.randomUUID());
    packet.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);

    packet.getDoubles()
            .write(0, location.getX())
            .write(1, location.getY())
            .write(2, location.getZ());

		packet.getIntegers()
            .write(4, (location.getPitch() * 256.0F / 360.0F).toInt())
            .write(5, (location.getYaw() * 256.0F / 360.0F).toInt());
		return packet
	}

	public fun destroyPacket(): PacketContainer{
		val packet = PacketContainer(PacketType.Play.Server.ENTITY_DESTROY)
		packet.getIntegerArrays().write(0, intArrayOf(100))
		return packet
	}
}