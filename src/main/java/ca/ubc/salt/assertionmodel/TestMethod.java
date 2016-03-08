package ca.ubc.salt.assertionmodel;

import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.Statement;

public class TestMethod {
	private int numberOfAssertions;
	private MethodDeclaration method;
	private String methodName;
	private String filePath;
	private String className;
	private int serialNumber;
	
	public void setMethod(MethodDeclaration method) {
		this.method = method;
	}
	
	public MethodDeclaration getMethod() {
		return this.method;
	}
	
	public void setNumberOfAssertions(int count) {
		this.numberOfAssertions = count;
	}
	
	public int getNumberOfAssertions() {
		return this.numberOfAssertions;
	}
	
	public void setMethodName(String name) {
		this.methodName = name;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public String getFilePath() {
		return this.filePath;
	}
	
	public void setFilePath(String path) {
		this.filePath = path;
	}
	
	public void setSerialNumber(int num) {
		this.serialNumber = num;
	}
	
	public int getSerialNumber() {
		return this.serialNumber;
	}
	
	public void setClassName(String currentClass) {
		this.className = currentClass;
	}
	
	public String getClassName() {
		return this.className;
	}
	
	
	
	public static boolean isTestMethod(MethodDeclaration n)
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

	public static boolean isAssertion(Statement stm)
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
