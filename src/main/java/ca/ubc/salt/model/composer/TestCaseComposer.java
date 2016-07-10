package ca.ubc.salt.model.composer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import Comparator.NaturalOrderComparator;
import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.Method;
import ca.ubc.salt.model.instrumenter.ProductionClassInstrumenter;
import ca.ubc.salt.model.state.StatementReadVariableVisitor;
import ca.ubc.salt.model.state.StatementVariableVisitor;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Pair;
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

    }

    public static String generateTestCaseName(Set<String> testCases)
    {
	StringBuilder sb = new StringBuilder();

	for (String testCase : testCases)
	{
	    sb.append(Utils.getTestCaseName(testCase));
	    sb.append('_');
	}
	sb.setLength(sb.length() - 1);
	return sb.toString();
    }

    public static void composeTestCases(List<Pair<Set<String>, List<List<TestStatement>>>> mergedTestCases)
    {
	
	Utils.copyProject(Settings.PROJECT_PATH, Settings.PROJECT_MERGED_PATH);
	
	for (Pair<Set<String>, List<List<TestStatement>>> pair : mergedTestCases)
	{
	    Set<String> connectedComponent = pair.getFirst();
	    List<List<TestStatement>> testCases = pair.getSecond();
	    String name = generateTestCaseName(connectedComponent);
	    if (testCases.size() == 1)
		composeTestCase(testCases.get(0), connectedComponent, name);
	    else
		for (int i = 0; i < testCases.size(); i++)
		{
		    composeTestCase(testCases.get(i), connectedComponent, name + i);
		}
	}
    }

    public static String composeTestCase(List<TestStatement> path, Set<String> testCases, String name)
    {

	populateStateField(path);

	performRenaming(path);

	try
	{
	    writeBackMergedTestCases(path, testCases, name);
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
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

    // could be improved later !
    private static String getTestClassWithMaxNumberOfTestCases(Map<String, Set<String>> map)
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

    private static void writeBackMergedTestCases(List<TestStatement> path, Set<String> testCases, String name)
	    throws IOException
    {
	Map<String, Set<String>> testClasses = Utils.getTestClassMapFromTestCases(testCases);

	while (!testClasses.isEmpty())
	{
	    String testClassName = getTestClassWithMaxNumberOfTestCases(testClasses);

	    String testClassPath = Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH);

	    String source = FileUtils.readFileToString(testClassPath);
	    Document document = new Document(source);
	    List<ClassModel> classes = ClassModel.getClasses(document.get());

	    Set<String> testCasesOfClass = testClasses.get(testClassName);

	    ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());

	    for (ClassModel clazz : classes)
	    {
		ListRewrite listRewrite = rewriter.getListRewrite(clazz.getTypeDec(),
			TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		if (clazz.getTypeDec().getName().toString().equals(testClassName))
		{
		    removeTestCasesFromTestClass(clazz, testCasesOfClass, listRewrite);

		    addMergedTestCase(path, name, clazz, listRewrite);
		}
	    }
	    TextEdit edits = rewriter.rewriteAST(document, null);
	    try
	    {
		edits.apply(document);
	    } catch (MalformedTreeException | BadLocationException e)
	    {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    Utils.writebackSourceCode(document, testClassPath);
	    System.out.println(document.get());

	    testClasses.remove(testClassName);
	    // parse each test class
	    // remove and replace
	    // if they have variables var with isField() add TestClass x = new
	    // TestClass(); var -> x.var
	}
    }

    private static void addMergedTestCase(List<TestStatement> path, String name, ClassModel clazz,
	    ListRewrite listRewrite)
    {
	TypeDeclaration td = clazz.getTypeDec();
	AST ast = td.getAST();
	MethodDeclaration md = ast.newMethodDeclaration();
	md.setName(ast.newSimpleName(name));

	MarkerAnnotation ma = ast.newMarkerAnnotation();
	ma.setTypeName(ast.newName("Test"));

	md.modifiers().add(ma);

	md.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

	ASTNode methodBlock = Utils.createBlockWithText(getTestMethodText(path));

	Block methodBlockWithAST = (Block) ASTNode.copySubtree(ast, methodBlock);

	md.setBody(methodBlockWithAST);

	// clazz.getTypeDec().bodyDeclarations().add(md);
	listRewrite.insertLast(md, null);
    }

    public static String getTestMethodText(List<TestStatement> path)
    {
	StringBuilder sb = new StringBuilder();
	// sb.append(String.format("public void %s(){", name));
	for (TestStatement ts : path)
	{
	    sb.append(ts.statement.toString());
	    sb.append('\n');
	}
	// sb.append("}");

	return sb.toString();

    }

    private static void removeTestCasesFromTestClass(ClassModel clazz, Set<String> testCasesOfClass,
	    ListRewrite listRewrite)
    {
	List<Method> methods = clazz.getMethods();

	for (Method m : methods)
	{
	    String methodName = m.getFullMethodName();
	    if (testCasesOfClass.contains(methodName))
	    {
		// clazz.getTypeDec().bodyDeclarations().remove(m.getMethodDec());
		listRewrite.remove(m.getMethodDec(), null);
	    }
	}

    }

    private static void performRenaming(List<TestStatement> path)
    {
	RunningState valueNamePairForCurrentState = new RunningState();
	for (TestStatement statement : path)
	{
	    Statement stmt = statement.statement;
	    if (statement.statement == null)
		continue;
	    Set<SimpleName> vars = getAllVars(statement);
	    Set<String> varsName = Utils.getNameSet(vars);

	    Map<String, String> nameValuePairOfStmtBefore = FileUtils.getNameValuePairs(statement.getName());
	    Map<String, String> renameMap = new HashMap<String, String>();

	    findPreqVarsRenames(valueNamePairForCurrentState, varsName, nameValuePairOfStmtBefore, renameMap);

	    findPostreqVarsRenames(statement, valueNamePairForCurrentState, renameMap);

	    valueNamePairForCurrentState.update(statement.getName(), renameMap, varsName);
	    rename(statement.statement, vars, renameMap);
	}
    }

    private static void findPostreqVarsRenames(TestStatement testStatement, RunningState curState,
	    Map<String, String> renameMap)
    {
	Statement stmt = testStatement.statement;
	StatementDefineVariableVisitor sdvv = new StatementDefineVariableVisitor();
	stmt.accept(sdvv);
	Set<SimpleName> vars = sdvv.getVars();
	for (SimpleName var : vars)
	{
	    String varInStmt = var.getIdentifier();
	    String varInState = curState.getValue(varInStmt);
	    if (varInState != null)
	    {
		String newVarInStmt = varInStmt + "_"
			+ Utils.getTestCaseNameFromTestStatementWithoutClassName(testStatement.getName());
		renameMap.put(varInStmt, newVarInStmt);
	    }
	}
    }

    private static void findPreqVarsRenames(RunningState valueNamePairForCurrentState, Set<String> varsName,
	    Map<String, String> nameValuePairOfStmtBefore, Map<String, String> renameMap)
    {
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
}
