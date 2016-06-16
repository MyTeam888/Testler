package ca.ubc.salt.model.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class FileUtils
{
    public static HashSet<String> readSet(String path) throws FileNotFoundException
    {

	Scanner prodSc = new Scanner(new File(path));
	HashSet<String> set = new HashSet<String>();
	while (prodSc.hasNext())
	{
	    set.add(prodSc.next());
	}
	return set;
    }

    public static String readFile(File file) throws FileNotFoundException
    {
	Scanner sc = new Scanner(file);
	StringBuffer sb = new StringBuffer();
	while (sc.hasNextLine())
	    sb.append(sc.nextLine());
	sc.close();
	return sb.toString();
    }

    public static void removeList(Set<String> a, Set<String> b)
    {
	for (Iterator<String> iterator = a.iterator(); iterator.hasNext();)
	    if (b.contains(iterator.next()))
		iterator.remove();

    }

    public static String readFileToString(String filePath) throws IOException
    {
	StringBuilder fileData = new StringBuilder(1000);
	BufferedReader reader = new BufferedReader(new FileReader(filePath));

	char[] buf = new char[1024];
	int numRead = 0;
	while ((numRead = reader.read(buf)) != -1)
	{
	    String readData = String.valueOf(buf, 0, numRead);
	    fileData.append(readData);
	}

	reader.close();

	return fileData.toString();
    }

    public static String readFileToString(File file) throws IOException
    {
	StringBuilder fileData = new StringBuilder(1000);
	BufferedReader reader = new BufferedReader(new FileReader(file));

	char[] buf = new char[1024];
	int numRead = 0;
	while ((numRead = reader.read(buf)) != -1)
	{
	    String readData = String.valueOf(buf, 0, numRead);
	    fileData.append(readData);
	}

	reader.close();

	return fileData.toString();
    }

    public static String getState(String stateName)
    {

	return getState(new File(Settings.tracePaths + "/" + stateName));
    }

    public static String getState(File file)
    {
	List<String> lines = FileUtils.getLines(file);
	if(lines.size() > 0)
	    return lines.get(lines.size() - 1);
	else
	    return "";
    }

    public static String getVars(String stateName)
    {
	List<String> lines = FileUtils.getLines(new File(Settings.tracePaths + "/" + stateName));
	return lines == null ? null : lines.get(lines.size() - 2);
    }

    public static String getMethodCalled(String stateName)
    {
	return getMethodCalled(new File(Settings.tracePaths + "/" + stateName));
    }

    static List<String> getLines(File fileName)
    {

	try
	{
	    Scanner sc = new Scanner(fileName);
	    List<String> lines = new LinkedList<String>();
	    while (sc.hasNextLine())
		lines.add(sc.nextLine());
	    sc.close();
	    return lines;
	} catch (FileNotFoundException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return null;
    }

    public static String getMethodCalled(File stateFile)
    {
	String str;
	try
	{
	    str = readFileToString(stateFile);
	    int index = str.indexOf("<vars>");
	    if (index == 0)
		return null;
	    if (index == -1)
		return str;
	    return str.substring(0, index);
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return null;

    }
    
    public static List<String> getStatesForTestCase(List<String> testCases)
    {
	List<String> states = new LinkedList<String>();
	for (String testCase : testCases)
	{
	    getStatesForTestCase(testCase, states);
	}
	return states;
    }
    

    public static List<String> getStatesForTestCase(String testCase, List<String> states)
    {
	File[] stateFiles = getStateFilesForTestCase(testCase);
	if (states == null)
	    states = new LinkedList<String>();
	for (File file : stateFiles)
	{
	    // String name = file.getName();
	    // int index = name.lastIndexOf('.');
	    // String state = name.substring(0, index);
	    // states.add(state);
	    states.add(file.getName());
	}

	return states;
    }

    public static File[] getStateFilesForTestCase(final String testCase)
    {
	File folder = new File(Settings.tracePaths);
	FilenameFilter filter = new FilenameFilter()
	{

	    @Override
	    public boolean accept(File dir, String name)
	    {
		return name.contains(testCase);
	    }
	};

	File[] traces = folder.listFiles(filter);
	return traces;
    }

}
