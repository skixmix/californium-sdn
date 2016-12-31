package org.eclipse.californium.examples;

import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.examples.Model.Action;
import org.eclipse.californium.examples.Model.FlowEntry;
import org.eclipse.californium.examples.Model.FlowTable;
import org.eclipse.californium.examples.Model.Node;
import org.eclipse.californium.examples.Model.Rule;
import org.eclipse.californium.examples.Model.Constants.Fields;
import org.eclipse.californium.examples.Model.Constants.Operators;
import org.eclipse.californium.examples.Model.Constants.TypeOfAction;


import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.NegativeInteger;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.UnsignedInteger;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;

public class NetworkResource extends CoapResource{
	
	private Hashtable<String, Node> nodes;
	
	public NetworkResource() {
		super("Network");
		// set display name
        getAttributes().setTitle("Network");
        nodes = new Hashtable<>();
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
        Node n;
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
        Node node, neighbour;
        System.out.println("Topology update from: " + exchange.getSourceAddress());
        //Extract the mac address of the client node
        address = exchange.getQueryParameter("mac");
        //Extract the payload which contains the topology update encoded as Cbor encoding
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
    	//Retrieve the information related to the sending node
    	UnsignedInteger version = (UnsignedInteger)dataItems.get(0);		//Version
    	UnsignedInteger battery = (UnsignedInteger)dataItems.get(1);		//Battery level (percentage)
    	UnsignedInteger queueUtil = (UnsignedInteger)dataItems.get(2);		//Queue utilization
    	//Check if the node is already present into the hash table
    	if(nodes.containsKey(address)){
    		//If so, just update its statistics
    		node = nodes.get(address);
    		node.updateInfo(version.getValue().intValue(), battery.getValue().intValue(), queueUtil.getValue().intValue());
    	}
    	else{
    		//Otherwise, create a new Node object and insert it into the hash table
    		node = new Node(address, version.getValue().intValue(), battery.getValue().intValue(), queueUtil.getValue().intValue());
    		nodes.put(address, node);
    	}
    	//At this point there is a Map of data items: <ByteString: Array> for instance: <0001000100010001: [10, 30]>
    	Map map = (Map) dataItems.get(3);
    	for(DataItem key : map.getKeys()){
    		//The key of the Map is the MAC address of a neighbour node
			ByteString bytes = (ByteString) key;
			//The array contains two statistics: RSSI and ETX
			array = (Array)map.get(key);
			dataItems = array.getDataItems();
			NegativeInteger rssi = (NegativeInteger)dataItems.get(0);		//RSSI
	    	UnsignedInteger etx = (UnsignedInteger)dataItems.get(1);		//ETX
			//Covert the ByteString into an actual String
			address = bytesToHex(bytes.getBytes());
			//Check if the hash table already contains the neighbour node
			if(nodes.containsKey(address))
				//If so, get it from the hash table
				neighbour = nodes.get(address);
			else
				//Otherwise create a new Node object with just the MAC address. (The other statistics will be inserted when that node performs a topology update itself)
				neighbour = new Node(address);
			//At the end, insert this node as a neighbour of the sending node
			node.addNeighbour(neighbour, rssi.getValue().intValue(), etx.getValue().intValue());
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
}
