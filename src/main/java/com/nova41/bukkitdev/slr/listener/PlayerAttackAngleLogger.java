package com.nova41.bukkitdev.slr.listener;

import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Class for logging angle between player's cross-hair and its target.
 *
 * We use string instead of Player object to identify players, because
 * we want to keep the record of a player if he/she re-logs.
 */
public class PlayerAttackAngleLogger implements Listener {

    // Lists all players whose loggedAngles need to be logged
    @Getter
    private Set<String> registeredPlayers = new HashSet<>();

    // Stores all players attacking and angle sequence they produced
    private Map<String, List<Float>> loggedAngles = new HashMap<>();

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        // Ignore if the event is not triggered by a player
        if (!(event.getDamager() instanceof Player))
            return;

        // Ignore if angle sequence from the player does not need to be logged
        if (!registeredPlayers.contains(event.getDamager().getName()))
            return;

        // Calculate angle
        Player player = (Player) event.getDamager();
        Entity entity = event.getEntity();
        Vector playerLookDir = player.getEyeLocation().getDirection();
        Vector playerEyeLoc = player.getEyeLocation().toVector();
        Vector entityLoc = entity.getLocation().toVector();
        Vector playerEntityVec = entityLoc.subtract(playerEyeLoc);
        float angle = playerLookDir.angle(playerEntityVec);

        // Log the angle
        if (!loggedAngles.containsKey(player.getName()))
            loggedAngles.put(player.getName(), new ArrayList<>());
        loggedAngles.get(player.getName()).add(angle);
    }

    // Tell the logger that angle sequence produced by this player need to be logged
    public void registerPlayer(Player player) {
        registeredPlayers.add(player.getName());
    }

    // Tell the logger to no longer log angle sequence produced by this player
    public void unregisterPlayer(Player player) {
        registeredPlayers.remove(player.getName());
    }

    // Get logged angle sequence of a player
    public List<Float> getLoggedAngles(Player player) {
        return loggedAngles.get(player.getName());
    }

    // Clear logged angle sequence (if there's any) for a player
    public void clearLoggedAngles(Player player) {
        loggedAngles.remove(player.getName());
    }

}
