package ca.ubc.salt.model.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import Comparator.NaturalOrderComparator;
import ca.ubc.salt.model.instrumenter.TestClassInstrumenter;

public class Utils
{

    public static Map<String, String> classFileMapping;
    static
    {
	classFileMapping = getClassFileMapping();
    }

    public static <T> Set intersection(List<Set<T>> sets)
    {
	Set common = new HashSet();
	if (sets.size() != 0)
	{
	    ListIterator<Set<T>> iterator = sets.listIterator();
	    common.addAll(iterator.next());
	    while (iterator.hasNext())
	    {
		common.retainAll(iterator.next());
	    }
	}
	return common;
    }

    public static boolean isTestClass(File classFile)
    {
	if (classFile.getAbsolutePath().contains("/test/"))
	    return true;
	else
	    return false;
    }

    public static void copyProject(String from, String to)
    {

	// String[] cmdRM = new String[]{"rm", "-r",
	// Settings.PROJECT_INSTRUMENTED_PATH};
	String[] cmdCP = new String[] { "cp", "-r", from, to };
	try
	{
	    System.out.println(runCommand(cmdCP, "/"));
	} catch (IOException | InterruptedException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static ASTNode createBlockWithText(String str)
    {
	ASTParser parser = ASTParser.newParser(AST.JLS8);
	parser.setKind(ASTParser.K_STATEMENTS);
	Map pOptions = JavaCore.getOptions();
	pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
	pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
	pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
	parser.setCompilerOptions(pOptions);

	parser.setSource(str.toCharArray());
	ASTNode cu = parser.createAST(null);

	return cu;
    }

    public static ASTNode createExpWithText(String str)
    {
	ASTParser parser = ASTParser.newParser(AST.JLS8);
	parser.setKind(ASTParser.K_EXPRESSION);
	Map pOptions = JavaCore.getOptions();
	pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
	pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
	pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
	parser.setCompilerOptions(pOptions);

	parser.setSource(str.toCharArray());
	ASTNode cu = parser.createAST(null);

	return cu;
    }

    public static String runCommand(String[] commands, String path) throws IOException, InterruptedException
    {
	StringBuffer result = new StringBuffer();

	ProcessBuilder builder = new ProcessBuilder(commands);
	builder.redirectErrorStream(true);
	builder.directory(new File(path));
	Process process = builder.start();

	Scanner scInput = new Scanner(process.getInputStream());
	while (scInput.hasNext())
	{
	    String line = scInput.nextLine();
	    result.append(line);
	    result.append("\n");
	}
	process.waitFor();
	return result.toString();

    }

    public static Map<String, String> getClassFileMapping()
    {
	try
	{
	    XStream xstream = new XStream(new StaxDriver());
	    return (Map<String, String>) xstream.fromXML(new File(Settings.classFileMappingPath));
	} catch (Exception e)
	{
	    Settings.consoleLogger.error("class file mapping is missing !! did you run the instrumenter first ?");
	}
	return null;
    }

    public static String getClassFile(String className)
    {
	return Utils.classFileMapping.get(className);
    }

    public static String getClassFileForProjectPath(String className, String projectPath)
    {
	return Utils.classFileMapping.get(className).replace(Settings.PROJECT_PATH, projectPath);
    }

    public static String getTestCaseFile(String testCase)
    {
	String className = getTestClassNameFromTestCase(testCase);
	return Utils.classFileMapping.get(className);
    }

    public static String getTestClassNameFromTestCase(String testCase)
    {
	int index = testCase.lastIndexOf('.');
	String className = testCase.substring(0, index);
	return className;
    }

    public static <K, V> void addToTheListInMap(Map<K, List<V>> map, K key, V value)
    {
	List<V> list = map.get(key);
	if (list == null)
	{
	    list = new LinkedList<V>();
	    map.put(key, list);
	}
	list.add(value);
    }

    public static <K, V> void addToTheSetInMap(Map<K, Set<V>> map, K key, V value)
    {
	Set<V> list = map.get(key);
	if (list == null)
	{
	    list = new HashSet<V>();
	    map.put(key, list);
	}
	list.add(value);
    }
    
    public static <K, V> void addAllTheSetInMap(Map<K, Set<V>> map, K key, Set<V> value)
    {
	Set<V> list = map.get(key);
	if (list == null)
	{
	    list = new HashSet<V>();
	    map.put(key, list);
	}
	if (value == null)
	    return;
	list.addAll(value);
    }
    
    
    public static <K, V> boolean containsInSetInMap(Map<K, Set<V>> map, K key, V value)
    {
	Set<V> set = map.get(key);
	if (set == null)
	{
	    return false;
	}
	return set.contains(value);
	}

    public static <K1, K2, V> void addToTheMapInMap(Map<K1, Map<K2, V>> map, K1 key1, K2 key2, V value)
    {
	Map<K2, V> m = map.get(key1);
	if (m == null)
	{
	    m = new HashMap<K2, V>();
	    map.put(key1, m);
	}
	m.put(key2, value);
    }
    
    public static <K, V> void removeFromTheSetInMap(Map<K, Set<V>> map, K key, V value)
    {
	Set<V> list = map.get(key);
	if (list != null)
	{
	    list.remove(value);
	}
	else
	    map.remove(key);
    }
    

    public static String getTestCaseName(String testCase)
    {
	int index = testCase.lastIndexOf('.');
	return testCase.substring(index + 1);
    }

    public static String getTestCaseNameFromTestStatementWithoutClassName(String testStatement)
    {
	String testCase = getTestCaseNameFromTestStatement(testStatement);
	return testCase.substring(testCase.lastIndexOf('.') + 1);
    }

    public static String getTestCaseNameFromTestStatement(String testStatement)
    {
	int index = testStatement.lastIndexOf('-');
	if (index == -1)
	    return null;
	return testStatement.substring(0, index);
    }

    public static Map<String, Set<String>> getTestClassMapFromTestCases(Set<String> testCases)
    {
	Map<String, Set<String>> map = new HashMap<String, Set<String>>();
	for (String testCase : testCases)
	{
	    String testClass = Utils.getTestClassNameFromTestCase(testCase);
	    Utils.addToTheSetInMap(map, testClass, testCase);
	}
	return map;
    }

    public static String nextOrPrevState(List<String> testCases, String state, boolean next)
    {

	ArrayList<String> sortedTestStates;
	try
	{
	    sortedTestStates = FileUtils.sortedAllStates.get(testCases);
	    return nextOrPrevState(state, sortedTestStates, next);
	} catch (ExecutionException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return "";
	}
    }

    public static String nextOrPrevState(String state, ArrayList<String> sortedTestStatements, boolean next)
    {
//	int index = Collections.binarySearch(sortedTestStatements, state, new NaturalOrderComparator());
	int index = sortedTestStatements.indexOf(state);
	if (index == -1 || (next && index == sortedTestStatements.size() - 1) || (!next && index == 0))
	    return "";

	String[] split = Utils.splitState(state);

	if (split.length != 2)
	    return "";

	String nextState = sortedTestStatements.get(index + (next ? 1 : -1));
	String[] splitNext = Utils.splitState(nextState);

	if (splitNext.length != 2)
	    return "";

	if (split[0].equals(splitNext[0]))
	    return nextState;
	else
	    return "";
    }

    public static String[] splitState(String state)
    {
	state = state.substring(0, state.lastIndexOf('.'));
	String[] split = state.split("-");
	return split;
    }
    
    
    public static String getTestClassNameFromTestStatement(String testStatement)
    {
	int index = testStatement.indexOf('.');
	return testStatement.substring(0, index);
    }
    
    public static Set<String> getNames(Collection<MethodInvocation> methodCalls)
    {
	Set<String> nameSet = new HashSet<String>();
	for (MethodInvocation mi : methodCalls)
	{
	    nameSet.add(mi.getName().getIdentifier());
	}
	
	return nameSet;
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

    public static void writebackSourceCode(Document document, String newPath)
    {
	try
	{
	    FileWriter fw = new FileWriter(newPath);
	    fw.write(document.get());
	    fw.close();
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static void addImports(Document document, Collection<String> imports)
    {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        Map<String, String> pOptions = JavaCore.getOptions();
        pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
        pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
        pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
        parser.setCompilerOptions(pOptions);
    
        parser.setSource(document.get().toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
    
        addImports(document, imports, cu);
    
    }

    private static void addImports(Document document, Collection<String> imports, CompilationUnit cu)
    {
	cu.recordModifications();
    
        // String[] imports = new String[] { "java.io.FileWriter",
        // "java.io.IOException", "java.io.ObjectOutputStream",
        // "com.thoughtworks.xstream.XStream",
        // "com.thoughtworks.xstream.io.xml.StaxDriver" };
        for (String name : imports)
            addImport(cu, name);
    
        TextEdit edits = cu.rewrite(document, null);
    
        try
	{
	    edits.apply(document);
	} catch (MalformedTreeException | BadLocationException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    
    public static void addImport(CompilationUnit cu, String name)
    {
	AST ast = cu.getAST();
	ImportDeclaration imp = ast.newImportDeclaration();
	imp.setName(ast.newName(name));
	cu.imports().add(imp);
    }

    // could be improved later !
    public static String getTestClassWithMaxNumberOfTestCases(Map<String, Set<String>> map)
    {
        int max = Integer.MIN_VALUE;
        String maxTestClass = null;
        for (Entry<String, Set<String>> entry : map.entrySet())
        {
            if (max < entry.getValue().size())
            {
        	max = entry.getValue().size();
        	maxTestClass = entry.getKey();
            }
        }
    
        return maxTestClass;
    }


}
