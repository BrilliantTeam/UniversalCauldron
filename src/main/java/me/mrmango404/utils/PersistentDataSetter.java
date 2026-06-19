package me.mrmango404.utils;

import me.mrmango404.UniversalCauldron;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PersistentDataSetter {

	private static final UniversalCauldron plugin = UniversalCauldron.getInstance();
	private static final NamespacedKey COLOR = new NamespacedKey(plugin, "color");
	private static final NamespacedKey LOCATIONS = new NamespacedKey(plugin, "locations");

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

	public static void storeDisplayUUID(Location blockLocation) {
		PersistentDataContainer pdc = blockLocation.getChunk().getPersistentDataContainer();
		List<Integer> updated = new ArrayList<>(
				pdc.getOrDefault(LOCATIONS, PersistentDataType.LIST.integers(), List.of()));
		updated.add(blockLocation.getBlockX());
		updated.add(blockLocation.getBlockY());
		updated.add(blockLocation.getBlockZ());
		pdc.set(LOCATIONS, PersistentDataType.LIST.integers(), updated);
	}

	public static void removeDisplayUUID(Location blockLocation) {
		PersistentDataContainer pdc = blockLocation.getChunk().getPersistentDataContainer();
		List<Integer> updated = new ArrayList<>(
				pdc.getOrDefault(LOCATIONS, PersistentDataType.LIST.integers(), List.of()));
		int x = blockLocation.getBlockX(), y = blockLocation.getBlockY(), z = blockLocation.getBlockZ();
		for (int i = 0; i + 2 < updated.size(); i += 3) {
			if (updated.get(i) == x && updated.get(i + 1) == y && updated.get(i + 2) == z) {
				updated.remove(i + 2);
				updated.remove(i + 1);
				updated.remove(i);
				break;
			}
		}
		if (updated.isEmpty()) {
			pdc.remove(LOCATIONS);
		} else {
			pdc.set(LOCATIONS, PersistentDataType.LIST.integers(), updated);
		}
	}

	public static boolean hasDisplayUUID(Location blockLocation) {
		List<Integer> list = blockLocation.getChunk().getPersistentDataContainer()
				.getOrDefault(LOCATIONS, PersistentDataType.LIST.integers(), List.of());
		int x = blockLocation.getBlockX(), y = blockLocation.getBlockY(), z = blockLocation.getBlockZ();
		for (int i = 0; i + 2 < list.size(); i += 3) {
			if (list.get(i) == x && list.get(i + 1) == y && list.get(i + 2) == z) return true;
		}
		return false;
	}

	public static Set<Location> getDisplayLocations(Chunk chunk) {
		List<Integer> list = chunk.getPersistentDataContainer()
				.getOrDefault(LOCATIONS, PersistentDataType.LIST.integers(), List.of());
		Set<Location> locations = new HashSet<>();
		for (int i = 0; i + 2 < list.size(); i += 3) {
			locations.add(new Location(chunk.getWorld(), list.get(i), list.get(i + 1), list.get(i + 2)));
		}
		return locations;
	}
}
