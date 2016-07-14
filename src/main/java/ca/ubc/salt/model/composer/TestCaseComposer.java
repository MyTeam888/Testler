package ca.ubc.salt.model.composer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.Method;
import ca.ubc.salt.model.instrumenter.ProductionClassInstrumenter;
import ca.ubc.salt.model.state.StatementReadVariableVisitor;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class TestCaseComposer
{

    static LoadingCache<String, Map<String, String>> nameValuePairs;
    static
    {
	nameValuePairs = CacheBuilder.newBuilder().maximumSize(1000) // maximum
								     // 100
								     // records
								     // can be
								     // cached
		.build(new CacheLoader<String, Map<String, String>>()
		{ // build the cacheloader

		    @Override
		    public Map<String, String> load(String stmt) throws Exception
		    {
			// make the expensive call
			return FileUtils.getNameValuePairs(stmt);
		    }
		});
    }

    public static void updateRunningState(TestStatement statement, RunningState valueNamePairForCurrentState)
    {
	Set<SimpleName> vars = getAllVars(statement.statement);
	if (vars == null)
	    return;
	Set<String> varsName = Utils.getNameSet(vars);

	Map<String, String> nameValuePairOfStmtBefore = null;
	try
	{
	    nameValuePairOfStmtBefore = nameValuePairs.get(statement.getName());
	} catch (ExecutionException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	Map<String, String> renameMap = new HashMap<String, String>();

	findPreqVarsRenames(statement, valueNamePairForCurrentState, varsName, nameValuePairOfStmtBefore, renameMap);

	findPostreqVarsRenames(statement, valueNamePairForCurrentState, renameMap);

	valueNamePairForCurrentState.update(statement.getName(), renameMap, varsName);
    }

    public static Set<SimpleName> getAllVars(Statement statement)
    {
	if (statement == null)
	    return null;
	StatementVariableVisitor srvv = new StatementVariableVisitor();
	statement.accept(srvv);
	return srvv.getVars();

    }

    public static List<SimpleName> getSimpleNamesInTheStatement(Statement statement, Set<SimpleName> vars)
    {
	if (statement == null)
	    return null;
	StatementSimpleNameVisitor srvv = new StatementSimpleNameVisitor();
	statement.accept(srvv);
	Set<String> varNames = Utils.getNameSet(vars);
	List<SimpleName> refinedList = new LinkedList<SimpleName>();
	for (SimpleName sn : srvv.getVars())
	{
	    if (varNames.contains(sn.getIdentifier()))
		refinedList.add(sn);
	}

	return refinedList;

    }

    public static Statement rename(Statement stmt, Set<SimpleName> vars, Map<String, String> renameSet)
    {

	Statement cpyStmt = (Statement) ASTNode.copySubtree(stmt.getAST(), stmt);
	List<SimpleName> cpyVars = getSimpleNamesInTheStatement(cpyStmt, vars);
	for (SimpleName var : cpyVars)
	{
	    String renamedVar = renameSet.get(var.getIdentifier());
	    if (renamedVar != null)
	    {
		ASTNode node = var.getParent();
		if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT)
		{
		    VariableDeclarationFragment vdf = (VariableDeclarationFragment) node;
		    vdf.setName(vdf.getAST().newSimpleName(renamedVar));
		} else
		    var.setIdentifier(renamedVar);
	    }
	}

	return cpyStmt;
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

    public static void composeTestCase(List<TestStatement> path, Set<String> testCases, String name)
    {

	populateStateField(path);

	List<Statement> renamedStatements = performRenaming(path);

	try
	{
	    writeBackMergedTestCases(renamedStatements, testCases, name);
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	// StringBuilder sb = new StringBuilder();
	// for (TestStatement statement : path)
	// {
	// sb.append(statement.getName() + " : "
	// + (statement.statement == null ? "null" :
	// statement.statement.toString()));
	// sb.append('\n');
	// }
	// System.out.println(sb.toString());

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

    private static void writeBackMergedTestCases(List<Statement> path, Set<String> testCases, String name)
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
		     removeTestCasesFromTestClass(clazz, testCasesOfClass,
		     listRewrite);
		    //
		     addMergedTestCase(path, name, clazz, listRewrite);
		    System.out.println(getMergedMethod(path, name, clazz.getTypeDec().getAST()).toString());
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
	    // System.out.println(document.get());

	    testClasses.remove(testClassName);
	    // parse each test class
	    // remove and replace
	    // if they have variables var with isField() add TestClass x = new
	    // TestClass(); var -> x.var
	}
    }

    private static void addMergedTestCase(List<Statement> path, String name, ClassModel clazz, ListRewrite listRewrite)
    {
	TypeDeclaration td = clazz.getTypeDec();
	AST ast = td.getAST();
	MethodDeclaration md = getMergedMethod(path, name, ast);

	System.out.println(md.toString());
	// clazz.getTypeDec().bodyDeclarations().add(md);
	listRewrite.insertLast(md, null);
    }

    private static MethodDeclaration getMergedMethod(List<Statement> path, String name, AST ast)
    {
	MethodDeclaration md = ast.newMethodDeclaration();
	md.setName(ast.newSimpleName(name));

	MarkerAnnotation ma = ast.newMarkerAnnotation();
	ma.setTypeName(ast.newName("Test"));

	md.modifiers().add(ma);

	md.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

	ASTNode methodBlock = Utils.createBlockWithText(getTestMethodText(path));

	Block methodBlockWithAST = (Block) ASTNode.copySubtree(ast, methodBlock);

	md.setBody(methodBlockWithAST);
	return md;
    }

    public static String getTestMethodText(List<Statement> path)
    {
	StringBuilder sb = new StringBuilder();
	// sb.append(String.format("public void %s(){", name));
	for (Statement ts : path)
	{
	    if (ts != null)
	    {
		sb.append(ts.toString());
		sb.append('\n');
	    }
	}
	// sb.append("}");

	return sb.toString();

    }

    public static String getTestMethodText(List<TestStatement> path, int n)
    {
	StringBuilder sb = new StringBuilder();
	// sb.append(String.format("public void %s(){", name));
	for (int j = 0; j <= n; j++)
	{
	    TestStatement ts = path.get(j);
	    if (ts.statement != null)
	    {
		sb.append(ts.statement.toString());
		sb.append('\n');
	    }
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

    public static Statement renameTestStatement(TestStatement statement, RunningState valueNamePairForCurrentState)
    {
	Set<SimpleName> vars = getAllVars(statement.statement);
	if (vars == null)
	    return null;
	Set<String> varsName = Utils.getNameSet(vars);

	Map<String, String> nameValuePairOfStmtBefore = FileUtils.getNameValuePairs(statement.getName());
	Map<String, String> renameMap = new HashMap<String, String>();

	findPreqVarsRenames(statement, valueNamePairForCurrentState, varsName, nameValuePairOfStmtBefore, renameMap);

	findPostreqVarsRenames(statement, valueNamePairForCurrentState, renameMap);

	valueNamePairForCurrentState.update(statement.getName(), renameMap, varsName);

	return rename(statement.statement, vars, renameMap);
    }

    private static List<Statement> performRenaming(List<TestStatement> path)
    {

	List<Statement> renamedStatements = new LinkedList<Statement>();

	RunningState valueNamePairForCurrentState = new RunningState();
	for (TestStatement statement : path)
	{

	    if (statement.statement == null)
		continue;

	    Statement renamedStatement = renameTestStatement(statement, valueNamePairForCurrentState);
	    renamedStatements.add(renamedStatement);
	}

	return renamedStatements;
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

		String newName = newVarInStmt;
		int counter = 2;
		while (curState.getValue(newName) != null)
		{
		    newName = newVarInStmt + counter;
		    counter++;
		}
		renameMap.put(varInStmt, newName);
	    }
	}
    }

    private static void findPreqVarsRenames(TestStatement stmt, RunningState valueNamePairForCurrentState,
	    Set<String> varsName, Map<String, String> nameValuePairOfStmtBefore, Map<String, String> renameMap)
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
		Settings.consoleLogger.error(
			String.format("something's wrong with %s--%s", stmt.getName(), stmt.statement.toString()));
	    } else if (!varNameInState.equals(varNameInStmt))
	    {
		renameMap.put(varNameInStmt, varNameInState);
	    }
	}
    }

    public static void populateStateField(Collection<TestStatement> path)
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
