package org.eclipse.californium.examples.Model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

//Tavoletta Simone
// Auxiliary class for defining the Steiner Tree data structure, used for the CDS
public class SteinerTree {
		private TreeNode root;
		private HashSet<String> nodes;
		private LinkedList<TreeNode> treeNodes;

	    public SteinerTree(String n) {
	        root = new TreeNode(n);
	        nodes = new HashSet<>();
	        nodes.add(n);
	        treeNodes = new LinkedList<>();
	        treeNodes.add(root);
	    }
	    
	    public TreeNode getRoot(){
	    	return root;
	    }
	    
	    public HashSet<String> getNodes(){
	    	return nodes;
	    }
	    
	    public LinkedList<TreeNode> getTreeNodes(){
	    	return treeNodes;
	    }
	    
	    public void addNode(String n){
	    	nodes.add(n);
	    }
	    
	    public void addTreeNode(TreeNode t){
	    	treeNodes.add(t);
	    }
	    
	    public TreeNode getTreeNode(String id){
	    	Iterator<TreeNode> it = treeNodes.iterator();
	    	while(it.hasNext()){
	    		TreeNode t = it.next();
	    		if(t.getNodeID().compareTo(id) == 0){
	    			return t;
	    		}
	    	}
	    	return null;
	    }
}
