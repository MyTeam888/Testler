package ca.ubc.salt.model.state;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jface.text.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.Method;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;
import ca.ubc.salt.model.utils.XMLUtils;

public class ReadVariableDetector
{

    // public static void main(String[] args) throws IOException
    // {
    // populateReadVarsForFile(
    // "/Users/Arash/Research/repos/commons-math/src/test/java/org/apache/commons/math4/fraction/FractionTest.java");
    // }

    public static Map<String, Set<SimpleName>> populateReadVarsForFile(String path) throws IOException
    {
	File testClass = new File(path);
	if (testClass.isFile())
	{
	    if (!Utils.isTestClass(testClass))
		return null;

	    String source = FileUtils.readFileToString(testClass);
	    Document document = new Document(source);
	    List<ClassModel> classes = ClassModel.getClasses(document.get(), true, "FractionTest.java",
		    new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });

	    Map<String, Set<SimpleName>> readVars = new HashMap<String, Set<SimpleName>>();

	    for (ClassModel clazz : classes)
		populateReadVarsForClass(clazz, null, document, readVars);
	    return readVars;

	}
	return null;
	// TODO Do I need to handle it ?!
	// else if (testClass.isDirectory())
	// {
	// File[] listOfFiles = testClass.listFiles();
	// for (int i = 0; i < listOfFiles.length; i++)
	// {
	// populateReadVarsForFile(listOfFiles[i].getAbsolutePath());
	// }
	// }
    }
    
    
    public static Map<String, Set<SimpleName>> populateReadVarsForTestCaseOfFile(String path, String testcase) throws IOException
    {
	File testClass = new File(path);
	if (testClass.isFile())
	{
	    if (!Utils.isTestClass(testClass))
		return null;

	    String source = FileUtils.readFileToString(testClass);
	    Document document = new Document(source);
	    List<ClassModel> classes = ClassModel.getClasses(document.get(), true, path,
		    new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });

	    Map<String, Set<SimpleName>> readVars = new HashMap<String, Set<SimpleName>>();

	    for (ClassModel clazz : classes)
		populateReadVarsForTestCaseOfClass(clazz, null, document, readVars, testcase);
	    return readVars;

	}
	return null;
	// TODO Do I need to handle it ?!
	// else if (testClass.isDirectory())
	// {
	// File[] listOfFiles = testClass.listFiles();
	// for (int i = 0; i < listOfFiles.length; i++)
	// {
	// populateReadVarsForFile(listOfFiles[i].getAbsolutePath());
	// }
	// }
    }

    public static void populateReadVarsForClass(ClassModel srcClass, List<String> loadedClasses, Document document,
	    Map<String, Set<SimpleName>> readVars)
    {
	if (readVars == null)
	    readVars = new HashMap<String, Set<SimpleName>>();
	List<Method> methods = srcClass.getMethods();

	for (Method m : methods)
	{
	    m.populateReadVars(document, loadedClasses, readVars);
	}

    }

    public static void populateReadVarsForTestCaseOfClass(ClassModel srcClass, List<String> loadedClasses,
	    Document document, Map<String, Set<SimpleName>> readVars, String testcase)
    {
	if (readVars == null)
	    readVars = new HashMap<String, Set<SimpleName>>();
	List<Method> methods = srcClass.getMethods();

	for (Method m : methods)
	{
	    if (m.getMethodDec().getName().toString().equals(Utils.getTestCaseName(testcase)))
		m.populateReadVars(document, loadedClasses, readVars);
	}

    }
    
    public static Map<String, Set<List<String>>> getReadValues(Map<String, Set<SimpleName>> readVars)
    {
	Map<String, Set<List<String>>> readValues = new HashMap<String, Set<List<String>>>();
	for (Entry<String, Set<SimpleName>> entry : readVars.entrySet())
	{
	    String stateName = entry.getKey();
	    Set<List<String>> readValuesForState = getReadValuesOfState(stateName, entry.getValue());
	    readValues.put(stateName, readValuesForState);
	}
	return readValues;
    }

    public static Set<List<String>> getReadValuesOfState(String stateName, Set<SimpleName> readVars)
    {
	Set<List<String>> varStateSet = new HashSet<List<String>>();

	String varXML = FileUtils.getVars(stateName);

	// TODO double check what to return
	if (varXML == null)
	    return varStateSet;

	Set<String> readVarNames = getNameSet(readVars);
	List<String> stateVarNames = XMLUtils.getVars(varXML);

	int index = 0;
	NodeList nodeList = XMLUtils.getNodeList(stateName);
	LinkedList<String> key = new LinkedList<String>();
	for (String stateVar : stateVarNames)
	{
	    if (readVarNames.contains(stateVar))
		processObjectValues(nodeList.item(index), varStateSet, key, stateName);
	    index++;
	}
	return varStateSet;
    }

    public static Set<String> getNameSet(Set<SimpleName> readVars)
    {
	Set<String> varNames = new HashSet<String>();
	for (SimpleName var : readVars)
	{
	    IBinding nodeBinding = var.resolveBinding();
	    IVariableBinding ivb = (IVariableBinding) nodeBinding;
	    varNames.add(ivb.getName());
	}
	return varNames;
    }

    public static void processObjectValues(Node object, Set<List<String>> varStateSet, LinkedList<String> key,
	    String stateName)
    {

	key.add(object.getNodeName());

	NodeList children = object.getChildNodes();
	if (children.getLength() == 0)
	{
	    key.add(object.getNodeValue());
	    varStateSet.add((List) key.clone());
	    key.removeLast();
	} else
	{
	    for (int i = 0; i < children.getLength(); i++)
	    {
		processObjectValues(children.item(i), varStateSet, key, stateName);
	    }
	}

	key.removeLast();
    }

}