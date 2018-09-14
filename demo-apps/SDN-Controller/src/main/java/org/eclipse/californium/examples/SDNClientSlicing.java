package org.eclipse.californium.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;


public class SDNClientSlicing{

	private static int slice = 10; //10, 15, 25, 35
	
	private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
	private static String multicastAddress = "";
	private static int message_id = 0;
	private static long startTime;
	private File log;
	private static int numeroLog;
	private static String client_address = "2001:0db8:0:f101::2";
	private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static String[] requiredNodes;
	private static CoapClient client = null;
	
	
	private final static int simulationspeed = 1; //Real time (for testbed), 500% for simulation (5)
	
	//Main
	public static void main(String args[]) {
		//Required nodes in the slice
		SDNClientSlicing s = new SDNClientSlicing();
	}
	
	//Client
	public SDNClientSlicing(){
		System.out.println(">> Slicing client started");
		//Required nodes
		
		
		//Testbed
		if(slice == 10){
			requiredNodes = new String[]{
					"fd00::201:1:1:15", //Node 21
					"fd00::201:1:1:4", //4
					"fd00::201:1:1:d", //13
					"fd00::201:1:1:f", //15
					"fd00::201:1:1:6", //6
					"fd00::201:1:1:3", //3
					"fd00::201:1:1:14", //20
					"fd00::201:1:1:13", //19
					"fd00::201:1:1:12", //18
					"fd00::201:1:1:10" //16
			};
		}
		else{
			requiredNodes = new String[]{
					"fd00::201:1:1:15", //Node 21
					"fd00::201:1:1:4", //4
					"fd00::201:1:1:d", //13
					"fd00::201:1:1:f", //15
					"fd00::201:1:1:6" //6
			};
		}			
		
		/* //Cooja	
		requiredNodes = new String[slice];
		
		for(int i = 2; i < slice + 2; i++){ //From node 2 to (slice+1), i.e. from 2 to 26 if the slice is 25
			
			String hexadecimal = Integer.toHexString(i);
			String ip = "fd00::2";
			if(hexadecimal.length() > 1) //eg. 10 -> 210
				ip += hexadecimal;
			else 						//eg. 4 -> 204
				ip += "0" + hexadecimal;
			ip += ":" + hexadecimal + ":" + hexadecimal + ":" + hexadecimal;
			requiredNodes[i-2] = ip; //i-2 to start from [0]
		}
		*/
		
		//Find log file number
		//System.out.println(System.getProperty("user.dir"));
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
		startTime = System.nanoTime(); 
		long currentTime = (System.nanoTime() - startTime)/1000; //Microseconds
		PrintWriter makeLogFile;
		
		try {
			makeLogFile = new PrintWriter("client-" + numeroLog + ".log", "UTF-8"); //Will overwrite the file if already exists
			makeLogFile.println(currentTime * simulationspeed + ":0:Slicing client started");
			makeLogFile.close();
		}catch (Exception e) {
			System.out.println("Error while generating the log file!");
		}
		
		if(client == null){
			client = new CoapClient();
			client.setEndpoint(new CoapEndpoint(new InetSocketAddress(client_address, COAP_PORT)));
		}
		
		//Every 5 minutes do a multicast to the slice
		scheduler.scheduleAtFixedRate(new MulticastTask(), 5/simulationspeed, 5/simulationspeed, TimeUnit.MINUTES); 	
	}


	//Auxiliary function
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
	
	
	//Periodic task
	private class MulticastTask implements Runnable  {

		@Override
		public void run() {
			
			if(multicastAddress.length() <= 0){
					client.setURI("coap://[2001:0db8:0:f101::1]:5683/se");
					client.useCONs();
					String payload = fromArrayToString(requiredNodes);
					//Read the multicast address assigned to this network slice
					CoapResponse response = client.put(payload, 0);
					if (response!=null) {			
						System.out.println("Response from controller: " + response.getResponseText());
						multicastAddress = response.getResponseText();	
					}
					else{
						System.out.println("No response from the SDN Controller");
						return;
					}	
					
					delay(10000/4); //Wait for rules to correctly install	
			}
			
			System.out.println(">> Sending multicast message");		
			message_id = message_id + 1;
			
			//Send multicast
			client.setURI("coap://["+ multicastAddress +"]:5683/slice");
			client.useNONs();
			
			long currentTime = (System.nanoTime() - startTime)/1000;
			String output = currentTime * simulationspeed + ":0:";
			output += "MULTICAST_SEND_" + message_id;
			try {
				Writer log = new BufferedWriter(new FileWriter("client-" + numeroLog + ".log", true));
				log.append(output);
				log.append("\n");
				log.close();
			} catch (Exception e) {
				System.out.println("ERROR! Unable to write client log!");
			}

			client.post("" + message_id, MediaTypeRegistry.TEXT_PLAIN);
			System.out.println(">> Done");
	 }
		
	 private void delay(int delay) {
			try {
			    Thread.sleep(delay);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}		
		}

   }
}//End class