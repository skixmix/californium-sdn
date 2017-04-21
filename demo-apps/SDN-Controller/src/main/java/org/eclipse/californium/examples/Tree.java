package org.eclipse.californium.examples;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.californium.examples.Model.sdnNode;
import org.jfree.util.StringUtils;

public class Tree<T> {
	private TreeNode<T> root;
	private Hashtable<T, TreeNode<T>> nodesTable;
	private HashSet<T> leafSet;
	
	public Tree(T node){
		root = new TreeNode<T>(node);
		nodesTable = new Hashtable<>();
		nodesTable.put(node, root);
		leafSet = new HashSet<>();
		leafSet.add(node);
	}
		
	public boolean addNodeToParent(T parent, T node){
		TreeNode<T> treeParent = nodesTable.get(parent);
		if(treeParent == null)
			return false;
		TreeNode<T> treeNode = new TreeNode(node);
		treeParent.addChild(treeNode);
		treeNode.setParent(treeParent);
		nodesTable.put(node, treeNode);
		leafSet.remove(parent);
		leafSet.add(node);
		return true;
	}
	
	public Set<T> getNodesSet(){
		return nodesTable.keySet();
	}


	private void DepthFirstScan(TreeNode<T> node, int level) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < level; i++)
			builder.append("\t");
		String tabs = builder.toString();
		for(TreeNode<T> child : node.getChildren()){
			System.out.println( tabs + "-> " + child.toString());
			DepthFirstScan(child, level+1);
		}
	}
	
	public void printTree(){
		System.out.println("-> " + root.toString());
		DepthFirstScan(root, 1);
	}

	@SuppressWarnings("unchecked")
	public T[] getLeafNodesArray() {
		return (T[])leafSet.toArray();
	}

	public Tree<T> getBranch(sdnNode selectedNode) {
		Tree<T> branch = new Tree(root.node);
		TreeNode<T> node = nodesTable.get(selectedNode);
		TreeNode<T> scan = node;
		ArrayList<T> branchNodes = new ArrayList<>();
		while(scan != null){
			branchNodes.add(0, scan.node);			
			scan = scan.getParent();
		}
		
		T[] branchNodesArray = (T[]) branchNodes.toArray();
		for(int i = 0; i < branchNodes.size() - 1; i++){			
			branch.addNodeToParent(branchNodesArray[i], branchNodesArray[i+1]);
			
		}
		return branch;
	}

	public void mergeWith(Tree<T> branch) {		
		TreeNode<T> root = branch.getRoot();
		ArrayList<TreeNode<T>> branchNodes = root.getChildren();
		for(int i = 0; i < branchNodes.size(); i++){
			TreeNode<T> elem = branchNodes.get(i);
			if(!nodesTable.containsKey(elem.node)){
				System.out.println("ADDING: " + elem.toString() + " to " + elem.getParent().toString());
				this.addNodeToParent(elem.getParent().node, elem.node);
			}
			for(TreeNode<T> neighbors : elem.getChildren()){
				branchNodes.add(neighbors);
			}
		}
	}

	private TreeNode<T> getRoot() {
		return root;
	}

	public T getParent(T node) {
		TreeNode<T> parent = nodesTable.get(node).getParent();
		if(parent == null)
			return null;
		else
			return parent.node;
	}


	
	
}

class TreeNode<T>{
	private TreeNode<T> parent;
	private ArrayList<TreeNode<T>> children;
	T node;
	
	public TreeNode(T node) {
		super();
		this.node = node;
		this.parent = null;
		this.children = new ArrayList<>();
	}
	
	public void setParent(TreeNode<T> node){
		this.parent = node;
	}
	
	public void addChild(TreeNode<T> node){
		children.add(node);
	}
	
	public TreeNode<T> getParent(){
		return parent;
	}
	
	public ArrayList<TreeNode<T>> getChildren(){
		return children;
	}
	
	@Override
	public String toString() {
		return node.toString();
	}
}