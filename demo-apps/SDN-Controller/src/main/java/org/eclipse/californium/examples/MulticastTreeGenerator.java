package org.eclipse.californium.examples;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;

import org.eclipse.californium.examples.Model.sdnNode;
import org.eclipse.californium.examples.Model.sdnNode.Neighbour;

public class MulticastTreeGenerator {

	private Hashtable<String, sdnNode> nodesTable;
	private HashSet<String> groupNodes;
	private Tree<sdnNode> multicastTree;
	private sdnNode sink;
	
	public MulticastTreeGenerator(sdnNode sink, Hashtable<String, sdnNode> nodesTable, String[] multicastNodeIPv6Addresses) {
		super();
		this.nodesTable = nodesTable;
		this.groupNodes = new HashSet<>();
		this.sink = sink;
		String[] macAddressList = convertIPv6ToIID(multicastNodeIPv6Addresses);
		for(int i = 0; i < macAddressList.length; i++)
			groupNodes.add(macAddressList[i]);
	}
	/*----------------------------------------------------------------------------------------------------------------------*/
	
	//Selective flooding tree built through breadth first exploring
	public Tree<sdnNode> computeSelectiveFloodingTree(){
		Tree<sdnNode> floodingTree = new Tree<sdnNode>(sink);
		ArrayList<sdnNode> nodesToVisit = new ArrayList<>();
		//Start from sink's neighbours
		for(int i = 0; i < sink.getNeighboursSize(); i++){
			sdnNode currentNode = nodesTable.get(sink.getNeighbor(i).getAddress());
			//Add these neighbour nodes to the queue 
			nodesToVisit.add(currentNode);
			//Attach these nodes to the root node (the sink)
			floodingTree.addNodeToParent(sink, currentNode);
		}
		//Explore the whole topology using breadth first approach 
		while(!nodesToVisit.isEmpty()){
			sdnNode currentNode = nodesToVisit.remove(0);
			for(int i = 0; i < currentNode.getNeighboursSize(); i++){
				sdnNode neighbour = nodesTable.get(currentNode.getNeighbor(i).getAddress());
				//Add it only if not present in the tree
				if(!floodingTree.getNodesSet().contains(neighbour)){
					//Add node's neighbours to the queue
					nodesToVisit.add(neighbour);
					//Attach neighbour nodes to the current one as children tree nodes
					floodingTree.addNodeToParent(currentNode, neighbour);
				}
			}
			System.out.println("Node " + currentNode.getAddress() + " visited.");
		}
		
		System.out.println("FLOODING TREE:");
		floodingTree.printTree();
		
		return floodingTree;
	}
	
	
	/*----------------------------------------------------------------------------------------------------------------------*/
	public Tree<sdnNode> computeMulticastTree(){
		Tree<sdnNode> broadcastTree = computeBroadcastTreeTwoSteps();
		broadcastTree.printTree();
		Tree<sdnNode> multicastTree = new Tree<sdnNode>(sink);
		sdnNode selectedNode = null;
		HashSet<String> uncoveredNodes = (HashSet<String>) groupNodes.clone(); 
		
		while(!uncoveredNodes.isEmpty()){
			//Select leaf node that covers the most of possible multicast node
			selectedNode  = selectNodeMaxCoverage(broadcastTree, uncoveredNodes);			
			//Scan the relative branch and add it to the multicast tree
			Tree<sdnNode> branch = broadcastTree.getBranch(selectedNode);
			System.out.println("BRANCH:");
			branch.printTree();
			//Remove from the uncovered nodes set all the multicast node covered by the just added branch
			removeUncoveredNodes(branch, uncoveredNodes);
			//Merge the branch to the multicast tree
			multicastTree.mergeWith(branch);
			//Repeat until the uncovered nodes set is empty
		}
		System.out.println("MULTICAST TREE:");
		multicastTree.printTree();
		return multicastTree;
	}
	
	private void removeUncoveredNodes(Tree<sdnNode> branch, HashSet<String> uncoveredNodes) {
		sdnNode[] nodesArray = branch.getNodesSet().toArray(new sdnNode[1]);
		for(sdnNode node : nodesArray){
			for(String neighbour : node.getNeighbours().keySet()){
				uncoveredNodes.remove(neighbour);
			}
		}
	}

	private sdnNode selectNodeMaxCoverage(Tree<sdnNode> broadcastTree, HashSet<String> groupNodes){
		sdnNode selectedNode = null;
		int maxCoveredNodes = 0;
		int currentCoveredNodes = 0;
		ArrayList<sdnNode> nodesArray = new ArrayList<>(Arrays.asList(broadcastTree.getNodesSet().toArray(new sdnNode[1])));
		sortNodesArray(nodesArray);
		for(sdnNode node : nodesArray){
			for(int i = 0; i < node.getNeighboursSize(); i++){
				if(groupNodes.contains(node.getNeighbor(i).getAddress())){
					currentCoveredNodes++;
				}
			}
			System.out.println("Node: " + node.getAddress() + " covered: " + currentCoveredNodes);
			if(maxCoveredNodes < currentCoveredNodes){
				maxCoveredNodes = currentCoveredNodes;
				selectedNode = node;
			}
			currentCoveredNodes = 0;
		}
		System.out.println("Selected Node: " + selectedNode.getAddress() + " covered: " + maxCoveredNodes);
		return selectedNode;
	}
	
	public Tree<sdnNode> computeBroadcastTreeTwoSteps(){
		Tree<sdnNode> broadcastTree = new Tree<sdnNode>(sink);
		HashSet<sdnNode> blackNodes = new HashSet<>();
		HashSet<sdnNode> greyNodes = new HashSet<>();
		HashSet<sdnNode> whiteNodes = new HashSet<>();
		
		//Color the sink node as black
		blackNodes.add(sink);
		//Color the neighbors of sink as grey 
		addNeighboursToSet(greyNodes, sink, blackNodes, whiteNodes);
		//Color as white all the rest of nodes
		for(sdnNode node : nodesTable.values()){
			addNeighboursToSet(whiteNodes, node, blackNodes, greyNodes);
		}
		
		while(!whiteNodes.isEmpty()){
			int yieldPair = 0;
			int singleNodeYield = 0;
			int maxSingleYield = 0;
			int maxPairYield = 0;
			sdnNode singleNode = null;
			sdnNode[] pairNodes = new sdnNode[2];
			
			for(sdnNode grey : greyNodes){
				singleNodeYield = computeYield(grey, whiteNodes);
				//System.out.println("Esamino nodo: " + grey.getAddress() + " yield = " + singleNodeYield);
				for(String addr : grey.getNeighbours().keySet()){
					sdnNode neighbour = nodesTable.get(addr);
					if(whiteNodes.contains(neighbour)){
						yieldPair = singleNodeYield + computeUnionYield(grey, neighbour, whiteNodes);
						//System.out.println("Esamino nodo successivo: " + neighbour.getAddress() + " yield = " + yieldPair);
						if(yieldPair >= maxPairYield){
							maxPairYield = yieldPair;
							pairNodes[0] = grey;
							pairNodes[1] = neighbour;
						}
					}
				}
				if(singleNodeYield > maxSingleYield){
					maxSingleYield = singleNodeYield;
					singleNode = grey;
				}
			}
			
			//System.out.println("Max single yield: " + maxSingleYield + " Max pair yield " + maxPairYield);
			
			if(maxSingleYield >= maxPairYield){
				sdnNode parent = findParent(singleNode, blackNodes);
				broadcastTree.addNodeToParent(parent, singleNode);
				blackNodes.add(singleNode);
				greyNodes.remove(singleNode);
				addNeighboursToSet(greyNodes, singleNode, blackNodes, null);
				removeNeighboursFromSet(whiteNodes, singleNode);
				//System.out.println("Added node: " + singleNode.toString());
			}
			else{
				sdnNode parent = findParent(pairNodes[0], blackNodes);
				broadcastTree.addNodeToParent(parent, pairNodes[0]);
				blackNodes.add(pairNodes[0]);
				greyNodes.remove(pairNodes[0]);
				addNeighboursToSet(greyNodes, pairNodes[0], blackNodes, null);
				removeNeighboursFromSet(whiteNodes, pairNodes[0]);
				broadcastTree.addNodeToParent(pairNodes[0], pairNodes[1]);
				blackNodes.add(pairNodes[1]);
				greyNodes.remove(pairNodes[1]);
				addNeighboursToSet(greyNodes, pairNodes[1], blackNodes, null);
				removeNeighboursFromSet(whiteNodes, pairNodes[1]);				
				
				//System.out.println("Added nodes: " + pairNodes[0].toString() + " and " + pairNodes[1].toString());
			}
			
			maxPairYield = 0;
			maxSingleYield= 0;
		}
		
		return broadcastTree;
	}
	
	private sdnNode findParent(sdnNode singleNode, HashSet<sdnNode> blackNodes) {
		for(String addr : singleNode.getNeighbours().keySet()){
			sdnNode neighbour = nodesTable.get(addr);
			if(blackNodes.contains(neighbour))
				return neighbour;
		}
		return null;
	}

	private void removeNeighboursFromSet(HashSet<sdnNode> nodeSet, sdnNode node) {
		for(String addr : node.getNeighbours().keySet()){
			sdnNode neighbour = nodesTable.get(addr);
			nodeSet.remove(neighbour);
		}
	}

	private int computeUnionYield(sdnNode grey, sdnNode neighbour, HashSet<sdnNode> whiteNodes) {
		int yield = 0;
		for(String addr : neighbour.getNeighbours().keySet()){
			sdnNode neighboursOfNeigh = nodesTable.get(addr);
			if(whiteNodes.contains(neighboursOfNeigh) && !grey.getNeighbours().contains(neighboursOfNeigh))
				yield++;
		}
		
		return yield;
	}

	private int computeYield(sdnNode node, HashSet<sdnNode> whiteNodes){
		int yield = 0;
		for(String addr : node.getNeighbours().keySet()){
			sdnNode neighbour = nodesTable.get(addr);
			if(whiteNodes.contains(neighbour))
				yield++;
		}
		return yield;
	}
	
	private void addNeighboursToSet(HashSet<sdnNode> nodeSet, sdnNode currentNode, HashSet<sdnNode> excludingSet1, HashSet<sdnNode> excludingSet2) {
		for(String addr : currentNode.getNeighbours().keySet()){
			sdnNode node = nodesTable.get(addr);			
			if(excludingSet1 != null && excludingSet1.contains(node) || excludingSet2 != null && excludingSet2.contains(node))
				continue;
			else{
				nodeSet.add(nodesTable.get(addr));
			}
		}
	}

	/*-------------------------------BIP algorithm------------------------------------------*/
	public Tree<sdnNode> computeBroadcastTreeBip(){
		Tree<sdnNode> broadcastTree = new Tree<sdnNode>(sink);	//TREE
		HashSet<String> coveredNodes = new HashSet<>();			//S
		Integer P[][] = null;									//P
		Integer linkCostMatrix[][] = null;						//Link_cost_matrix
		/*---------------Auxiliary data structures---------------*/
		Hashtable<String, Integer> nodeIndexMap = new Hashtable<>();
	
		coveredNodes.add(sink.getAddress());					//S = {source}
		P = createConnectivityMatrix(nodesTable, nodeIndexMap);
		linkCostMatrix = P.clone();
		
		//Main loop: it ends when all nodes are covered by the tree, i.e. when |S| == |Graph|
		while(coveredNodes.size() < nodesTable.size()){
			sdnNode[] parent_child = computeNearestLink();		//(I,J) = compute_nearest_link(TREE, link_cost_matrix);
			coveredNodes.add(parent_child[1].getAddress());		//S = S union {J}
			broadcastTree.addNodeToParent(parent_child[0], parent_child[1]);	//TREE[J] = I
			
			for(String str : nodesTable.keySet()){				//
				if(!coveredNodes.contains(str)){					//while (j is not in S)
					int parentIndex = nodeIndexMap.get(parent_child[0].getAddress());	//Derive I
					int childIndex = nodeIndexMap.get(parent_child[1].getAddress());	//Derive J
					int currentChildIndex =  nodeIndexMap.get(str);						//Derive j
					//link_cost_matrix[I][j] = P[I][j] − P[I][J];
					linkCostMatrix[parentIndex][currentChildIndex] = P[parentIndex][currentChildIndex] - P[parentIndex][childIndex];
																	
				}
			}
		}
		return broadcastTree;		
	}
	
	private sdnNode[] computeNearestLink() {
		
		return null;
	}
	
	private Integer[][] createConnectivityMatrix(Hashtable<String, sdnNode> graph, Hashtable<String, Integer> nodeIndexMap){
		Integer linkCostMatrix[][] = new Integer[graph.size()][graph.size()];
		ArrayList<sdnNode> nodesArray = new ArrayList<sdnNode>(graph.values());
		sortNodesArray(nodesArray);
		
		for(int i = 0; i < graph.size(); i++){
			sdnNode row = nodesArray.get(i);
			nodeIndexMap.put(row.getAddress(), i);
			for(int j = 0; j < row.getNeighboursSize(); j++){
				sdnNode neighbour = graph.get(row.getNeighbor(j).getAddress());
				int neighboursIndex = nodesArray.indexOf(neighbour);
				linkCostMatrix[i][neighboursIndex] = row.getNeighbor(j).getEtx();
				
			}
		}				
		
		return linkCostMatrix;
	}
	/*-------------------------------BIP algorithm------------------------------------------*/
	
	
	public void sortNodesArray(ArrayList<sdnNode> nodesArray) {	
		// Sorting
		Collections.sort(nodesArray, new Comparator<sdnNode>() {
		        @Override
		        public int compare(sdnNode node1, sdnNode node2)
		        {
		        	Integer value1 = new Integer(fromByteArrayToInt(hexStringToByteArray(node1.getAddress())));
		        	Integer value2 = new Integer(fromByteArrayToInt(hexStringToByteArray(node2.getAddress())));
		            return value1.compareTo(value2);
		        }
		    });
		
	}

		
	
	//-------------------UTILITY FUNCTIONS--------------------------------------
	
	public static void printMatrix(Integer[][] matrix){
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[i].length; j++){
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println("");
		}
	}
	
	public static String[] convertIPv6ToIID(String[] multicastNodeIPv6Addresses) {
		String[] result = new String[multicastNodeIPv6Addresses.length];
		for(int i = 0; i < multicastNodeIPv6Addresses.length; i++){
			InetAddress IPaddress;
			try {
				IPaddress = InetAddress.getByName(multicastNodeIPv6Addresses[i]);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			result[i] = bytesToHex(iidFromIPv6Addres(IPaddress.getAddress()));
		}
		return result;
	}
	
	private int fromByteArrayToInt(byte[] array){
		ByteBuffer wrapped = ByteBuffer.wrap(array); // big-endian by default
		int Value = wrapped.getInt();
		return Value;
	}

	private static byte[] iidFromIPv6Addres(byte[] applicationAddress) {
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
	
	private static String[] generateIPv6Addresses(int num){
		StringBuilder builder = null;
		String[] NodeIPv6Addresses = new String[num]; 
		for(int i = 0; i < NodeIPv6Addresses.length; i++){
			builder = new StringBuilder();
			builder.append("fd00::20");
			builder.append(String.format("%x:", i+1));
			builder.append(String.format("%x:", i+1));
			builder.append(String.format("%x:", i+1));
			builder.append(String.format("%x", i+1));
			NodeIPv6Addresses[i] = builder.toString();
		}
		return NodeIPv6Addresses;
	}
	
	
	
	public static void main(String[] args){
		Hashtable<String, sdnNode> nodesTable = new Hashtable<>();
		String[] NodeIPv6Addresses = null;
		String[] multicastNodeIPv6Addresses = {"fd00::205:5:5:5","fd00::207:7:7:7","fd00::209:9:9:9"};
		String[] macAddressDerived = null;
		sdnNode[] nodesArray = null;
		int num = 11;
		int from = 5;
		NodeIPv6Addresses = generateIPv6Addresses(num);
		System.out.println("Nodes:");
		for(String str : NodeIPv6Addresses){
			System.out.println(str);
		}
		//multicastNodeIPv6Addresses = Arrays.copyOfRange(NodeIPv6Addresses, 5, num);
		System.out.println("Multicast nodes:");
		for(String str : multicastNodeIPv6Addresses){
			System.out.println(str);
		}
		macAddressDerived = MulticastTreeGenerator.convertIPv6ToIID(NodeIPv6Addresses);
		nodesArray = new sdnNode[num];
		int i = 0;
		for(String mac : macAddressDerived){
			sdnNode node = new sdnNode(mac);
			nodesTable.put(mac, node);
			nodesArray[i] = node;
			i++;
		}
		//Node 1
		nodesArray[0].addNeighbour(nodesArray[1], 10, 10);
		nodesArray[0].addNeighbour(nodesArray[2], 10, 10);
		//Node 2
		nodesArray[1].addNeighbour(nodesArray[0], 10, 10);
		nodesArray[1].addNeighbour(nodesArray[3], 10, 10);
		nodesArray[1].addNeighbour(nodesArray[4], 10, 10);
		//Node 3
		nodesArray[2].addNeighbour(nodesArray[0], 10, 10);
		nodesArray[2].addNeighbour(nodesArray[3], 10, 10);
		nodesArray[2].addNeighbour(nodesArray[5], 10, 10);
		nodesArray[2].addNeighbour(nodesArray[8], 10, 10);
		nodesArray[2].addNeighbour(nodesArray[9], 10, 10);
		//Node 4
		nodesArray[3].addNeighbour(nodesArray[1], 10, 10);
		nodesArray[3].addNeighbour(nodesArray[2], 10, 10);
		nodesArray[3].addNeighbour(nodesArray[4], 10, 10);
		nodesArray[3].addNeighbour(nodesArray[5], 10, 10);
		//Node 5
		nodesArray[4].addNeighbour(nodesArray[1], 10, 10);
		nodesArray[4].addNeighbour(nodesArray[3], 10, 10);
		nodesArray[4].addNeighbour(nodesArray[6], 10, 10);
		//Node 6
		nodesArray[5].addNeighbour(nodesArray[2], 10, 10);
		nodesArray[5].addNeighbour(nodesArray[3], 10, 10);
		nodesArray[5].addNeighbour(nodesArray[6], 10, 10);
		nodesArray[5].addNeighbour(nodesArray[9], 10, 10);
		//Node 7
		nodesArray[6].addNeighbour(nodesArray[4], 10, 10);
		nodesArray[6].addNeighbour(nodesArray[5], 10, 10);
		nodesArray[6].addNeighbour(nodesArray[7], 10, 10);
		//Node 8
		nodesArray[7].addNeighbour(nodesArray[6], 10, 10);
		nodesArray[7].addNeighbour(nodesArray[10], 10, 10);
		//Node 9
		nodesArray[8].addNeighbour(nodesArray[2], 10, 10);
		nodesArray[8].addNeighbour(nodesArray[9], 10, 10);
		//Node 10
		nodesArray[9].addNeighbour(nodesArray[8], 10, 10);
		nodesArray[9].addNeighbour(nodesArray[2], 10, 10);
		nodesArray[9].addNeighbour(nodesArray[5], 10, 10);
		//Node 11
		nodesArray[10].addNeighbour(nodesArray[7], 10, 10);
		nodesArray[10].addNeighbour(nodesArray[9], 10, 10);
		
		MulticastTreeGenerator test = new MulticastTreeGenerator(nodesTable.get(macAddressDerived[0]), nodesTable, multicastNodeIPv6Addresses);
		//test.computeMulticastTree();
		test.computeSelectiveFloodingTree();
	}
}
