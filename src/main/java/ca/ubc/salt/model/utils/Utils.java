package ca.ubc.salt.model.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class Utils
{
    
    public static Map<String, String> classFileMapping;
    static {
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

    
    
    public static void copyProject()
    {
	
//	String[] cmdRM = new String[]{"rm", "-r", Settings.PROJECT_INSTRUMENTED_PATH};
	String[] cmdCP = new String[]{"cp","-r", Settings.PROJECT_PATH, Settings.PROJECT_INSTRUMENTED_PATH};
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
	XStream xstream = new XStream(new StaxDriver());
	return (Map<String, String>) xstream.fromXML(new File(Settings.classFileMappingPath));
    }


    public static String getTestCaseFile(String testCase)
    {
        int index = testCase.lastIndexOf('.');
        String className = testCase.substring(0, index);
        return Utils.classFileMapping.get(className);
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


    public static String getTestCaseName(String testCase)
    {
        int index = testCase.lastIndexOf('.');
        return testCase.substring(index + 1);
    }

    public static String getTestCaseNameFromTestStatement(String testStatement)
    {
        int index = testStatement.lastIndexOf('-');
        return testStatement.substring(0, index);
    }


    public static String nextOrPrevState(String state, List<String> sortedTestCases, boolean next)
    {
        int index = sortedTestCases.indexOf(state);
        if (index == -1 || (next && index == sortedTestCases.size() - 1) || (!next && index == 0))
            return "";
    
        String[] split = Utils.splitState(state);
    
        if (split.length != 2)
            return "";
    
        String nextState = sortedTestCases.get(index + (next ? 1 : -1));
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

    
}
