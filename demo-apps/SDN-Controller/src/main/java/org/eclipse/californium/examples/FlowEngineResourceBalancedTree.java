package org.eclipse.californium.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.examples.Model.Action;
import org.eclipse.californium.examples.Model.FlowEntry;
import org.eclipse.californium.examples.Model.FlowTable;
import org.eclipse.californium.examples.Model.Rule;
import org.eclipse.californium.examples.Model.SixLoWPANPacket;
import org.eclipse.californium.examples.Model.Constants.Fields;
import org.eclipse.californium.examples.Model.Constants.Operators;
import org.eclipse.californium.examples.Model.Constants.TypeOfAction;
import org.eclipse.californium.examples.Model.sdnNode.Neighbour;

import co.nstant.in.cbor.CborException;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Node;

public class FlowEngineResourceBalancedTree extends CoapResource {
	private int counter = 0;
	private byte[] cborEncoding = null;
	private PathsTable ForwardingPlan;
	public FlowEngineResourceBalancedTree() {
		super("fe");
		// set display name
        getAttributes().setTitle("fe");
        
        int[] nodes = new int[21];
        for(int i = 0; i < nodes.length; i++)
        	nodes[i] = i+1;
        ForwardingPlan = new PathsTable(nodes);
        /*--------------DOWNSTREAM PATHS---------------*/
        //NODE 1
        ForwardingPlan.addPathtoNode(1, 3, 2);
        ForwardingPlan.addPathtoNode(1, 4, 2);
        ForwardingPlan.addPathtoNode(1, 5, 2);
        //NODE 2 
        ForwardingPlan.addPathtoNode(2, 4, 3);
        ForwardingPlan.addPathtoNode(2, 5, 3);
        //NODE 3
        ForwardingPlan.addPathtoNode(3, 5, 4);
        
        /*--------------UPSTREAM PATHS---------------*/
        //NODE 1
        ForwardingPlan.addPathtoServer(1, "8005", 2);
        //NODE 3
        ForwardingPlan.addPathtoServer(3, "0200000000000002", 2);
        //NODE 2
        ForwardingPlan.addPathtoServer(2, "0200000000000002", 1);
        ForwardingPlan.addPathtoServer(2, "8005", 3);
        //NODE 4
        ForwardingPlan.addPathtoServer(4, "0200000000000002", 3);
        //NODE 5
        ForwardingPlan.addPathtoServer(5, "0200000000000002", 4);
        ForwardingPlan.printTable();
	}

	@Override
    public void handleGET(CoapExchange exchange) {
        
        // respond to the request
        exchange.respond("SDN Controller payload");
    }
	
	@Override
    public void handlePOST(CoapExchange exchange) {
		byte[] payload;
		SixLoWPANPacket packet;
		counter++;
		String src = exchange.getQueryParameter("mac");
		String dst = null;
		payload = exchange.getRequestPayload();
		//System.out.println("PACKET DIM = " + payload.length);
		packet = new SixLoWPANPacket(payload);
		try {
			byte[] finalAddress = packet.getFinalAddress();
			dst = bytesToHex(finalAddress);
			System.out.println("From: " + src + " to " + dst);
			if(finalAddress != null){
		        FlowTable ft = ForwardingPlan.getEntry(src, finalAddress);
		        if(ft == null){
		        	exchange.respond(ResponseCode.CHANGED);
		        	return;
		        }
		        
				cborEncoding = ft.toCbor();
		        exchange.respond(ResponseCode.CHANGED, cborEncoding);
		        System.out.println(bytesToHex(cborEncoding));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		exchange.respond(ResponseCode.CHANGED);
	}		
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
}


class PathsTable{
	private Hashtable<String, Hashtable<String, byte[]>> paths;
	
	private String fromIDtoMAC(int id){
		//Four times the hex representation of id
		String ret = String.format("%04x",id);
		ret += String.format("%04x",id);
		ret += String.format("%04x",id);
		ret += String.format("%04x",id);
		return ret;
	}
	
	/*
	private String fromIDtoMAC(int id){
		//Four times the hex representation of id
		String ret = "c10c";
		ret += String.format("%04x",0);
		ret += String.format("%04x",0);
		ret += String.format("%04x",id);
		return ret;
	}
	*/
	public PathsTable(int[] nodeArray){	
		paths = new Hashtable<>();
		for(int i = 0; i < nodeArray.length; i++){
			String macAddress = fromIDtoMAC(nodeArray[i]);
			paths.put(macAddress, new Hashtable<String, byte[]>());
		}
	}
	
	public void addPathtoNode(int node, int finalAddress, int nextHop){
		String nodeString = fromIDtoMAC(node);
		String finalString = fromIDtoMAC(finalAddress);
		String nextHopString = fromIDtoMAC(nextHop);
		paths.get(nodeString).put(finalString, hexStringToByteArray(nextHopString));
	}
	
	public void addPathtoServer(int node, String server, int nextHop){
		String serverMeshMacAddress = server;
		String nodeString = fromIDtoMAC(node);
		String nextHopString = fromIDtoMAC(nextHop);
		paths.get(nodeString).put(serverMeshMacAddress, hexStringToByteArray(nextHopString));
	}
	
	private byte[] getNextHop(String node, byte[] finalAddress){
		return paths.get(node).get(bytesToHex(finalAddress));
	}
	
	public FlowTable getEntry(String node, byte[] finalAddress){
		byte[] nextHop = getNextHop(node, finalAddress);
		byte[] finalAddrBytes = finalAddress;
		if(nextHop == null || nextHop.length == 0)
			return null;
		System.out.println("NEXT HOP = " + bytesToHex(nextHop));
		FlowTable ft = new FlowTable();
		FlowEntry fe = new FlowEntry(30, 0);
		Rule r = null;
		if(finalAddress.length == 8)
			r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, finalAddrBytes);
		else
			r = new Rule(Fields.MH_DST_ADDR, 0, 16, Operators.EQUAL, finalAddrBytes);
		Action a = null;
		if(Arrays.equals(nextHop, hexStringToByteArray(node))){
			a = new Action(TypeOfAction.TO_UPPER_L, Fields.NO_FIELD, 0, 0, null);
		}
		else
			a = new Action(TypeOfAction.FORWARD, Fields.NO_FIELD, 0, 64, nextHop);
		fe.addRule(r);
		fe.addAction(a);
		ft.insertFlowEntry(fe);
		return ft;
	}
	
	public void printTable(){
		System.out.println("Table:");
		for(String key : paths.keySet()){
			System.out.println("Node " + key);
			for(String finalAddr : paths.get(key).keySet())
				System.out.println("\t" + finalAddr + " -> " + bytesToHex(paths.get(key).get(finalAddr)));
		}
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
}