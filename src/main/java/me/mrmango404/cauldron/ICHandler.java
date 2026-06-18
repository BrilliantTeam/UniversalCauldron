package me.mrmango404.cauldron;

import me.mrmango404.api.events.CauldronCleanEvent;
import me.mrmango404.api.events.CauldronDyeEvent;
import me.mrmango404.api.events.ItemDyeEvent;
import me.mrmango404.api.events.ItemWashEvent;
import me.mrmango404.utils.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public abstract class ICHandler {

	Block block;
	Location blockLoc;
	Player player;
	EquipmentSlot hand;

	public ICHandler(Block block, Player player, EquipmentSlot hand) {
		this.block = block;
		this.player = player;
		this.blockLoc = block.getLocation();
		this.hand = hand;
	}

	protected ItemStack getItemInHand() {
		return hand == EquipmentSlot.HAND
				? player.getInventory().getItemInMainHand()
				: player.getInventory().getItemInOffHand();
	}

	protected void setItemInHand(ItemStack item) {
		if (hand == EquipmentSlot.HAND) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.getInventory().setItemInOffHand(item);
		}
	}

	protected void consumeDye() {
		if (!PermissionManager.hasPermission(player, PermissionManager.INFINITE_DYE)) {
			ItemStack itemInHand = getItemInHand();
			itemInHand.setAmount(itemInHand.getAmount() - 1);
		}
	}

	protected boolean isItemDyeEventCancelled(TextDisplay entity) {
		ItemDyeEvent customEvent = new ItemDyeEvent(block, entity, player);
		Bukkit.getPluginManager().callEvent(customEvent);
		return customEvent.isCancelled();
	}

	protected boolean isItemWashEventCancelled() {
		ItemWashEvent customEvent = new ItemWashEvent(block, null, player);
		Bukkit.getPluginManager().callEvent(customEvent);
		return customEvent.isCancelled();
	}

	protected boolean isCauldronDyeEventCancelled(TextDisplay entity) {
		CauldronDyeEvent customEvent = new CauldronDyeEvent(block, entity, player);
		Bukkit.getPluginManager().callEvent(customEvent);
		return customEvent.isCancelled();
	}

	protected boolean isCauldronCleanEventCancelled(TextDisplay entity) {
		CauldronCleanEvent customEvent = new CauldronCleanEvent(block, entity, player);
		Bukkit.getPluginManager().callEvent(customEvent);
		return customEvent.isCancelled();
	}

	public abstract void handle();
}
