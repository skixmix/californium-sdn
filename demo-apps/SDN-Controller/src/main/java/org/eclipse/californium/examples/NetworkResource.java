package org.eclipse.californium.examples;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.*;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.examples.Model.sdnNode;
import org.eclipse.californium.examples.Topology.NetworkGraph;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;


import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class NetworkResource extends CoapResource{
	
	private Hashtable<String, sdnNode> nodes;
	private static NetworkGraph network;

	public NetworkResource() {
		super("Network");
		// set display name
        getAttributes().setTitle("Network");
        nodes = new Hashtable<>();
		network = new NetworkGraph();

	}

	@Override
    public void handleGET(CoapExchange exchange) {
        String value;
        System.out.println("Received request from: " + exchange.getSourceAddress());
        // respond to the request
        value = exchange.getQueryParameter("value");
        if(value == null)
        	exchange.respond("SDN Controller payload: 0");
        	//exchange.respond(ResponseCode.BAD_REQUEST);
        else
        	exchange.respond("SDN Controller payload: " + value);
        
        //DEBUG
        String key;
        sdnNode n;
        Enumeration keys = nodes.keys();
        while(keys.hasMoreElements()){
        	key = (String) keys.nextElement();
        	n = nodes.get(key);
        	System.out.println(n.toString());
        }
    }
	
	@Override
    public void handlePOST(CoapExchange exchange) {
        String address;
        byte[] payload;
        sdnNode sdnNode, neighbour;
        System.out.println("Topology update from: " + exchange.getSourceAddress());
        //Extract the mac address of the client sdnNode
        address = exchange.getQueryParameter("mac");
        //Extract the payload which contains the Topology update encoded as Cbor encoding
        payload = exchange.getRequestPayload();
        //System.out.println(bytesToHex(payload));	//Debug
		//Decode the Cbor structure
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        List<DataItem> dataItems = null;
		try {
			dataItems = new CborDecoder(bais).decode();
		} catch (CborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			exchange.respond(ResponseCode.BAD_REQUEST);
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
    	}
    	else{
    		//Otherwise, create a new sdnNode object and insert it into the hash table
    		sdnNode = new sdnNode(address, version.getValue().intValue(), battery.getValue().intValue(), queueUtil.getValue().intValue());
    		nodes.put(address, sdnNode);
    	}
    	//At this point there is a Map of data items: <ByteString: Array> for instance: <0001000100010001: [10, 30]>
    	Map map = (Map) dataItems.get(3);
    	for(DataItem key : map.getKeys()){
    		//The key of the Map is the MAC address of a neighbour sdnNode
			ByteString bytes = (ByteString) key;
			//The array contains two statistics: RSSI and ETX
			array = (Array)map.get(key);
			dataItems = array.getDataItems();
			NegativeInteger rssi = (NegativeInteger)dataItems.get(0);		//RSSI
	    	UnsignedInteger etx = (UnsignedInteger)dataItems.get(1);		//ETX
			//Covert the ByteString into an actual String
			address = bytesToHex(bytes.getBytes());
			//Check if the hash table already contains the neighbour sdnNode
			if(nodes.containsKey(address))
				//If so, get it from the hash table
				neighbour = nodes.get(address);
			else
				//Otherwise create a new sdnNode object with just the MAC address. (The other statistics will be inserted when that sdnNode performs a Topology update itself)
				neighbour = new sdnNode(address);
			//At the end, insert this sdnNode as a neighbour of the sending sdnNode
			sdnNode.addNeighbour(neighbour, rssi.getValue().intValue(), etx.getValue().intValue());
		}
		exchange.respond(ResponseCode.CHANGED);
		network.updateMap(sdnNode);
    }
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}

	public static Graph getGraph() {
		return network.getGraph();
	}

	public static Node getNode(String src) {
		return network.getNode(src);
	}
}
