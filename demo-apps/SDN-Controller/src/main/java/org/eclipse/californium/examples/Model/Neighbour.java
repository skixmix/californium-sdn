package org.eclipse.californium.examples.Model;

public class Neighbour{
	sdnNode sdnNode;
	int rssi;
	int etx;

	public Neighbour(sdnNode n, int rssi, int etx){
		this.sdnNode = n;
		this.rssi = rssi;
		this.etx = etx;
	}
	public String getAddress(){
		return sdnNode.getAddress();
	}

	public int getRssi() {
		return rssi;
	}

	public int getEtx() {
		return etx;
	}
	public sdnNode getNode(){
		return sdnNode;
	}
}