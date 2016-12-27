package org.eclipse.californium.examples;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;

public class NetworkResource extends CoapResource{

	public NetworkResource() {
		super("Network");
		// set display name
        getAttributes().setTitle("Network");
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
    }
	
	@Override
    public void handlePOST(CoapExchange exchange) {
        String value;
        byte[] payload;
        ByteString str;
        System.out.print("Received request from: " + exchange.getSourceAddress());
        System.out.println(" mac = " + exchange.getQueryParameter("mac"));
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
    	System.out.println("CBOR DECODED:");
    	DataItem arrayItem = dataItems.get(0);
    	Array array = (Array)arrayItem;
    	
        for(DataItem dataItem : array.getDataItems()) {
        	System.out.print("TYPE = " + dataItem.getMajorType().getValue() + " ");
        	if(dataItem.getMajorType().getValue() == 5){
        		Map map = (Map) dataItem;
        		System.out.println("MAP:");
        		for(DataItem key : map.getKeys()){
        			ByteString bytes = (ByteString) key;
        			System.out.print(bytesToHex(bytes.getBytes()) + " -> " + map.get(key));
        		}
        	}
        	else
        		System.out.println(dataItem);
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
