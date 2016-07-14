package ca.ubc.salt.model.merger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import Comparator.NaturalOrderComparator;
import ca.ubc.salt.model.composer.RunningState;
import ca.ubc.salt.model.composer.TestCaseComposer;
import ca.ubc.salt.model.state.ProductionCallingTestStatement;
import ca.ubc.salt.model.state.ReadVariableDetector;
import ca.ubc.salt.model.state.StateComparator;
import ca.ubc.salt.model.state.TestState;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Utils;

public class IncrementalTestMerger
{

    public static void main(String[] args) throws FileNotFoundException, ClassNotFoundException, IOException, CloneNotSupportedException
    {
	merge2();

    }

    public static Map<String, TestStatement> getAllTestStatements(ArrayList<String> allStmtStr, Map<String, TestState> graph)
    {
	Map<String, TestStatement> allStmts = new HashMap<String, TestStatement>();
	for (String stmt : allStmtStr)
	{
	    TestStatement ts = getTestStatementFromStr(allStmtStr, graph, stmt);
	    if (ts != null)
		allStmts.put(stmt, ts);
	}

	return allStmts;
    }

    private static TestStatement getTestStatementFromStr(ArrayList<String> allStmtStr, Map<String, TestState> graph,
	    String stmt)
    {
	String nextState = Utils.nextOrPrevState(stmt, allStmtStr, true);
	if (nextState.equals(""))
	nextState = "init.init-.xml";
	TestState end = graph.get(nextState);
	TestStatement ts = end.getParents().get(stmt);
	return ts;
    }

    private static void merge2() throws IOException, FileNotFoundException, ClassNotFoundException, CloneNotSupportedException
    {
	XStream xstream = new XStream(new StaxDriver());

	File file = new File("components.txt");
	List<Set<String>> connectedComponents = null;
	Map<String, List<String>> connectedComponentsMap = null;
	if (!file.exists())
	{
	    long setupCost = 10;
	    Map<String, List<String>> uniqueTestStatements = ProductionCallingTestStatement.getUniqueTestStatements();
	    connectedComponents = ProductionCallingTestStatement.getTestCasesThatShareTestStatement(1,
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
	    if (connectedComponent.size() < 2)
		continue;

	    System.out.println(connectedComponentsMap);

//	    connectedComponent = new HashSet<String>();
////	    connectedComponent.add("ComplexTest.testReciprocal");
//	    connectedComponent.add("ComplexTest.testTanh");
//	    connectedComponent.add("ComplexTest.testMultiply");
	    // connectedComponent.add("ComplexTest.testExp");
	    // connectedComponent.add("ComplexTest.testScalarAdd");

	    List<String> testCases = new LinkedList<String>();
	    testCases.addAll(connectedComponent);

	    Map<String, Set<String>> readValues = getAllReadValues(testCases);

	    testCases.add("init.init");
	    Map<String, TestState> graph = StateComparator.createGraph(testCases);

	    TestState root = graph.get("init.init-.xml");
	    System.out.println(root.printDot(true));

	    Pair<TestStatement, RunningState> first = null;
	    List<List<TestStatement>> paths = new LinkedList<List<TestStatement>>();

	    Pair<Set<String>, List<List<TestStatement>>> pair = new Pair<Set<String>, List<List<TestStatement>>>(
		    connectedComponent, paths);
	    mergedTestCases.add(pair);

	    ArrayList<String> allStates = FileUtils.getStatesForTestCase(testCases);
	    Collections.sort(allStates, new NaturalOrderComparator());
	    
	    Map<String, TestStatement> allTestStatements = getAllTestStatements(allStates, graph);
	    TestCaseComposer.populateStateField(allTestStatements.values());

	    do
	    {
		LinkedList<TestStatement> path = new LinkedList<TestStatement>();

		Pair<TestStatement, RunningState> frontier = new Pair<TestStatement, RunningState>(
			new TestStatement(root, root, "init.xml"), new RunningState());

		do
		{
		    frontier = dijkstra(frontier.getFirst(), graph, frontier.getSecond(), readValues,
			    connectedComponentsMap, allTestStatements);
		    if (frontier == null)
			break;
		    TestMerger.markAsCovered(frontier.getFirst(), connectedComponentsMap);
		    path.add(frontier.getFirst());
		} while (frontier != null);

		paths.add(TestMerger.returnThePath(root, path));

		TestCaseComposer.composeTestCase(TestMerger.returnThePath(root, path), connectedComponent,
			TestCaseComposer.generateTestCaseName(connectedComponent));

	    } while (first != null);

	    // System.out.println(TestCaseComposer.composeTestCase(firstPath));
	}

	// TestCaseComposer.composeTestCases(mergedTestCases);
    }

    public static Pair<TestStatement, RunningState> dijkstra(TestStatement root, Map<String, TestState> graph,
	    RunningState runningState, Map<String, Set<String>> readValues,
	    Map<String, List<String>> connectedComponentsMap, Map<String, TestStatement> testStatementMap) throws CloneNotSupportedException
    {

	Set<TestStatement> visited = new HashSet<TestStatement>();
	root.curStart = root;
	root.distFrom.put(root, (long) 0);
	PriorityQueue<Pair<TestStatement, RunningState>> queue = new PriorityQueue<Pair<TestStatement, RunningState>>();

	queue.add(new Pair<TestStatement, RunningState>(root, runningState));

	while (queue.size() != 0)
	{
	    Pair<TestStatement, RunningState> pair = queue.poll();
	    TestStatement parent = pair.getFirst();
	    runningState = pair.getSecond();
	    if (visited.contains(parent))
		continue;

	    visited.add(parent);

	    
	    if (connectedComponentsMap.containsKey(parent.getName()))
	    {
		return new Pair<TestStatement, RunningState>(parent, runningState.clone());
	    }

	    TestCaseComposer.updateRunningState(parent, runningState);
	    List<Pair<Integer, TestStatement>> comps = getAllCompatibleTestStatements(testStatementMap, readValues, runningState);

	    Collections.sort(comps, Collections.reverseOrder());
	    for (Pair<Integer, TestStatement> stmtPair : comps)
	    {
		TestStatement stmt = stmtPair.getSecond();
		if (!visited.contains(stmt))
		{
		    relaxChild(root, queue, parent, stmt, runningState, stmtPair.getFirst());
		}

	    }
	}

	return null;
    }

    private static void relaxChild(TestStatement root, PriorityQueue<Pair<TestStatement, RunningState>> queue,
	    TestStatement parent, TestStatement stmt, RunningState runningState, int bonus) throws CloneNotSupportedException
    {
	long newD = parent.distFrom.get(root) + stmt.time - bonus;
	Long childDist = stmt.distFrom.get(root);
	if (childDist == null || newD < childDist)
	{
	    stmt.parent.put(root, parent);
	    stmt.distFrom.put(root, newD);
	    stmt.curStart = root;
	    // queue.remove(stmt);
	    queue.add(new Pair<TestStatement, RunningState>(stmt, runningState.clone()));
	    // queue.add(child.clone());
	}
    }

    public static List<Pair<Integer, TestStatement>> getAllCompatibleTestStatements(Map<String, TestStatement> allTestStatements,
	    Map<String, Set<String>> readValues, RunningState runningState)
    {
	List<Pair<Integer, TestStatement>> comps = new LinkedList<Pair<Integer, TestStatement>>();
	for (TestStatement stmt : allTestStatements.values())
	{
	    Set<String> readVals = readValues.get(stmt.getName());
	    if (readVals == null)
		continue;
//	    if(stmt.equals("ComplexTest.testTanh-2.xml"))
//		System.out.println();
	    boolean isComp = true;
	    for (String readVal : readVals)
	    {
		if (runningState.getName(readVal) == null)
		{
		    isComp = false;
		    break;
		}

	    }

	    if (isComp)
		comps.add(new Pair<Integer, TestStatement>(readVals.size(), stmt));

	}

	return comps;
    }

    public static Map<String, Set<String>> getAllReadValues(List<String> testCases) throws IOException
    {
	Map<String, Set<String>> readValues = new HashMap<String, Set<String>>();

	for (String testCase : testCases)
	{
	    // state1 -> <a, b, c>
	    Map<String, Set<SimpleName>> readVars = ReadVariableDetector
		    .populateReadVarsForTestCaseOfFile(Utils.getTestCaseFile(testCase), testCase);

//	    ReadVariableDetector.accumulateReadVars(readVars);

	    // state1 ->
	    // <object1(a), field 1, field 2, ... >
	    // <object2(a), field 1, field 2, ... >
	    // <object3(a), field 1, field 2, ... >
	    ReadVariableDetector.getReadValues(readVars, readValues);

	}

	return readValues;
    }
}
