package ca.ubc.salt.model.state;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;

public class ProductionCallingTestStatement
{

    public static void main(String[] args)
    {
	Map<String, List<String>> uniqueTestStatements = getUniqueTestStatements();

	for (Entry<String, List<String>> entry : uniqueTestStatements.entrySet())
	{
	    System.out.println(entry.getValue().size());
	}
//	System.out.println(uniqueTestStatements.size());
    }

    public static Map<String, List<String>> getUniqueTestStatements()
    {
	Map<String, List<String>> uniqueTestStatements = new HashMap<String, List<String>>();

	File folder = new File(Settings.tracePaths);
	File[] traces = folder.listFiles();
	int counter = 1;

	for (File trace : traces)
	{
	    String methodCalled = FileUtils.getMethodCalled(trace);
	    List<String> states = uniqueTestStatements.get(methodCalled);
	    if (states == null)
	    {
		states = new LinkedList<String>();
		states.add(trace.getName());
		uniqueTestStatements.put(methodCalled, states);
	    } else
		states.add(trace.getName());
	    
	    counter++;
	    if (counter % 1000 == 0)
		Settings.consoleLogger.error(String.format("processed %d logs", counter));

	}

	return uniqueTestStatements;
    }
}
