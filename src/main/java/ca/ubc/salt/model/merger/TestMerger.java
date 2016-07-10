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
import java.util.Iterator;
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

import ca.ubc.salt.model.composer.TestCaseComposer;
import ca.ubc.salt.model.state.ProductionCallingTestStatement;
import ca.ubc.salt.model.state.ReadVariableDetector;
import ca.ubc.salt.model.state.StateComparator;
import ca.ubc.salt.model.state.StateCompatibilityChecker;
import ca.ubc.salt.model.state.TestState;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class TestMerger
{
    public static void main(String[] args) throws SAXException, IOException, ClassNotFoundException
    {

	XStream xstream = new XStream(new StaxDriver());

	File file = new File("components.txt");
	List<Set<String>> connectedComponents = null;
	Map<String, List<String>> connectedComponentsMap = null;
	if (!file.exists())
	{
	    long setupCost = 10;
	    Map<String, List<String>> uniqueTestStatements = ProductionCallingTestStatement.getUniqueTestStatements();
	    connectedComponents = ProductionCallingTestStatement.getTestCasesThatShareTestStatement(2,
		    uniqueTestStatements);
	    // connectedComponents.remove(0);

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
	    ObjectInputStream in = new ObjectInputStream(new FileInputStream("unique.txt"));
	    connectedComponentsMap = (Map<String, List<String>>) in.readObject();
	    in.close();
	}

	Formatter fr = new Formatter("stat.csv");
	for (Set<String> connectedComponent : connectedComponents)
	{
	    fr.format("%d,%s\n", connectedComponent.size(), connectedComponent.toString());
	}
	fr.close();

	
	List<Pair<Set<String>, List<List<TestStatement>>>> mergedTestCases = new LinkedList<Pair<Set<String>, List<List<TestStatement>>>>();
	
	for (Set<String> connectedComponent : connectedComponents)
	{
	    if (connectedComponent.size() < 2 || connectedComponent.size() > 4)
		continue;

	    // System.out.println(connectedComponentsMap);

	    List<String> testCases = new LinkedList<String>();
	    testCases.addAll(connectedComponent);
	    Map<String, TestState> graph = createModelForTestCases(testCases);
	    TestState root = graph.get("init.init-.xml");
	    System.out.println(root.printDot(true));

	    TestStatement first = null;
	    List<List<TestStatement>> paths = new LinkedList<List<TestStatement>>();
	    
	    Pair<Set<String>, List<List<TestStatement>>> pair = new Pair<Set<String>, List<List<TestStatement>>>(connectedComponent, paths);
	    mergedTestCases.add(pair);
	    
	    do
	    {
		LinkedList<TestStatement> path = new LinkedList<TestStatement>();

		first = dijkstra(new TestStatement(root, root, "init.xml"), false,
			connectedComponentsMap);
		path.add(first);

		TestStatement frontier = first;
		markAsCovered(frontier, connectedComponentsMap);

		while (frontier != null)
		{
		    frontier = dijkstra(frontier, true, connectedComponentsMap);
		    if (frontier == null)
			break;
		    markAsCovered(frontier, connectedComponentsMap);
		    path.add(frontier);
		}
		
		paths.add(returnThePath(root, path));
		
	    } while (first != null);

	    
	    TestCaseComposer.composeTestCases(mergedTestCases);

//	    System.out.println(TestCaseComposer.composeTestCase(returnThePath(root, firstPath), connectedComponent,
//		    TestCaseComposer.generateTestCaseName(connectedComponent)));
	    // System.out.println(TestCaseComposer.composeTestCase(firstPath));

	}
    }

    public static LinkedList<TestStatement> returnThePath(TestState root, LinkedList<TestStatement> frontierPaths)
    {

	LinkedList<TestStatement> path = new LinkedList<TestStatement>();
	Iterator<TestStatement> it = frontierPaths.descendingIterator();
	TestStatement cur = it.next();
	if (cur != null)
	    path.addLast(cur);
	while (it.hasNext())
	{
	    TestStatement parent = it.next();

	    while (cur != parent && cur != null)
	    {
		cur = (TestStatement) cur.getStart().parent.get(parent.getEnd());
		if (cur != null)
		    path.addFirst(cur);
	    }
	    path.addFirst(parent);

	    cur = parent;
	}

	while (cur != null)
	{
	    cur = (TestStatement) cur.getStart().parent.get(root);
	    if (cur != null)
		path.addFirst(cur);
	}

	return path;
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

    public static TestStatement dijkstra(TestStatement root, boolean returnFirst,
	    Map<String, List<String>> connectedComponentsMap)
    {
	Set<TestStatement> visited = new HashSet<TestStatement>();
	root.curStart = root;
	root.distFrom.put(root, (long) 0);
	PriorityQueue<TestStatement> queue = new PriorityQueue<TestStatement>();

	queue.add(root);
	TestStatement first = null;

	while (queue.size() != 0)
	{
	    TestStatement parent = queue.poll();
	    if (visited.contains(parent))
		continue;

	    visited.add(parent);

	    for (Entry<String, TestStatement> edge : parent.getEnd().getChildren().entrySet())
	    {
		TestStatement stmt = edge.getValue();
		if (!visited.contains(stmt))
		{
		    relaxChild(root, queue, parent, stmt);
		    if (first == null && connectedComponentsMap.containsKey(stmt.getName()))
		    {
			if (returnFirst)
			    return stmt;
			first = stmt;
		    }

		}
	    }

	    for (TestStatement stmt : parent.getEnd().getCompatibleStatements())
	    {
		if (!visited.contains(stmt))
		{
		    if (first == null && connectedComponentsMap.containsKey(stmt.getName()))
		    {
			if (returnFirst)
			    return stmt;
			first = stmt;
		    }

		    relaxChild(root, queue, parent, stmt);
		}
	    }

	}

	return first;

    }

    private static void relaxChild(TestStatement root, PriorityQueue<TestStatement> queue, TestStatement parent,
	    TestStatement stmt)
    {
	long newD = parent.distFrom.get(root) + stmt.time;
	Long childDist = stmt.distFrom.get(root);
	if (childDist == null || newD < childDist)
	{
	    stmt.parent.put(root, parent);
	    stmt.distFrom.put(root, newD);
	    stmt.curStart = root;
	    queue.remove(stmt);
	    queue.add(stmt);
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

	Set<String> allStates = new HashSet<String>(FileUtils.getStatesForTestCase(testCases));
	for (String testCase : testCases)
	{
	    // state1 -> <a, b, c>
	    Map<String, Set<SimpleName>> readVars = ReadVariableDetector
		    .populateReadVarsForTestCaseOfFile(Utils.getTestCaseFile(testCase), testCase);

	    ReadVariableDetector.accumulateReadVars(readVars);

	    // state1 ->
	    // <object1(a), field 1, field 2, ... >
	    // <object2(a), field 1, field 2, ... >
	    // <object3(a), field 1, field 2, ... >
	    Map<String, Set<String>> readValues = ReadVariableDetector.getReadValues(readVars);
	    StateCompatibilityChecker.getCompatibleStates(compatibleStates, scc.varStateSet, readValues, allStates);

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

}
