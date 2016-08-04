package ca.ubc.salt.model.composer;

import java.beans.Expression;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
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
import ca.ubc.salt.model.merger.BackwardTestMerger;
import ca.ubc.salt.model.state.StatementReadVariableVisitor;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.state.VarDefinitionPreq;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class TestCaseComposer
{

    public static Map<String, Set<String>> addedMethods = new HashMap<String, Set<String>>();
    public static LoadingCache<String, Map<String, String>> nameValuePairs;
    static
    {
	nameValuePairs = CacheBuilder.newBuilder().maximumSize(1000)
		.build(new CacheLoader<String, Map<String, String>>()
		{ // build
		  // the
		  // cacheloader

		    @Override
		    public Map<String, String> load(String stmt) throws Exception
		    {
			// make the expensive call
			return FileUtils.getNameValuePairs(stmt);
		    }
		});
    }

    public static void updateRunningState(TestStatement statement, RunningState valueNamePairForCurrentState,
	    Map<String, Map<String, String>> readVals, Map<String, Set<VarDefinitionPreq>> definitionPreq,
	    Map<String, String> batchRename)
    {

	Map<String, String> renameMap = new HashMap<String, String>();

	findPreqVarsRenames(statement, valueNamePairForCurrentState, renameMap, readVals, definitionPreq, batchRename);

	findPostreqVarsRenames(statement, valueNamePairForCurrentState, renameMap);

	valueNamePairForCurrentState.update(statement, renameMap, definitionPreq);
	batchRename.putAll(renameMap);
    }

    public static Map<String, SimpleName> getAllVars(Statement statement)
    {
	if (statement == null)
	    return null;
	StatementVariableVisitor srvv = new StatementVariableVisitor();
	statement.accept(srvv);
	return srvv.getVars();

    }

    public static Map<String, SimpleName> getAllRightHandSideVars(Statement statement)
    {
	if (statement == null)
	    return null;
	StatementVariableVisitor srvv = new StatementVariableVisitor();
	if (statement.getNodeType() == ASTNode.EXPRESSION_STATEMENT)
	{
	    ExpressionStatement exp = (ExpressionStatement) statement;
	    org.eclipse.jdt.core.dom.Expression e = exp.getExpression();
	    if (e instanceof Assignment)
	    {
		Assignment a = (Assignment) e;
		a.getRightHandSide().accept(srvv);
	    } else
		statement.accept(srvv);
	} else
	    statement.accept(srvv);
	return srvv.getVars();

    }

    public static List<SimpleName> getSimpleNamesInTheStatement(ASTNode statement, Collection<SimpleName> vars)
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

    public static ASTNode rename(ASTNode stmt, Map<String, SimpleName> vars, Map<String, String> renameSet)
    {

	ASTNode cpyStmt = ASTNode.copySubtree(stmt.getAST(), stmt);
	List<SimpleName> cpyVars = getSimpleNamesInTheStatement(cpyStmt, vars.values());
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

    // public static void composeTestCases(List<Pair<Set<String>,
    // List<List<TestStatement>>>> mergedTestCases)
    // {
    //
    // Utils.copyProject(Settings.PROJECT_PATH, Settings.PROJECT_MERGED_PATH);
    //
    // for (Pair<Set<String>, List<List<TestStatement>>> pair : mergedTestCases)
    // {
    // Set<String> connectedComponent = pair.getFirst();
    // List<List<TestStatement>> testCases = pair.getSecond();
    // String name = generateTestCaseName(connectedComponent);
    // if (testCases.size() == 1)
    // composeTestCase(testCases.get(0), connectedComponent, name);
    // else
    // for (int i = 0; i < testCases.size(); i++)
    // {
    // composeTestCase(testCases.get(i), connectedComponent, name + i);
    // }
    // }
    // }

    public static void composeTestCase(List<TestStatement> path, Set<String> testCases, String name,
	    Map<String, Map<String, String>> readVals, Map<String, Set<VarDefinitionPreq>> definitionPreq, List<TestStatement> additionalStmts)
    {

	// populateStateField(path);

	Map<String, Set<String>> testClasses = Utils.getTestClassMapFromTestCases(testCases);

	String mainClassName = Utils.getTestClassWithMaxNumberOfTestCases(testClasses);

	List<TestStatement> renamedStatements = performRenaming(path, testCases, mainClassName, readVals,
		definitionPreq);
	renamedStatements.addAll(additionalStmts);

	try
	{
	    writeBackMergedTestCases(renamedStatements, testCases, name, testClasses, mainClassName);
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

    private static List<FieldDeclaration> getRequiredFieldDecs(List<TestStatement> path, String mainClassName)
    {

	List<FieldDeclaration> fieldDecStatements = new ArrayList<FieldDeclaration>();

	Map<String, Set<String>> fieldVars = new HashMap<String, Set<String>>();
	for (TestStatement statement : path)
	{
	    Statement stmt = statement.statement;
	    StatementFieldVisitor sfv = new StatementFieldVisitor(fieldVars);
	    stmt.accept(sfv);
	    // renameFieldVarsInStmt(statement, sfv);
	}

	for (Entry<String, Set<String>> entry : fieldVars.entrySet())
	{
	    String className = entry.getKey();

	    if (className.equals(mainClassName))
		continue;

	    Set<String> usedFields = entry.getValue();

	    String testClassPath = Utils.getClassFile(className);

	    if (testClassPath == null)
		continue;

	    String source;
	    try
	    {
		source = FileUtils.readFileToString(testClassPath);
		Document document = new Document(source);
		List<ClassModel> classes = ClassModel.getClasses(document.get(), true, testClassPath,
			new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });
		for (ClassModel clazz : classes)
		{
		    List<FieldDeclaration> classFields = clazz.getAllFields();
		    for (FieldDeclaration fieldDec : classFields)
		    {
			for (Object varDecObj : fieldDec.fragments())
			{
			    if (varDecObj instanceof VariableDeclarationFragment)
			    {
				VariableDeclarationFragment varDec = (VariableDeclarationFragment) varDecObj;
				SimpleName varInFieldDec = varDec.getName();
				if (usedFields.contains(varInFieldDec.getIdentifier()))
				{
				    Map<String, SimpleName> varSet = new HashMap<String, SimpleName>();
				    varSet.put(varInFieldDec.getIdentifier(), varInFieldDec);
				    Map<String, String> renameMap = new HashMap<String, String>();
				    renameMap.put(varInFieldDec.getIdentifier(),
					    varInFieldDec.getIdentifier() + "_" + className);
				    fieldDecStatements.add((FieldDeclaration) rename(fieldDec, varSet, renameMap));
				}

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

	return fieldDecStatements;
    }

    private static void renameFieldVarsInStmt(TestStatement statement, StatementFieldVisitor sfv)
    {
	HashMap<String, String> renameMap = new HashMap<String, String>();
	for (SimpleName sn : sfv.fields.values())
	{
	    renameMap.put(sn.getIdentifier(), sn.getIdentifier() + "_" + sfv.className);
	}

	Statement renamedStmt = (Statement) rename(statement.refactoredStatement, sfv.fields, renameMap);
	statement.refactoredStatement = renamedStmt;
    }

    private static void addFieldDecsToPath(List<FieldDeclaration> fieldDecs, List<ASTNode> path)
    {
	for (FieldDeclaration fieldDec : fieldDecs)
	{
	    List mds = fieldDec.modifiers();
	    for (Iterator iterator = mds.iterator(); iterator.hasNext();)
	    {
		Object obj = iterator.next();
		if (obj instanceof Modifier)
		{
		    Modifier mod = (Modifier) obj;
		    if(!mod.isFinal())
			iterator.remove();
			
		}
	    }
	    path.add(0, fieldDec);
	}
    }

    public static void getNonTestMethods(ClassModel clazz, Set<Method> testMethods)
    {
	List<Method> methods = clazz.getMethods();
	for (Method method : methods)
	{
	    if (!method.isTestMethod())
		testMethods.add(method);
	}
    }

    public static List<MethodInvocation> getTestMethodInvocations(TestStatement stmt)
    {
	TestMethodInvocationVisitor smiv = new TestMethodInvocationVisitor(
		Utils.getTestClassNameFromTestStatement(stmt.getName()));
	stmt.statement.accept(smiv);
	return smiv.getMethodInvocations();
    }

    public static List<MethodInvocation> getMethodInvocations(Statement stmt)
    {
	MethodInvocationVisitor smiv = new MethodInvocationVisitor();
	stmt.accept(smiv);
	return smiv.getMethodInvocations();
    }

    public static Statement renameMethodInvocs(TestStatement stmt)
    {
	Statement cpyStmt = (Statement) ASTNode.copySubtree(stmt.refactoredStatement.getAST(),
		stmt.refactoredStatement);
	List<MethodInvocation> methodInvocs = getMethodInvocations(cpyStmt);
	List<MethodInvocation> testMethodInvocation = getTestMethodInvocations(stmt);
	Set<String> testMethodNames = Utils.getNames(testMethodInvocation);
	for (MethodInvocation methodCall : methodInvocs)
	{
	    if (testMethodNames.contains(methodCall.getName().getIdentifier()))
	    {
		String renamedVar = methodCall.getName().getIdentifier() + "_"
			+ Utils.getTestClassNameFromTestStatement(stmt.getName());
		methodCall.setName(methodCall.getAST().newSimpleName(renamedVar));
	    }
	}
	return cpyStmt;
    }

    public static void renameMethodCalls(List<TestStatement> originalStatements, String mainClass)
    {
	for (TestStatement statement : originalStatements)
	{
	    if (statement.statement == null)
		continue;
	    if (!mainClass.equals(Utils.getTestClassNameFromTestStatement(statement.getName())))
		statement.refactoredStatement = renameMethodInvocs(statement);
	}
    }

    public static List<ASTNode> getPathFromStatements(List<TestStatement> statements)
    {
	List<ASTNode> path = new ArrayList<ASTNode>();
	for (TestStatement stmt : statements)
	{
	    path.add(stmt.refactoredStatement);
	}
	return path;
    }

    
    private static void writeBackMergedTestCases(List<TestStatement> originalStatements, Set<String> testCases,
	    String name, Map<String, Set<String>> testClasses, String mainClassName) throws IOException
    {

	// class -> testCases
	renameMethodCalls(originalStatements, mainClassName);

	List<FieldDeclaration> fieldDecs = getRequiredFieldDecs(originalStatements, mainClassName);
	List<ASTNode> path = getPathFromStatements(originalStatements);
	addFieldDecsToPath(fieldDecs, path);

	Set<String> imports = new HashSet<String>();

	Set<Method> nonTestMethods = new HashSet<Method>();

	while (!testClasses.isEmpty())
	{

	    String testClassName = Utils.getTestClassWithMaxNumberOfTestCases(testClasses);

	    Document document = getDocumentForClassName(testClassName);
	    List<ClassModel> classes = ClassModel.getClasses(document.get());

	    Set<String> testCasesOfClass = testClasses.get(testClassName);

	    ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());

	    for (ClassModel clazz : classes)
	    {
		if (clazz.getTypeDec().getName().toString().equals(testClassName))
		{

		    ListRewrite listRewrite = rewriter.getListRewrite(clazz.getTypeDec(),
			    TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		    removeTestCasesFromTestClass(clazz, testCasesOfClass, listRewrite);

		    if (testClassName.equals(mainClassName))
		    {
			Settings.consoleLogger
				.error(String.format("adding tests to %s", Utils.getClassFile(mainClassName)));
			addMergedTestCase(path, name, clazz, listRewrite);
			// System.out.println(getMergedMethod(path, name,
			// clazz.getTypeDec().getAST()).toString());
		    } else
		    {
			getNonTestMethods(clazz, nonTestMethods);
			// adding other imports
			getAllImports(imports, clazz);
		    }

		}
	    }
	    TextEdit edits = rewriter.rewriteAST(document, null);
	    try
	    {
		edits.apply(document);
	    } catch (MalformedTreeException | BadLocationException e)
	    {
		e.printStackTrace();
	    }

	    Utils.writebackSourceCode(document,
		    Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH));
	    // System.out.println(document.get());

	    testClasses.remove(testClassName);
	    // parse each test class
	    // remove and replace
	    // if they have variables var with isField() add TestClass x = new
	    // TestClass(); var -> x.var
	}

	addImportsAndNonTestMethodsToMainClass(nonTestMethods, mainClassName, imports);
    }

    private static void getAllImports(Set<String> imports, ClassModel clazz)
    {
	for (Object obj : clazz.getCu().imports())
	{
	    if (obj instanceof ImportDeclaration)
	    {
		ImportDeclaration imDec = (ImportDeclaration) obj;
		imports.add(imDec.getName().getFullyQualifiedName());
	    }
	}
    }

    private static void addNonTestMethods(Set<Method> nonTestMethods, Document document, String mainClassName)
    {
	try
	{
	    List<ClassModel> classes = ClassModel.getClasses(document.get());
	    ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());
	    for (ClassModel clazz : classes)
	    {
		if (clazz.getTypeDec().getName().toString().equals(mainClassName))
		{
		    ListRewrite listRewrite = rewriter.getListRewrite(clazz.getTypeDec(),
			    TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		    List bodyDecs = clazz.getTypeDec().bodyDeclarations();

		    for (Method nonTestMethod : nonTestMethods)
		    {
			String newName = nonTestMethod.getMethodDec().getName().toString() + "_"
				+ nonTestMethod.getClassName();
			String signature = newName + "(" + nonTestMethod.getMethodDec().parameters().toString() + ")";
			if (Utils.containsInSetInMap(addedMethods, mainClassName, signature))
			    continue;
			AST ast = clazz.getTypeDec().getAST();
			MethodDeclaration methodCpy = (MethodDeclaration) ASTNode.copySubtree(ast,
				nonTestMethod.getMethodDec());
			methodCpy.setName(ast.newSimpleName(newName));

			listRewrite.insertLast(methodCpy, null);
			Utils.addToTheSetInMap(addedMethods, mainClassName, signature);
		    }
		}
	    }
	    TextEdit edits = rewriter.rewriteAST(document, null);
	    edits.apply(document);

	} catch (IOException | MalformedTreeException | BadLocationException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    private static void addImportsAndNonTestMethodsToMainClass(Set<Method> nonTestMethods, String mainClassName,
	    Set<String> imports) throws IOException
    {
	Document document = getDocumentForClassName(mainClassName);
	addNonTestMethods(nonTestMethods, document, mainClassName);
	Utils.addImports(document, imports);
	Utils.writebackSourceCode(document,
		Utils.getClassFileForProjectPath(mainClassName, Settings.PROJECT_MERGED_PATH));
    }

    private static Document getDocumentForClassName(String testClassName) throws IOException
    {
	String testClassPath = Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH);

	String source = FileUtils.readFileToString(testClassPath);
	Document document = new Document(source);
	return document;
    }

    private static void addMergedTestCase(List<ASTNode> path, String name, ClassModel clazz, ListRewrite listRewrite)
    {
	TypeDeclaration td = clazz.getTypeDec();
	AST ast = td.getAST();
	MethodDeclaration md = getMergedMethod(path, name, ast);

	// System.out.println(md.toString());
	// clazz.getTypeDec().bodyDeclarations().add(md);
	listRewrite.insertLast(md, null);
    }

    private static MethodDeclaration getMergedMethod(List<ASTNode> path, String name, AST ast)
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

    public static String getTestMethodText(List<ASTNode> path)
    {
	StringBuilder sb = new StringBuilder();
	// sb.append(String.format("public void %s(){", name));
	for (ASTNode ts : path)
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
	// Settings.consoleLogger.error(String.format("removing %s from %s",
	// testCasesOfClass, clazz.getTypeDec().getName().toString()));
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

    public static Statement renameTestStatement(TestStatement statement, RunningState valueNamePairForCurrentState,
	    Map<String, Map<String, String>> readVals, Map<String, Set<VarDefinitionPreq>> definitionPreq,
	    Map<String, String> batchRename)
    {
	Map<String, String> renameMap;
	if (statement.renameMap == null)
	    renameMap = new HashMap<String, String>();
	else
	    renameMap = statement.renameMap;

	findPreqVarsRenames(statement, valueNamePairForCurrentState, renameMap, readVals, definitionPreq, batchRename);

	findPostreqVarsRenames(statement, valueNamePairForCurrentState, renameMap);

	Map<String, SimpleName> vars = statement.getAllVars();
	if (vars == null)
	    return null;

	valueNamePairForCurrentState.update(statement, renameMap, definitionPreq);

	batchRename.putAll(renameMap);

	return (Statement) rename(statement.statement, vars, renameMap);
    }

    private static List<TestStatement> performRenaming(List<TestStatement> path, Set<String> testCases,
	    String mainClassName, Map<String, Map<String, String>> readVals,
	    Map<String, Set<VarDefinitionPreq>> definitionPreq)
    {

	RunningState runningState = new RunningState(testCases, mainClassName);

	List<TestStatement> renamedStatements = cloneStatements(path);

	Map<String, String> batchRename = new HashMap<String, String>();
	BackwardTestMerger.populateGoalsInStatements(definitionPreq, readVals, runningState, renamedStatements);
	for (TestStatement statement : renamedStatements)
	{
	    Statement renamedStatement = renameTestStatement(statement, runningState, readVals,
		    definitionPreq, batchRename);
	    statement.refactoredStatement = renamedStatement;
	}

	return renamedStatements;
    }
    public static List<TestStatement> performRenamingWithRunningState(List<TestStatement> path, Set<String> testCases,
	    String mainClassName, Map<String, Map<String, String>> readVals,
	    Map<String, Set<VarDefinitionPreq>> definitionPreq, RunningState runningState)
    {
	
	List<TestStatement> renamedStatements = cloneStatements(path);
	
	Map<String, String> batchRename = new HashMap<String, String>();
	BackwardTestMerger.populateGoalsInStatements(definitionPreq, readVals, runningState, renamedStatements);
	for (TestStatement statement : renamedStatements)
	{
	    Statement renamedStatement = renameTestStatement(statement, runningState, readVals,
		    definitionPreq, batchRename);
	    statement.refactoredStatement = renamedStatement;
	}
	
	return renamedStatements;
    }

    public static List<TestStatement> cloneStatements(List<TestStatement> path)
    {
	List<TestStatement> renamedStatements = new ArrayList<TestStatement>();
	for (TestStatement statement : path)
	{

	    if (statement.statement == null)
		continue;

	    TestStatement cpyStatement = null;
	    try
	    {
		cpyStatement = statement.clone();
	    } catch (CloneNotSupportedException e)
	    {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    renamedStatements.add(cpyStatement);
	}
	return renamedStatements;
    }

    private static void findPostreqVarsRenames(TestStatement testStatement, RunningState curState,
	    Map<String, String> renameMap)
    {
	if (testStatement.statement == null)
	    return;
	Statement stmt = testStatement.statement;
	StatementDefineVariableVisitor sdvv = new StatementDefineVariableVisitor();
	stmt.accept(sdvv);
	Set<SimpleName> vars = sdvv.getVars();
	for (SimpleName var : vars)
	{
	    String varInStmt = var.getIdentifier();
	    // if (renameMap.containsKey(varInStmt))
	    // varInStmt = renameMap.get(varInStmt);
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

    private static void findPreqVarsRenames(TestStatement stmt, RunningState runningState,
	    Map<String, String> renameMap, Map<String, Map<String, String>> readVals,
	    Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, String> batchRename)
    {
	// if
	// (stmt.statement.toString().contains("Assert.assertTrue(mColumn3[0]"))
	// System.out.println();
	Map<String, String> nameValuePairOfStmtBefore = readVals.get(stmt.getName());

	if (nameValuePairOfStmtBefore == null)
	    return;

	Set<String> chosenNames = new HashSet<String>();
	for (Entry<String, String> entry : nameValuePairOfStmtBefore.entrySet())
	{

	    String varNameInStmt = entry.getKey();

	    if (renameMap.containsKey(varNameInStmt))
		continue;
	    String value = entry.getValue();

	    Set<String> varNameInState = runningState.getName(value);
	    if (varNameInState == null || varNameInState.size() == 0)
	    {
		Settings.consoleLogger.error(
			String.format("something's wrong with %s--%s", stmt.getName(), stmt.statement.toString()));
	    } else if (!varNameInState.contains(varNameInStmt))
	    {
		// TODO for choosing the varname do a edit distance and choose
		// the most simillar one
		boolean success = false;
		for (String name : varNameInState)
		{
		    if (!chosenNames.contains(name))
		    {
			renameMap.put(varNameInStmt, name);
			chosenNames.add(name);
			success = true;
			break;
		    }
		}
		if (success != true)
		{
		    Settings.consoleLogger.error(
			    String.format("something's wrong with %s--%s", stmt.getName(), stmt.statement.toString()));
		}
	    } else
	    {
		chosenNames.add(varNameInStmt);
	    }
	}

	Set<VarDefinitionPreq> defPreqs = definitionPreq.get(stmt.getName());
	if (defPreqs != null)
	{
	    for (VarDefinitionPreq defPreq : defPreqs)
	    {
		String neededType = defPreq.getType();
		Set<String> varsInState = runningState.getNameForType(neededType);
		if (varsInState == null || varsInState.isEmpty())
		    Settings.consoleLogger.error(
			    String.format("something's wrong with %s--%s", stmt.getName(), stmt.statement.toString()));
		else
		{
		    if (renameMap.containsKey(defPreq.getName().getIdentifier()))
			continue;
		    if (stmt.readGoals != null)
		    {
			boolean renamed = false;
			for (String varName : varsInState)
			{
			    String value = runningState.getValue(varName);
			    Set<String> neededVals = stmt.readGoals.get(value);
			    Set<String> namesWithThisValue = runningState.getName(value);
			    if (neededVals == null || neededVals.size() <= namesWithThisValue.size() - 1)
			    {
				renameMap.put(defPreq.getName().getIdentifier(), varName);
				renamed = true;
				break;
			    }
			}

			if (renamed == false)
			{
			    Settings.consoleLogger.error(String.format("renaming happend for  %s--%s", stmt.getName(),
				    stmt.statement.toString()));
			}
		    } else
		    {

			String renamedName = renameMap.get(defPreq.getName().getIdentifier());
			if (renamedName == null)
			{
			    renamedName = defPreq.getName().getIdentifier();
			}

			if ((runningState.getValue(renamedName) == null
				|| !runningState.getType(renamedName).equals(neededType))
				|| !batchRename.containsValue(renamedName))
			{
			    boolean renamed = false;
			    for (String var : varsInState)
			    {
				if (!batchRename.containsValue(var))
				{
				    renamed = true;
				    renameMap.put(defPreq.getName().getIdentifier(), var);
				    break;
				}

			    }
			    if (renamed == false)
			    {
				if (batchRename.containsKey(renamedName))
				    renameMap.put(renamedName, batchRename.get(renamedName));
				else
				    Settings.consoleLogger.error(String.format("renaming happend for  %s--%s",
					    stmt.getName(), stmt.statement.toString()));

			    }
			}
		    }
		}

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
			    // if (methodName.equals("testSetSubMatrix") &&
			    // filePath.contains("Array2DRowRealMatrixTest"))
			    // System.out.println();
			    List stmts = m.getMethodDec().getBody().statements();
			    int index = getTestStatementNumber(stmt.getName());
			    if (0 <= index && index < stmts.size())
				stmt.statement = (Statement) stmts.get(index);
			    // StatementNumberingVisitor snv = new
			    // StatementNumberingVisitor();
			    // m.getMethodDec().accept(snv);
			    // if (0 <= index && index < snv.statements.size())
			    // stmt.statement = snv.statements.get(index);
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
