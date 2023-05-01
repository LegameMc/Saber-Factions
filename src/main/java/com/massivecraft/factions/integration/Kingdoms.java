package com.massivecraft.factions.integration;

import com.gmail.legamemc.yvernalkingdom.YvernalKingdom;
import com.gmail.legamemc.yvernalkingdom.data.Kingdom;
import com.massivecraft.factions.Faction;
import org.bukkit.Location;

import java.util.Optional;

public class Kingdoms {


    public static boolean canPlayerBuildAndUse(Location location, Faction faction){
        Optional<Kingdom> optional = YvernalKingdom.getInstance().getKingdomArea().getKingdom(location);
        if(optional.isPresent()){
            Kingdom kingdom = optional.get();
            return kingdom.getCrystal().isDestroyed() || faction.getAllClaims().size() > faction.getPower();
        }

        return false;
    }
}
