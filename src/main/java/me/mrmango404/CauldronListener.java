package me.mrmango404;

import me.mrmango404.cauldron.CauldronCleanHandler;
import me.mrmango404.cauldron.CauldronDyeHandler;
import me.mrmango404.cauldron.CauldronPotionHandler;
import me.mrmango404.cauldron.ItemDyeWashHandler;
import me.mrmango404.utils.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.Chunk;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CauldronListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onCauldronInteraction(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (ConfigHandler.Settings.DISABLED_WORLD.stream().anyMatch(player.getWorld().getName()::equals)) return;
		if (!PermissionManager.hasPermission(player, PermissionManager.INTERACTION)) return;
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;

		Block block = event.getClickedBlock();
		Material materialInHand = player.getInventory().getItemInMainHand().getType();

		if (block.getType() == Material.CAULDRON) {
			if (isPotion(materialInHand)) {
				event.setUseItemInHand(Event.Result.DENY);
				event.setUseInteractedBlock(Event.Result.DENY);
				event.setCancelled(true);
				new CauldronPotionHandler(block, player).handlePourPotion(player.getInventory().getItemInMainHand());
			}
			return;
		}

		if (block.getType() != Material.WATER_CAULDRON) return;

		if (PotionDataStore.has(block.getLocation())) {
			event.setUseItemInHand(Event.Result.DENY);
			event.setUseInteractedBlock(Event.Result.DENY);
			event.setCancelled(true);

			CauldronPotionHandler handler = new CauldronPotionHandler(block, player);
			if (isPotion(materialInHand)) {
				handler.handlePourPotion(player.getInventory().getItemInMainHand());
			} else if (materialInHand == Material.GLASS_BOTTLE) {
				handler.handleTakePotion();
			} else if (materialInHand == Material.ARROW) {
				handler.handleDipArrows(player.getInventory().getItemInMainHand());
			}
			return;
		}

		if (isPotion(materialInHand)) {
			if (ColorLayerManager.getEntity(block.getLocation()).isEmpty()) {
				event.setUseItemInHand(Event.Result.DENY);
				event.setUseInteractedBlock(Event.Result.DENY);
				event.setCancelled(true);
				new CauldronPotionHandler(block, player).handlePourPotion(player.getInventory().getItemInMainHand());
			}
			return;
		}

		Material washItem = Material.getMaterial(ConfigHandler.Settings.WASH_ITEM.toUpperCase());

		if (materialInHand == washItem) {
			new CauldronCleanHandler(block, player).handle();
		} else if (ColorManager.DyeItemColor.fromMaterial(materialInHand).isPresent()) {
			new CauldronDyeHandler(block, player).handle();
		} else {
			ItemStack itemInHand = player.getInventory().getItemInMainHand();
			if (new ItemMatcher(itemInHand).isItemDyeable()) {
				event.setUseItemInHand(Event.Result.DENY);
				event.setUseInteractedBlock(Event.Result.DENY);
				event.setCancelled(true);
				new ItemDyeWashHandler(block, player).handle();
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent event) {
		ColorLayerManager.cleanupChunk(event.getChunk());
		PotionDataStore.cleanupChunk(event.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBreak(BlockBreakEvent event) {
		Location location = event.getBlock().getLocation();
		ColorLayerManager.remove(location);
		PotionDataStore.remove(location);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityExplosion(EntityExplodeEvent event) {
		onExplosion(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockExplosion(BlockExplodeEvent event) {
		onExplosion(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		onPiston(event, event.getBlocks());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		onPiston(event, event.getBlocks());
	}

	private void onPiston(BlockPistonEvent event, List<Block> blocks) {
		BlockFace direction = event.getDirection();

		for (Block block : blocks) {
			Location oldLoc = block.getLocation();
			Location newLoc = block.getRelative(direction).getLocation();

			ColorLayerManager.getEntity(oldLoc).ifPresent(textDisplay -> {
				PersistentDataSetter.getColorData(textDisplay).ifPresent(entityColor -> {
					ColorLayerManager.teleport(textDisplay, newLoc);
				});
			});

			if (PotionDataStore.has(oldLoc)) {
				PotionDataStore.getType(oldLoc).ifPresent(type -> {
					Material bottleType = PotionDataStore.getBottleType(oldLoc);
					PotionDataStore.remove(oldLoc);
					PotionDataStore.store(newLoc, type, bottleType);
				});
			}
		}
	}

	private void onExplosion(List<Block> blocks) {
		for (Block block : blocks) {
			if (block.getType() == Material.WATER_CAULDRON) {
				ColorLayerManager.remove(block.getLocation());
				PotionDataStore.remove(block.getLocation());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWaterLevelChange(CauldronLevelChangeEvent event) {
		Location location = event.getBlock().getLocation();
		BlockData newData = event.getNewState().getBlockData();
		BlockData oldData = event.getBlock().getBlockData();
		Material material = newData.getMaterial();

		if (!(newData instanceof Levelled) || material != Material.WATER_CAULDRON) {
			ColorLayerManager.remove(location);
			PotionDataStore.remove(location);
			return;
		}

		int newLevel = ((Levelled) newData).getLevel();
		int oldLevel = oldData instanceof Levelled ? ((Levelled) oldData).getLevel() : 0;

		if (newLevel > oldLevel && PotionDataStore.has(location)) {
			PotionDataStore.remove(location);
			ColorLayerManager.remove(location);
		}

		ColorLayerManager.getEntity(location).ifPresent(textDisplay -> {
			PersistentDataSetter.getColorData(textDisplay).ifPresent(entityColor -> {
				if (newLevel > oldLevel && ConfigHandler.Settings.REMOVE_COLOR_ON_REFILL) {
					ColorLayerManager.remove(location);
				} else {
					ColorLayerManager.update(location, entityColor, newLevel);
				}
			});
		});
	}

	private static boolean isPotion(Material material) {
		return material == Material.POTION
				|| material == Material.SPLASH_POTION
				|| material == Material.LINGERING_POTION;
	}
}
