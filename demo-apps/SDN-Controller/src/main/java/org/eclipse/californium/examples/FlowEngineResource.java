package org.eclipse.californium.examples;

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

public class FlowEngineResource extends CoapResource {
	private int counter = 0;
	private byte[] cborEncoding = null;
	
	public FlowEngineResource() {
		super("Flow_engine");
		// set display name
        getAttributes().setTitle("Flow_engine");
        
        byte[] FinalAddress4 = {0x00,0x04,0x00,0x04,0x00,0x04,0x00,0x04};
        byte[] FinalAddress5 = {0x00,0x05,0x00,0x05,0x00,0x05,0x00,0x05};
        byte[] NextHopAddress = {0x00,0x03,0x00,0x03,0x00,0x03,0x00,0x03};
		FlowTable ft = new FlowTable();
		FlowEntry fe = new FlowEntry(10, 0);
		Rule r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, FinalAddress4);
		Action a = new Action(TypeOfAction.FORWARD, Fields.NO_FIELD, 0, 64, NextHopAddress);
		fe.addRule(r);
		fe.addAction(a);
		ft.insertFlowEntry(fe);
		fe = new FlowEntry(10, 0);
		r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, FinalAddress5);
		a = new Action(TypeOfAction.FORWARD, Fields.NO_FIELD, 0, 64, NextHopAddress);
		fe.addRule(r);
		fe.addAction(a);
		ft.insertFlowEntry(fe);
		try {
			cborEncoding = ft.toCbor();
		} catch (CborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		//exchange.accept();
		if(exchange.getQueryParameter("mac").equals(new String("0002000200020002")) )
			exchange.respond(ResponseCode.CHANGED, cborEncoding);
		else
			exchange.respond(ResponseCode.CHANGED);
		System.out.println("Received request " + counter +" from: " + exchange.getSourceAddress());
        System.out.println(" type = " + exchange.getQueryParameter("type"));
        System.out.println(" mac = " + exchange.getQueryParameter("mac"));
        exchange.respond(ResponseCode.CHANGED);
        payload = exchange.getRequestPayload();
        System.out.println("PAYLOAD:");
        System.out.println(bytesToHex(payload));
        packet = new SixLoWPANPacket(payload);
        System.out.println("\tFA = " + bytesToHex(packet.getFinalAddress()));
        System.out.println("\tOA = " + bytesToHex(packet.getOriginatorAddress()));
        System.out.println("\tFirst = " + packet.isFragmentationFirstHeader() + " FRAG = " + packet.getDatagramTag()+ " OFFSET = " + packet.getDatagramOffset());
        
	}
	
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
}
