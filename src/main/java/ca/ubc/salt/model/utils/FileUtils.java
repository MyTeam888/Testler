package ca.ubc.salt.model.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
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

    public static String readFile(File file, boolean skipFirstLine) throws FileNotFoundException
    {
	Scanner sc = new Scanner(file);
	StringBuffer sb = new StringBuffer();
	if (skipFirstLine && sc.hasNextLine())
	    sc.nextLine();
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
    
    
    public static String readFileToString(String filePath) throws IOException {
	StringBuilder fileData = new StringBuilder(1000);
	BufferedReader reader = new BufferedReader(new FileReader(filePath));

	char[] buf = new char[10];
	int numRead = 0;
	while ((numRead = reader.read(buf)) != -1) {
		String readData = String.valueOf(buf, 0, numRead);
		fileData.append(readData);
		buf = new char[1024];
	}

	reader.close();

	return  fileData.toString();	
}
    public static String readFileToString(File file) throws IOException {
	StringBuilder fileData = new StringBuilder(1000);
	BufferedReader reader = new BufferedReader(new FileReader(file));
	
	char[] buf = new char[10];
	int numRead = 0;
	while ((numRead = reader.read(buf)) != -1) {
	    String readData = String.valueOf(buf, 0, numRead);
	    fileData.append(readData);
	    buf = new char[1024];
	}
	
	reader.close();
	
	return  fileData.toString();	
    }
    
    public static String getState(String stateName)
    {
	
	return getState(new File(Settings.tracePaths+"/"+stateName));
    }
    
    public static String getState(File file)
    {
	try
	{
	    return FileUtils.readFile(file, true);
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return "";
	}
    }
    
    
    
    public static String getVars(String stateName)
    {
	Scanner sc = null;
	try
	{
	    sc = new Scanner(new File(Settings.tracePaths+"/"+stateName));
	} catch (FileNotFoundException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	String vars = sc.nextLine();
	sc.close();
	return vars;
    }
    
    
}
