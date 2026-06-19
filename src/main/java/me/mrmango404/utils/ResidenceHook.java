package me.mrmango404.utils;

import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ResidenceHook {

	public static boolean hasPermission(Player player, Location loc, String flagName) {
		Flags flag = Flags.valueOf(flagName);
		boolean hasPerm = FlagPermissions.has(loc, player, flag, true);
		if (!hasPerm) {
			player.sendActionBar(LegacyComponentSerializer.legacySection()
					.deserialize(lm.Flag_Deny.getMessage(flagName)));
		}
		return hasPerm;
	}
}
