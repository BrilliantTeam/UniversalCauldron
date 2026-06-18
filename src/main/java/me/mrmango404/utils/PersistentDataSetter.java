package me.mrmango404.utils;

import me.mrmango404.UniversalCauldron;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PersistentDataSetter {

	private static final UniversalCauldron plugin = UniversalCauldron.getInstance();
	private static final NamespacedKey COLOR = new NamespacedKey(plugin, "color");

	public static void storeColorData(TextDisplay entity, Color color) {
		entity.getPersistentDataContainer().set(
				COLOR,
				PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER),
				List.of(color.getRed(), color.getGreen(), color.getBlue())
		);
	}

	public static boolean hasColorData(TextDisplay entity) {
		return entity.getPersistentDataContainer().has(COLOR);
	}

	public static Optional<Color> getColorData(TextDisplay entity) {
		List<Integer> rgb = entity.getPersistentDataContainer().get(COLOR, PersistentDataType.LIST.integers());
		if (rgb == null || rgb.size() < 3) return Optional.empty();
		return Optional.of(Color.fromRGB(rgb.get(0), rgb.get(1), rgb.get(2)));
	}

	private static NamespacedKey locationKey(Location loc) {
		return new NamespacedKey(plugin, loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ());
	}

	public static void storeDisplayUUID(Location blockLocation, UUID uuid) {
		blockLocation.getChunk().getPersistentDataContainer()
				.set(locationKey(blockLocation), PersistentDataType.STRING, uuid.toString());
	}

	public static void removeDisplayUUID(Location blockLocation) {
		blockLocation.getChunk().getPersistentDataContainer()
				.remove(locationKey(blockLocation));
	}

	public static boolean hasDisplayUUID(Location blockLocation) {
		return blockLocation.getChunk().getPersistentDataContainer()
				.has(locationKey(blockLocation), PersistentDataType.STRING);
	}

	public static Set<Location> getDisplayLocations(Chunk chunk) {
		Set<Location> locations = new HashSet<>();
		String namespace = plugin.getName().toLowerCase();

		for (NamespacedKey key : chunk.getPersistentDataContainer().getKeys()) {
			if (!key.getNamespace().equals(namespace)) continue;
			String[] parts = key.getKey().split("_");
			if (parts.length != 3) continue;
			try {
				locations.add(new Location(chunk.getWorld(),
						Integer.parseInt(parts[0]),
						Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2])));
			} catch (NumberFormatException ignored) {}
		}
		return locations;
	}
}
