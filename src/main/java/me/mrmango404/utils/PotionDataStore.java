package me.mrmango404.utils;

import me.mrmango404.UniversalCauldron;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PotionDataStore {

	private static final UniversalCauldron plugin = UniversalCauldron.getInstance();
	private static final NamespacedKey POT_DATA = new NamespacedKey(plugin, "pot_data");

	private static List<String> getList(Chunk chunk) {
		return new ArrayList<>(chunk.getPersistentDataContainer()
				.getOrDefault(POT_DATA, PersistentDataType.LIST.strings(), List.of()));
	}

	private static void saveList(Chunk chunk, List<String> list) {
		PersistentDataContainer pdc = chunk.getPersistentDataContainer();
		if (list.isEmpty()) {
			pdc.remove(POT_DATA);
		} else {
			pdc.set(POT_DATA, PersistentDataType.LIST.strings(), list);
		}
	}

	private static String encode(Location loc, PotionType type, Material bottleType) {
		return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
				+ "," + type.name() + "," + bottleType.name();
	}

	private static boolean matchesLoc(String entry, Location loc) {
		return entry.startsWith(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ",");
	}

	public static void store(Location loc, PotionType type, Material bottleType) {
		Chunk chunk = loc.getChunk();
		List<String> list = getList(chunk);
		String encoded = encode(loc, type, bottleType);
		boolean found = false;
		for (int i = 0; i < list.size(); i++) {
			if (matchesLoc(list.get(i), loc)) {
				list.set(i, encoded);
				found = true;
				break;
			}
		}
		if (!found) list.add(encoded);
		saveList(chunk, list);
	}

	public static boolean has(Location loc) {
		return getList(loc.getChunk()).stream().anyMatch(e -> matchesLoc(e, loc));
	}

	public static Optional<PotionType> getType(Location loc) {
		return getList(loc.getChunk()).stream()
				.filter(e -> matchesLoc(e, loc))
				.findFirst()
				.map(e -> {
					try {
						return PotionType.valueOf(e.split(",", 5)[3]);
					} catch (IllegalArgumentException ex) {
						return null;
					}
				});
	}

	public static Material getBottleType(Location loc) {
		return getList(loc.getChunk()).stream()
				.filter(e -> matchesLoc(e, loc))
				.findFirst()
				.map(e -> {
					try {
						return Material.valueOf(e.split(",", 5)[4]);
					} catch (IllegalArgumentException ex) {
						return Material.POTION;
					}
				})
				.orElse(Material.POTION);
	}

	public static void updateBottleType(Location loc, Material bottleType) {
		getType(loc).ifPresent(type -> store(loc, type, bottleType));
	}

	public static void remove(Location loc) {
		Chunk chunk = loc.getChunk();
		List<String> list = getList(chunk);
		list.removeIf(e -> matchesLoc(e, loc));
		saveList(chunk, list);
	}

	public static void cleanupChunk(Chunk chunk) {
		List<String> list = getList(chunk);
		list.removeIf(entry -> {
			try {
				String[] parts = entry.split(",", 5);
				Location loc = new Location(chunk.getWorld(),
						Integer.parseInt(parts[0]),
						Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2]));
				return loc.getBlock().getType() != Material.WATER_CAULDRON;
			} catch (Exception ignored) {
				return true;
			}
		});
		saveList(chunk, list);
	}
}
