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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
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
import ca.ubc.salt.model.utils.TwoPair;
import ca.ubc.salt.model.utils.Utils;

public class Planning
{

    // public static Pair<TestStatement, RunningState> forward(TestStatement
    // root, Map<String, TestState> graph,
    // RunningState runningState, Map<String, Map<String, String>> readValues,
    // Map<String, List<String>> connectedComponentsMap, Map<String,
    // TestStatement> testStatementMap,
    // Set<String> assertions, int cutoff) throws CloneNotSupportedException
    // {
    //
    // TestStatement.curStart = root;
    // root.distFrom.put(root, (long) 0);
    // PriorityQueue<Pair<TestStatement, RunningState>> queue = new
    // PriorityQueue<Pair<TestStatement, RunningState>>();
    //
    // queue.add(new Pair<TestStatement, RunningState>(root, runningState));
    //
    // while (queue.size() != 0)
    // {
    // Pair<TestStatement, RunningState> pair = queue.poll();
    // TestStatement parent = pair.getFirst();
    // runningState = pair.getSecond();
    //
    // if (parent.distFrom.get(TestStatement.curStart) > cutoff * 1000)
    // return null;
    //
    // if (connectedComponentsMap.containsKey(parent.getName()) ||
    // assertions.contains(parent.getName()))
    // {
    // return new Pair<TestStatement, RunningState>(parent,
    // runningState.clone());
    // }
    //
    // TestCaseComposer.updateRunningState(parent, runningState, readValues);
    // List<Pair<Integer, TestStatement>> comps = BackwardTestMerger
    // .getAllCompatibleTestStatements(testStatementMap, readValues,
    // runningState, null, null);
    //
    // Collections.sort(comps, Collections.reverseOrder());
    // for (Pair<Integer, TestStatement> stmtPair : comps)
    // {
    // TestStatement stmt = stmtPair.getSecond();
    // relaxChild(root, queue, parent, stmt, runningState, stmtPair.getFirst());
    // }
    // }
    //
    // return null;
    // }

    public static Map<String, Set<VarDefinitionPreq>> getTheVarDefMap(Set<VarDefinitionPreq> defPreq)
    {
	Map<String, Set<VarDefinitionPreq>> preqs = new HashMap<String, Set<VarDefinitionPreq>>();
	if (defPreq != null)
	{
	    for (VarDefinitionPreq def : defPreq)
	    {
		Utils.addToTheSetInMap(preqs, def.getType(), def);
	    }
	}
	return preqs;
    }

    public static List<TestStatement> backward(TestStatement goal, RunningState initialState,
	    Map<String, Map<String, String>> readValues, Map<String, List<String>> connectedComponentsMap,
	    Map<String, TestStatement> testStatementMap, Map<String, Set<VarDefinitionPreq>> definitionPreq)
	    throws CloneNotSupportedException
    {
	PriorityQueue<TwoPair<Long, List<TestStatement>, Map<String, Set<String>>, Map<String, Set<VarDefinitionPreq>>>> queue = new PriorityQueue<TwoPair<Long, List<TestStatement>, Map<String, Set<String>>, Map<String, Set<VarDefinitionPreq>>>>();

	ArrayList<TestStatement> initialList = new ArrayList<TestStatement>();
	initialList.add(goal);
	Map<String, Set<String>> initGoals = initGoal(goal, readValues);
	queue.add(new TwoPair<Long, List<TestStatement>, Map<String, Set<String>>, Map<String, Set<VarDefinitionPreq>>>(
		(long) 0, initialList, initGoals, getTheVarDefMap(definitionPreq.get(goal.getName()))));

	while (queue.size() != 0)
	{

	    TwoPair<Long, List<TestStatement>, Map<String, Set<String>>, Map<String, Set<VarDefinitionPreq>>> frontier = queue
		    .poll();

	    Long cost = frontier.getFirst();
	    List<TestStatement> listTillNow = frontier.getSecond();
	    Map<String, Set<String>> pGoals = frontier.getThird();
	    Map<String, Set<VarDefinitionPreq>> pDefines = frontier.getForth();

	    if (areGoalsSatisfied(pGoals, pDefines, initialState))
		return frontier.getSecond();
	    TestStatement parent = listTillNow.get(0);
	    List<TestStatement> preqs = getPreqStmts(parent, pGoals, pDefines, testStatementMap);

	    int parentInd = TestCaseComposer.getTestStatementNumber(parent.getName());
	    for (TestStatement preq : preqs)
	    {
		int preqInd = TestCaseComposer.getTestStatementNumber(preq.getName());
		if (preqInd >= parentInd)
		    continue;
		if (listTillNow.contains(preq))
		    continue;
		Map<String, Set<String>> newGoals = new HashMap<String, Set<String>>();
		for (Entry<String, Set<String>> entry : pGoals.entrySet())
		{
		    Utils.addAllTheSetInMap(newGoals, entry.getKey(), entry.getValue());
		}

		Map<String, Set<VarDefinitionPreq>> newDefGoal = new HashMap<String, Set<VarDefinitionPreq>>();
		for (Entry<String, Set<VarDefinitionPreq>> entry : pDefines.entrySet())
		{
		    Utils.addAllTheSetInMap(newDefGoal, entry.getKey(), entry.getValue());
		}

		List<TestStatement> newList = new ArrayList<TestStatement>();
		newList.add(preq);
		newList.addAll(listTillNow);

		int score = updateGoals(preq, newGoals, initialState, newDefGoal, readValues, definitionPreq,
			new HashMap<String, String>());
		queue.add(
			new TwoPair<Long, List<TestStatement>, Map<String, Set<String>>, Map<String, Set<VarDefinitionPreq>>>(
				cost + preq.time - score * 100, newList, newGoals, newDefGoal));
	    }

	}

	return null;

    }

    public static Map<String, Set<String>> initGoal(TestStatement goal, Map<String, Map<String, String>> readValues)
    {
	Map<String, Set<String>> initGoals = new HashMap<String, Set<String>>();
//	if (readValues.get(goal.getName()) != null)
	for (Entry<String, String> entry : readValues.get(goal.getName()).entrySet())
	{
	    Utils.addToTheSetInMap(initGoals, entry.getValue(), entry.getKey());
	}
	return initGoals;
    }

    private static boolean areGoalsSatisfied(Map<String, Set<String>> readGoals,
	    Map<String, Set<VarDefinitionPreq>> defineGoals, RunningState initialState)
    {
	for (Entry<String, Set<String>> entry : readGoals.entrySet())
	{
	    String readGoal = entry.getKey();
	    Set<String> goalNames = entry.getValue();
	    Set<String> names = initialState.getName(readGoal);
	    if (goalNames.size() != 0 && (names == null || names.size() < goalNames.size()))
		return false;
	}
	for (Entry<String, Set<VarDefinitionPreq>> entry : defineGoals.entrySet())
	{

	    String neededType = entry.getKey();
	    Set<VarDefinitionPreq> preqs = entry.getValue();
	    Set<String> varsInState = initialState.getNameForType(neededType);
	    if ((varsInState == null ? 0 : varsInState.size()) < preqs.size())
		return false;

	}
	return true;
    }

    public static int updateGoals(TestStatement stmt, Map<String, Set<String>> readGoals, RunningState rs,
	    Map<String, Set<VarDefinitionPreq>> defineGoals, Map<String, Map<String, String>> readValues,
	    Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, String> renameMap)
    {
	int score = 0;
	for (Entry<String, Pair<String, String>> entry : stmt.getSideEffects().entrySet())
	{
	    String name = entry.getKey();
	    Pair<String, String> changedVal = entry.getValue();
	    if (readGoals.containsKey(changedVal.getSecond()))
	    {
		Set<String> set = readGoals.get(changedVal.getSecond());
		if (set.contains(name))
		    set.remove(name);
		else
		{
		    if (set.size() != 0)
		    {
			String str = set.iterator().next();
			set.remove(str);
			renameMap.put(name, str);
			if (set.isEmpty())
			{
			    readGoals.remove(changedVal.getSecond());
			}
		    }
		}
		if (rs.getName(changedVal.getSecond()) == null)
		    score += 10;
	    }
	}
	if (stmt.getNewVars() != null)
	    for (Entry<String, String> entry : stmt.getNewVars().entrySet())
	    {
		String name = entry.getKey();
		String newVal = entry.getValue();
		if (readGoals.containsKey(newVal))
		{
		    Set<String> set = readGoals.get(newVal);
		    if (set.contains(name))
			set.remove(name);
		    else
		    {
			if (set.size() != 0)
			{
			    String str = set.iterator().next();
			    set.remove(str);
			    renameMap.put(name, str);
			    if (set.isEmpty())
			    {
				readGoals.remove(newVal);
			    }
			}
		    }
		    if (rs.getName(newVal) == null)
			score += 10;
		}
		String type = stmt.getTypeOfVar(name);
		if (defineGoals.containsKey(type))
		{
		    Set<VarDefinitionPreq> set = defineGoals.get(type);
		    if (set.size() > 0)
		    {
			VarDefinitionPreq preq = set.iterator().next();
			set.remove(preq);
			renameMap.put(name, preq.getName().getIdentifier());
		    } else
			defineGoals.remove(set);
		    if (rs.getNameForType(type) == null)
			score += 10;
		}

	    }
	if (readValues.get(stmt.getName()) != null)
	    for (Entry<String, String> entry : readValues.get(stmt.getName()).entrySet())
	    {
		String name = entry.getKey();
		String readVal = entry.getValue();
		Utils.addToTheSetInMap(readGoals, readVal, name);
	    }

	if (definitionPreq.get(stmt.getName()) != null)
	    for (VarDefinitionPreq defPreq : definitionPreq.get(stmt.getName()))
	    {
		Utils.addToTheSetInMap(defineGoals, defPreq.getType(), defPreq);
	    }
	return score;

    }

    private static List<TestStatement> getPreqStmts(TestStatement stmt, Map<String, Set<String>> readGoals,
	    Map<String, Set<VarDefinitionPreq>> defineGoals, Map<String, TestStatement> testStatementMap)
    {
	List<TestStatement> preqs = new LinkedList<TestStatement>();
	outer: for (Entry<String, TestStatement> entry : testStatementMap.entrySet())
	{

	    TestStatement testStmt = entry.getValue();
	    for (Pair<String, String> changedVal : testStmt.getSideEffects().values())
	    {
		if (readGoals.containsKey(changedVal.getSecond()))
		{
		    preqs.add(testStmt);
		    continue outer;
		}
	    }

	    for (Entry<String, String> newEntry : testStmt.getNewVars().entrySet())
	    {
		String name = newEntry.getKey();
		String newVal = newEntry.getValue();
		if (readGoals.containsKey(newVal))
		{
		    preqs.add(testStmt);
		    continue outer;
		}

		if (defineGoals.containsKey(stmt.getTypeOfVar(name)))
		{
		    preqs.add(testStmt);
		    continue outer;
		}

	    }

	}

	return preqs;
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

    public static Map<String, Map<String, TestStatement>> getTestCaseTestStatementMapping(
	    Map<String, TestStatement> stmts)
    {
	Map<String, Map<String, TestStatement>> mapping = new HashMap<String, Map<String, TestStatement>>();
	for (Entry<String, TestStatement> entry : stmts.entrySet())
	{
	    String testCaseName = Utils.getTestCaseNameFromTestStatement(entry.getKey());
	    Utils.addToTheMapInMap(mapping, testCaseName, entry.getKey(), entry.getValue());
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
