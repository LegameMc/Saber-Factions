package com.massivecraft.factions.zcore.util;

import com.cryptomorin.xseries.XMaterial;
import com.massivecraft.factions.*;
import com.massivecraft.factions.cmd.Aliases;
import com.massivecraft.factions.cmd.audit.FLogManager;
import com.massivecraft.factions.cmd.check.CheckTask;
import com.massivecraft.factions.cmd.check.WeeWooTask;
import com.massivecraft.factions.cmd.reserve.ListParameterizedType;
import com.massivecraft.factions.cmd.reserve.ReserveObject;
import com.massivecraft.factions.data.helpers.FactionDataHelper;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.integration.Essentials;
import com.massivecraft.factions.integration.dynmap.EngineDynmap;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.util.Metrics;
import com.massivecraft.factions.util.timer.TimerManager;
import com.massivecraft.factions.zcore.file.impl.FileManager;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.saberdev.corex.CoreX;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static com.massivecraft.factions.Conf.safeZoneNerfedCreatureTypes;
import static com.massivecraft.factions.Conf.territoryDenyUsageMaterials;

public class StartupParameter {

    public static void initData(FactionsPlugin plugin, Runnable finish) {
        new Metrics(FactionsPlugin.getInstance(), 7013);

        FactionsPlugin.getInstance().fileManager = new FileManager();
        FactionsPlugin.getInstance().fileManager.setupFiles();

        Essentials.setup();

        FactionsPlugin.getInstance().fLogManager = new FLogManager();

        FPlayers.getInstance().load(playersLoaded -> Factions.getInstance().load(factionsLoaded -> {
            for (FPlayer fPlayer : FPlayers.getInstance().getAllFPlayers()) {
                Faction faction = Factions.getInstance().getFactionById(fPlayer.getFactionId());
                if (faction == null) {
                    Logger.print("Invalid faction id on " + fPlayer.getName() + ":" + fPlayer.getFactionId(), Logger.PrefixType.WARNING);
                    fPlayer.resetFactionData(false);
                    continue;
                }
                if (fPlayer.isAlt()) {
                    faction.addAltPlayer(fPlayer);
                } else {
                    faction.addFPlayer(fPlayer);
                }
            }

            Factions.getInstance().getAllFactions().forEach(Faction::refreshFPlayers);

            Board.getInstance().load();
            Board.getInstance().clean();

            Aliases.load();
            EngineDynmap.getInstance().init();

            FactionsPlugin.getInstance().hookedPlayervaults = setupPlayerVaults();

            Econ.setup();

            initReserves();

            FactionsPlugin.cachedRadiusClaim = Conf.useRadiusClaimSystem;

            CoreX.init();

            FactionDataHelper.init();

            if (Conf.useCheckSystem) {
                FactionsPlugin.getInstance().getServer().getScheduler().runTaskTimerAsynchronously(plugin, CheckTask.getInstance(), 0L, 1200L);
                FactionsPlugin.getInstance().getServer().getScheduler().runTaskTimer(plugin, CheckTask.getInstance()::cleanupTask, 0L, 1260L);

                FactionsPlugin.getInstance().getServer().getScheduler().runTaskTimerAsynchronously(plugin, WeeWooTask::new, 600L, 600L);
            }


            populateConfSets();

            FactionsPlugin.getInstance().fLogManager.loadLogs(plugin);

            FactionsPlugin.getInstance().timerManager = new TimerManager(plugin);
            FactionsPlugin.getInstance().timerManager.reloadTimerData();
            Logger.print("Loaded " + FactionsPlugin.getInstance().timerManager.getTimers().size() + " timers into list!", Logger.PrefixType.DEFAULT);

            finish.run();
        }));

    }

    public static void populateConfSets() {
        if (FactionsPlugin.getInstance().version >= 17) {
            safeZoneNerfedCreatureTypes.add(EntityType.GLOW_SQUID);
            safeZoneNerfedCreatureTypes.add(EntityType.AXOLOTL);
            safeZoneNerfedCreatureTypes.add(EntityType.ZOMBIFIED_PIGLIN);
        } else if (FactionsPlugin.getInstance().version == 16) {
            safeZoneNerfedCreatureTypes.add(EntityType.ZOMBIFIED_PIGLIN);
        } else {
            safeZoneNerfedCreatureTypes.add(EntityType.valueOf("PIG_ZOMBIE"));
        }

        territoryDenyUsageMaterials.add(XMaterial.FLINT_AND_STEEL.parseMaterial());
        if(FactionsPlugin.getInstance().version != 8) {
            territoryDenyUsageMaterials.add(XMaterial.END_CRYSTAL.parseMaterial());
        }
        territoryDenyUsageMaterials.add(XMaterial.BUCKET.parseMaterial());
        territoryDenyUsageMaterials.add(XMaterial.WATER_BUCKET.parseMaterial());
        territoryDenyUsageMaterials.add(XMaterial.LAVA_BUCKET.parseMaterial());

        if (FactionsPlugin.getInstance().version != 7) {
            territoryDenyUsageMaterials.add(XMaterial.ARMOR_STAND.parseMaterial());
        }

        if (FactionsPlugin.getInstance().version >= 13) {
            territoryDenyUsageMaterials.add(XMaterial.COD_BUCKET.parseMaterial());
            territoryDenyUsageMaterials.add(XMaterial.PUFFERFISH_BUCKET.parseMaterial());
            territoryDenyUsageMaterials.add(XMaterial.SALMON_BUCKET.parseMaterial());
            territoryDenyUsageMaterials.add(XMaterial.TROPICAL_FISH_BUCKET.parseMaterial());
        }

        if (FactionsPlugin.getInstance().version >= 17) {
            territoryDenyUsageMaterials.add(XMaterial.AXOLOTL_BUCKET.parseMaterial());
            territoryDenyUsageMaterials.add(XMaterial.POWDER_SNOW_BUCKET.parseMaterial());
        }

        Conf.save();
    }


    public static void initReserves() {
        FactionsPlugin.getInstance().reserveObjects = new ArrayList<>();
        String path = Paths.get(FactionsPlugin.getInstance().getDataFolder().getAbsolutePath()).toAbsolutePath() + File.separator + "data" + File.separator + "reserves.json";
        File file = new File(path);
        try {
            String json;
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            json = String.join("", Files.readAllLines(Paths.get(file.getPath()))).replace("\n", "").replace("\r", "");
            if (json.equalsIgnoreCase("")) {
                Files.write(Paths.get(path), "[]".getBytes());
                json = "[]";
            }
            FactionsPlugin.getInstance().reserveObjects = FactionsPlugin.getInstance().getGson().fromJson(json, new ListParameterizedType(ReserveObject.class));
            if (FactionsPlugin.getInstance().reserveObjects == null)
                FactionsPlugin.getInstance().reserveObjects = new ArrayList<>();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }


    public static boolean setupPlayerVaults() {
        Plugin plugin = FactionsPlugin.getInstance().getServer().getPluginManager().getPlugin("PlayerVaults");
        return plugin != null && plugin.isEnabled();
    }
}
