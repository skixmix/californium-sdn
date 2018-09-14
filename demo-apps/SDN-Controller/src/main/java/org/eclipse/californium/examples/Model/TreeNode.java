package org.eclipse.californium.examples.Model;

import java.util.LinkedList;
import java.util.List;

//Tavoletta Simone
// Class defining the nodes of a tree
public class TreeNode {
  private String nodeID;
  private String parent;
  private List<TreeNode> childrens;
  
  public TreeNode(String n){
  	nodeID = n;
  	parent = null;
  	childrens = new LinkedList<>();
  }
  
  public void setParent(String parent){
	  this.parent = parent;
  }
  
  public String getParent(){
	  return parent;
  }
  
  public void addChildren(TreeNode n){
  	this.childrens.add(n);
  }

  public List<TreeNode> getChildrens(){
  	return childrens;
  }
  
  public void removeChildren(TreeNode n){
  	this.childrens.remove(n);
  }
  
  public String getNodeID(){
  	return nodeID;
  }
}

