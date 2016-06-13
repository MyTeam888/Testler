package ca.ubc.salt.model.merger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import ca.ubc.salt.model.state.ProductionCallingTestStatement;
import ca.ubc.salt.model.state.ReadVariableDetector;
import ca.ubc.salt.model.state.StateComparator;
import ca.ubc.salt.model.state.StateCompatibilityChecker;
import ca.ubc.salt.model.state.TestState;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;

public class TestMerger
{
    static Map<String, String> classFileMapping;

    public static void main(String[] args) throws SAXException, IOException, ClassNotFoundException
    {

	classFileMapping = getClassFileMapping();
	XStream xstream = new XStream(new StaxDriver());

	File file = new File("components.txt");
	List<Set<String>> connectedComponents = null;
	Map<String, List<String>> connectedComponentsMap = null;
	if (!file.exists())
	{
	    long setupCost = 10;
	    Map<String, List<String>> uniqueTestStatements = ProductionCallingTestStatement.getUniqueTestStatements();
	    connectedComponents = ProductionCallingTestStatement.getTestCasesThatShareTestStatement(10, uniqueTestStatements);
	    connectedComponents.remove(0);
	    
	 
	    connectedComponentsMap = ProductionCallingTestStatement.convertTheSetToMap(uniqueTestStatements);

	    String components = xstream.toXML(connectedComponents);

	    FileWriter fw = new FileWriter("components.txt");
	    fw.write(components);
	    fw.close();
	    
	    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("unique.txt"));
	    out.writeObject(connectedComponentsMap);
	    out.close();
	    
	    
	} else
	{
	    connectedComponents = (List<Set<String>>) xstream.fromXML(new File("components.txt"));
	    ObjectInputStream in = new ObjectInputStream( new FileInputStream("unique.txt"));
	    connectedComponentsMap = (Map<String, List<String>>) in.readObject();
	    in.close();
	}

	
	Formatter fr = new Formatter("stat.csv");
	for (Set<String> connectedComponent : connectedComponents)
	{
	    fr.format("%d,%s\n", connectedComponent.size(), connectedComponent.toString());
	}
	fr.close();
	
	
	
	for (Set<String> connectedComponent : connectedComponents)
	{
	    if (connectedComponent.size() < 2)
		continue;
	    List testCases = new LinkedList<String>();
	    testCases.addAll(connectedComponent);
	    Map<String, TestState> graph = createModelForTestCases(testCases);
	    TestState root = graph.get("init.init.xml");
	    System.out.println(root.printDot(true));
	    TestStatement first = dijkstra(root, false, connectedComponentsMap);

	    LinkedList<TestState> firstPath = new LinkedList<TestState>();
	    firstPath.add(root);
	    firstPath.add(first.getEnd());
	    List<LinkedList<TestState>> paths = new LinkedList<LinkedList<TestState>>();
	    paths.add(firstPath);

	    TestStatement frontier = first;
	    while (frontier != null)
	    {
		frontier = dijkstra(frontier.getEnd(), true, connectedComponentsMap);
		if (frontier == null)
		    break;
		markAsCovered(frontier, connectedComponentsMap);
		firstPath.add(frontier.getEnd());
	    }

	    System.out.println(firstPath);

	}
    }

    public static Map<String, String> getClassFileMapping()
    {
	XStream xstream = new XStream(new StaxDriver());
	return (Map<String, String>) xstream.fromXML(new File(Settings.classFileMappingPath));
    }

    public static void markAsCovered(TestStatement stmt, Map<String, List<String>> connectedComponentsMap)
    {
	if (stmt == null)
	    return;
	List<String> equivalentStatements = connectedComponentsMap.get(stmt.getName());
	for (String st : equivalentStatements)
	{
	    connectedComponentsMap.remove(st);
	}
    }

    public static TestStatement dijkstra(TestState root, boolean returnFirst, Map<String, List<String>> connectedComponentsMap)
    {
	Set<TestState> visited = new HashSet<TestState>();
	root.curStart = root;
	root.distFrom.put(root, (long) 0);
	PriorityQueue<TestState> queue = new PriorityQueue<TestState>();

	queue.add(root);
	TestStatement first = null;

	while (queue.size() != 0)
	{
	    TestState parent = queue.poll();
	    if (visited.contains(parent))
		continue;

	    visited.add(parent);

	    for (Entry<String, TestStatement> edge : parent.getChildren().entrySet())
	    {
		TestStatement stmt = edge.getValue();
		TestState child = stmt.getEnd();
		if (!visited.contains(child))
		{
		    if (first == null
			    && connectedComponentsMap.containsKey(stmt.getName()))
		    {
			if (returnFirst)
			    return stmt;
			first = stmt;
		    }

		    relaxChild(root, queue, parent, stmt, child);
		}
	    }

	    for (TestStatement stmt : parent.getCompatibleStatements())
	    {
		TestState child = stmt.getEnd();
		if (!visited.contains(child))
		{
		    if (first == null
			    && connectedComponentsMap.containsKey(stmt.getName()))
		    {
			if (returnFirst)
			    return stmt;
			first = stmt;
		    }

		    relaxChild(root, queue, parent, stmt, child);
		}
	    }

	}

	return first;

    }

    private static void relaxChild(TestState root, PriorityQueue<TestState> queue, TestState parent, TestStatement stmt,
	    TestState child)
    {
	long newD = parent.distFrom.get(root) + stmt.time;
	Long childDist = child.distFrom.get(root);
	if (childDist == null || newD < childDist)
	{
	    child.distFrom.put(root, newD);
	    child.parent.put(root, stmt);
	    stmt.parent.put(root, parent);
	    child.curStart = root;
	    queue.remove(child);
	    queue.add(child);
	    // queue.add(child.clone());
	}
    }

    public static Map<String, TestState> createModelForTestCases(List<String> testCases) throws IOException
    {
	StateCompatibilityChecker scc = new StateCompatibilityChecker();
	// scc.processState("testSubtract-17.xml");
	scc.populateVarStateSet(testCases);
	// System.out.println(scc.varStateSet);

	Map<String, Set<String>> compatibleStates = new HashMap<String, Set<String>>();
	for (String testCase : testCases)
	{
	    // state1 -> <a, b, c>
	    Map<String, Set<SimpleName>> readVars = ReadVariableDetector
		    .populateReadVarsForTestCaseOfFile(getTestCaseFile(testCase), testCase);
	    // state1 ->
	    // <object1(a), field 1, field 2, ... >
	    // <object2(a), field 1, field 2, ... >
	    // <object3(a), field 1, field 2, ... >

	    Map<String, Set<List<String>>> readValues = ReadVariableDetector.getReadValues(readVars);
	    StateCompatibilityChecker.getCompatibleStates(compatibleStates, scc.varStateSet, readValues);

	}
	testCases.add("init.init");
	Map<String, TestState> graph = StateComparator.createGraph(testCases);

	StateCompatibilityChecker.setCompabilityFields(graph, compatibleStates);

	// TestState root = graph.get("init.xml");
	// System.out.println(root.printDot(true));

	return graph;
	// List<List<TestStatement>> paths = root.getAllPaths();
	// System.out.println(paths.size());
	// for (List<TestStatement> path : paths)
	// System.out.println(path);
    }

    public static String getTestCaseFile(String testCase)
    {
	int index = testCase.lastIndexOf('.');
	String className = testCase.substring(0, index);
	return classFileMapping.get(className);
    }

}
