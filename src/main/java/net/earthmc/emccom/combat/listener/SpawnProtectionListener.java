package net.earthmc.emccom.combat.listener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.event.teleport.SuccessfulTownyTeleportEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.WorldCoord;
import net.earthmc.emccom.manager.ResidentMetadataManager;
import net.earthmc.emccom.object.SpawnProtPref;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnProtectionListener implements Listener {
    private final Map<UUID, Integer> playerChunkCountMap = new HashMap<>();

    private static final int CHUNK_DISTANCE = 8;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(SuccessfulTownyTeleportEvent event) {
        playerChunkCountMap.put(event.getResident().getUUID(), CHUNK_DISTANCE);
    }

    @EventHandler
    public void onRespawnEvent(PlayerRespawnEvent event) {
        playerChunkCountMap.put(event.getPlayer().getUniqueId(), CHUNK_DISTANCE);
    }


    // 😭😭😭
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Location from = event.getFrom();
        final int fromChunkX = from.getBlockX() >> 4;
        final int fromChunkZ = from.getBlockZ() >> 4;

        Location to = event.getTo();
        final int toChunkX = to.getBlockX() >> 4;
        final int toChunkZ = to.getBlockZ() >> 4;

        if (playerChunkCountMap.containsKey(playerId) && (fromChunkX != toChunkX || fromChunkZ != toChunkZ)) {
            int remainingChunks = getRemainingChunks(playerId);
            if (remainingChunks > 0) {
                remainingChunks--;
                updateRemainingChunks(playerId, remainingChunks);

                Resident playerAsResident = TownyAPI.getInstance().getResident(player);
                SpawnProtPref SpawnProtPrefOfResident = ResidentMetadataManager.getResidentSpawnProtPref(playerAsResident);
                if (!(SpawnProtPrefOfResident == SpawnProtPref.HIDE)) {
                    Component message;
                    if (remainingChunks > 0) {
                        message = Component.text()
                                .append(Component.text("[Towny] ", NamedTextColor.GOLD))
                                .append(Component.text("You have " + remainingChunks + " chunks left before losing items on death!", NamedTextColor.RED))
                                .build();
                    } else {
                        message = Component.text()
                                .append(Component.text("[Towny] ", NamedTextColor.GOLD))
                                .append(Component.text("You will now lose your items if you die! Run ", NamedTextColor.RED))
                                .append(Component.text("/spawnprotpref HIDE", NamedTextColor.GREEN))
                                .append(Component.text(" to disable chunk notification warnings", NamedTextColor.RED))
                                .build();
                    }
                    player.sendMessage(message);
                }
            }
        }
    }

    private int getRemainingChunks(UUID playerId) {
        return playerChunkCountMap.getOrDefault(playerId, CHUNK_DISTANCE);
    }

    private void updateRemainingChunks(UUID playerId, int remainingChunks) {
        playerChunkCountMap.put(playerId, remainingChunks);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        final WorldCoord coord = WorldCoord.parseWorldCoord(event.getEntity().getLocation());
        if (TownyAPI.getInstance().isWilderness(coord))
            return;

        if (playerChunkCountMap.containsKey(playerId)) {
            int remainingChunks = getRemainingChunks(playerId);
            if (remainingChunks > 0) {
                event.setKeepInventory(true);
                event.setKeepLevel(true);
                event.getDrops().clear();
                event.setDroppedExp(0);

                playerChunkCountMap.remove(playerId);
            }
        }
    }
}

