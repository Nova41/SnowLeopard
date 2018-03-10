package org.encanta.mc.ac;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

public class PlayerDataLogger implements Listener {
	private EncantaAC main;
	private Map<String, Entity> targets;

	public PlayerDataLogger(EncantaAC main) {
		this.main = main;
		this.targets = new HashMap<String, Entity>();
	}

	@EventHandler
	public void onAttack(EntityDamageByEntityEvent e) {
		if (!shouldRecord(e.getDamager()))
			return;
		this.targets.put(e.getDamager().getName(), e.getEntity());
		this.record(e.getDamager());
	}

	@EventHandler
	public void onClickOnAir(PlayerInteractEvent e) {
		if (!shouldRecord(e.getPlayer()))
			return;
		if (e.getPlayer().getNearbyEntities(10, 10, 10).size() == 0)
			return;
		// Entity entity = e.getPlayer().getNearbyEntities(10, 10, 10).get(0);
		// this.record(e.getPlayer());
	}

	public void record(Entity p) {
		Player player = (Player) p;
		Entity entity = this.targets.get(p.getName());
		AimDataManager manager = main.getDataManager();
		AimDataSeries datas = manager.getDataSeries(player.getName());
		Vector playerLookDir = player.getEyeLocation().getDirection();
		Vector playerEyeLoc = player.getEyeLocation().toVector();
		Vector entityLoc = entity.getLocation().toVector();
		Vector playerEntityVec = entityLoc.subtract(playerEyeLoc);
		float angle = playerLookDir.angle(playerEntityVec);
		datas.add(angle);
		// p.sendMessage(Arrays.asList(datas.getAllDump()).toString());
	}

	public boolean shouldRecord(Entity p) {
		if (p instanceof Player) {
			Player player = (Player) p;
			AimDataManager manager = main.getDataManager();
			return manager.isRecording(player.getName());
		} else {
			return false;
		}
	}

}
