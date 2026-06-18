package me.mrmango404.cauldron;

import me.mrmango404.utils.ColorLayerManager;
import me.mrmango404.utils.PotionDataStore;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Set;

public class CauldronPotionHandler {

	private static final Set<PotionType> BASE_POTIONS = Set.of(
			PotionType.WATER, PotionType.MUNDANE, PotionType.THICK, PotionType.AWKWARD
	);

	private final Block block;
	private final Player player;
	private final EquipmentSlot hand;

	public CauldronPotionHandler(Block block, Player player, EquipmentSlot hand) {
		this.block = block;
		this.player = player;
		this.hand = hand;
	}

	public void handlePourPotion(ItemStack potion) {
		if (!(potion.getItemMeta() instanceof PotionMeta meta)) return;
		PotionType type = meta.getBasePotionType();
		if (type == null || BASE_POTIONS.contains(type)) return;

		Material bottleType = potion.getType();

		if (block.getType() == Material.CAULDRON) {
			block.setType(Material.WATER_CAULDRON);
			PotionDataStore.store(block.getLocation(), type, bottleType);
			setLevel(1);
			ColorLayerManager.spawn(block.getLocation(), getPotionColor(type), 1);
			consumeItem(potion);
			giveItem(new ItemStack(Material.GLASS_BOTTLE));
			player.swingHand(hand);
			playSound(Sound.ITEM_BOTTLE_EMPTY);
			return;
		}

		int currentLevel = getLevel();
		if (currentLevel >= 3) return;

		if (PotionDataStore.has(block.getLocation())) {
			PotionType existingType = PotionDataStore.getType(block.getLocation()).orElse(null);
			if (existingType == null || !existingType.equals(type)) {
				clearCauldron();
				consumeItem(potion);
				giveItem(new ItemStack(Material.GLASS_BOTTLE));
				playSound(Sound.ENTITY_GENERIC_SPLASH);
				return;
			}
			PotionDataStore.updateBottleType(block.getLocation(), bottleType);
		} else {
			PotionDataStore.store(block.getLocation(), type, bottleType);
		}

		int newLevel = currentLevel + 1;
		setLevel(newLevel);
		Color color = getPotionColor(type);
		if (ColorLayerManager.getEntity(block.getLocation()).isPresent()) {
			ColorLayerManager.update(block.getLocation(), color, newLevel);
		} else {
			ColorLayerManager.spawn(block.getLocation(), color, newLevel);
		}
		consumeItem(potion);
		giveItem(new ItemStack(Material.GLASS_BOTTLE));
		player.swingHand(hand);
		playSound(Sound.ITEM_BOTTLE_EMPTY);
	}

	public void handleTakePotion() {
		if (!PotionDataStore.has(block.getLocation())) return;

		PotionType type = PotionDataStore.getType(block.getLocation()).orElse(null);
		if (type == null) return;

		Material bottleType = PotionDataStore.getBottleType(block.getLocation());
		int currentLevel = getLevel();
		if (currentLevel <= 0) return;

		consumeItem(getItemInHand());
		giveItem(buildPotion(type, bottleType));
		player.swingHand(hand);

		if (currentLevel - 1 == 0) {
			clearCauldron();
		} else {
			int newLevel = currentLevel - 1;
			setLevel(newLevel);
			ColorLayerManager.update(block.getLocation(), getPotionColor(type), newLevel);
		}

		playSound(Sound.ITEM_BOTTLE_FILL);
	}

	public void handleDipArrows(ItemStack arrows) {
		if (!PotionDataStore.has(block.getLocation())) return;

		PotionType type = PotionDataStore.getType(block.getLocation()).orElse(null);
		if (type == null) return;

		int level = getLevel();
		int maxConvert = level * 16;
		int convert = Math.min(arrows.getAmount(), maxConvert);
		if (convert <= 0) return;

		arrows.setAmount(arrows.getAmount() - convert);
		if (arrows.getAmount() <= 0) {
			setItemInHand(new ItemStack(Material.AIR));
		}

		giveItem(buildTippedArrows(type, convert));
		clearCauldron();
		player.swingHand(hand);
		playSound(Sound.ENTITY_PLAYER_SPLASH);
	}

	private void clearCauldron() {
		ColorLayerManager.remove(block.getLocation());
		PotionDataStore.remove(block.getLocation());
		block.setType(Material.CAULDRON);
	}

	@SuppressWarnings("deprecation")
	private static Color getPotionColor(PotionType type) {
		PotionEffectType effectType = type.getEffectType();
		if (effectType != null) {
			Color color = effectType.getColor();
			if (color != null) return color;
		}
		return Color.fromRGB(0x385DC6); // fallback: water potion blue
	}

	private int getLevel() {
		if (block.getBlockData() instanceof Levelled data) return data.getLevel();
		return 0;
	}

	private void setLevel(int level) {
		if (block.getBlockData() instanceof Levelled data) {
			data.setLevel(level);
			block.setBlockData(data);
		}
	}

	private ItemStack buildPotion(PotionType type, Material bottleType) {
		ItemStack item = new ItemStack(bottleType);
		if (item.getItemMeta() instanceof PotionMeta meta) {
			meta.setBasePotionType(type);
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack buildTippedArrows(PotionType type, int amount) {
		ItemStack item = new ItemStack(Material.TIPPED_ARROW, amount);
		if (item.getItemMeta() instanceof PotionMeta meta) {
			meta.setBasePotionType(type);
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack getItemInHand() {
		return hand == EquipmentSlot.HAND
				? player.getInventory().getItemInMainHand()
				: player.getInventory().getItemInOffHand();
	}

	private void setItemInHand(ItemStack item) {
		if (hand == EquipmentSlot.HAND) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.getInventory().setItemInOffHand(item);
		}
	}

	private void consumeItem(ItemStack item) {
		if (item.getAmount() <= 1) {
			setItemInHand(new ItemStack(Material.AIR));
		} else {
			item.setAmount(item.getAmount() - 1);
		}
	}

	private void giveItem(ItemStack item) {
		if (player.getInventory().firstEmpty() == -1) {
			player.getWorld().dropItem(player.getLocation(), item);
		} else {
			player.getInventory().addItem(item);
		}
	}

	private void playSound(Sound sound) {
		block.getWorld().playSound(block.getLocation().clone().add(0.5, 1, 0.5), sound, 1f, 1f);
	}
}
