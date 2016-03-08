package ca.ubc.salt.model.assertion;

import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.stmt.AssertStmt;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.visitor.ModifierVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TestSuiteComposer
{
    ArrayList<TestMethod> methods;

    private static HashSet<Integer> assertionsToRemove;
    public static int serialNumber;
    public static int totalPossibleNumberOfAssertions;
    private File testsContainer;
    private int numberOfAssertionsToInclude;
    private HashSet<Integer> tryStatements;
    static public ArrayList<Integer> removedAssertionLines = new ArrayList<Integer>();

    public TestSuiteComposer(File testsContainer, int numberOfAssertionsToInclude) throws ParseException, IOException
    {
	this.testsContainer = testsContainer;
	this.numberOfAssertionsToInclude = numberOfAssertionsToInclude;
	this.setupEnviroment();
    }

    public TestSuiteComposer(File testsContainer) throws ParseException, IOException
    {
	this.testsContainer = testsContainer;
	this.setupEnviroment();
    }

    private void setupEnviroment() throws ParseException, IOException
    {
	this.serialNumber = 0;
	this.assertionsToRemove = new HashSet<Integer>();
	TestMethodsParser parser = new TestMethodsParser(this.testsContainer);
	this.totalPossibleNumberOfAssertions = 1;
	this.tryStatements = parser.getTryStatements();
    }

    // return false when there is a conflict between bucket number and number of
    // test methods to include
    public boolean composeRandomly() throws ParseException, IOException
    {
	this.randomPickAssertions();
	this.parseAndModify(this.testsContainer);
	return true;
    }

    public boolean compose(HashSet<Integer> assertionsToRemove) throws ParseException, IOException
    {
	serialNumber = 0;
	removedAssertionLines.clear();
	this.assertionsToRemove = assertionsToRemove;
	this.parseAndModify(this.testsContainer);
	return true;
    }

    private void randomPickAssertions()
    {
	while (this.assertionsToRemove.size() < this.numberOfAssertionsToInclude)
	{
	    int randomSerialNumber = (int) (Math.random() * (this.totalPossibleNumberOfAssertions));
	    if (!this.assertionsToRemove.contains(randomSerialNumber)
		    && !this.tryStatements.contains(randomSerialNumber))
	    {
		assertionsToRemove.add(randomSerialNumber);
	    }
	}
    }

    private void parseAndModify(File file) throws ParseException, IOException
    {
	if (file.isFile())
	{
	    String filename = file.getName();
	    if (filename.contains(".java") && filename.contains("Test"))
	    {
		FileInputStream in = new FileInputStream(file.getPath());
		CompilationUnit cu;
		try
		{
		    // parse the file
		    cu = JavaParser.parse(in);
		} finally
		{
		    in.close();
		}
		new MethodVisitor().visit(cu, null);
		PrintWriter out = new PrintWriter(file);
		out.write(cu.toString());
		out.flush();
	    }
	} else if (file.isDirectory())
	{
	    File[] listOfFiles = file.listFiles();
	    for (int i = 0; i < listOfFiles.length; i++)
	    {
		parseAndModify(listOfFiles[i]);
	    }
	}
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class MethodVisitor extends ModifierVisitorAdapter
    {

	
	@Override
	public Node visit(ExpressionStmt n, Object arg)
	{
	    if(isAssertion(n) )
	    {
		if (assertionsToRemove.contains(serialNumber))
		{
		    System.out.println(n.toString());
		    serialNumber++;
		    
		    return new BlockStmt(new LinkedList<Statement>());
		}
		serialNumber++;
	    }
	    return n; 
	}
//	@Override
//	public void visit(E n, Object arg)
//	{
//	    System.out.println(n.toString());
//	    if (!assertionsToRemove.contains(serialNumber))
//	    {
//		n.getParentNode().getChildrenNodes().remove(n);
//	    }
//	    serialNumber++;
//	    super.visit(n, arg);
//	}

	// @Override
	// public void visit(MethodDeclaration n, Object arg)
	// {
	// if (this.isTestMethod(n))
	// {
	// List<Statement> selectedStatements = new ArrayList<Statement>();
	//
	// BlockStmt body = n.getBody();
	// if (body == null)
	// return;
	// List<Statement> stms = body.getStmts();
	// if (stms != null)
	// {
	// if (stms.size() > 0)
	// {
	// for (Statement stm : stms)
	// {
	//
	// if (this.isAssertion(stm))
	// {
	// if (stm.toString().contains("try"))
	// {
	// selectedStatements.add(stm);
	// } else
	// {
	// if (!assertionsToRemove.contains(serialNumber))
	// {
	// selectedStatements.add(stm);
	// }
	// else
	// {
	// removedAssertionLines.add(stm.getBeginLine());
	// }
	// }
	// serialNumber++;
	// } else
	// {
	// selectedStatements.add(stm);
	// }
	// }
	// }
	// }
	// body.setStmts(selectedStatements);
	// n.setBody(body);
	// }
	// }

	private boolean isTestMethod(MethodDeclaration n)
	{
	    // List<AnnotationExpr> annotations = n.getAnnotations();
	    // if (annotations != null)
	    // {
	    // for (AnnotationExpr a : n.getAnnotations())
	    // {
	    // if (a.toString().contains("@Test"))
	    // {
	    // return true;
	    // }
	    // }
	    // }
	    // return false;

	    return true;
	}

	private boolean isAssertion(Statement stm)
	{

	    String[] asserts = { "assertArrayEquals", "assertEquals", "assertFalse", "assertNotEquals", "assertNotNull",
		    "assertNotSame", "assertNull", "assertSame", "assertThat", "assertTrue", "fail" };

	    String statement = stm.toString();
	    for (String assertStr : asserts)
		if (statement.contains(assertStr))
		    return true;
	    return false;
	}
    }

    public static int getTotalPossibleNumberOfAssertions()
    {
	return totalPossibleNumberOfAssertions;
    }

    public static void setTotalPossibleNumberOfAssertions(int totalPossibleNumberOfAssertions)
    {
	TestSuiteComposer.totalPossibleNumberOfAssertions = totalPossibleNumberOfAssertions;
    }

}