package org.eclipse.californium.examples;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;

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
        System.out.println("Received request from: " + exchange.getSourceAddress());
       
        payload = exchange.getRequestPayload();
        System.out.println(bytesToHex(payload));
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        List<DataItem> dataItems = null;
		try {
			dataItems = new CborDecoder(bais).decode();
		} catch (CborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	System.out.println("CBOR DECODED:");
        for(DataItem dataItem : dataItems) {
        	System.out.println(dataItem.toString());
        }
    }
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
}
