package com.massivecraft.factions.cmd;

import com.massivecraft.factions.SaberFactions;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CmdGetVault extends FCommand {
	public CmdGetVault() {
		super();

		this.aliases.add("getvault");

		this.permission = Permission.GETVAULT.node;
		this.disableOnLock = true;


		senderMustBePlayer = true;
		senderMustBeMember = true;
		senderMustBeModerator = false;
		senderMustBeColeader = false;
		senderMustBeAdmin = false;
	}

	@Override
	public void perform() {
		if (!SaberFactions.plugin.getConfig().getBoolean("fvault.Enabled")) {
			fme.sendMessage("This command is disabled!");
			return;
		}
		Location vaultLocation = fme.getFaction().getVault();
		ItemStack vault = SaberFactions.plugin.createItem(Material.CHEST, 1, (short) 0, SaberFactions.plugin.color(SaberFactions.plugin.getConfig().getString("fvault.Item.Name")), SaberFactions.plugin.colorList(SaberFactions.plugin.getConfig().getStringList("fvault.Item.Lore")));


		//check if vault is set
		if (vaultLocation != null) {
			fme.msg(TL.COMMAND_GETVAULT_ALREADYSET);
			return;
		}


		//has enough money?
		int amount = SaberFactions.plugin.getConfig().getInt("fvault.Price");
		if (!fme.hasMoney(amount)) {
			return;
		}


		//success :)
		fme.takeMoney(amount);
		me.getInventory().addItem(vault);
		fme.msg(TL.COMMAND_GETVAULT_RECEIVE);

	}

	public boolean inventoryContains(Inventory inventory, ItemStack item) {
		int count = 0;
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && items[i].getType() == item.getType() && items[i].getDurability() == item.getDurability()) {
				count += items[i].getAmount();
			}
			if (count >= item.getAmount()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public TL getUsageTranslation() {
		return TL.COMMAND_GETVAULT_DESCRIPTION;
	}

}
