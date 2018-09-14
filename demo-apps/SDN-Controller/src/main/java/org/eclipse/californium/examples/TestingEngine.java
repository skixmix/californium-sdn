package org.eclipse.californium.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.server.resources.CoapExchange;

//Tavoletta Simone
// Testing engine used for experiments in the testbed
public class TestingEngine  extends CoapResource{

	private static long startTime;
	private File log;
	//For polling
	public static Map<String, Integer> nodeIPs_msgIDs;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private int numeroLog = 0;
	
	public TestingEngine() throws URISyntaxException {
		super("ts");
		getAttributes().setTitle("ts");
		
		nodeIPs_msgIDs = new HashMap<>();
		//scheduler.scheduleAtFixedRate(new PollTask(), 3, 3, TimeUnit.MINUTES); //Every 3 minutes do a poll to all nodes
		
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:3", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:8", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:17", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:4", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:16", 0);
		
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:15", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:a", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:b", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:c", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:d", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:e", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:7", 0);
		
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:13", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:14", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:5", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:9", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:12", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:6", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:f", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:11", 0);
		nodeIPs_msgIDs.put("/fd00:0:0:0:201:1:1:10", 0);
		
		
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
			makeLogFile.println(currentTime + ":1:Testing engine started");
			makeLogFile.close();
		}catch (Exception e) {
			System.out.println("Error while generating the log file!");
		}

	}
	
	private class PollTask implements Runnable  {

		@Override
		public void run() {
			//Pick the next node
			System.out.println(">> Poll Running");
			List<String> ips = new ArrayList<String>(nodeIPs_msgIDs.keySet());
			
			for(int i = 0; i < ips.size(); i++){
				String node = ips.get(i);
				int msgID = nodeIPs_msgIDs.get(node) + 1;
				nodeIPs_msgIDs.put(node, msgID);
				int nodeID = Integer.parseInt(node.split(":")[7], 16);
				System.out.println("Polling node " + nodeID);
				//Send a COAP get
				try {
					URI uri = new URI("coap://["+node.replaceAll("/", "").trim()+"]:5683/hello");
					CoapClient client = new CoapClient(uri);
					client.setTimeout(8000); //8 seconds timeout
									
					long currentTime = (System.nanoTime() - startTime)/1000;
					String output = currentTime + ":1:";
					output += "UDP_POLL_SENT_" + msgID + "_" + nodeID;
					try {
						Writer log = new BufferedWriter(new FileWriter("controller-" + numeroLog + ".log", true));
						log.append(output);
						log.append("\n");
						log.close();
					} catch (Exception e) {
						System.out.println("ERROR! Unable to write controller log!");
					}
	
					CoapResponse response = client.get();
				
					
					if (response!=null) { //Response to get
						currentTime = (System.nanoTime() - startTime)/1000;
						output = currentTime + ":1:UDP_POLL_RESPONSE_" + msgID + "_" + nodeID;
						try {
							Writer log = new BufferedWriter(new FileWriter("controller-" + numeroLog + ".log", true));
							log.append(output);
							log.append("\n");
							log.close();
						} catch (Exception e) {
							System.out.println("ERROR! Unable to write controller log!");
						}
					} else { //No response
						System.out.println("No response received");
					}
					
				} catch (URISyntaxException e) {
					System.out.println("Error while trying to do a poll!");
				}
			}
		}
	}
	
	@Override
    public void handlePOST(CoapExchange exchange) { 
		int msgID = Integer.parseInt(exchange.getQueryParameter("id"));
        
		long currentTime = (System.nanoTime() - startTime)/1000; //Time into testbed logs is in MICROseconds (/1000)
		int sourceNode = Integer.parseInt(exchange.getSourceAddress().toString().split(":")[7], 16);
		/* We can get a message like:
		 *  - "?onlyup=1&id=%d" if it is a UDP_ONLYUP_SEND_msgID
		 *  - "?updown=1&id=%d" if it is a UDP_UPDOWN_SEND_msgID
		*/
		
		
		// respond to the request (with the message id)        
		String output = currentTime + ":1:";
		
		if(exchange.getQueryParameter("onlyup") != null){
			//It is an ONLYUP message
			output += "UDP_ONLYUP_RECEIVED_" + msgID + "_" + sourceNode;
		}
		else if(exchange.getQueryParameter("updown") != null){
			//It is a UPDOWN message
			output += "UDP_UPDOWN_RECEIVED_" + msgID + "_" + sourceNode;
		}
		
		try {
			Writer log = new BufferedWriter(new FileWriter("controller-" + numeroLog + ".log", true));
			log.append(output);
			log.append("\n");
			log.close();
		} catch (Exception e) {
			System.out.println("ERROR! Unable to write controller log!");
		}
		
		exchange.respond("" + msgID); //Answer back with the message ID
    }

}
