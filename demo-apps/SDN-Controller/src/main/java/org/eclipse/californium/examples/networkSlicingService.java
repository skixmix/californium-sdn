package org.eclipse.californium.examples;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.examples.Model.Action;
import org.eclipse.californium.examples.Model.FlowEntry;
import org.eclipse.californium.examples.Model.FlowTable;
import org.eclipse.californium.examples.Model.Rule;
import org.eclipse.californium.examples.Model.sdnNode;

import co.nstant.in.cbor.CborException;

import org.eclipse.californium.examples.Model.Constants.Fields;
import org.eclipse.californium.examples.Model.Constants.Operators;
import org.eclipse.californium.examples.Model.Constants.TypeOfAction;
import org.eclipse.californium.examples.NetworkResource;

public class networkSlicingService extends CoapResource{
	private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
	private NetworkResource networkResource;
	private int multicastCounter;
	private HashMap<InetAddress, String> allowedAddresses;
	private Hashtable<String, sdnNode> nodesTable;
	
	
	public networkSlicingService(NetworkResource networkResource) {
		super("Network_Slicing_Service");
		// set display name
        getAttributes().setTitle("Network_Slicing_Service");
        
        allowedAddresses = new HashMap<>();
        
        this.networkResource = networkResource;
        
        multicastCounter = 1;
        
        //Install firewall rule into the gateway node 
        new java.util.Timer().schedule( 
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                    	try {
							//sendCheckPointFirewall();
							//sendFireWallRule("fd00::1");
							//sendFireWallRule("fd00::201:1:1:1");
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    }
                }, 
                10000 
        );
	}
	
	@Override
    public void handleGET(CoapExchange exchange) {
		exchange.respond(ResponseCode.VALID);
    }	
	
	
	@Override
    public void handlePUT(CoapExchange exchange) {
		InetAddress multicastAddress = null;
		//Obtain list of IPv6 addresses from request payload
		byte[] payload = exchange.getRequestPayload();
		String stringPayload = new String(payload);
		String[] ipv6Addresses = stringPayload.split("\n");
		for(String str:ipv6Addresses){
			System.out.println(str);
		}
		exchange.accept();
		//Store requesting application's ip address
		InetAddress applicationAddress = exchange.getSourceAddress();
		if(allowedAddresses.containsKey(applicationAddress))
			exchange.respond(ResponseCode.CREATED, allowedAddresses.get(allowedAddresses));
		try {			
			//Create and send rule that opens the access to the WSN for this requesting application
			System.out.println(applicationAddress);
			//sendFireWallRule(applicationAddress);
			//Then, generate an IPv6 multicast address and assign it to the requested nodes
			multicastAddress = generateMulticastAddress();
			allowedAddresses.put(applicationAddress, multicastAddress.getHostName());
			assignMulticastToNodes(multicastAddress, ipv6Addresses);
			//Create the multicast tree to reach these nodes
			nodesTable = networkResource.getNodesTable();
			sdnNode sink = nodesTable.get("0001000100010001");
			MulticastTreeGenerator treeGenerator = new MulticastTreeGenerator(sink, nodesTable, ipv6Addresses);
			//Tree<sdnNode> multicastTree = treeGenerator.computeBroadcastTreeTwoSteps();
			//Tree<sdnNode> multicastTree = treeGenerator.computeMulticastTree();		
			Tree<sdnNode> multicastTree = treeGenerator.computeSelectiveFloodingTree();
			sendRulesMulticast(applicationAddress, multicastAddress, multicastTree);
			sendRulesUnicast(ipv6Addresses, applicationAddress, multicastTree);
		} catch (Exception e) {
			e.printStackTrace();
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
		exchange.respond(ResponseCode.CREATED, multicastAddress.getHostAddress());
    }
	
	private InetAddress generateMulticastAddress() throws UnknownHostException{
		String multicastAddr = "ff0e::" + multicastCounter;
		multicastCounter++;
		InetAddress multicastAddress = InetAddress.getByName(multicastAddr);
		return multicastAddress;
	}
	
	private boolean coapClientSend(String uriString, String type, byte[] payload){
		Request request = null;	
		CoapEndpoint endpoint = (CoapEndpoint) this.getEndpoints().get(0);
		if(type.equals("POST"))
			request = Request.newPost();
		else if(type.equals("PUT"))
			request = Request.newPut();
		else
			request = Request.newGet();
		if(payload != null)
			request.setPayload(payload);
		request.setURI(uriString);	
		request.setConfirmable(true);
		request.send(endpoint);
		
		Response response = null;
		try {
			response = request.waitForResponse(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (response != null) {
			return true;
		} else {
			return false;
		}
		
		
	}
	
	
	private void sendRulesUnicast(String[] targetAddresses, InetAddress applicationAddress, Tree<sdnNode> multicastTree) throws UnknownHostException{
		ArrayList<sdnNode> retries = new ArrayList<>();
		for(sdnNode node : multicastTree.getNodesSet()){
			sdnNode parent = multicastTree.getParent(node);
			byte[] ruleCbor = generateRuleUnicast(applicationAddress, node, parent);
			if(ruleCbor == null)
				continue;
			boolean result = coapClientSend("[" + node.getIpAddress() + "]:5683/local_control_agent/flow_table", "PUT", ruleCbor);
			if(result == false)
				retries.add(node);
			delay(100);
		}
		
		//Retry failed sends		
		for(sdnNode node : retries){
			sdnNode parent = multicastTree.getParent(node);
			byte[] ruleCbor = generateRuleUnicast(applicationAddress, node, parent);
			boolean result = coapClientSend("[" + node.getIpAddress() + "]:5683/local_control_agent/flow_table", "PUT", ruleCbor);
			delay(200);
		}
		
		for(String target : targetAddresses){
			String meshAddress = bytesToHex(iidFromIPv6Addres(InetAddress.getByName(target).getAddress()));
			sdnNode node = nodesTable.get(meshAddress);
			for(sdnNode parent : multicastTree.getNodesSet()){
				if(parent.getNeighbours().containsKey(meshAddress)){
					byte[] ruleCbor = generateRuleUnicast(applicationAddress, node ,parent);
					coapClientSend("[" + node.getIpAddress() + "]:5683/local_control_agent/flow_table", "PUT", ruleCbor);
					delay(200);
					break;
				}
			}
		}
	}
	

	private byte[] generateRuleUnicast(InetAddress applicationAddress, sdnNode node, sdnNode parent) {
		byte[] cborEncoding;
		FlowTable ft = new FlowTable();
		FlowEntry fe = new FlowEntry(30, 0);
		byte[] applicationMeshAddress = iidFromIPv6Addres(applicationAddress.getAddress());
		if(parent == null)
			return null;
		Rule r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, applicationMeshAddress);
		fe.addRule(r);
		Action a = new Action(TypeOfAction.FORWARD, Fields.NO_FIELD, 0, 64, hexStringToByteArray(parent.getAddress()));
		fe.addAction(a);
		ft.insertFlowEntry(fe);		
		try {
			cborEncoding = ft.toCbor();
		} catch (CborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return cborEncoding;
	}

	private void sendRulesMulticast(InetAddress applicationAddress, InetAddress multicastAddress, Tree<sdnNode> multicastTree) {
		for(sdnNode node : multicastTree.getNodesSet()){
			sdnNode parent = multicastTree.getParent(node);
			byte[] ruleCbor = generateRuleMulticast(applicationAddress, multicastAddress, node, parent);
			String nodeUnicastAddress = node.getIpAddress();
			if(nodeUnicastAddress == null)
				continue;
			boolean result = coapClientSend("[" + nodeUnicastAddress + "]:5683/local_control_agent/flow_table", "PUT", ruleCbor);
			/*
			Request request = Request.newPut();
			request.setURI("[" + node.getIpAddress() + "]:5683/local_control_agent/flow_table");	
			request.setPayload(ruleCbor);
			request.send();
			*/
			delay(100);
		}
		
		
	}

	private byte[] generateRuleMulticast(InetAddress appIPaddress, InetAddress multicastAddress, sdnNode node, sdnNode parent) {
		byte[] cborEncoding;
		FlowTable ft = new FlowTable();
		FlowEntry fe = new FlowEntry(40, 0);
		byte[] meshMulticastAddress = deriveMeshMulticast(multicastAddress);
		Rule r = new Rule(Fields.MH_DST_ADDR, 0, 16, Operators.EQUAL, meshMulticastAddress);
		fe.addRule(r);
		if(parent != null){
			r = new Rule(Fields.LINK_SRC_ADDR, 0, 64, Operators.EQUAL, hexStringToByteArray(parent.getAddress()));		
			fe.addRule(r);
		}	
		else{
			r = new Rule(Fields.LINK_SRC_ADDR, 0, 64, Operators.EQUAL, hexStringToByteArray(node.getAddress()));		
			fe.addRule(r);
		}
		r = new Rule(Fields.MH_SRC_ADDR, 0, 64, Operators.EQUAL, iidFromIPv6Addres(appIPaddress.getAddress()));		
		fe.addRule(r);
		Action a = new Action(TypeOfAction.BROADCAST, Fields.NO_FIELD, 0, 0, null);
		fe.addAction(a);
		ft.insertFlowEntry(fe);		
		try {
			cborEncoding = ft.toCbor();
		} catch (CborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return cborEncoding;
	}

	private byte[] deriveMeshMulticast(InetAddress multicastAddress) {
		byte[] meshMulticast = Arrays.copyOfRange(multicastAddress.getAddress(), multicastAddress.getAddress().length - 2, multicastAddress.getAddress().length);
		meshMulticast[0] &= (1 << 5) - 1;
		meshMulticast[0] |= 0x80;
		return meshMulticast;
	}

	private void assignMulticastToNodes(InetAddress multicastAddress, String[] ipv6Addresses) {
		ArrayList<String> retries = new ArrayList<>();
		for(String addr : ipv6Addresses){
			boolean result = coapClientSend("[" + addr + "]" + ":5683/coap-group", "POST", multicastAddress.getAddress());
			if(result == false)
				retries.add(addr);
			delay(100);
		}	
		//Retry failed sends		
		for(String addr : retries){			
			boolean result = coapClientSend("[" + addr + "]" + ":5683/coap-group", "POST", multicastAddress.getAddress());
			delay(200);
		}
	}

	private void delay(int delay) {
		try {
		    Thread.sleep(delay);
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}		
	}

	private void sendCheckPointFirewall() throws CborException{
		FlowTable ft = new FlowTable();
		FlowEntry fe = new FlowEntry(20, 0);
		byte[] value_zero = {0};
		//IF NODE_STATE == 0 THEN DROP_PACKET
		Rule r = new Rule(Fields.NODE_STATE, 0, 8, Operators.EQUAL, value_zero);
		Action a = new Action(TypeOfAction.DROP, Fields.NO_FIELD, 0, 0, null);
		fe.addRule(r);
		fe.addAction(a);
		ft.insertFlowEntry(fe);
		
		//IF NODE_STATE == 1 THEN NODE_STATE = 0
		byte[] value_one = {1};
		fe = new FlowEntry(20, 0);
		r = new Rule(Fields.NODE_STATE, 0, 8, Operators.EQUAL, value_one);
		fe.addRule(r);
		a = new Action(TypeOfAction.MODIFY, Fields.NODE_STATE, 0, 8, value_zero);		
		fe.addAction(a);
		a = new Action(TypeOfAction.CONTINUE, Fields.NO_FIELD, 0, 0, null);		
		fe.addAction(a);
		ft.insertFlowEntry(fe);
		byte[] cborEncoding = ft.toCbor();
		coapClientSend("[fd00::201:1:1:1]:5683/local_control_agent/flow_table", "PUT", cborEncoding);
		/*
		Request request = Request.newPut();
		request.setURI("[fd00::201:1:1:1]:5683/local_control_agent/flow_table");	
		request.setPayload(cborEncoding);
		request.send();
		*/
	}

	private void sendFireWallRule(String applicationAddress) throws CborException, UnknownHostException {
		InetAddress address = InetAddress.getByName(applicationAddress);
		sendFireWallRule(address);
	}

	
	private void sendFireWallRule(InetAddress applicationAddress) throws CborException, UnknownHostException {
		//Check if this IP address has been already added to the firewall rules set
		if(allowedAddresses.containsKey(applicationAddress))
			return;
		
		FlowTable ft = new FlowTable();
		FlowEntry fe = new FlowEntry(10, 0);
		byte[] value = {1};
		//From the address extract the relative mesh source address
		byte[] sourceAddress = iidFromIPv6Addres(applicationAddress.getAddress());
		Rule r = new Rule(Fields.IP_SRC_ADDR, 0, 128, Operators.EQUAL, applicationAddress.getAddress());
		Action a1 = new Action(TypeOfAction.MODIFY, Fields.NODE_STATE, 0, 8, value);
		Action a2 = new Action(TypeOfAction.CONTINUE, Fields.NO_FIELD, 0, 0, null);
		fe.addRule(r);
		fe.addAction(a1);
		fe.addAction(a2);
		ft.insertFlowEntry(fe);
		byte[] cborEncoding = ft.toCbor();
		coapClientSend("[fd00::201:1:1:1]:5683/local_control_agent/flow_table", "PUT", cborEncoding);
		/*
		Request request = Request.newPut();
		request.setURI("[fd00::201:1:1:1]:5683/local_control_agent/flow_table");
		request.setPayload(cborEncoding);
		request.send();	
		*/
		
	}

	private byte[] iidFromIPv6Addres(byte[] applicationAddress) {
		byte[] iid = Arrays.copyOfRange(applicationAddress, applicationAddress.length / 2, applicationAddress.length);
		iid[0] ^= 0x02;
		return iid;
	}
	
	private static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	private static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
	
	public static void main(String[] args) {
		try {
			networkSlicingService net = new networkSlicingService(null);
			InetAddress multicastAddress = InetAddress.getByName("ff0e::5");
			net.deriveMeshMulticast(multicastAddress);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
