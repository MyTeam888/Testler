package ca.ubc.salt.model.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class TestState
{
    HashMap<String, TestStatement> children;
    HashMap<String, TestStatement> parents;
    List<String> states;
    List<String> asserts;

    public TestState()
    {
	children = new HashMap<String, TestStatement>();
	parents = new HashMap<String, TestStatement>();
	states = new LinkedList<String>();
	asserts = new LinkedList<String>();
    }

    public String printDot(boolean printWithTestStatements)
    {
	StringBuilder sb = new StringBuilder();
	HashSet<TestState> visited = new HashSet<TestState>();
	sb.append("digraph{\n");
	DFSPrint(this, sb, visited, printWithTestStatements);
	sb.append("}");

	return sb.toString();
    }

    public void DFSPrint(TestState root, StringBuilder sb, HashSet<TestState> visited, boolean printWithTestStatements)
    {
	// System.out.println(root);
	visited.add(root);
	for (TestStatement child : root.getChildren().values())
	{
	    if (printWithTestStatements)
	    {
		sb.append(printEdge(child));
		sb.append(printCompatibilityEdges(child));
	    } else
		sb.append(printEdge(root, child.getEnd()));
	    if (!visited.contains(child.getEnd()))
		DFSPrint(child.getEnd(), sb, visited, printWithTestStatements);
	}
    }

    public String printCompatibilityEdges(TestStatement ts)
    {
	StringBuilder sb = new StringBuilder();
	for (TestState state : ts.getCompatibleStates())
	{
//	    if (state != ts.getStart())
		sb.append("\"" + ts.toString() + "\"" + " -> " + "\"" + state.toString() + "\"[color=red]" + "\n");
	}
	return sb.toString();
    }

    public String printEdge(TestState root, TestState child)
    {
	return "\"" + root.toString() + "\"" + " -> " + "\"" + child.toString() + "\"" + "\n";
    }

    public String printEdge(TestStatement child)
    {
	String str = "\"" + child.toString() + "\"[shape=box];\n";
	str += "\"" + child.getStart().toString() + "\"" + " -> " + "\"" + child.toString() + "\"" + "\n";
	str += "\"" + child.toString() + "\"" + " -> " + "\"" + child.getEnd().toString() + "\"" + "\n";
	return str;
    }

    public String toString()
    {
	return states.toString();
    }

    public HashMap<String, TestStatement> getChildren()
    {
	return children;
    }

    public void setChildren(HashMap<String, TestStatement> children)
    {
	this.children = children;
    }

    public HashMap<String, TestStatement> getParents()
    {
	return parents;
    }

    public void setParents(HashMap<String, TestStatement> parents)
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
