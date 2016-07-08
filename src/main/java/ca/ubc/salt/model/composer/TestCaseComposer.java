package ca.ubc.salt.model.composer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jface.text.Document;

import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.Method;
import ca.ubc.salt.model.state.StatementReadVariableVisitor;
import ca.ubc.salt.model.state.StatementVariableVisitor;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class TestCaseComposer
{

    public static Set<SimpleName> getAllVars(TestStatement statement)
    {
	if (statement.statement == null)
	    return null;
	StatementVariableVisitor srvv = new StatementVariableVisitor();
	statement.statement.accept(srvv);
	return srvv.getVars();

    }

    public static void rename(Statement stmt, Set<SimpleName> vars, Map<String, String> renameSet)
    {
	for (SimpleName var : vars)
	{
	    String renamedVar = renameSet.get(var.getIdentifier());
	    if (renamedVar != null)
		var.setIdentifier(renamedVar);
	}

	System.out.println(stmt);
    }

    public static String composeTestCase(List<TestStatement> path)
    {

	populateStateField(path);

	RunningState valueNamePairForCurrentState = new RunningState();
	for (TestStatement statement : path)
	{
	    if (statement.statement == null)
		continue;
	    Set<SimpleName> vars = getAllVars(statement);
	    Set<String> varsName = Utils.getNameSet(vars);

	    Map<String, String> nameValuePairOfStmtBefore = FileUtils.getNameValuePairs(statement.getName());
	    Map<String, String> renameMap = new HashMap<String, String>();
	    for (Entry<String, String> entry : nameValuePairOfStmtBefore.entrySet())
	    {
		String varNameInStmt = entry.getKey();
		if (!varsName.contains(varNameInStmt))
		    continue;

		String value = entry.getValue();

		String varNameInState = valueNamePairForCurrentState.getName(value);
		if (varNameInState == null)
		{
		    valueNamePairForCurrentState.put(value, varNameInStmt);
		} else if (!varNameInState.equals(varNameInStmt))
		{
		    renameMap.put(varNameInStmt, varNameInState);
		}
	    }

	    valueNamePairForCurrentState.update(statement.getName(), renameMap, varsName);
	    rename(statement.statement, vars, renameMap);
	}

	StringBuilder sb = new StringBuilder();
	for (TestStatement statement : path)
	{
	    sb.append(statement.getName() + "  :  "
		    + (statement.statement == null ? "null" : statement.statement.toString()));
	    sb.append('\n');
	}

	return sb.toString();
    }

    public static void populateStateField(List<TestStatement> path)
    {
	// file path to list of statements
	Map<String, List<TestStatement>> fileTestStatementMapping = new HashMap<String, List<TestStatement>>();
	for (TestStatement stmt : path)
	{

	    String testCaseStr = getTestCaseName(stmt);
	    String filePath = Utils.getTestCaseFile(testCaseStr);
	    Utils.addToTheListInMap(fileTestStatementMapping, filePath, stmt);
	}

	for (Entry<String, List<TestStatement>> entry : fileTestStatementMapping.entrySet())
	{
	    String filePath = entry.getKey();
	    List<TestStatement> statements = entry.getValue();

	    populateForFile(filePath, statements);
	}

    }

    private static String getTestCaseName(TestStatement stmt)
    {
	String stmtStr = stmt.getName();
	int index = stmtStr.lastIndexOf('-');
	String testCaseStr = stmtStr.substring(0, index);
	return testCaseStr;
    }

    public static void populateForFile(String filePath, List<TestStatement> statements)
    {
	try
	{
	    String source = FileUtils.readFileToString(filePath);
	    Document document = new Document(source);
	    List<ClassModel> classes = ClassModel.getClasses(document.get(), true, filePath,
		    new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });

	    for (TestStatement stmt : statements)
	    {
		String stmtMethodName = Utils.getTestCaseName(Utils.getTestCaseNameFromTestStatement(stmt.getName()));
		for (ClassModel clazz : classes)
		{
		    List<Method> methods = clazz.getMethods();

		    for (Method m : methods)
		    {

			String methodName = m.getMethodDec().getName().toString();
			if (methodName.equals(stmtMethodName))
			{

			    List stmts = m.getMethodDec().getBody().statements();
			    int index = getTestStatementNumber(stmt.getName());
			    if (0 <= index && index < stmts.size())
				stmt.statement = (Statement) stmts.get(index);
			    break;
			}
		    }
		}
	    }

	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static int getTestStatementNumber(String xmlTestStatementStr)
    {
	int index = xmlTestStatementStr.lastIndexOf('-');
	int endIndex = xmlTestStatementStr.lastIndexOf('.');
	return Integer.valueOf(xmlTestStatementStr.substring(index + 1, endIndex));
    }

}

class RunningState
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
	String nextState = Utils.nextOrPrevState(prevState,
		Arrays.asList(FileUtils.getStatesForTestCase(Utils.getTestCaseNameFromTestStatement(prevState))), true);
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
}
