package org.eclipse.californium.examples;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class FlowEngineResource extends CoapResource {

	public FlowEngineResource() {
		super("Flow Engine");
		// set display name
        getAttributes().setTitle("Flow Engine");
	}

	@Override
    public void handleGET(CoapExchange exchange) {
        
        // respond to the request
        exchange.respond("SDN Controller payload");
    }
}
