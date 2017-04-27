package org.eclipse.californium.examples;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

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
    public static void main(String[] args) {
        
        try {

            // create server
            SDN_Controller server = new SDN_Controller();
            // add endpoints on all IP addresses
            server.addEndpoints();
            server.start();

        } catch (SocketException e) {
            System.err.println("Failed to initialize server: " + e.getMessage());
        }
    }

    /**
     * Add individual endpoints listening on default CoAP port on all IPv4 addresses of all network interfaces.
     */
    private void addEndpoints() {
    	for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
    		//binds to all IPv4 addresses, localhost and IPv6 fd00::1
			if (addr instanceof Inet4Address || addr instanceof Inet6Address || addr.isLoopbackAddress()) {
				if(addr.getHostAddress().equals("fd00:0:0:0:0:0:0:1%tun0")){
					InetSocketAddress bindToAddress = new InetSocketAddress(addr, COAP_PORT);
					addEndpoint(new CoapEndpoint(bindToAddress));
				}
			}
		}
    }

    /*
     * Constructor for a new Hello-World server. Here, the resources
     * of the server are initialized.
     */
    public SDN_Controller() throws SocketException {
    	CoapResource FlowEngine =  new FlowEngineResourceDijkstra();
    	//CoapResource FlowEngine =  new FlowEngineResourceBalancedTree();
    	NetworkResource networkResource = new NetworkResource();
    	//networkSlicingService slicingService = new networkSlicingService(networkResource);
    	//add(slicingService);
        add(networkResource);
        add(FlowEngine);
    }

}
