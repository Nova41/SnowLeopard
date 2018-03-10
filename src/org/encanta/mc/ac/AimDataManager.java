package org.encanta.mc.ac;

import java.util.HashMap;

public class AimDataManager {
	private EncantaAC main;
	private HashMap<String, AimDataSeries> datas;
	
	public AimDataManager(EncantaAC main) {
		this.main = main;
		this.datas = new HashMap<String, AimDataSeries>();
	}
	
	public void addPlayer(String playername) {
		this.datas.put(playername, new AimDataSeries());
	}
	
	public void removePlayer(String playername) {
		this.datas.remove(playername);
	}
	
	public boolean isRecording(String playername) {
		return this.datas.containsKey(playername);
	}
	
	public void clearData(String playername) {
		this.datas.get(playername).clear();
	}
	
	public AimDataSeries getDataSeries(String playername) {
		return this.datas.get(playername);
	}

}
