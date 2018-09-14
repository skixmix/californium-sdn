package org.eclipse.californium.examples.Model;


import java.net.InetAddress;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;


public class sdnNode {

	private String address;
	private InetAddress ipAddress;
	private int version;
	private int batteryLevel;
	private int queueUtilization;
	private long lastUpdate;
	private Hashtable<String, Neighbour> neighbours;
	
	public sdnNode(String address){
		this.address = address;
		neighbours = new Hashtable<>();
		this.lastUpdate = new Date(System.currentTimeMillis()).getTime();
	}
	
	public sdnNode(String address, int version, int batteryLevel, int queueUtilization) {
		this.address = address;
		this.version = version;
		this.batteryLevel = batteryLevel;
		this.queueUtilization = queueUtilization;
		neighbours = new Hashtable<>();
		this.lastUpdate = new Date(System.currentTimeMillis()).getTime();
	}
	public void addNeighbour(sdnNode neighbour, int rssi, int etx){
		Neighbour n;
		if(neighbours.containsKey(neighbour.getAddress())){
			n = neighbours.get(neighbour.getAddress());
			n.rssi = rssi;
			n.etx = etx;
		}
		else{
			n = new Neighbour(neighbour, rssi, etx);
			neighbours.put(neighbour.getAddress(), n);
		}
		this.lastUpdate = new Date(System.currentTimeMillis()).getTime();
		//DEBUG
		//System.out.println(this.lastUpdate + " update: rssi=" + n.rssi + " etx=" + n.etx + " to " + n.getAddress());
	}
	
	public void setLastUpdate(){
		this.lastUpdate = new Date(System.currentTimeMillis()).getTime();
	}
	
	public void setIpAdddress(InetAddress ipAddress){
		this.ipAddress = ipAddress;
	}
	
	public String getIpAddress() {
		if(this.ipAddress != null)
			return this.ipAddress.getHostAddress();
		else
			return null;
	}
	
	public void updateInfo(int version, int battery, int queue){
		this.version = version;
		this.batteryLevel = battery;
		this.queueUtilization = queue;
		this.lastUpdate = new Date(System.currentTimeMillis()).getTime();
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public int getBatteryLevel() {
		return batteryLevel;
	}
	public void setBatteryLevel(int batteryLevel) {
		this.batteryLevel = batteryLevel;
	}
	public int getQueueUtilization() {
		return queueUtilization;
	}
	public void setQueueUtilization(int queueUtilization) {
		this.queueUtilization = queueUtilization;
	}
	public Hashtable<String, Neighbour> getNeighbours() {
		return neighbours;
	}
	public void setNeighbours(Hashtable<String, Neighbour> neighbours) {
		this.neighbours = neighbours;
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	public int getNeighboursSize(){
		return neighbours.size();
	}

	public Neighbour getNeighbor(int i){
		Object[] keys = neighbours.keySet().toArray();
		return neighbours.get(keys[i]);
	}

	@Override
	public String toString() {
		return this.address;
	}
}
