package org.eclipse.californium.examples;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.examples.Model.Action;
import org.eclipse.californium.examples.Model.FlowEntry;
import org.eclipse.californium.examples.Model.FlowTable;
import org.eclipse.californium.examples.Model.Rule;
import org.eclipse.californium.examples.Model.SixLoWPANPacket;
import org.eclipse.californium.examples.Model.sdnNode;
import org.eclipse.californium.examples.Model.Neighbour;
import org.graphstream.algorithm.APSP;
import org.graphstream.algorithm.APSP.APSPInfo;
import org.graphstream.algorithm.ConnectedComponents;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.eclipse.californium.examples.Model.Constants.Fields;
import org.eclipse.californium.examples.Model.Constants.Operators;
import org.eclipse.californium.examples.Model.Constants.TypeOfAction;
import org.eclipse.californium.examples.Model.TreeNode;

import co.nstant.in.cbor.CborException;

// Tavoletta Simone
//In this flow engine we route packets based on ETX of the links between nodes
public class FlowEngineDiProva extends CoapResource{
	
	private long waitingTime = 1000 * 1;		//1 Minutes
	private long startingTime;
	private byte[] cborEncoding = null;
	public static LinkedList<TreeNode> Stored_CDS = null;
	
	public FlowEngineDiProva() {
		super("fe");
        getAttributes().setTitle("fe");
        startingTime = System.currentTimeMillis();
    }
	
	@Override
    public void handleGET(CoapExchange exchange) { 
		System.out.println("Handling GET request");
		
        // respond to the request
        exchange.respond("SDN Controller response payload to GET");
    }
	
	// TABLE MISS HANDLING
	@Override
    public void handlePOST(CoapExchange exchange) {
		System.out.println("----- Handling a Table Miss -----");
		
		if(System.currentTimeMillis() - startingTime < waitingTime){
			System.out.println(">> Wait");
			exchange.respond(ResponseCode.CHANGED);
			return;
		}
		
		//Getting info from the query, in particular source and destination MAC
		String sourceMAC = exchange.getQueryParameter("mac");		
		byte[] payload = exchange.getRequestPayload();
		
		SixLoWPANPacket SixLoPacket = new SixLoWPANPacket(payload);
		byte[] finalAddress = SixLoPacket.getFinalAddress();
		
		if(finalAddress != null){
			String destination = bytesToHex(finalAddress);
			
			//======= Otherwise, get the source node info from the Topology Manager
			System.out.println(" >> Source: " + sourceMAC);
			System.out.println(" >> Destination: " + destination);
			
			
			//Special slice
			if(destination.compareTo("0001000000000001") == 0){
				if(sourceMAC.compareTo("0001000100010001") == 0){
					FlowTable ft = new FlowTable();
					FlowEntry fe = new FlowEntry(5, 0); //No timeout for the slicing rules
					
					//Special Sink node rule
					System.out.println("Special rule for the Sink");
					Rule r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, hexStringToByteArray("0001000000000001"));
					fe.addRule(r);
					Action a = new Action(TypeOfAction.MODIFY, Fields.MH_DST_ADDR, 0, 64, hexStringToByteArray("0001000000000002")); //Special identifier
					fe.addAction(a);
					a = new Action(TypeOfAction.BROADCAST, Fields.NO_FIELD, 0, 0, null);
					fe.addAction(a);	
					ft.insertFlowEntry(fe);	
					try {
						cborEncoding = ft.toCbor();
						exchange.respond(ResponseCode.CHANGED, cborEncoding);	
					} catch (CborException e) {
						return;
					}
					return;
				}
			}
			
	    	//Slice
	    	if(destination.compareTo("0001000000000002") == 0){
	    		//Check if this node is in the CDS
	    		TreeNode thisNode = null;
	    		//Considering the 2-fold Steiner Tree
	    		Iterator<TreeNode> it = Stored_CDS.iterator();
	    		//boolean fold = false;
	    		String sink = "";
	    		while(it.hasNext()){
	    			TreeNode node = it.next();
	    			if(sink.length() <= 0)
	    				sink = node.getNodeID();
	    			
	    			/*1-fold
	    			if(node.getNodeID().compareTo(sink) == 0){
	    				if(fold)
	    					break;
	    				else
	    					fold = true;
	    			}
	    			*/
	    			
	    			if(node.getNodeID().compareTo(sourceMAC) == 0){
	    				thisNode = node;
	    				break;
	    			}
	    		}
	    		
	    		FlowTable ft = new FlowTable();
				//Medium priority, timeout set to 0 for infinite duration entry, otherwise in seconds
				FlowEntry fe = new FlowEntry(5, 0); //No timeout for the slicing rules
				
				if(thisNode == null){
				    //Rule(Fields field, int offset, int size, Operators operator, byte[] value)
					Rule r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, finalAddress);		
					//Action(TypeOfAction action, Fields field, int offset, int size, byte[] value) 
					Action a = new Action(TypeOfAction.DROP, Fields.NO_FIELD, 0, 0, null);
					fe.addRule(r);
				    fe.addAction(a);
					ft.insertFlowEntry(fe);
					try {
						cborEncoding = ft.toCbor();
					} catch (CborException e) {
						System.out.println("  >>> Encoding response error!");
					}
					exchange.respond(ResponseCode.CHANGED, cborEncoding);	
					System.out.println("Drop slice packet because you are not in the CDS");
					
				}
				else{
					Rule r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, hexStringToByteArray("0001000000000002"));
					fe.addRule(r);
					if(thisNode.getParent() != null){
						r = new Rule(Fields.LINK_SRC_ADDR, 0, 64, Operators.EQUAL, hexStringToByteArray(thisNode.getParent()));
						fe.addRule(r);
						Action a = new Action(TypeOfAction.BROADCAST, Fields.NO_FIELD, 0, 0, null);
						fe.addAction(a);
						System.out.println("Broadcast packet because you are in the CDS");
						
						FlowEntry fe2 = new FlowEntry(6, 0);
						r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, hexStringToByteArray("0001000000000002"));
						fe2.addRule(r);
						r = new Rule(Fields.LINK_SRC_ADDR, 0, 64, Operators.NOT_EQUAL, hexStringToByteArray(thisNode.getParent()));
						fe2.addRule(r);
						a = new Action(TypeOfAction.DROP, Fields.NO_FIELD, 0, 0, null);
						fe2.addAction(a);
						ft.insertFlowEntry(fe);
						ft.insertFlowEntry(fe2);
						System.out.println("And drop it if not coming from your parent");
					}
					else{
						Action a = new Action(TypeOfAction.DROP, Fields.NO_FIELD, 0, 0, null);
						fe.addAction(a);
						System.out.println("You have no parent in the CDS, drop this packet");
						ft.insertFlowEntry(fe);
					}
					
					try {
						cborEncoding = ft.toCbor();
					} catch (CborException e) {
						System.out.println("  >>> Encoding response error!");
					}
					exchange.respond(ResponseCode.CHANGED, cborEncoding);	
					
				}
				
				return;
	    	}
			
	    	
			Node srcNode = NetworkResource.getNode(sourceMAC);
		    Node dstNode = NetworkResource.getNode(destination);
		    
		    if(srcNode != null && dstNode != null){

		        //Compute the shortest path based on hop count (see NetworkGraph for details on edge weights)
			    APSP apsp = new APSP(); //All pair shortest path
			    Graph g = NetworkResource.getGraph();
			    apsp.init(g);
			    apsp.setDirected(false);
			    apsp.setWeightAttributeName("length"); //Weight of the edge is called "length" here
			    apsp.compute();  //Compute shortest path based on weight (in this case, ETX)
			    
			    APSPInfo info = g.getNode(srcNode.getId()).getAttribute(APSPInfo.ATTRIBUTE_NAME);		
				
				//===================== If there is a path
			    if(info != null){
			    	try{
				 	//	System.out.println(">> Shortest computed path: " + info.getShortestPathTo(dstNode.getId()));
						Iterator<Node> it = (Iterator<Node>) info.getShortestPathTo(dstNode.getId()).getEachNode().iterator();
						Node nextHop = null;
						while(it.hasNext()){
							nextHop = it.next();
							if(nextHop.getId().compareTo(srcNode.getId()) != 0)
								break;
						}
						//No next hop found
						if(nextHop == null){
							System.out.println("  >> No possible candidate next hop found!");
							exchange.respond(ResponseCode.CHANGED);	
							return;
						}
						
						FlowTable ft = new FlowTable();
						//Medium priority, timeout set to 0 for infinite duration entry, otherwise in seconds
						FlowEntry fe = new FlowEntry(30, 60 * 20); //20 Minutes timeout
						
					    //Rule(Fields field, int offset, int size, Operators operator, byte[] value)
						Rule r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, finalAddress);		
						//Action(TypeOfAction action, Fields field, int offset, int size, byte[] value) 
						Action a = new Action(TypeOfAction.FORWARD, Fields.NO_FIELD, 0, 64, hexStringToByteArray(nextHop.getId()));
						fe.addRule(r);
					    fe.addAction(a);
						ft.insertFlowEntry(fe);
						try {
							cborEncoding = ft.toCbor();
						} catch (CborException e) {
							System.out.println("  >>> Encoding response error!");
						}
							
						exchange.respond(ResponseCode.CHANGED, cborEncoding);
						
						System.out.println(" >> Next hop: " + nextHop.getId());
						System.out.println(" >> Rule added!");
						return;
						
			    	}catch(NullPointerException e){
						System.out.println("  >> No possible candidate next hop found!");
						exchange.respond(ResponseCode.CHANGED);	
						return;				    		
			    	}
			    }
			    //There is no path found
			    else{
					System.out.println("  >> No possible candidate next hop found!");
					exchange.respond(ResponseCode.CHANGED);	
					return;		    	
			    }				
			}// end if on source && destination
		    else{
		    	System.out.println(" >> Nodes not known, waiting for topology update...");
		    	exchange.respond(ResponseCode.CHANGED);	
		    	return;
		    }
		}//end finaladdress if
		
		exchange.respond(ResponseCode.CHANGED);		
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}

}