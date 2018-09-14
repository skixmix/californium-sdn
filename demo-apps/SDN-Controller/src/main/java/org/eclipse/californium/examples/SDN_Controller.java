package org.eclipse.californium.examples;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;


public class SDN_Controller extends CoapServer {

	private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
    /*
     * Application entry point.
     */
	
    public static void main(String[] args) throws URISyntaxException {
        
        try {
            // create server
            SDN_Controller server = new SDN_Controller();
            //Server (Controller) address should be off-link, so we use a Global ipv6 address like 2001:0db8:0:f101::1
            server.addEndpoint(new CoapEndpoint(new InetSocketAddress("2001:0db8:0:f101::1", COAP_PORT)));
            
            server.start();
            System.out.println("Controller Started!");

        } catch (SocketException e) {
            System.err.println("Failed to initialize server: " + e.getMessage());
        }
    }


    //Constructor for the SDN Controller, here we initialize the available resources
    public SDN_Controller() throws SocketException, URISyntaxException {
    	
    	//The FlowEngine is the one managing flow tables
    	CoapResource FlowEngine =  new FlowEngineDiProva();
    	
    	//The NetworkResource is the one managing topology
    	NetworkResource networkResource = new NetworkResource();
    	
    	//Testing resource for basic communication experiments
    	//CoapResource TestingEngine = new TestingEngine();
    	
    	//Slicing engine
    	CoapResource SlicingEngine = new SlicingEngine();

    	//Add the resources to the SDN Controller
        add(networkResource);
        add(FlowEngine);
       // add(TestingEngine);
        add(SlicingEngine);
    }

}
