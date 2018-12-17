package eu.wdaqua.qanary.component.geospatialsearch;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
	
	String nodeID;
	List<TreeNode> childs = new ArrayList<TreeNode>();
	List<String> annotations = new ArrayList<String>();
	String postag="";
	List<TreeNode> parents = new ArrayList<TreeNode>();
	
}
