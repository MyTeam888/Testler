package ca.ubc.salt.model.composer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
    Map<String, String> valueNamePairForCurrentState;

    public RunningState()
    {
	nameValuePairForCurrentState = new HashMap<String, String>();
	valueNamePairForCurrentState = new HashMap<String, String>();
    }

    public String getValue(String name)
    {
	return nameValuePairForCurrentState.get(name);
    }

    public String getName(String value)
    {
	return valueNamePairForCurrentState.get(value);
    }

    public void put(String name, String value)
    {
	String prevVal = this.getValue(name);
	if (prevVal != null)
	    valueNamePairForCurrentState.remove(prevVal);
	nameValuePairForCurrentState.put(name, value);
	valueNamePairForCurrentState.put(value, name);
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
	rs.valueNamePairForCurrentState.putAll(this.valueNamePairForCurrentState);
	return rs;
    }
}