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
import java.util.Map.Entry;
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
import ca.ubc.salt.model.state.VarDefinitionPreq;
import ca.ubc.salt.model.utils.Counter;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Triple;
import ca.ubc.salt.model.utils.Utils;

public class BackwardTestMerger
{

    public static void main(String[] args)
	    throws FileNotFoundException, ClassNotFoundException, IOException, CloneNotSupportedException
    {
	merge2();

    }

    public static Map<String, TestStatement> getAllTestStatements(ArrayList<String> allStmtStr,
	    Map<String, TestState> graph)
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

    public static void initSideEffectForStatements(Map<String, TestStatement> testStatements, List<String> testCases, Map<String, Set<VarDefinitionPreq>> defPreq)
    {
	for (Entry<String, TestStatement> entry : testStatements.entrySet())
	{
	    entry.getValue().initSideEffects(testCases, defPreq.get(entry.getKey()));
	}
    }

    private static void merge2()
	    throws IOException, FileNotFoundException, ClassNotFoundException, CloneNotSupportedException
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

	int totalBeforeMerging = 0, totalAftermerging = 0;
	int numberOfMergedTests = 0;
	int counter = 0;
	int limit = 2;

	for (Set<String> connectedComponent : connectedComponents)
	{
	    if (connectedComponent.size() < 2)
		continue;

	    // System.out.println(connectedComponentsMap);

	    Settings.consoleLogger.error(
		    String.format("merging %d tests : %s", connectedComponent.size(), connectedComponent.toString()));

	    counter++;
	    if (counter < limit)
		continue;

	    // connectedComponent = new HashSet<String>();
	    // connectedComponent.add("FastFourierTransformerTest.test2DData");
	    // connectedComponent.add("FastFourierTransformerTest.testSinFunction");
	    // connectedComponent.add("FastFourierTransformerTest.test2DDataUnitary");
	    // connectedComponent.add("FastFourierTransformerTest.testAdHocData");
	    // connectedComponent.add("Array2DRowRealMatrixTest.testGetColumn");
	    // connectedComponent.add("ComplexTest.testExp");
	    // connectedComponent.add("ComplexTest.testScalarAdd");

	    List<String> testCases = new LinkedList<String>();
	    testCases.addAll(connectedComponent);

	    Map<String, Set<VarDefinitionPreq>> definitionPreq = new HashMap<String, Set<VarDefinitionPreq>>();
	    Map<String, Map<String, String>> readValues = getAllReadValues(testCases, definitionPreq);
	    // System.out.println(readValues);

	    testCases.add("init.init");
	    Map<String, TestState> graph = StateComparator.createGraph(testCases);

	    TestState root = graph.get("init.init-.xml");
	    // System.out.println(root.printDot(true));

	    Pair<TestStatement, RunningState> first = null;
	    List<List<TestStatement>> paths = new LinkedList<List<TestStatement>>();

	    Pair<Set<String>, List<List<TestStatement>>> pair = new Pair<Set<String>, List<List<TestStatement>>>(
		    connectedComponent, paths);
	    mergedTestCases.add(pair);

	    ArrayList<String> allStates = FileUtils.getStatesForTestCase(testCases);

	    Collections.sort(allStates, new NaturalOrderComparator());

	    Map<String, TestStatement> allTestStatements = getAllTestStatements(allStates, graph);
	    TestCaseComposer.populateStateField(allTestStatements.values());

	    initSideEffectForStatements(allTestStatements, testCases, definitionPreq);

	    Set<String> assertions = getAllAssertions(allTestStatements);

	    Map<String, Set<String>> testClasses = Utils.getTestClassMapFromTestCases(connectedComponent);

	    String mainClassName = Utils.getTestClassWithMaxNumberOfTestCases(testClasses);

	    RunningState initialState = new RunningState(connectedComponent, mainClassName);
	    // do
	    {

		LinkedList<TestStatement> path = new LinkedList<TestStatement>();

		TestStatement rootStmt = new TestStatement(root, root, "init.xml");
		Triple<TestStatement, RunningState, Map<String, String>> frontier = new Triple<TestStatement, RunningState, Map<String, String>>(
			rootStmt, initialState, new HashMap<String, String>());
		Triple<TestStatement, RunningState, Map<String, String>> prevFrontier;
		do
		{
		    prevFrontier = frontier;
		    frontier = dijkstra(frontier.getFirst(), graph, frontier.getSecond(), readValues,
			    connectedComponentsMap, allTestStatements, assertions, definitionPreq);
		    if (frontier == null)
			break;
		    TestMerger.markAsCovered(frontier.getFirst(), connectedComponentsMap);
		    assertions.remove(frontier.getFirst().getName());
		    path.add(frontier.getFirst());
		} while (frontier != null);

		Settings.consoleLogger.error("first phase finished");
		List<TestStatement> secondPhasePath = new LinkedList<TestStatement>();
		if (!assertions.isEmpty())
		{
		    Map<String, Set<String>> assertionView = Planning.getTestCaseTestStatementStringMapping(assertions);
		    Map<String, Map<String, TestStatement>> allStmtsView = Planning
			    .getTestCaseTestStatementMapping(allTestStatements);
		    RunningState runningState = prevFrontier.getSecond();
		    for (Entry<String, Set<String>> testCaseEntry : assertionView.entrySet())
		    {
			String testCase = testCaseEntry.getKey();
			Set<String> assertionsToCover = testCaseEntry.getValue();
			for (String assertion : assertionsToCover)
			{
			    List<TestStatement> stmts = Planning.backward(allTestStatements.get(assertion),
				    runningState, readValues, connectedComponentsMap, allStmtsView.get(testCase),
				    definitionPreq);

			    // populateGoalsInStatements(definitionPreq,
			    // readValues, runningState, stmts);
			    //
			    // Map<String, String> batchRename = new
			    // HashMap<String, String>();
			    // for (TestStatement stmt : stmts)
			    // {
			    // TestCaseComposer.updateRunningState(stmt,
			    // runningState, readValues, definitionPreq,
			    // batchRename);
			    // }

			    if (stmts == null)
			    {
				Settings.consoleLogger.error(String.format("Couldn't satisfay %s - %s", allTestStatements.get(assertion), assertion));
				continue;
			    }
			    stmts = TestCaseComposer.performRenamingWithRunningState(stmts, connectedComponent,
				    mainClassName, readValues, definitionPreq, runningState);

			    secondPhasePath.addAll(stmts);

			}
		    }
		}
		Settings.consoleLogger.error("second phase finished");

		List<TestStatement> mergedPath = TestMerger.returnThePath(rootStmt, path);
		paths.add(mergedPath);
		paths.add(secondPhasePath);
		// mergedPath.addAll(secondPhasePath);

		ArrayList<TestStatement> arrMergedPath = new ArrayList<TestStatement>();
		arrMergedPath.addAll(mergedPath);
		TestCaseComposer.composeTestCase(arrMergedPath, connectedComponent,
			TestCaseComposer.generateTestCaseName(connectedComponent), readValues, definitionPreq,
			secondPhasePath);

	    }
	    // while (first != null);

	    int totalNumberOfStatements = allStates.size() - testCases.size() * 2;
	    int totalMerged = 0;
	    for (List<TestStatement> path : paths)
		totalMerged += path.size();
	    Settings.consoleLogger.error(String.format("Before Merging : %d, After Merging %d, saved : %d",
		    totalNumberOfStatements, totalMerged, totalNumberOfStatements - totalMerged));
	    // System.out.println(getSavedStmts(allTestStatements,
	    // paths.get(0)));
	    totalBeforeMerging += totalNumberOfStatements;
	    totalAftermerging += totalMerged;
	    numberOfMergedTests += connectedComponent.size();
	    Settings.consoleLogger.error(
		    String.format("Total Before merging : %d, After merging : %d, NumberOfTestsBefore : %d, After : %d",
			    totalBeforeMerging, totalAftermerging, numberOfMergedTests, counter));
	}

	// TestCaseComposer.composeTestCases(mergedTestCases);
    }

    public static void populateGoalsInStatements(Map<String, Set<VarDefinitionPreq>> definitionPreq,
	    Map<String, Map<String, String>> readValues, RunningState runningState, List<TestStatement> stmts)
    {
	Map<String, Set<String>> readGoals = Planning.initGoal(stmts.get(stmts.size() - 1), readValues);
	Map<String, Set<VarDefinitionPreq>> defineGoals = Planning
		.getTheVarDefMap(definitionPreq.get(stmts.get(stmts.size() - 1).getName()));

	for (int i = stmts.size() - 2; i >= 0; i--)
	{

	    Map<String, Set<String>> newGoals = new HashMap<String, Set<String>>();
	    for (Entry<String, Set<String>> entry : readGoals.entrySet())
	    {
		Utils.addAllTheSetInMap(newGoals, entry.getKey(), entry.getValue());
	    }

	    Map<String, Set<VarDefinitionPreq>> newDefGoal = new HashMap<String, Set<VarDefinitionPreq>>();
	    for (Entry<String, Set<VarDefinitionPreq>> entry : defineGoals.entrySet())
	    {
		Utils.addAllTheSetInMap(newDefGoal, entry.getKey(), entry.getValue());
	    }
	    stmts.get(i).defineGoals = newDefGoal;
	    stmts.get(i).readGoals = newGoals;
	    Map<String, String> renameMap = new HashMap<String, String>();
	    Planning.updateGoals(stmts.get(i), readGoals, runningState, defineGoals, readValues, definitionPreq,
		    renameMap);
	    // stmts.get(i).renameMap = renameMap;
	}
    }

    public static List<TestStatement> getSavedStmts(Map<String, TestStatement> allTestStatements,
	    List<TestStatement> path)
    {
	List<TestStatement> savedStmts = new LinkedList<TestStatement>();

	Set<String> pathSet = new HashSet<String>();

	for (TestStatement stmt : path)
	{
	    pathSet.add(stmt.getName());
	}

	for (TestStatement stmt : allTestStatements.values())
	{
	    if (!pathSet.contains(stmt.getName()))
	    {
		savedStmts.add(stmt);
	    }
	}

	return savedStmts;

    }

    public static Set<String> getAllAssertions(Map<String, TestStatement> allTestStatements)
    {
	Set<String> assertions = new HashSet<String>();
	for (Entry<String, TestStatement> entry : allTestStatements.entrySet())
	{
	    TestStatement stmt = entry.getValue();
	    if (isAssertion(stmt))
		assertions.add(entry.getKey());
	}

	return assertions;
    }

    public static boolean isAssertion(TestStatement statement)
    {

	// TODO check for indirect assertion checking
	if (statement.statement == null)
	    return false;
	String str = statement.statement.toString();
	if (str.toLowerCase().contains("assert") || str.toLowerCase().contains("check"))
	    return true;
	return false;
    }

    public static Triple<TestStatement, RunningState, Map<String, String>> dijkstra(TestStatement root,
	    Map<String, TestState> graph, RunningState runningState, Map<String, Map<String, String>> readValues,
	    Map<String, List<String>> connectedComponentsMap, Map<String, TestStatement> testStatementMap,
	    Set<String> assertions, Map<String, Set<VarDefinitionPreq>> definitionPreq)
	    throws CloneNotSupportedException
    {

	Set<TestStatement> visited = new HashSet<TestStatement>();
	root.curStart = root;
	root.distFrom.put(root, (long) 0);
	PriorityQueue<Triple<TestStatement, RunningState, Map<String, String>>> queue = new PriorityQueue<Triple<TestStatement, RunningState, Map<String, String>>>();

	queue.add(new Triple<TestStatement, RunningState, Map<String, String>>(root, runningState,
		new HashMap<String, String>()));

	while (queue.size() != 0)
	{
	    Triple<TestStatement, RunningState, Map<String, String>> pair = queue.poll();
	    TestStatement parent = pair.getFirst();
	    runningState = pair.getSecond();
	    Map<String, String> batchRename = pair.getThird();
	    if (visited.contains(parent))
		continue;

	    visited.add(parent);

	    if (connectedComponentsMap.containsKey(parent.getName()) || assertions.contains(parent.getName()))
	    {
		Map<String, String> renameClone = new HashMap<String, String>();
		renameClone.putAll(batchRename);
		return new Triple<TestStatement, RunningState, Map<String, String>>(parent, runningState.clone(),
			renameClone);
	    }

	    TestCaseComposer.updateRunningState(parent, runningState, readValues, definitionPreq, batchRename);
	    List<Pair<Integer, TestStatement>> comps = getAllCompatibleTestStatements(testStatementMap, readValues,
		    runningState, visited, definitionPreq);

	    Collections.sort(comps, Collections.reverseOrder());
	    for (Pair<Integer, TestStatement> stmtPair : comps)
	    {
		TestStatement stmt = stmtPair.getSecond();
		if (!visited.contains(stmt))
		{

		    relaxChild(root, queue, parent, stmt, runningState,
			    stmtPair.getFirst() + readValues.get(stmt.getName()).size(), batchRename);
		}

	    }
	}

	return null;
    }

    private static void relaxChild(TestStatement root,
	    PriorityQueue<Triple<TestStatement, RunningState, Map<String, String>>> queue, TestStatement parent,
	    TestStatement stmt, RunningState runningState, int bonus, Map<String, String> batchRename)
	    throws CloneNotSupportedException
    {
	long newD = parent.distFrom.get(root) + stmt.time - bonus + stmt.getSideEffects().size() * 1000000
		+ TestCaseComposer.getTestStatementNumber(stmt.getName());
	Long childDist = stmt.distFrom.get(root);
	if (childDist == null || newD < childDist)
	{
	    stmt.parent.put(root, parent);
	    stmt.distFrom.put(root, newD);
	    stmt.curStart = root;
	    // queue.remove(stmt);
	    Map<String, String> renameClone = new HashMap<String, String>();
	    renameClone.putAll(batchRename);
	    queue.add(new Triple<TestStatement, RunningState, Map<String, String>>(stmt, runningState.clone(),
		    renameClone));
	    // queue.add(child.clone());
	}
    }

    public static List<Pair<Integer, TestStatement>> getAllCompatibleTestStatements(
	    Map<String, TestStatement> allTestStatements, Map<String, Map<String, String>> readValues,
	    RunningState runningState, Set<TestStatement> visited, Map<String, Set<VarDefinitionPreq>> definitionPreq)
    {
	List<Pair<Integer, TestStatement>> comps = new LinkedList<Pair<Integer, TestStatement>>();
	for (TestStatement stmt : allTestStatements.values())
	{
	    if (visited != null && visited.contains(stmt))
		continue;
	    Map<String, String> readVals = readValues.get(stmt.getName());
	    if (readVals == null)
		continue;
	    // if (stmt.equals("Array2DRowRealMatrixTest.testSetRow-1.xml"))
	    // System.out.println();
	    boolean isComp = true;
	    Counter counter = new Counter();
	    for (Entry<String, String> entry : readVals.entrySet())
	    {
		String readVal = entry.getValue();
		Set<String> varsNameInState = runningState.getName(readVal);
		if (varsNameInState == null || varsNameInState.size() == 0)
		{
		    isComp = false;
		    break;
		} else
		{
		    counter.increment(readVal);
		    if (counter.get(readVal) > varsNameInState.size())
		    {
			isComp = false;
			break;
		    }
		}

	    }

	    Set<VarDefinitionPreq> defPreqs = definitionPreq.get(stmt.getName());
	    if (defPreqs != null)
	    {
		for (VarDefinitionPreq defPreq : defPreqs)
		{
		    String neededType = defPreq.getType();
		    Set<String> varsInState = runningState.getNameForType(neededType);
		    if (varsInState == null || varsInState.isEmpty())
		    {
			isComp = false;
			break;
		    }
		}
	    }

	    if (isComp)
		comps.add(new Pair<Integer, TestStatement>(readVals.size(), stmt));

	}

	return comps;
    }

    public static Map<String, Map<String, String>> getAllReadValues(List<String> testCases,
	    Map<String, Set<VarDefinitionPreq>> definitionPreq) throws IOException
    {
	Map<String, Map<String, String>> readValues = new HashMap<String, Map<String, String>>();

	for (String testCase : testCases)
	{
	    // state1 -> <a, b, c>
	    Map<String, Set<SimpleName>> readVars = ReadVariableDetector
		    .populateReadVarsForTestCaseOfFile(Utils.getTestCaseFile(testCase), testCase, definitionPreq);

	    // System.out.println(readVars);
	    // ReadVariableDetector.accumulateReadVars(readVars);

	    // state1 ->
	    // <object1(a), field 1, field 2, ... >
	    // <object2(a), field 1, field 2, ... >
	    // <object3(a), field 1, field 2, ... >
	    ReadVariableDetector.getReadValues(readVars, readValues);

	}

	return readValues;
    }
}
