package ca.ubc.salt.model.state;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class TestState
{
    HashSet<TestState> children;
    HashSet<TestState> parents;
    List<String> states;
    List<String> asserts;
    
    
    
    public TestState()
    {
	children = new HashSet<TestState>();
	parents = new HashSet<TestState>();
	states = new LinkedList<String>();
	asserts = new LinkedList<String>();
    }
    
    
    public String printDot()
    {
	StringBuilder sb = new StringBuilder();
	HashSet<TestState> visited = new HashSet<TestState>();
	sb.append("graph{\n");
	DFSPrint(this, sb, visited);
	sb.append("}");
	
	return sb.toString();
    }
    
    public void DFSPrint(TestState root, StringBuilder sb, HashSet<TestState> visited)
    {
//	System.out.println(root);
	visited.add(root);
	for (TestState child : root.getChildren())
	{
	    sb.append(printEdge(root, child));
	    if (!visited.contains(child))
		DFSPrint(child, sb, visited);
	}
    }
    
    
    public String printEdge(TestState root, TestState child)
    {
	return "\""+ root.toString() + "\""+ " -- " + "\""+ child.toString() + "\""+ "\n";
    }
    
    
    public String toString()
    {
	return states.toString();
    }
    
   
    public HashSet<TestState> getChildren()
    {
        return children;
    }


    public void setChildren(HashSet<TestState> children)
    {
        this.children = children;
    }


    public HashSet<TestState> getParents()
    {
        return parents;
    }


    public void setParents(HashSet<TestState> parents)
    {
        this.parents = parents;
    }


    public List<String> getStates()
    {
        return states;
    }
    public void setStates(List<String> states)
    {
        this.states = states;
    }
    public List<String> getAsserts()
    {
        return asserts;
    }
    public void setAsserts(List<String> asserts)
    {
        this.asserts = asserts;
    }
    
    
    
}
