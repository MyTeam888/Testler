package ca.ubc.salt.model.composer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.Map.Entry;

import Comparator.NaturalOrderComparator;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Utils;

public class RunningState
{
    Map<String, String> nameValuePairForCurrentState;
    Map<String, Set<String>> valueNamePairForCurrentState;

    public RunningState()
    {
	nameValuePairForCurrentState = new HashMap<String, String>();
	valueNamePairForCurrentState = new HashMap<String, Set<String>>();
    }

    public String getValue(String name)
    {
	return nameValuePairForCurrentState.get(name);
    }

    public Set<String> getName(String value)
    {
	return valueNamePairForCurrentState.get(value);
    }

    public void put(String name, String value)
    {
	String prevVal = this.getValue(name);
	if (prevVal != null)
	{
//	    valueNamePairForCurrentState.remove(prevVal);
	    Utils.removeFromTheSetInMap(valueNamePairForCurrentState, prevVal, name);
	}
	nameValuePairForCurrentState.put(name, value);
	Utils.addToTheSetInMap(valueNamePairForCurrentState, value, name);
//	valueNamePairForCurrentState.put(value, name);
    }

    public void update(String prevState, Map<String, String> renameMap, Set<String> varsName)
    {
	String nextState = Utils.nextOrPrevState(
		Arrays.asList(new String[] { Utils.getTestCaseNameFromTestStatement(prevState) }), prevState, true);
	Map<String, String> nameValuePair = null;
	try
	{
	    nameValuePair = TestCaseComposer.nameValuePairs.get(nextState);
	} catch (ExecutionException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	for (Entry<String, String> entry : nameValuePair.entrySet())
	{
	    String name = entry.getKey();
	    if (varsName.contains(name))
	    {
		String value = entry.getValue();

		String renamedName = renameMap.get(name);

		if (renamedName != null)
		    name = renamedName;

		this.put(name, value);
	    }
	}

    }

    @Override
    public RunningState clone() throws CloneNotSupportedException
    {
	// TODO Auto-generated method stub
	RunningState rs = new RunningState();
	rs.nameValuePairForCurrentState.putAll(this.nameValuePairForCurrentState);
	for (Entry<String, Set<String>> entry : this.valueNamePairForCurrentState.entrySet())
	{
	    Set<String> newSet = new HashSet<String>();
	    newSet.addAll(entry.getValue());
	    rs.valueNamePairForCurrentState.put(entry.getKey(), newSet);
	    
	}
	return rs;
    }

    public RunningState(Collection<String> testCases)
    {
	nameValuePairForCurrentState = new HashMap<String, String>();
	valueNamePairForCurrentState = new HashMap<String, Set<String>>();
	for (String testCase : testCases)
	{
	    Map<String, String> nameValuePair = FileUtils.getNameValuePairs(testCase + "-0.xml");
	    for (Entry<String, String> entry : nameValuePair.entrySet())
		this.put(entry.getKey(), entry.getValue());
	}

    }

    public RunningState(Collection<String> testCases, String mainTestClass)
    {
	nameValuePairForCurrentState = new HashMap<String, String>();
	valueNamePairForCurrentState = new HashMap<String, Set<String>>();

	Set<String> done = new HashSet<String>();
	for (String testCase : testCases)
	{
	    String testClass = Utils.getTestClassNameFromTestCase(testCase);
	    if (done.contains(testClass))
		continue;
	    Map<String, String> nameValuePair = FileUtils.getNameValuePairs(testCase + "-0.xml");
	    for (Entry<String, String> entry : nameValuePair.entrySet())
	    {
		String name = entry.getKey();
		if (!testClass.equals(mainTestClass))
		    name = name + "_" + testClass;
		this.put(name, entry.getValue());
	    }
	    done.add(testClass);
	}
    }

}