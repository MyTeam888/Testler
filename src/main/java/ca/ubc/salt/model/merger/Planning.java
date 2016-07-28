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
import java.util.Collection;
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
import ca.ubc.salt.model.utils.Counter;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class Planning
{

    public static Pair<TestStatement, RunningState> dijkstra(TestStatement root, Map<String, TestState> graph,
	    RunningState runningState, Map<String, Map<String, String>> readValues,
	    Map<String, List<String>> connectedComponentsMap, Map<String, TestStatement> testStatementMap,
	    Set<String> assertions, int cutoff) throws CloneNotSupportedException
    {

	TestStatement.curStart = root;
	root.distFrom.put(root, (long) 0);
	PriorityQueue<Pair<TestStatement, RunningState>> queue = new PriorityQueue<Pair<TestStatement, RunningState>>();

	queue.add(new Pair<TestStatement, RunningState>(root, runningState));

	while (queue.size() != 0)
	{
	    Pair<TestStatement, RunningState> pair = queue.poll();
	    TestStatement parent = pair.getFirst();
	    runningState = pair.getSecond();

	    if (parent.distFrom.get(TestStatement.curStart) > cutoff * 1000)
		return null;

	    if (connectedComponentsMap.containsKey(parent.getName()) || assertions.contains(parent.getName()))
	    {
		return new Pair<TestStatement, RunningState>(parent, runningState.clone());
	    }

	    TestCaseComposer.updateRunningState(parent, runningState, readValues);
	    List<Pair<Integer, TestStatement>> comps = IncrementalTestMerger
		    .getAllCompatibleTestStatements(testStatementMap, readValues, runningState, null);

	    Collections.sort(comps, Collections.reverseOrder());
	    for (Pair<Integer, TestStatement> stmtPair : comps)
	    {
		TestStatement stmt = stmtPair.getSecond();
		relaxChild(root, queue, parent, stmt, runningState, stmtPair.getFirst());

	    }
	}

	return null;
    }

    private static void relaxChild(TestStatement root, PriorityQueue<Pair<TestStatement, RunningState>> queue,
	    TestStatement parent, TestStatement stmt, RunningState runningState, int bonus)
	    throws CloneNotSupportedException
    {
	long newD = parent.distFrom.get(root) + stmt.time - bonus + stmt.getSideEffects().size() * 10;
	Long childDist = stmt.distFrom.get(root);
	stmt = stmt.clone();
	stmt.parent.put(root, parent);
	stmt.distFrom.put(root, newD);
	stmt.curStart = root;
	// queue.remove(stmt);
	queue.add(new Pair<TestStatement, RunningState>(stmt, runningState.clone()));
	// queue.add(child.clone());
    }
    
    
    public static Map<String, Set<TestStatement>> getTestCaseTestStatementMapping(Collection<TestStatement> stmts)
    {
	Map<String, Set<TestStatement>> mapping = new HashMap<String, Set<TestStatement>>();
	for (TestStatement stmt : stmts)
	{
	    String testCaseName = Utils.getTestCaseNameFromTestStatement(stmt.getName());
	    Utils.addToTheSetInMap(mapping, testCaseName, stmt);
	}
	
	return mapping;
	
    }
    
    public static Map<String, Map<String, TestStatement>> getTestCaseTestStatementMapping(Map<String, TestStatement> stmts)
    {
	Map<String, Map<String, TestStatement>> mapping = new HashMap<String, Map<String, TestStatement>>();
	for (Entry<String, TestStatement> entry : stmts.entrySet())
	{
	    String testCaseName = Utils.getTestCaseNameFromTestStatement(entry.getKey());
	    Utils.addToTheMapInMap(mapping, testCaseName,entry.getKey(), entry.getValue());
	}
	
	return mapping;
	
    }
    
    public static Map<String, Set<String>> getTestCaseTestStatementStringMapping(Collection<String> stmts)
    {
	Map<String, Set<String>> mapping = new HashMap<String, Set<String>>();
	for (String stmt : stmts)
	{
	    String testCaseName = Utils.getTestCaseNameFromTestStatement(stmt);
	    Utils.addToTheSetInMap(mapping, testCaseName, stmt);
	}
	
	return mapping;
	
    }
    
    
    
    
    public static Set<TestStatement> getTestStatementSet(Set<String> stmts, Map<String, TestStatement> stmtMap)
    {
	Set<TestStatement> stmtSet = new HashSet<TestStatement>();
	for (String stmt : stmts)
	{
	    stmtSet.add(stmtMap.get(stmt));
	}
	return stmtSet;
    }
}