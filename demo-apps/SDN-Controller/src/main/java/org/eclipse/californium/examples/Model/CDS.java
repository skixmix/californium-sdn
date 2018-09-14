package org.eclipse.californium.examples.Model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.californium.examples.NetworkResource;
import org.graphstream.algorithm.APSP;
import org.graphstream.algorithm.APSP.APSPInfo;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSourceDGS;

// Tavoletta Simone
// Class that defines the algorithms needed for generating a (1 or 2-fold) Connected Dominating Set, for the slicing service

public class CDS {
	
		
	/* Algorithm A
	 * Input: G(V, E, w), P
	 * Output: D
	 * 
	 * This algorithm takes as input the netowrk graph G and the set of preferred nodes P (the slice)
	 * and outputs a set D of paths that connect each node in P
	 */
	public static HashSet<D_Path> Alrogithm_A(Graph G, LinkedList<String> P){
		HashSet<D_Path> D = new HashSet<>(); 													//D <- {}	
		APSP apsp = new APSP(); //All pair shortest path algorithm
	    apsp.init(G);
	    apsp.setDirected(false);
	    apsp.setWeightAttributeName("length"); //Weight of the edge is called "length" here
	    apsp.compute();  //Compute shortest paths based on weight (in this case, ETX)
	    
		while (!P.isEmpty()){ 																    //While P is not empty
			String v = P.pop();																    //Take v, P <- P \ v
			Iterator<String> it = P.iterator(); 
			while (it.hasNext()){ 																//For each u belonging to P
				String u = it.next();
		    
			    APSPInfo info = G.getNode(v).getAttribute(APSPInfo.ATTRIBUTE_NAME);
				Path vu = info.getShortestPathTo(u); 											//Compute the path(v, u)
				if(vu != null && vu.getNodeCount() > 1){ 										//If the path exists
					D_Path pathvu =  new D_Path(vu, v, u);
					D.add(pathvu);															    //D <- D U path(v,u)
					//System.out.println("Path from " + v + " to " + u + " added to D");
					
				
					//Put also path(u, v), for taking into account the asymmetry of wireless links
					//This may cause a nullpointer exception in some cases, so it will be needed to wait for more topology updates
					info = G.getNode(u).getAttribute(APSPInfo.ATTRIBUTE_NAME);
					Path uv = info.getShortestPathTo(v); 
					D_Path pathuv =  new D_Path(uv, u, v);
					D.add(pathuv);
					//System.out.println("Path from " + u + " to " + v + " added to D");
				}
			}
		}
		
		return D;
	}
	
	
	/* Algorithm B
	 * Input: G(V, E, w), P
	 * Output: T
	 * 
	 * This algorithm takes as input the netowrk graph G and the set of preferred nodes P (the slice)
	 * and outputs a Minimum Steiner Tree T
	 */	
	public static SteinerTree Algorithm_B(Graph G, LinkedList<String> P){
		LinkedList<String> auxP = (LinkedList<String>) P.clone();
		HashSet<D_Path> D = Alrogithm_A(G, (LinkedList<String>)P.clone());				   //D <- Algorithm_A(G, P)			

		String Sink = P.pop();				   
		SteinerTree T = new SteinerTree(Sink);									   		   //Set Sink as root of T (Sink is the first node in the list)
		
		System.out.println("ALG B -> Graph size: " + G.getNodeCount() + ", " + G.getEdgeCount());
		
		while(!P.isEmpty()){															   //While P is not empty
						
			//Select v ∈ T \ N and u ∈ P \ T such that the cost of path(v, u) is minimum
			//System.out.println("Searching path in D with minimum cost");
			D_Path selectedPath = null;
			double min_cost = -1;														   // minimum cost
			Iterator<String> it = T.getNodes().iterator();
			while(it.hasNext()){
				String v = it.next();
				//System.out.println("Selected (in T) node " + v);
				if(!auxP.contains(v)) 													   // v ∈ T \ N (if v is in T, but not in P, skip)
					continue;
				
				//System.out.println("It is a preferred node, checking for paths");
				Iterator<D_Path> it2 = D.iterator();									   																	   
				while (it2.hasNext()){
					D_Path path = it2.next();
					String u = "";
					if(path.getStartingNode().compareTo(v) == 0){
						u = path.getEndingNode();
					}
					else
						continue;
					
					//System.out.println("From node " + Integer.parseInt(v.substring(Math.max(v.length() - 2, 0)), 16) + " to " + Integer.parseInt(u.substring(Math.max(u.length() - 2, 0)), 16) + "(Cost: " + path.getCost() + ")");
					//System.out.println("(Minimum cost up to now: " + min_cost);
					
					if(auxP.contains(u) && !T.getNodes().contains(u)){						   // u ∈ P \ T
						//System.out.println("u is in P, but not in T");
						if(min_cost <= -1.0){
							//System.out.println("Updating minimum cost");
							min_cost = path.getCost();
							selectedPath = path;
						}
						else{
							if(min_cost > path.getCost()){
								//System.out.println("Updating minimum cost");
								min_cost = path.getCost();
								selectedPath = path;
							}
						}
					}
				}
				
			}
			if(selectedPath == null){
				System.out.println("No path found!");
				return null;
			}
			//End selecting v and u

			String v = selectedPath.getStartingNode();
			String u = selectedPath.getEndingNode();
			P.remove(u);	
			//System.out.println("Searching for " + v + " in the tree");
			TreeNode vtree = T.getTreeNode(v);
			
			//For each n belonging to path(v,u)
			Iterator<Node> itpath = selectedPath.getPath().getNodePath().iterator();
			while(itpath.hasNext()){													  
				String n = itpath.next().getId();
				if(n.compareTo(v) != 0 && !T.getNodes().contains(n)){					  //If n != v && n ∉ T 
					TreeNode t = new TreeNode(n);
					t.setParent(vtree.getNodeID());
					vtree.addChildren(t);												  //Set n as children of v in T
					T.addNode(n);
					T.addTreeNode(t);
					
					//If n belongs to P, remove it from the list
					P.remove(n);														  //P <- P \ n
					
				}
				vtree = T.getTreeNode(n);												  //v <- n
			}
			
		}
		
		return T;
	}
	
	
	
	/* Algorithm C
	 * Input: G(V, E, w), P
	 * Output: 2CDS
	 * 
	 * This algorithm takes as input the netowrk graph G and the set of preferred nodes P (the slice)
	 * and outputs a 1-connected, 2-fold CDS
	 */		
	public static LinkedList<TreeNode> Algorithm_C(Graph G, LinkedList<String> P){
		SteinerTree CDS = Algorithm_B(G, (LinkedList<String>) P.clone());				//2CDS <- Algorithm_B(G, P)
		
		System.out.println("ALG C -> Graph size: " + G.getNodeCount() + ", " + G.getEdgeCount());
		
		if(CDS == null){
			System.out.println("NO CDS FOUND!");
			return null;
		}
		
		LinkedList<String> C = new LinkedList<>();
		C.add(P.getFirst());															//C <- {Sink}
		
		int i = 0;
		while(C.size() < CDS.getNodes().size()){										//While (|C| < |2CDS|)
			String v = C.get(i);														//Take the next v in C
			i++;
			TreeNode vnode = CDS.getTreeNode(v);
			Iterator<TreeNode> it = vnode.getChildrens().iterator();
			while(it.hasNext()){													   //For each children u of v in 2CDS
				TreeNode children = it.next();
				String u = children.getNodeID();
				C.add(u);										  					   //C <- C U u
				Edge vu = G.getEdge(v + "-" + u);
				Edge uv = G.getEdge(u + "-" + v);
				
				if(vu != null)														   //E' <- E' \ {(v,u)}
					G.removeEdge(vu);
				if(uv != null)
					G.removeEdge(uv);
				
				//System.out.println("Trying to cut out the edge " + vu.getId());
				if(Alrogithm_A(G, (LinkedList<String>) P.clone()).size() < 2 * binomial(P.size(), 2)){              //If |Algorithm_A(G,P)| < binomial coefficient (|P|, 2)
					//System.out.println("No more connected, reverting");
					if(vu != null){
						G.addEdge(vu.getId(), vu.getSourceNode(), vu.getTargetNode());
						G.getEdge(vu.getId()).setAttribute("length", vu.getAttribute("length"));
					}
					if(uv != null){
						G.addEdge(uv.getId(), uv.getSourceNode(), uv.getTargetNode());
						G.getEdge(uv.getId()).setAttribute("length", uv.getAttribute("length"));
					}
				}
				
			}
		}
		
		SteinerTree CDS2 = Algorithm_B(G, P);
		
		LinkedList<TreeNode> result = CDS.getTreeNodes();
		
		if(CDS2 != null)
			result.addAll(CDS2.getTreeNodes());
		
		return result;
	}
	
	
	//Auxiliary function for computing the binomial coefficient
	private static long binomial(int n, int k)
    {
        if (k>n-k)
            k=n-k;

        long b=1;
        for (int i=1, m=n; i<=k; i++, m--)
            b=b*m/i;
        return b;
    }
	
	// ------------------------------ Debugging -------------------------------------- 
	public static void main(String[] args) throws IOException{
		 String my_graph = 
		 		"DGS004\n" 
		 		+ "my 0 0\n" 
		 		+ "an 0000000000000000 \n" //Sink
		 		+ "an 0001000100010001 \n" 
		 		+ "an 0002000200020002 \n"
		 		+ "an 0003000300030003 \n"
		 		+ "an 0004000400040004 \n"
		 		+ "an 0005000500050005 \n"
		 		+ "an 0006000600060006 \n"
		 		+ "an 0007000700070007 \n"
		 		+ "an 0008000800080008 \n"
		 		+ "an 0009000900090009 \n"
		 		+ "an 000a000a000a000a \n"
		 		+ "an 000b000b000b000b \n"
		 		+ "an 000c000c000c000c \n"
		 		+ "an 000d000d000d000d \n"	
		 		+ "an 000e000e000e000e \n"
		 		+ "an 000f000f000f000f \n"
		 		+ "an 0010001000100010 \n"
		 		+ "an 0011001100110011 \n"
		 		+ "an 0012001200120012 \n"
		 		+ "an 0013001300130013 \n"
		 		+ "an 0014001400140014 \n"
		 		
		 		+ "ae 0000000000000000-0002000200020002 0000000000000000 0002000200020002 length:5 \n"
		 		+ "ae 0000000000000000-0001000100010001 0000000000000000 0001000100010001 length:3 \n"
		 		+ "ae 0000000000000000-0003000300030003 0000000000000000 0003000300030003 length:3 \n"
		 		+ "ae 0003000300030003-0007000700070007 0003000300030003 0007000700070007 length:6 \n"
		 		+ "ae 0003000300030003-0006000600060006 0003000300030003 0006000600060006 length:5 \n"
		 		+ "ae 0003000300030003-0004000400040004 0003000300030003 0004000400040004 length:4 \n"
		 		+ "ae 0004000400040004-0005000500050005 0004000400040004 0005000500050005 length:3 \n"
		 		+ "ae 0006000600060006-0008000800080008 0006000600060006 0008000800080008 length:6 \n"
		 		+ "ae 0008000800080008-0007000700070007 0008000800080008 0007000700070007 length:5 \n"
		 		+ "ae 0007000700070007-0006000600060006 0007000700070007 0006000600060006 length:3 \n"
		 		+ "ae 0001000100010001-0009000900090009 0001000100010001 0009000900090009 length:7 \n"
		 		+ "ae 0001000100010001-0002000200020002 0001000100010001 0002000200020002 length:4 \n"
		 		+ "ae 0002000200020002-000a000a000a000a 0002000200020002 000a000a000a000a length:3 \n"
		 		+ "ae 0009000900090009-000a000a000a000a 0009000900090009 000a000a000a000a length:5 \n"
		 		+ "ae 000b000b000b000b-000a000a000a000a 000b000b000b000b 000a000a000a000a length:3 \n"
		 		+ "ae 000c000c000c000c-000a000a000a000a 000c000c000c000c 000a000a000a000a length:5 \n"
		 		+ "ae 000c000c000c000c-000b000b000b000b 000c000c000c000c 000b000b000b000b length:4 \n"
		 		+ "ae 000c000c000c000c-000d000d000d000d 000c000c000c000c 000d000d000d000d length:7 \n"
		 		
				+ "ae 0014001400140014-000d000d000d000d 0014001400140014 000d000d000d000d length:6 \n"
				+ "ae 000c000c000c000c-0012001200120012 000c000c000c000c 0012001200120012 length:5 \n"
				+ "ae 0013001300130013-0012001200120012 0013001300130013 0012001200120012 length:4 \n"
				+ "ae 0010001000100010-000d000d000d000d 0010001000100010 000d000d000d000d length:7 \n"
				+ "ae 0010001000100010-0012001200120012 0010001000100010 0012001200120012 length:8 \n"
				+ "ae 0011001100110011-0009000900090009 0011001100110011 0009000900090009 length:7 \n"
				+ "ae 000f000f000f000f-0002000200020002 000f000f000f000f 0002000200020002 length:4 \n"
				+ "ae 000c000c000c000c-000e000e000e000e 000c000c000c000c 000e000e000e000e length:7 \n"
		 		;
		Graph graph = new DefaultGraph("APSP Test");
 		ByteArrayInputStream bs = new ByteArrayInputStream(my_graph.getBytes());
 		
 		FileSourceDGS source = new FileSourceDGS();
 		source.addSink(graph);
 		source.readAll(bs);
 		
 		for (Node node : graph) {
 			node.addAttribute("ui.label", Integer.parseInt(node.getId().substring(Math.max(node.getId().length() - 2, 0)), 16) + "");
 		}
 		
 		/*
 		Iterator<Edge> it = graph.getEdgeIterator();
 		while(it.hasNext()){
 			Edge e = it.next();
 			e.addAttribute("ui.label", e.getAttribute("length"));
 		}
 		*/
 		
 		graph.addAttribute("ui.stylesheet", "node {text-size: 20px; fill-color: #36A6FE; size: 20px, 20px; text-background-color: white; text-background-mode: plain;} edge {fill-color: #FF8000; size: 4px; text-size: 16px;}");
 		//graph.display();
 		LinkedList<String> P = new LinkedList<>();
 		P.add("0000000000000000"); //Sink
 		P.add("000e000e000e000e"); //14
 		P.add("0010001000100010"); //16
 		P.add("0005000500050005"); //5
 		P.add("0008000800080008"); //8
 		
 		 /*
 		HashSet<D_Path> D = Alrogithm_A(graph, (LinkedList<String>)P.clone());
 		System.out.println("--------------------- SET D obtained From Algorithm A ---------------------\n");
 		Iterator<D_Path> it2 = D.iterator();
 		while(it2.hasNext()){
 			D_Path p = it2.next();
 			System.out.print("Path from " + Integer.parseInt(p.getStartingNode().substring(Math.max(p.getStartingNode().length() - 2, 0)), 16) + " to " + Integer.parseInt(p.getEndingNode().substring(Math.max(p.getEndingNode().length() - 2, 0)), 16));
 			System.out.print(" is: ");
 			Iterator<Node> itnode = p.getPath().getNodeIterator();
 			while(itnode.hasNext()){
 				Node n = itnode.next();
 				System.out.print(Integer.parseInt(n.getId().substring(Math.max(n.getId().length() - 2, 0)), 16) + " -> ");
 			}
 			System.out.print("Cost: "+ p.getCost() + "\n----------\n");
 		}
 		
 		System.out.println("--------------------- Steiner Tree obtained From Algorithm B ---------------------\n");
 		SteinerTree T = Algorithm_B(graph, (LinkedList<String>)P.clone());
 		Iterator<String> it3 = T.getNodes().iterator();
 		while(it3.hasNext()){
 			String n = it3.next();
 			System.out.print(n);
 			if(!T.getTreeNode(n).getChildrens().isEmpty()){
 				System.out.print(" (with childrens: ");
 				Iterator<TreeNode> chi = T.getTreeNode(n).getChildrens().iterator();
 				while(chi.hasNext()){
 					TreeNode children = chi.next();
 					System.out.print(children.getNodeID());
 					if(chi.hasNext())
 						System.out.print(", ");
 				}
 				System.out.print(")");
 			}
 			System.out.print("\n");
 		}
 		
 		System.out.println("\n--------------------- 2CDS obtained From Algorithm C ---------------------\n");
 		LinkedList<TreeNode> CDS2 = Algorithm_C(graph, P);
 		Iterator<TreeNode> it4 = CDS2.iterator();
 		while (it4.hasNext()){
 			TreeNode n = it4.next();
 			System.out.print(n.getNodeID());
 			if(n.getChildrens().size() > 0){
 				System.out.print(" (with childrens:");
 				Iterator<TreeNode> chi = n.getChildrens().iterator();
 				while(chi.hasNext()){
 					TreeNode children = chi.next();
 					System.out.print(children.getNodeID());
 					if(chi.hasNext())
 						System.out.print(", ");
 				}
 				System.out.print(")");
 			}
 			System.out.print("\n");
 		}// */
 		
 		/*For slicing test
 		SteinerTree T = Algorithm_B(graph, (LinkedList<String>)P.clone());
 		
 		Graph old_graph = graph;
 		Graph new_graph = new DefaultGraph("Slicing test");
 	    bs = new ByteArrayInputStream(my_graph.getBytes());
 		source = new FileSourceDGS();
 		source.addSink(new_graph);
 		source.readAll(bs);
 		for (Node node : new_graph) {
 			node.addAttribute("ui.label", Integer.parseInt(node.getId().substring(Math.max(node.getId().length() - 2, 0)), 16) + "");
 		}
 		new_graph.addAttribute("ui.stylesheet", "node {text-size: 20px; fill-color: #36A6FE; size: 20px, 20px; text-background-color: white; text-background-mode: plain;} edge {fill-color: #FF8000; size: 4px; text-size: 16px;}");
 		new_graph.removeEdge("000c000c000c000c-0012001200120012");
 		new_graph.removeEdge("0000000000000000-0002000200020002");
 		
 		LinkedList<TreeNode> cds = new LinkedList<>();
 		Iterator<TreeNode> itnodes = T.getTreeNodes().iterator();
 		cds.add(T.getRoot());
 		while(itnodes.hasNext()){
 			TreeNode t = itnodes.next();
 			cds.add(t);
 		}
 		LinkedList<TreeNode> newT = Maintenance.maintenance_algorithm(old_graph, new_graph, cds, (LinkedList<String>)P.clone());
 		
 		 /*
 		Iterator<String> it5 = T.getNodes().iterator();
 		while(it5.hasNext()){
 			String n = it5.next();
 			if(!P.contains(n))
 				new_graph.getNode(n).addAttribute("ui.style", "fill-color: red;");
 			else
 				new_graph.getNode(n).addAttribute("ui.style", "fill-color: #01A741;");

 			if(!T.getTreeNode(n).getChildrens().isEmpty()){
 				Iterator<TreeNode> childs = T.getTreeNode(n).getChildrens().iterator();
 				while(childs.hasNext()){
 					TreeNode c = childs.next();
 					Edge e1 = new_graph.getEdge(n + "-" + c.getNodeID());
 					Edge e2 = new_graph.getEdge(c.getNodeID() + "-" + n);
 					if(e1 != null)
 						e1.addAttribute("ui.style", "fill-color: #CA0202;");
 					else if (e2 != null)
 						e2.addAttribute("ui.style", "fill-color: #CA0202;");
 				}
 			}
 		}
 		// */
 		
 	     /*
 		Iterator<TreeNode> it6 = newT.iterator();
 		while(it6.hasNext()){
 			TreeNode t = it6.next();
 			String n = t.getNodeID();
 			if(!P.contains(n))
 				new_graph.getNode(n).addAttribute("ui.style", "fill-color: red;");
 			else
 				new_graph.getNode(n).addAttribute("ui.style", "fill-color: #01A741;"); 		
 			
 			if(!t.getChildrens().isEmpty()){
 				Iterator<TreeNode> childs = t.getChildrens().iterator();
 				while(childs.hasNext()){
 					TreeNode c = childs.next();
 					Edge e1 = new_graph.getEdge(n + "-" + c.getNodeID());
 					Edge e2 = new_graph.getEdge(c.getNodeID() + "-" + n);
 					if(e1 != null)
 						e1.addAttribute("ui.style", "fill-color: #CA0202;");
 					else if (e2 != null)
 						e2.addAttribute("ui.style", "fill-color: #CA0202;");
 				}
 			}			
 			
 		}
 		new_graph.getEdge("0002000200020002-000a000a000a000a").addAttribute("ui.style", "fill-color: #CA0202;");
 		new_graph.getEdge("000c000c000c000c-000e000e000e000e").addAttribute("ui.style", "fill-color: #CA0202;");
 		new_graph.display();
 		// */
 		
 		
	}
	// */
	
	
}

//Auxiliary Class for the set of paths D
class D_Path{
	private Path p;
	private String startNode, endNode;
	
	public D_Path(Path x, String s, String e){
		p = x;
		startNode = s;
		endNode = e;
	}
	
	public String getStartingNode(){
		return startNode;
	}
	
	public String getEndingNode(){
		return endNode;
	}
	
	public Path getPath(){
		return p;
	}
	
	public double getCost(){
		
		if(p == null || p.getEdgeCount() <= 0)
			return 50000;
		
		double cost = 0;
		Iterator<Edge> it = p.getEdgeIterator();
		while(it.hasNext()){
			Edge e = it.next();
			cost += Double.parseDouble(e.getAttribute("length").toString());
		}
		return cost;
	}
}