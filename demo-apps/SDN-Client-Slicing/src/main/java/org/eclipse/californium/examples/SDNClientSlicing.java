/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 ******************************************************************************/
package org.eclipse.californium.examples;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;


public class SDNClientSlicing implements Runnable{

	private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
	private String[] requiredNodes;
	private final int bindingPort;
	private final String myAddress;
	private String multicastAddress;
	
	private int repetitions = 100;			//Repeat sending 100 times
	private int period = 10 * 1000; 		//10 secs in milliseconds;
	
	private MyHandler responseHandler;
	
	public SDNClientSlicing(String multicastAddress, String address, String[] groupNodes, int bindingPort){
		this.requiredNodes = groupNodes;
		this.bindingPort = bindingPort;
		this.myAddress = address;
		this.multicastAddress = multicastAddress;
	}
	
	public void run(){
		URI ControllerUri;		
		String payload;
		CoapEndpoint endpoint;
		String assignedMulticastAddress;
		responseHandler = new MyHandler();
		try {
			endpoint = new CoapEndpoint(new InetSocketAddress(myAddress, bindingPort));			
			ControllerUri = new URI("coap://[fd00::1]:5683/Network_Slicing_Service");		
			
			//Ask the slice to the SDN controller
			CoapClient client = new CoapClient(ControllerUri);
			client.setEndpoint(endpoint);
			client.useCONs();
			payload = fromArrayToString(requiredNodes);
			//Read the multicast address assigned to this network slice
			CoapResponse response = client.put(payload, 0);
			if (response!=null) {			
				System.out.println(response.getResponseText());
				assignedMulticastAddress = response.getResponseText();	
				multicastAddress = assignedMulticastAddress;
			}
			else{
				System.out.println("No response from the SDN Controller");
			}
			
			//Start sending multicast messages
			while(true){
				sendMulticastGet(endpoint , multicastAddress);
				delay(period);
			}
			
			
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void sendMulticastGet(CoapEndpoint endpoint, String assignedMulticastAddress) throws URISyntaxException {
		URI multicastUri = new URI("coap://[" + assignedMulticastAddress + "]:5683/coap-group");
		CoapClient client = new CoapClient(multicastUri);
		client.useNONs();
		client.setEndpoint(endpoint);
		client.get(responseHandler);
		
			
	}
	
	private static void delay(int delay) {
		try {
		    Thread.sleep(delay);
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}		
	}

	/*
	 * Application entry point.
	 * 
	 */	
	public static void main(String args[]) {
		String[] requiredNodes1 = {"fd00::204:4:4:4", "fd00::205:5:5:5", "fd00::206:6:6:6"};
		String[] requiredNodes2 = {"fd00::207:7:7:7", "fd00::208:8:8:8", "fd00::209:9:9:9"};
		String address1 = "fd00:0:0:0:0:0:0:1%tun0";
		String address2 = "fd00:0:0:0:0:0:0:2%tun0";
		/*
		SDNClientSlicing client1 = new SDNClientSlicing(requiredNodes1, COAP_PORT+10);
		SDNClientSlicing client2 = new SDNClientSlicing(requiredNodes2, COAP_PORT+20);
		client1.start();
		client2.start();
		*/
		(new Thread(new SDNClientSlicing("ff0e::1", address1, requiredNodes1, COAP_PORT+10))).start();	//CLIENT 1
		delay(10000);
		(new Thread(new SDNClientSlicing("ff0e::2", address2, requiredNodes2, COAP_PORT+20))).start();	//CLIENT 2
	}
	
	private static String fromArrayToString(String[] array){
		StringBuilder builder = new StringBuilder();

		for (String string : array) {
		    if (builder.length() > 0) {
		        builder.append("\n");
		    }
		    builder.append(string);
		}

		String string = builder.toString();
		return string;
	}

}

class MyHandler implements CoapHandler{

	@Override
	public void onLoad(CoapResponse response) {
		System.out.println(response.getResponseText());		
	}

	@Override
	public void onError() {
		// TODO Auto-generated method stub
		
	}
	
}
