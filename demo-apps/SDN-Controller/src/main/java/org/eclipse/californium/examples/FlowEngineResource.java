package org.eclipse.californium.examples;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class FlowEngineResource extends CoapResource {

	public FlowEngineResource() {
		super("Flow_engine");
		// set display name
        getAttributes().setTitle("Flow_engine");
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
		System.out.println("Received request from: " + exchange.getSourceAddress());
        System.out.println(" type = " + exchange.getQueryParameter("type"));
        System.out.println(" mac = " + exchange.getQueryParameter("mac"));
        exchange.respond(ResponseCode.CHANGED);
        payload = exchange.getRequestPayload();
        System.out.println("PAYLOAD:");
        System.out.println(bytesToHex(payload));
        packet = new SixLoWPANPacket(payload);
        System.out.println("\tFA = " + bytesToHex(packet.getFinalAddress()));
        System.out.println("\tOA = " + bytesToHex(packet.getOriginatorAddress()));
        System.out.println("\tFirst = " + packet.fragmentationFirstHeader + " FRAG = " + packet.getDatagramTag()+ " OFFSET = " + packet.getDatagramOffset());
    }
	
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
}
