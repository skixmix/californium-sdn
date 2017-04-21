package org.eclipse.californium.examples;

import java.util.Arrays;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.examples.Model.Action;
import org.eclipse.californium.examples.Model.FlowEntry;
import org.eclipse.californium.examples.Model.FlowTable;
import org.eclipse.californium.examples.Model.Rule;
import org.eclipse.californium.examples.Model.SixLoWPANPacket;
import org.eclipse.californium.examples.Model.Constants.Fields;
import org.eclipse.californium.examples.Model.Constants.Operators;
import org.eclipse.californium.examples.Model.Constants.TypeOfAction;

import co.nstant.in.cbor.CborException;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Node;

public class FlowEngineResourceDijkstra extends CoapResource {
	private int counter = 0;
	private byte[] cborEncoding = null;
	private final Dijkstra dijkstra;
	private long startingTime;
	private long waitingTime = 1000 * 60 * 1;		//1 Minutes
	
	public FlowEngineResourceDijkstra() {
		super("Flow_engine");
		// set display name
        getAttributes().setTitle("Flow_engine");
		dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "length");
		startingTime = System.currentTimeMillis();
    }

	@Override
    public void handleGET(CoapExchange exchange) {
        
        // respond to the request
        exchange.respond("SDN Controller payload");
    }
	
	@Override
    public void handlePOST(CoapExchange exchange) {
		
		if(System.currentTimeMillis() - startingTime < waitingTime){
			exchange.respond(ResponseCode.CHANGED);
			return;
		}
		
		byte[] payload;
		SixLoWPANPacket packet;
		counter++;
		String src = exchange.getQueryParameter("mac");
		String dst = null;
		payload = exchange.getRequestPayload();
		//System.out.println("PACKET DIM = " + payload.length);
		packet = new SixLoWPANPacket(payload);
		
		byte[] finalAddress = packet.getFinalAddress();
		if(finalAddress != null){
	        dst = bytesToHex(finalAddress);
			dijkstra.init(NetworkResource.getGraph());
			Node source = NetworkResource.getNode(src);
			Node destination = NetworkResource.getNode(dst);
			if(source == null || destination == null)
				return;
			System.out.println("From: " + src + " to " + dst);
			dijkstra.setSource(source);
			dijkstra.compute();
			Node nextHop = null;
			System.out.print("PATH: ");
			Iterable<Node> path = dijkstra.getPathNodes(destination);
			for (Node node : path){
				System.out.print(" -> " + node.getId());
				if(node.equals(source)){
					break;
				}
				nextHop = node;
			}
			System.out.println("");
			if(nextHop != null){
				System.out.println("Nxt: " + bytesToHex(hexStringToByteArray(nextHop.getId())));
				FlowTable ft = new FlowTable();
				FlowEntry fe = new FlowEntry(30, 0);
				Rule r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, packet.getFinalAddress());
				Action a = new Action(TypeOfAction.FORWARD, Fields.NO_FIELD, 0, 64, hexStringToByteArray(nextHop.getId()));
				fe.addRule(r);
				fe.addAction(a);
				ft.insertFlowEntry(fe);
				try {
					cborEncoding = ft.toCbor();
				} catch (CborException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				exchange.respond(ResponseCode.CHANGED, cborEncoding);
				System.out.println(bytesToHex(cborEncoding));
			}
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
