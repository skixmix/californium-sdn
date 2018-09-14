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
	private byte[] cborEncoding = null;
	private final Dijkstra dijkstra;
	private long startingTime;
	private long waitingTime = 1000 * 1;		//1 Minutes
	
	public FlowEngineResourceDijkstra() {
		super("fe");
		// set display name
        getAttributes().setTitle("fe");
		dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "length");
		startingTime = System.currentTimeMillis();	
    }

	@Override
    public void handleGET(CoapExchange exchange) { 
        // respond to the request
        exchange.respond("SDN Controller payload");
        System.out.println("Handling GET request");
    }
	
	@Override
    public void handlePOST(CoapExchange exchange) {
		System.out.println("----- Handling Table miss -----");
		
		if(System.currentTimeMillis() - startingTime < waitingTime){
			System.out.println(">> Resource Changed");
			exchange.respond(ResponseCode.CHANGED);
			return;
		}

		byte[] payload; //payload received
		SixLoWPANPacket packet;
		String src = exchange.getQueryParameter("mac"); //node mac
		String dst = null;
		payload = exchange.getRequestPayload();
		
		System.out.println(">> Source: " + src);
		
		packet = new SixLoWPANPacket(payload);
		
		byte[] finalAddress = packet.getFinalAddress();
		if(finalAddress != null){
	        dst = bytesToHex(finalAddress);
	        
	        System.out.println(">> Destination: " + dst);
	        
			dijkstra.init(NetworkResource.getGraph());
			Node source = NetworkResource.getNode(src);
			Node destination = NetworkResource.getNode(dst);
			
			if(source == null || destination == null){
				System.out.println(">> Both source and destination are NULL");
				return;
			}
			
			//Compute path with dijkstra and hop count as metric
			dijkstra.setSource(source);
			dijkstra.compute();
			Node nextHop = null;
			Iterable<Node> path = dijkstra.getPathNodes(destination);
			for (Node node : path){
				if(node.equals(source)){
					break;
				}
				nextHop = node;
			}

			if(nextHop != null){
				System.out.println(">> Next hop: " + bytesToHex(hexStringToByteArray(nextHop.getId())));
				FlowTable ft = new FlowTable();
				FlowEntry fe = new FlowEntry(30, 0); //30 priority, 0 = infinite duration
				
				Rule r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, packet.getFinalAddress());
				Action a = new Action(TypeOfAction.FORWARD, Fields.NO_FIELD, 0, 64, hexStringToByteArray(nextHop.getId()));
				fe.addRule(r);
				fe.addAction(a);
				ft.insertFlowEntry(fe);
				try {
					cborEncoding = ft.toCbor();
				} catch (CborException e) {
					e.printStackTrace();
				}
				exchange.respond(ResponseCode.CHANGED, cborEncoding);
				System.out.println(">> Rule added");
			}
			else
				System.out.println(">> No next hop found, waiting for topology update");
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
