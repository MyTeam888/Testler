package ca.ubc.salt.model.composer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	nameValuePairForCurrentState.put(name, value);
	valueNamePairForCurrentState.put(value, name);
    }

    public void update(String prevState, Map<String, String> renameMap, Set<String> varsName)
    {
	List<String> sortedTestStates = Arrays
		.asList(FileUtils.getStatesForTestCase(Utils.getTestCaseNameFromTestStatement(prevState)));

	Collections.sort(sortedTestStates, new NaturalOrderComparator());

	String nextState = Utils.nextOrPrevState(prevState, sortedTestStates, true);
	Map<String, String> nameValuePair = FileUtils.getNameValuePairs(nextState);

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