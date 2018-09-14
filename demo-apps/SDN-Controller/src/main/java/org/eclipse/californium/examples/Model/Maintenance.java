package org.eclipse.californium.examples.Model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.graphstream.algorithm.APSP;
import org.graphstream.algorithm.APSP.APSPInfo;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.MultiGraph;

/* Tavoletta Simone
 * 
 * Class defining the Local and Global maintenance algorithms utilized for CDS repair.
 */
public class Maintenance {
	
	
	private static final int threshold = 2; //Threshold value for the maintenance algorithm
	
	/* Local repair algorithm
	 * Input: G(V, E, w), CDS, P
	 * Output: rCDS
	 * 
	 * Local repair procedure on the provided CDS, reconnecting (if possible) all nodes that lost their direct connections
	 */
	public static LinkedList<TreeNode> local_repair(Graph G, LinkedList<TreeNode> CDS, HashSet<D_Path> P){
		LinkedList<TreeNode> rCDS = new LinkedList<>();
		
		Iterator<D_Path> it = P.iterator();
		APSP apsp = new APSP(); //All pair shortest path algorithm
	    apsp.init(G);
	    apsp.setDirected(false);
	    apsp.setWeightAttributeName("length"); //Weight of the edge is called "length" here
	    apsp.compute();  //Compute shortest paths based on weight
	    
		while(it.hasNext()){
			D_Path pathToRepair = it.next(); 																	//Take a path(v, u)q to repair
			remove_nodes_from_CDS(CDS, pathToRepair.getPath()); 												//CDS <- CDS \ {n | n ∈ path(v, u)}
			APSPInfo info = G.getNode(pathToRepair.getStartingNode()).getAttribute(APSPInfo.ATTRIBUTE_NAME);
			Path vu = info.getShortestPathTo(pathToRepair.getEndingNode());
			if(vu == null){																						//If path does not exist
				System.out.println("No alternate path found in the local repair algorithm!");
				return null;
			}
			Iterator<Node> it2 = vu.getNodePath().iterator(); 													//rCDS <- rCDS ∪ {n | n ∈ path(v, u)}
			String v = pathToRepair.getStartingNode();
			String u = pathToRepair.getEndingNode();
			
			System.out.println("Local repair: " + Integer.parseInt(v.substring(Math.max(v.length() - 2, 0)), 16) + "-" + Integer.parseInt(u.substring(Math.max(u.length() - 2, 0)), 16));
			TreeNode t1 = new TreeNode(v);

			if(!cds_contains(rCDS, t1.getNodeID()))
				rCDS.add(t1);
			
			TreeNode vtree = getNode(rCDS, v);
			while(it2.hasNext()){
				Node n = it2.next();
				if(n.getId().compareTo(v) != 0 && !cds_contains(rCDS, n.getId())){	
					TreeNode tn = new TreeNode(n.getId());
					tn.setParent(vtree.getNodeID());
					vtree.addChildren(tn);
					rCDS.add(tn);
				}
				vtree = getNode(rCDS, n.getId());
			}						
		}
		rCDS.addAll(CDS);																						//rCDS <- rCDS ∪ CDS
		return rCDS;
	}
	
	
	/* Maintenance algorithm
	 * 
	 * Input: G(V, E, w), G(V', E', w'), CDS, P
	 * Output: rCDS
	 */
	public static LinkedList<TreeNode> maintenance_algorithm(Graph old_graph, Graph new_graph, LinkedList<TreeNode> cds, LinkedList<String> P){
		//Compute the set of missing nodes
		HashSet<String> missing_nodes = getMissingNodes(old_graph, new_graph);
		//Compute the set of missing links
		HashSet<Edge> changed_edges = getChangedEdges(old_graph, new_graph);
		
		if(missing_nodes.isEmpty() && changed_edges.isEmpty()){
			System.out.println(">> Maintenance algorithm: the new graph is identical to the old one, no maintenance required!");
			return null;
		}
		
		//Check if CDS needs repair
		System.out.println(">> Maintenance algorithm: some nodes are missing and/or some edges have changed, checking if it affects the CDS");
		//On missing nodes
		Iterator<String> it = missing_nodes.iterator();
		while(it.hasNext()){
			String n = it.next();
			if(!cds_contains(cds, n)){
				missing_nodes.remove(n);
			}
		}
		
		//Check also edges
		Iterator<Edge> it2 = changed_edges.iterator();
		while(it2.hasNext()){ //For each missing edge
			Edge e = it2.next();
			TreeNode u = getNode(cds, e.getNode0().getId());
			TreeNode v = getNode(cds, e.getNode1().getId());
			if(u != null && v != null){
				//The CDS contains both nodes in this edge, check whether they are in a direct parent-children relationship or not
				boolean directLink = false;
			    Iterator<TreeNode> childrens = u.getChildrens().iterator();
			    while(childrens.hasNext()){
			    	TreeNode c = childrens.next();
			    	if(c.getNodeID().compareTo(v.getNodeID()) == 0 && c.getParent().compareTo(u.getNodeID()) == 0){
			    		directLink = true;
			    		break;
			    	}
			    }
			    
			    if(!directLink){
				    childrens = v.getChildrens().iterator();
				    while(childrens.hasNext()){
				    	TreeNode c = childrens.next();
				    	if(c.getNodeID().compareTo(u.getNodeID()) == 0 && c.getParent().compareTo(v.getNodeID()) == 0){
				    		directLink = true;
				    		break;
				    	}
				    }	
				    
				    if(!directLink)
				    	changed_edges.remove(e);
			    }
			    
			}
		}
		
		if(changed_edges.isEmpty() && missing_nodes.isEmpty()){
			System.out.println(">> Maintenance algorithm: none of them affects the CDS, no repair needed");
			return null;
		}
		
		HashSet<D_Path> pathsToRepair = null;
		if(!missing_nodes.isEmpty()){
			System.out.println(">> Maintenance algorithm: some nodes in the CDS are missing! Computing paths to replace");
			pathsToRepair = computePathsToRepair(missing_nodes, cds, old_graph);
		}
		
		if(!changed_edges.isEmpty()){
			System.out.println(">> Maintenance algorithm: some edges have changed or are missing! Computing paths to replace");
			if(pathsToRepair == null)
				pathsToRepair = computePathsToRepairEdges(changed_edges, cds);
			else
				pathsToRepair.addAll(computePathsToRepairEdges(changed_edges, cds));
		}
		
		LinkedList<TreeNode> Global_rCDS = new LinkedList<TreeNode>();
		SteinerTree T = CDS.Algorithm_B(new_graph, P);
		Iterator<TreeNode> itcds = T.getTreeNodes().iterator();
		Global_rCDS.add(T.getRoot());
		while(itcds.hasNext()){
			TreeNode t = itcds.next();
			Global_rCDS.add(t);
		}
		LinkedList<TreeNode> Local_rCDS = local_repair(new_graph, (LinkedList<TreeNode>)cds.clone(), pathsToRepair);
		
		if(Local_rCDS == null || Local_rCDS.isEmpty()){
			System.out.println(">> Maintenance algorithm: no local repair possible, returning a completely new CDS");
			return Global_rCDS;
		}
		
		int n_global = nodesAffected(Global_rCDS, cds);
		int n_local = nodesAffected(Local_rCDS, cds);
		
		System.out.println(">> Maintenance algorithm: global repair affects " + n_global + " nodes, while local affects " + n_local + " nodes. Threshold value: " + threshold);
		
		if(n_global - threshold > n_local){
			System.out.println(">> Maintenance algorithm: LOCAL repair is better");
			return Local_rCDS;
		}
		else{
			System.out.println(">> Maintenance algorithm: GLOBAL repair is better");
			return Global_rCDS;
		}
	}
	
	
	// ================================================= Auxiliary functions ==================================================== //
	
	private static void remove_nodes_from_CDS(LinkedList<TreeNode> CDS, Path p){
		Iterator<Node> it = (Iterator<Node>) p.getEachNode().iterator();
		while(it.hasNext()){
			Node n = it.next();
			String nodeID = n.getId();
			Iterator<TreeNode> it2 = CDS.iterator();
			while(it2.hasNext()){
				TreeNode t = it2.next();
				if(t.getNodeID().compareTo(nodeID) == 0){
					CDS.remove(t);
					break;
				}
			}
		}
	}
	
	private static boolean cds_contains(LinkedList<TreeNode> CDS, String n){	
		Iterator<TreeNode> it = CDS.iterator();
		while(it.hasNext()){
			TreeNode t = it.next();
			if(t.getNodeID().compareTo(n) == 0){
				return true;
			}
		}
		return false;
	}
	
	private static TreeNode getNode(LinkedList<TreeNode> CDS, String n){
		Iterator<TreeNode> it = CDS.iterator();
		while(it.hasNext()){
			TreeNode t = it.next();
			if(t.getNodeID().compareTo(n) == 0){
				return t;
			}
		}
		return null;		
	}
	
	private static HashSet<String> getMissingNodes (Graph old, Graph newgraph){
		HashSet<String> missingNodes = new HashSet<>();
		
		Iterator<Node> it1 = old.getNodeIterator();
		//Missing nodes
		while(it1.hasNext()){ //For each node in the Old graph
			Node n = it1.next();
			Iterator<Node> newIt = newgraph.getNodeIterator();
			boolean isPresent = false;
			while(newIt.hasNext()){ //Check if it is in the New graph
				Node v = newIt.next();
				if(v.getId().compareTo(n.getId()) == 0){ //Node is also in the new graph
					 isPresent = true;
					 break;
				}
			}
			//If it is not, it's missing
			if(!isPresent){
				missingNodes.add(n.getId());
			}
		}
		return missingNodes;
	}
	
	private static HashSet<Edge> getChangedEdges(Graph old, Graph newgraph){
		HashSet<Edge> changedEdges = new HashSet<>();
		
		Iterator<Edge> it1 = old.getEdgeIterator();
		
		while(it1.hasNext()){ //For each edge in the Old graph
			Edge e = it1.next();
			Iterator<Edge> newIt = newgraph.getEdgeIterator();
			boolean isPresent = false;
			boolean changedCost = false;
			while(newIt.hasNext()){
				Edge e2 = newIt.next();
				if((e.getNode0().getId().compareTo(e2.getNode0().getId()) == 0 && e.getNode1().getId().compareTo(e2.getNode1().getId()) == 0) || 
						(e.getNode0().getId().compareTo(e2.getNode1().getId()) == 0 && e.getNode1().getId().compareTo(e2.getNode0().getId()) == 0)){
					isPresent = true;
					int cost1 = e.getAttribute("length");
					int cost2 = e2.getAttribute("length");
					if(cost1 != cost2){
						//System.out.println("Edge changed cost");
						changedCost = true;
					}
					
					break;
				}
			}
			
			if(!isPresent || changedCost){
				changedEdges.add(e);
			}
		}
		
		return changedEdges;
	}
	
	
	private static HashSet<D_Path> computePathsToRepair(HashSet<String> missingNodes, LinkedList<TreeNode> CDS, Graph old_graph){
		HashSet<D_Path> toRepair = new HashSet<>();
		
		//For each node missing, check what are its children and parents
		Iterator<String> it = missingNodes.iterator();
		while(it.hasNext()){
			String n = it.next();
			System.out.println("Node " + Integer.parseInt(n.substring(Math.max(n.length() - 2, 0)), 16) + " is missing");
			TreeNode t = getNode(CDS, n);
			if(t != null){ //Need to reconnect the parent of n to the children of n
				String itsParent = t.getParent();
				Iterator<TreeNode> it2 = t.getChildrens().iterator();
				while(it2.hasNext()){
					TreeNode c = it2.next();
					if(c.getParent().compareTo(n) == 0){
						Path p = new Path();
						p.setRoot(old_graph.getNode(itsParent));
						Edge e1 = old_graph.getEdge(itsParent +"-" + n);
						Edge e2 = old_graph.getEdge(n +"-" + itsParent);
						
						if(e1 != null || e2 != null){
							if(e1 != null)
								p.add(e1);
							else
								p.add(e2);
							
							e1 = old_graph.getEdge(n +"-" + c.getNodeID());
							e2 = old_graph.getEdge(c.getNodeID() +"-" + n);
							if(e1 != null || e2 != null){
								if(e1 != null)
									p.add(e1);
								else
									p.add(e2);
								
								toRepair.add(new D_Path(p, itsParent, c.getNodeID()));
							}
						}
					}
				}
			}
		}//End while
		
		return toRepair;
	}
	
	
	private static HashSet<D_Path> computePathsToRepairEdges(HashSet<Edge> edges, LinkedList<TreeNode> CDS){
		HashSet<D_Path> paths = new HashSet<>();
		HashSet<String> links_already_inserted = new HashSet<>();
		
		//For each edge that changed, need to repair/maintain its path
		Iterator<Edge> it = edges.iterator();
		while(it.hasNext()){
			Edge e = it.next();
			TreeNode u = getNode(CDS, e.getNode1().getId());
			TreeNode v = getNode(CDS, e.getNode0().getId());
			System.out.println("Edge " + Integer.parseInt(e.getNode0().getId().substring(Math.max(e.getNode0().getId().length() - 2, 0)), 16) + "-" + Integer.parseInt(e.getNode1().getId().substring(Math.max(e.getNode1().getId().length() - 2, 0)), 16) + " has changed / is missing");
			if(u != null && v != null){
				if(!links_already_inserted.contains(u.getNodeID() + "-" + v.getNodeID())){		
					//If there is a parent-children relationship
					if((v.getParent() != null && u.getNodeID().compareTo(v.getParent()) == 0) || (u.getParent() != null && v.getNodeID().compareTo(u.getParent()) == 0)){ 
						Path p = new Path();
						p.add(e.getNode0(), e);
						paths.add(new D_Path(p, e.getNode0().getId(), e.getNode1().getId()));
						links_already_inserted.add(u.getNodeID() + "-" + v.getNodeID());
						links_already_inserted.add(v.getNodeID() + "-" + u.getNodeID());
					}
				}
			}
		}//End while
		
		return paths;
	}
	
	
	private static int nodesAffected(LinkedList<TreeNode> newCDS, LinkedList<TreeNode> oldCDS){
		int affected = 0;
		
		Iterator<TreeNode> it = newCDS.iterator(); //For each node in the newCDS
		while(it.hasNext()){
			TreeNode t = it.next();
			TreeNode oldt = getNode(oldCDS, t.getNodeID()); //Check if it was already in the oldCDS
			if(oldt == null)
				affected++;
			else{
				//If it was, check if they have the same parent
				if(t.getParent() != null && t.getParent().compareTo(oldt.getParent()) != 0){ //If not, needs to be counted as update
					affected++;
					continue;
				}
				//Also, if there are different children, it is affected
				Iterator<TreeNode> childs = t.getChildrens().iterator();
				int presentNodes = 0;
				while(childs.hasNext()){
					TreeNode c = childs.next();
					Iterator<TreeNode> childs2 = oldt.getChildrens().iterator();
					while(childs2.hasNext()){
						TreeNode c2 = childs2.next();
						if(c.getNodeID().compareTo(c2.getNodeID()) == 0){
							presentNodes++;
							break;
						}
					}
				}
				
				if(presentNodes != oldt.getChildrens().size())
					affected++;
			}
		}
		
		return affected;
	}

}
