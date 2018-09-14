package org.eclipse.californium.examples;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.examples.Model.Action;
import org.eclipse.californium.examples.Model.CDS;
import org.eclipse.californium.examples.Model.FlowEntry;
import org.eclipse.californium.examples.Model.FlowTable;
import org.eclipse.californium.examples.Model.Rule;
import org.eclipse.californium.examples.Model.SteinerTree;
import org.eclipse.californium.examples.Model.TreeNode;
import org.eclipse.californium.examples.Model.sdnNode;
import org.graphstream.graph.Node;
import org.eclipse.californium.examples.Model.Constants.Fields;
import org.eclipse.californium.examples.Model.Constants.Operators;
import org.eclipse.californium.examples.Model.Constants.TypeOfAction;
import org.jfree.util.StringUtils;

import co.nstant.in.cbor.CborException;

//Tavoletta Simone
// Northbound interface provided by the controller to external applications, which will ask for multiple slices of the network
public class SlicingEngine extends CoapResource{
	private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
	private static HashSet<String> nodeIPs; //Node ips known to the controller
	private static HashMap<String, String[]> multicast_group; //Mapping with multicast group id and the relative set of preferred nodes
	private static HashMap<String, String[]> app_mc; //Mapping between app IPv6 and all the multicast groups handled
	private String Sink;
	private int multicastCounter;
	private int numeroLog = 0;
	private static long startTime;
	
	public SlicingEngine(){
		super("se");
		getAttributes().setTitle("se");
		//Initialize the set of KNOWN node IPs as empty
		nodeIPs = new HashSet<>();
		multicast_group = new HashMap<>();
		app_mc = new HashMap<>();
		Sink = "0001000100010001"; //Sink node is known
		multicastCounter = 1;
		
		//Trova il numero di file di log
				System.out.println(System.getProperty("user.dir"));
				File folder = new File(System.getProperty("user.dir"));
				File[] listOfFiles = folder.listFiles();
				int maxNumero = 0;
				    for (int i = 0; i < listOfFiles.length; i++) {
				      if (listOfFiles[i].isFile()) {
				        if(listOfFiles[i].getName().contains("log")){
				        	//controller-X.log
				        	int numer = Integer.parseInt(listOfFiles[i].getName().split("-")[1].replace(".log", ""));
				        	if(numer > maxNumero)
				        		maxNumero = numer;
				        }
				      }
				    }
				numeroLog = maxNumero + 1;    
				
				System.out.println("Log " + numeroLog);
				startTime = System.nanoTime(); 
				long currentTime = (System.nanoTime() - startTime)/1000;
				PrintWriter makeLogFile;
				
				try {
					makeLogFile = new PrintWriter("controller-" + numeroLog + ".log", "UTF-8"); //Will overwrite the file if already exists
					makeLogFile.println(currentTime + ":1:Slicing Engine started");
					makeLogFile.close();
				}catch (Exception e) {
					System.out.println("Error while generating the log file!");
				}
	}
	
	public static void addNodeIP(String ip){
		nodeIPs.add(ip.replace("/", ""));
	}
	
	@Override
    public void handleGET(CoapExchange exchange) {
		System.out.println("----------------- List of available nodes in the network --------------------\n");
		if(nodeIPs.size() <= 0){
			System.out.println("No nodes currently known.");
		}
		else{
			Iterator<String> it = nodeIPs.iterator();
			while(it.hasNext()){
				String ip = it.next();
				System.out.println(ip);
			}
		}
		exchange.respond(ResponseCode.VALID);
    }
	
	@Override
    public void handlePUT(CoapExchange exchange) {
		System.out.println("Application IPv6 address: " + exchange.getSourceAddress().toString());
		//Here, given the app IPv6, we can limit the nodes that are accessible to the specific application
		//We can also limit the maximum number of multicast groups that one application can ask, let's say 2
		if(app_mc.size() > 2){
			exchange.respond(ResponseCode.FORBIDDEN);
			return;
		}
		
		if(FlowEngineDiProva.Stored_CDS != null){
			exchange.respond(ResponseCode.CREATED, "fd00::201:0:0:1");
			return;
		}
		
		//Now, the app will send a payload containing the node IPv6 addresses wantend for the slice
		byte[] payload = exchange.getRequestPayload();
		String stringPayload = new String(payload);
		String[] ipv6Addresses = stringPayload.split("\n");
		
		if(ipv6Addresses.length <= 0){
			System.out.println("No nodes in the slice, aborting");
			exchange.respond(ResponseCode.BAD_REQUEST);
			return;
		}
		
		System.out.println("Requested slice");
		boolean hasSink = false;
		for(String str:ipv6Addresses){
			System.out.println(str);
		}
		
		//Always add the sink as a node in the slice
		if(!hasSink){
			String[] temp = new String[ipv6Addresses.length + 1];
			for(int i = 0; i < ipv6Addresses.length; i++)
				temp[i] = ipv6Addresses[i];
			temp[ipv6Addresses.length] = Sink;
			ipv6Addresses = temp;
		}
		
		exchange.accept(); //Accept the request, then process
		
		//Construct the set P of preferred nodes
		LinkedList<String> P = new LinkedList<>();
		P.add(Sink);
		//System.out.print("Preferred nodes MACs: ");
		long currentTime = (System.nanoTime() - startTime)/1000;
		String output = currentTime + ":1:SE Contacted by Client. Preferred nodes MACs are listed below\n";
		output += currentTime + ":1:";
		for(String str: ipv6Addresses){
			if(str.compareTo(Sink) == 0) //Avoid the Sink, already inserted
				continue;
			
			//System.out.print(macFromIPv6(str) + " ");
			output += macFromIPv6(str) + " ";
			P.add(macFromIPv6(str));
			
			if(NetworkResource.getNode(macFromIPv6(str)) != null)
				System.out.println("Node " + macFromIPv6(str) + " is known!");
			else
				System.out.println("Warning: Node " + macFromIPv6(str) + " is NOT known!!");
		}
		//System.out.print("\n");
		
		
		//Write to log
		try {
			Writer log = new BufferedWriter(new FileWriter("controller-" + numeroLog + ".log", true));
			log.append(output);
			log.append("\n");
			log.close();
		} catch (Exception e) {
			System.out.println("ERROR! Unable to write controller log!");
		}
		
		output = "";
		
		//1-Fold case
		/*
		SteinerTree t = CDS.Algorithm_B(NetworkResource.getGraph(), (LinkedList<String>)P.clone());
		
		//System.out.println("1-fold CDS:");
		output += currentTime + ":1: 1-Fold CDS constructed:\n";
		Iterator<String> it = t.getNodes().iterator();
		
		
		LinkedList<TreeNode> cds = new LinkedList<>();
		
		while(it.hasNext()){
 			String n = it.next();
 			//System.out.print(n);
 			output += currentTime + ":1: " + n;
 			
 			cds.add(t.getTreeNode(n));
 			
 			if(!t.getTreeNode(n).getChildrens().isEmpty()){
 				//System.out.print(" (with children: ");
 				output += " (with children: ";
 				Iterator<TreeNode> chi = t.getTreeNode(n).getChildrens().iterator();
 				while(chi.hasNext()){
 					TreeNode children = chi.next();
 					//System.out.print(children.getNodeID());
 					output += children.getNodeID();
 					if(chi.hasNext()){
 						//System.out.print(", ");
 						output += ", ";
 					}
 				}
 				//System.out.print(")");
 				output += ")";
 			}
 			//System.out.print("\n");
 			output += "\n";
 		}
		*/
		
		//2-Fold case
		LinkedList<TreeNode> cds = CDS.Algorithm_C(NetworkResource.getGraph(), (LinkedList<String>)P.clone());
		
		output += currentTime + ":1: 2-Fold CDS constructed:\n";	
		Iterator<TreeNode> it4 = cds.iterator();
 		while (it4.hasNext()){
 			TreeNode n = it4.next();
 			output += currentTime + ":1:" + n.getNodeID();
 			if(n.getChildrens().size() > 0){
 				output += " (with children: ";
 				Iterator<TreeNode> chi = n.getChildrens().iterator();
 				while(chi.hasNext()){
 					TreeNode children = chi.next();
 					output += children.getNodeID();
 					if(chi.hasNext())
 						output += ", ";
 				}
 				output += ")";
 			}
 			output += "\n";
 		}
 		
		output += currentTime + ":1: Nodes in the backbone  " + cds.size() + "\n";
		Iterator<TreeNode> itt = cds.iterator();
		HashSet<String> contorno = new HashSet<>();
		while(itt.hasNext()){
			//Per ogni nodo nello slice, vedi chi sono i vicini e conta quelli di contorno
			TreeNode n = itt.next();
			Iterator<Node> ittt = NetworkResource.getNode(n.getNodeID()).getNeighborNodeIterator();
			while(ittt.hasNext()){
				Node nn = ittt.next();
				if(!contorno.contains(nn.getId())){
					contorno.add(nn.getId());
				}
			}
		}
		output += currentTime + ":1: 2-Fold CONTORNO " + (contorno.size() - cds.size()) + "\n";
		output += currentTime + ":1: Installing rules and answering to client\n";

		
		
		//Store the CDS inside the controller
		FlowEngineDiProva.Stored_CDS = cds;

		if(cds != null)
			System.out.println("CDS constructed!");
		
		//Generate a multicast address for the group
		InetAddress multicastAddress;
		try {
			multicastAddress = generateMulticastAddress();
		} catch (UnknownHostException e) {
			System.out.println("Error while generating the multicast address, aborting.");
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
			return;
		}
		
		//Answer
		exchange.respond(ResponseCode.CREATED, multicastAddress.getHostAddress());
		
		//Update the sets
		multicast_group.put(multicastAddress.getHostAddress(), ipv6Addresses);
		String[] ms = app_mc.get(exchange.getSourceAddress().toString());
		
		if(ms == null){
			String[] added = new String[1];
			added[0] = multicastAddress.getHostAddress();
			app_mc.put(exchange.getSourceAddress().toString(), added);
		}else{
			app_mc.remove(exchange.getSourceAddress().toString());
			ms[ms.length] = multicastAddress.getHostAddress();
			app_mc.put(exchange.getSourceAddress().toString(), ms);
		}
		
		//Now we should install a rule in the Sink (BR)
		
		
		//And then, install a rule in each node of the slice for accepting and retransmitting the multicast packet
		setSliceRules(cds, multicastAddress);
		
		
		//For what regards normal communication (App <--> Node), the standard SDN rules are utilized, and the installed rules at the
		//BR are used to avoid the application talking to nodes that DO NOT belong to the slice		
		currentTime = (System.nanoTime() - startTime)/1000;
		output += currentTime + ":1: Done!";
		//Write to log
		try {
			Writer log = new BufferedWriter(new FileWriter("controller-" + numeroLog + ".log", true));
			log.append(output);
			log.append("\n");
			log.close();
		} catch (Exception e) {
			System.out.println("ERROR! Unable to write controller log!");
		}
	}
	
	
	
	
	//Auxiliary Functions
	
	private void setSliceRules(LinkedList<TreeNode> cds, InetAddress multicast){
		//We know that all packets coming from the BR will be assigned the MAC address of tunslip, for which rules are already inside the network
		//So, the only rules needed are the ones telling the nodes to accept (and / or) forward a multicast packet
		
		//Considering the 1-fold CDS or 2-fold CDS
		Iterator<TreeNode> it = cds.iterator();
		boolean fold = false;
		while(it.hasNext()){
			TreeNode node = it.next();
			
			/*1-fold
			if(node.getNodeID().compareTo(Sink) == 0){
				if(fold)
					break;
				else
					fold = true;
			}
			*/
			byte[] rule = generateRuleMulticast(multicast, node);
			if(rule != null){
				String nodeIP = NetworkResource.getNodeIP(node.getNodeID());
				if(nodeIP == null)
					continue;
				
				System.out.println("Installing rule on node " + node.getNodeID() + " -> " + nodeIP);
				boolean result = coapClientSend("[" + nodeIP + "]:5683/lca/ft", "PUT", rule);
				delay(100);
			}
		}

		
	}
	
	//Generate the multicast rule
	private byte[] generateRuleMulticast(InetAddress multicast, TreeNode node){
		byte[] cborEncoding;
		FlowTable ft = new FlowTable();
		FlowEntry fe = new FlowEntry(5, 0); //High priority
		Rule r;
		Action a;
		
		//Sink-specific rule
		if(node.getNodeID().compareTo(Sink) == 0){
			System.out.println("Special rule for the Sink");
			r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, hexStringToByteArray("0001000000000001"));
			fe.addRule(r);
			a = new Action(TypeOfAction.MODIFY, Fields.MH_DST_ADDR, 0, 64, hexStringToByteArray("0001000000000002")); //Special identifier
			fe.addAction(a);
			a = new Action(TypeOfAction.BROADCAST, Fields.NO_FIELD, 0, 0, null);
			fe.addAction(a);	
			ft.insertFlowEntry(fe);	
			try {
				cborEncoding = ft.toCbor();
			} catch (CborException e) {
				e.printStackTrace();
				return null;
			}
			return cborEncoding;
		}
		
		r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, hexStringToByteArray("0001000000000002"));
		fe.addRule(r);
		r = new Rule(Fields.LINK_SRC_ADDR, 0, 64, Operators.EQUAL, hexStringToByteArray(node.getParent()));
		fe.addRule(r);
		a = new Action(TypeOfAction.BROADCAST, Fields.NO_FIELD, 0, 0, null);
		fe.addAction(a);
		ft.insertFlowEntry(fe);		
		try {
			cborEncoding = ft.toCbor();
		} catch (CborException e) {
			e.printStackTrace();
			return null;
		}
		return cborEncoding;
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
	
	private byte[] deriveMeshMulticast(InetAddress multicastAddress) {
		byte[] meshMulticast = Arrays.copyOfRange(multicastAddress.getAddress(), multicastAddress.getAddress().length - 2, multicastAddress.getAddress().length);
		meshMulticast[0] &= (1 << 5) - 1;
		meshMulticast[0] |= 0x80;
		return meshMulticast;
	}
	
	//Generate multicast address
	private InetAddress generateMulticastAddress() throws UnknownHostException{
		String multicastAddr = "fd00::201:0:0:" + multicastCounter;
		multicastCounter++;
		InetAddress multicastAddress = InetAddress.getByName(multicastAddr);
		return multicastAddress;
	}
	
	//Extract MAC address from node IPv6
	private String macFromIPv6(String ip){
		//Based on the assumption that in our network the mapping is fd00:0:0:0:201:1:1:1  -->  0001000100010001
	
		//On the testbed it is 000100010001000x
		String nodeNumber = ip.split(":")[ip.split(":").length - 1];
		
		//MAC address should be of length equal to 16
		String mac = "000100010001";
		//for the testbed
		for(int j = 0; j < 4 - nodeNumber.length(); j++)
			mac += "0";
		mac += nodeNumber; //Es. from fd00::201:1:1:1a --> 000100010001001a

		return mac;
	}
	
	
	//Send COAP request to node
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
	
	
	private void delay(int delay) {
		try {
		    Thread.sleep(delay);
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}		
	}
}
