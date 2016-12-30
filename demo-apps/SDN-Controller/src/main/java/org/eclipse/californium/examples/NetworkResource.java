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
        address = exchange.getQueryParameter("mac");
        payload = exchange.getRequestPayload();
        //System.out.println(bytesToHex(payload));
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        List<DataItem> dataItems = null;
		try {
			dataItems = new CborDecoder(bais).decode();
		} catch (CborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	DataItem arrayItem = dataItems.get(0);
    	Array array = (Array)arrayItem;
    	dataItems = array.getDataItems();
    	
    	UnsignedInteger version = (UnsignedInteger)dataItems.get(0);
    	UnsignedInteger battery = (UnsignedInteger)dataItems.get(1);
    	UnsignedInteger queueUtil = (UnsignedInteger)dataItems.get(2);
    	
    	if(nodes.containsKey(address)){
    		node = nodes.get(address);
    		node.updateInfo(version.getValue().intValue(), battery.getValue().intValue(), queueUtil.getValue().intValue());
    	}
    	else{
    		node = new Node(address, version.getValue().intValue(), battery.getValue().intValue(), queueUtil.getValue().intValue());
    		nodes.put(address, node);
    	}
    	
    	Map map = (Map) dataItems.get(3);
    	for(DataItem key : map.getKeys()){
			ByteString bytes = (ByteString) key;
			array = (Array)map.get(key);
			dataItems = array.getDataItems();
			NegativeInteger rssi = (NegativeInteger)dataItems.get(0);
	    	UnsignedInteger etx = (UnsignedInteger)dataItems.get(1);
			address = bytesToHex(bytes.getBytes());
			
			if(nodes.containsKey(address))
				neighbour = nodes.get(address);
			else
				neighbour = new Node(address);
			
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
