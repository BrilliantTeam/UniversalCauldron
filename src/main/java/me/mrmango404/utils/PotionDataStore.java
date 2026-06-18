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

	private static NamespacedKey key(Location loc) {
		return new NamespacedKey(plugin, "pot_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ());
	}

	public static void store(Location loc, PotionType type, Material bottleType) {
		loc.getChunk().getPersistentDataContainer()
				.set(key(loc), PersistentDataType.STRING, type.name() + "|" + bottleType.name());
	}

	public static boolean has(Location loc) {
		return loc.getChunk().getPersistentDataContainer().has(key(loc));
	}

	public static Optional<PotionType> getType(Location loc) {
		String raw = loc.getChunk().getPersistentDataContainer().get(key(loc), PersistentDataType.STRING);
		if (raw == null) return Optional.empty();
		try {
			return Optional.of(PotionType.valueOf(raw.split("\\|")[0]));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	public static Material getBottleType(Location loc) {
		String raw = loc.getChunk().getPersistentDataContainer().get(key(loc), PersistentDataType.STRING);
		if (raw == null) return Material.POTION;
		String[] parts = raw.split("\\|");
		if (parts.length < 2) return Material.POTION;
		try {
			return Material.valueOf(parts[1]);
		} catch (IllegalArgumentException e) {
			return Material.POTION;
		}
	}

	public static void updateBottleType(Location loc, Material bottleType) {
		getType(loc).ifPresent(type -> store(loc, type, bottleType));
	}

	public static void remove(Location loc) {
		loc.getChunk().getPersistentDataContainer().remove(key(loc));
	}

	public static void cleanupChunk(Chunk chunk) {
		String namespace = plugin.getName().toLowerCase();
		List<NamespacedKey> toRemove = new ArrayList<>();

		for (NamespacedKey k : chunk.getPersistentDataContainer().getKeys()) {
			if (!k.getNamespace().equals(namespace)) continue;
			if (!k.getKey().startsWith("pot_")) continue;
			String[] parts = k.getKey().split("_");
			if (parts.length != 4) continue;
			try {
				Location loc = new Location(chunk.getWorld(),
						Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2]),
						Integer.parseInt(parts[3]));
				if (loc.getBlock().getType() != Material.WATER_CAULDRON) {
					toRemove.add(k);
				}
			} catch (NumberFormatException ignored) {}
		}

		PersistentDataContainer pdc = chunk.getPersistentDataContainer();
		for (NamespacedKey k : toRemove) {
			pdc.remove(k);
		}
	}
}
