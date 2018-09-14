package org.eclipse.californium.examples;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.*;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.examples.Model.sdnNode;
import org.eclipse.californium.examples.Model.Neighbour;
import org.eclipse.californium.examples.Topology.NetworkGraph;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.stream.file.FileSourceGEXF.GEXFConstants.NODESAttribute;

import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class NetworkResource extends CoapResource{
	
	private static Hashtable<String, sdnNode> nodes;
	private static NetworkGraph network;
	
	//private boolean mostrato;
	//private int conta = 0;

	public NetworkResource() {
		super("6lo");
		// set display name
        getAttributes().setTitle("6lo");
        nodes = new Hashtable<>();
		network = new NetworkGraph();
		
		//mostrato = false;
	}
	
	public Hashtable<String, sdnNode> getNodesTable(){
		return nodes;
	}
	

	@Override
    public void handleGET(CoapExchange exchange) {
        String value;
        System.out.println("Received GET request from: " + exchange.getSourceAddress());
        // respond to the request
        value = exchange.getQueryParameter("value");
        if(value == null)
        	exchange.respond("SDN Controller payload: 0");
        	//exchange.respond(ResponseCode.BAD_REQUEST);
        else
        	exchange.respond("SDN Controller payload: " + value);
        
        /* DEBUG
        String key;
        sdnNode n;
        Enumeration keys = nodes.keys();
        while(keys.hasMoreElements()){
        	key = (String) keys.nextElement();
        	n = nodes.get(key);
        	System.out.println(n.toString());
        }
        */
    }
	
	@Override
    public void handlePOST(CoapExchange exchange) {
        String address;
        byte[] payload;
        sdnNode sdnNode, neighbour;
        System.out.println("Received topology update from: " + exchange.getSourceAddress());
        
        SlicingEngine.addNodeIP(exchange.getSourceAddress().toString());
        
        /*
        System.out.println(">>>>>>>>>>> Ho " + (nodes.size()+1) + " nodi nel grafo");
        conta++;
        if(conta >= 20){
        	System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Stampa grafo <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        	Graph grafo = network.getGraph();
        	for (Node node : grafo) {
        	     node.addAttribute("ui.label", Integer.parseInt(node.getId().substring(Math.max(node.getId().length() - 2, 0)), 16) + "");
        	}
        	grafo.display();
        	conta = 0;
        }
        */
        
        //Extract the mac address of the client sdnNode
        address = exchange.getQueryParameter("mac");
        //Extract the payload which contains the Topology update encoded as Cbor encoding
        payload = exchange.getRequestPayload();
        //System.out.println(bytesToHex(payload));	//Debug
        
		//Decode the Cbor structure
        if(payload != null && payload.length != 0){
	        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
	        List<DataItem> dataItems = null;
			try {
				dataItems = new CborDecoder(bais).decode();
			} catch (CborException e) {
				System.out.println("Error during Cbor decoding for topology update: " + e.getMessage());
				exchange.respond(ResponseCode.BAD_REQUEST); //Era BAD_REQUEST
				return;
			}
			//The very beginning of the Cbor structure is an array of data items
	    	DataItem arrayItem = dataItems.get(0);
	    	Array array = (Array)arrayItem;
	    	dataItems = array.getDataItems();
	    	//Retrieve the information related to the sending sdnNode
	    	UnsignedInteger version = (UnsignedInteger)dataItems.get(0);		//Version
	    	UnsignedInteger battery = (UnsignedInteger)dataItems.get(1);		//Battery level (percentage)
	    	UnsignedInteger queueUtil = (UnsignedInteger)dataItems.get(2);		//Queue utilization
	    	
	    	//Check if the sdnNode is already present into the hash table
	    	if(nodes.containsKey(address)){
	    		//If so, just update its statistics
	    		sdnNode = nodes.get(address);
	    		sdnNode.updateInfo(version.getValue().intValue(), battery.getValue().intValue(), queueUtil.getValue().intValue());
	    		
	    		//Added by Simone
	    		nodes.remove(address);
	    		nodes.put(address, sdnNode);
	    	}
	    	else{
	    		//Otherwise, create a new sdnNode object and insert it into the hash table
	    		sdnNode = new sdnNode(address, version.getValue().intValue(), battery.getValue().intValue(), queueUtil.getValue().intValue());
	    		sdnNode.setIpAdddress(exchange.getSourceAddress());
	    		nodes.put(address, sdnNode);
	    	}
	    	
	    	//At this point there is a Map of data items: <ByteString: Array> for instance: <0001000100010001: [10, 30]>
	    	Map map = (Map) dataItems.get(3);
	    	Hashtable<String, Neighbour> myNeighbours = new Hashtable<>();
	    	for(DataItem key : map.getKeys()){
	    		//The key of the Map is the MAC address of a neighbour sdnNode
				ByteString bytes = (ByteString) key;
				//The array contains two statistics: RSSI and ETX
				array = (Array)map.get(key);
				dataItems = array.getDataItems();
				if(dataItems != null){
					NegativeInteger rssi = (NegativeInteger)dataItems.get(0);		//RSSI
					UnsignedInteger etx = (UnsignedInteger)dataItems.get(1);		//ETX
					//Convert the ByteString into an actual String
					address = bytesToHex(bytes.getBytes());
					//Check if the hash table already contains the neighbour sdnNode
					if(nodes.containsKey(address))
						//If so, get it from the hash table
						neighbour = nodes.get(address);
					else
						//Otherwise create a new sdnNode object with just the MAC address. (The other statistics will be inserted when that sdnNode performs a Topology update itself)
						neighbour = new sdnNode(address);
					
					//At the end, insert this sdnNode as a neighbour of the sending sdnNode (by Simone)
					Neighbour n = new Neighbour(neighbour, rssi.getValue().intValue(), etx.getValue().intValue());
					myNeighbours.put(neighbour.getAddress(), n);
				}
			}
	    	
	    	//Set the neighbours and update the graph (by Simone)
			if(sdnNode != null){
		    	sdnNode.setNeighbours(myNeighbours); 
		    	sdnNode.setLastUpdate();
				network.updateMap(sdnNode);	
			}
	
        }
        exchange.respond(ResponseCode.CHANGED);
    }
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}

	public static Graph getGraph() {
		Graph g = network.getGraph();
		return g;
	}

	public static Node getNode(String src) {
		return network.getNode(src);
	}
	
	public static String getNodeIP(String n){
		sdnNode s = nodes.get(n);
		if(s != null)
			return s.getIpAddress();
		else
			return null;
	}
	
}
