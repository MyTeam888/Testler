package ca.ubc.salt.model.state;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.eclipse.core.internal.utils.FileUtil;

import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;

public class StateComparator
{

    static HashMap<Integer, List<String>> xmlHashes;

    /*
     * public static void main(String[] args) throws FileNotFoundException {
     * long time = System.nanoTime(); Map<String, TestState> graph =
     * createGraph(); TestState root = graph.get("init.xml");
     * System.out.println(root.printDot(false));
     * 
     * Settings.consoleLogger.info(String.format("took %d milliseconds",
     * (System.nanoTime() - time) / 1000000)); }
     * 
     * public static Map<String, TestState> createGraph() throws
     * FileNotFoundException { xmlHashes = createHashes();
     * 
     * Map<String, TestState> testStateOfState = new HashMap<String,
     * TestState>(); List<TestState> testStates = new LinkedList<TestState>();
     * 
     * for (Entry<Integer, List<String>> entry : xmlHashes.entrySet()) {
     * compareStates(entry.getKey(), testStateOfState, testStates); }
     * 
     * populateGraph(testStateOfState, testStates);
     * 
     * return testStateOfState; }
     */

    public static Map<String, TestState> createGraph(List<String> testCases) throws FileNotFoundException
    {
	Collections.sort(testCases);
	xmlHashes = createHashes(testCases);

	Map<String, TestState> testStateOfState = new HashMap<String, TestState>();
	List<TestState> testStates = new LinkedList<TestState>();

	for (Entry<Integer, List<String>> entry : xmlHashes.entrySet())
	{
	    compareStates(entry.getKey(), testStateOfState, testStates);
	}

	
	populateGraph(testStateOfState, testStates, FileUtils.getStatesForTestCase(testCases));

	return testStateOfState;
    }

    private static void populateGraph(Map<String, TestState> testStateMap, List<TestState> testStates,
	    List<String> sortedTestStates)
    {

	for (int i = 0; i < testStates.size(); i++)
	{
	    TestState ts = testStates.get(i);
	    for (String state : ts.getStates())
	    {
		String nextState = nextOrPrevState(state, sortedTestStates, true);
		if (testStateMap.containsKey(nextState))
		{
		    ts.getChildren().put(state, new TestStatement(ts, testStateMap.get(nextState), state));
		}

		String prevState = nextOrPrevState(state, sortedTestStates, false);
		if (testStateMap.containsKey(prevState))
		{
		    ts.getParents().put(state, new TestStatement(ts, testStateMap.get(prevState), state));
		}
	    }
	}

    }

    private static String nextOrPrevState(String state, List<String> sortedTestCases, boolean next)
    {
	int index = sortedTestCases.indexOf(state);
	if (index == -1 || (next && index == sortedTestCases.size() - 1) || (!next && index == 0))
	    return "";

	String[] split = splitState(state);

	if (split.length != 2)
	    return "";

	String nextState = sortedTestCases.get(index + (next ? 1 : -1));
	String[] splitNext = splitState(nextState);

	if (splitNext.length != 2)
	    return "";

	if (split[0].equals(splitNext[0]))
	    return nextState;
	else
	    return "";
    }

    private static String[] splitState(String state)
    {
	state = state.substring(0, state.lastIndexOf('.'));
	String[] split = state.split("-");
	return split;
    }

    private static void compareStates(int hash, Map<String, TestState> testStateOfState, List<TestState> testStates)
    {

	List<String> states = xmlHashes.get(hash);

	for (ListIterator<String> it = states.listIterator(); it.hasNext();)
	{
	    String firstState = it.next();
	    it.remove();

	    TestState ts = new TestState();
	    testStates.add(ts);

	    ts.states.add(firstState);
	    testStateOfState.put(firstState, ts);

	    for (ListIterator<String> it2 = states.listIterator(); it2.hasNext();)
	    {
		String secondState = it2.next();
		if (compareStates(firstState, secondState))
		{

		    ts.states.add(secondState);
		    testStateOfState.put(secondState, ts);
		    it2.remove();
		}
	    }
	}

    }

    private static boolean compareStates(String first, String second)
    {
	String firstState = FileUtils.getState(first);
	String secondState = FileUtils.getState(second);
	return firstState.equals(secondState);
    }

    private static HashMap<Integer, List<String>> createHashes() throws FileNotFoundException
    {

	HashMap<Integer, List<String>> hashes = new HashMap<Integer, List<String>>();

	File folder = new File(Settings.tracePaths);
	File[] traces = folder.listFiles();

	for (File trace : traces)
	{
	    int hash = hash(trace);
	    if (hashes.containsKey(hash))
	    {
		hashes.get(hash).add(trace.getName());
	    } else
	    {
		List<String> list = new LinkedList<String>();
		list.add(trace.getName());
		hashes.put(hash, list);
	    }
	}

	return hashes;
    }

    private static HashMap<Integer, List<String>> createHashes(List<String> testCases) throws FileNotFoundException
    {
	HashMap<Integer, List<String>> hashes = new HashMap<Integer, List<String>>();

	for (final String testCase : testCases)
	{

	    File[] traces = FileUtils.getStateFilesForTestCase(testCase);

	    for (File trace : traces)
	    {
		int hash = hash(trace);
		if (hashes.containsKey(hash))
		{
		    hashes.get(hash).add(trace.getName());
		} else
		{
		    List<String> list = new LinkedList<String>();
		    list.add(trace.getName());
		    hashes.put(hash, list);
		}
	    }
	}

	return hashes;
    }

    private static int hash(File file) throws FileNotFoundException
    {
	String xml = FileUtils.getState(file);
	int hash = xml.hashCode();

	return hash;
    }

}