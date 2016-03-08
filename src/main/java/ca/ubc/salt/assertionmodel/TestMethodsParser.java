package ca.ubc.salt.assertionmodel;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class TestMethodsParser {
	private static ArrayList<TestMethod> methods;
	private static int totalNumberOfAssertions;
	private static int totalLinesOfTestCode;
	private static int totalNumberOfStatementsInTestCode;
	private static ArrayList<TestMethod> bucket0;
	private static ArrayList<TestMethod> bucket1;
	private static ArrayList<TestMethod> bucket2;
	private static ArrayList<TestMethod> bucket3;
	private static ArrayList<TestMethod> bucket4;
	private static ArrayList<TestMethod> bucket5;
	private static int serialNumber;
	private static int statementSerialNumber;
	private static HashSet<Integer> tryStatements;
	private String allJunitTestClassNames;
	private static int assertTrueOrFalse;
	private static int assertEqualsOrNot;
	private static int assertNullOrNot;
	
	public TestMethodsParser(File folder) throws ParseException, IOException {
		tryStatements = new HashSet<Integer>();
		this.methods = new ArrayList<TestMethod>();
		this.bucket0 = new ArrayList<TestMethod>();
		this.bucket1 = new ArrayList<TestMethod>();
		this.bucket2 = new ArrayList<TestMethod>();
		this.bucket3 = new ArrayList<TestMethod>();
		this.bucket4 = new ArrayList<TestMethod>();
		this.bucket5 = new ArrayList<TestMethod>();
		this.totalNumberOfAssertions = 0;
		this.serialNumber = 0;
		statementSerialNumber = 0;
		this.totalLinesOfTestCode = 0;
		this.totalNumberOfStatementsInTestCode = 0;
		this.allJunitTestClassNames = "";
		this.assertTrueOrFalse = 0;
		this.assertEqualsOrNot = 0;
		this.assertNullOrNot = 0;
		this.parse(folder);
	}

	public void parse(File file) throws ParseException, IOException {
		if(file.isFile()) {
			String filename = file.getName();
//			System.out.println(file.getAbsolutePath());
			if(filename.contains(".java") && filename.contains("Test")
				&& ! filename.contains("TimeSeriesCollectionTest")
				&& ! filename.contains("AbstractRendererTest")
			){
				FileInputStream in = new FileInputStream(file.getPath());
				CompilationUnit cu;
				String currentClassPackage;
				String currentClassName;
				String currentClass;
				try {
			        // parse the file
//					System.out.println(file.getPath());
			        cu = JavaParser.parse(in);
			        currentClassPackage = cu.getPackage().getName().toString();
			        currentClassName = filename.substring(0, filename.indexOf("."));
			        currentClass = currentClassPackage+"."+currentClassName;
			        
			        allJunitTestClassNames = allJunitTestClassNames+" "+currentClass;
			    } finally {
			        in.close();
			    }
				new MethodVisitor(currentClass).visit(cu, null);
			}
		}
		else if (file.isDirectory()) {
			File[] listOfFiles = file.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
		      parse(listOfFiles[i]);
		    }
		}
	}
	
	public void printAllJunitTestClassNames() {
		System.out.println(this.allJunitTestClassNames);
	}
	
	public static ArrayList<TestMethod> getMethods() {
		return methods;
	}
	
	public int getTotalNumberOfAssertions() {
		return this.totalNumberOfAssertions;
	}
	
	public int getTotalLinesOfTestCode() {
		return this.totalLinesOfTestCode;
	}
	
	public int getTotalNumberOfStatementsInTestCode() {
		return this.totalNumberOfStatementsInTestCode;
	}
	
	public static ArrayList<TestMethod> getBucket0() {
		return bucket0;
	}
	
	public static ArrayList<TestMethod> getBucket1() {
		return bucket1;
	}
	
	public static ArrayList<TestMethod> getBucket2() {
		return bucket2;
	}
	
	public static ArrayList<TestMethod> getBucket3() {
		return bucket3;
	}

	public static ArrayList<TestMethod> getBucket4() {
		return bucket4;
	}

	public static ArrayList<TestMethod> getBucket5() {
		return bucket5;
	}
	
	public HashSet<Integer> getTryStatements() {
		return tryStatements;
	}
	
	public int getNumberOfAssertions() {
		return this.statementSerialNumber;
	}
	
	public int getNumberOfAssertTrueOrFaulse() {
		return this.assertTrueOrFalse;
	}
	
	public int getNumberOfAssertEqualsOrNot() {
		return this.assertEqualsOrNot;
	}
	
	public int getNumberOfAssertNullOrNot() {
		return this.assertNullOrNot;
	}

	/**
	 * Simple visitor implementation for visiting MethodDeclaration nodes. 
	 */
	private static class MethodVisitor extends VoidVisitorAdapter {
		private String currentClass;
		
		public MethodVisitor(String currentClass) {
			this.currentClass = currentClass;
		}
		
		@Override
	    public void visit(MethodDeclaration n, Object arg) {
	    	if(this.isTestMethod(n)) {
	    		if(n.getBody() != null) {
	    			if(n.getBody().getStmts() != null) {
	    				totalNumberOfStatementsInTestCode += n.getBody().getStmts().size();
	    			}
	    		}
	    		
	    		totalLinesOfTestCode += n.getEndLine() - n.getBeginLine() +1;
	    		int numberOfAssertions = 0;
	    		if(n.getBody() != null) {
	    			if(n.getBody().getStmts() != null) {
			        	for (Statement stm : n.getBody().getStmts()) {
			        		if(this.isAssertion(stm)) numberOfAssertions++;
			        	}
	    			}
	    		}
	        	totalNumberOfAssertions += numberOfAssertions;
	        	TestMethod method = this.composeTestMethod(n, numberOfAssertions, serialNumber, currentClass);
	        	getMethods().add(method);
	        	if(numberOfAssertions==0) {
	        		getBucket0().add(method);
	        	}
	        	else if(numberOfAssertions==1) {
	        		getBucket1().add(method);
	        	}
	        	else if(numberOfAssertions==2) {
	        		getBucket2().add(method);
	        	}
	        	else if(numberOfAssertions==3){
	        		getBucket3().add(method);
	        	}
	        	else if(numberOfAssertions==4){
	        		getBucket4().add(method);
	        	}
	        	else if(numberOfAssertions>=5){
	        		getBucket5().add(method);
	        	}
	        	serialNumber++;
	        	
	        	if(n.getBody() != null ) {
		        	BlockStmt body = n.getBody();
		    		List<Statement> stms = body.getStmts();
		    		if(stms != null) {
			    		if(stms.size() > 0) {
				    		for (Statement stm : stms) {
				    			if(this.isAssertion(stm)) {
				        			if(stm.toString().contains("try")) {
				        				tryStatements.add(statementSerialNumber);
				        			}
				        			else {
				        				if(stm.toString().contains("assertTrue")
				        						|| stm.toString().contains("assertFalse")){
				        					assertTrueOrFalse++;
				        				}else if(stm.toString().contains("assertEquals")
				        						|| stm.toString().contains("assertNotEquals")){
				        					assertEqualsOrNot++;
				        				}else if(stm.toString().contains("assertNull")
				        						|| stm.toString().contains("assertNotNull")){
				        					assertNullOrNot++;
				        				}
				        			}
//				        			else {
//				        				System.out.println(n.toString());
//				        			}
				        			statementSerialNumber++;
				        		}
				    		}
			    		}
		    		}
	        	}
	    	}
	    }
		
		private TestMethod composeTestMethod(MethodDeclaration n, int numOfAssertions, int serialNumber, String currentClass) {
			TestMethod method = new TestMethod();
        	method.setMethod(n);
        	method.setNumberOfAssertions(numOfAssertions);
        	method.setClassName(this.currentClass);
        	method.setMethodName(n.getName());
        	method.setSerialNumber(serialNumber);
        	return method;
		}
		
		private boolean isTestMethod(MethodDeclaration n) {
			List<AnnotationExpr> annotations = n.getAnnotations();
			if(annotations!=null) {
				for(AnnotationExpr a : n.getAnnotations()) {
					if(a.toString().contains("@Test")) {
						return true;
					}
				}
			}
			return false;
		}
		
	
		private boolean isAssertion(Statement stm) {
			String statement = stm.toString();
			return statement.contains("assert") 
					&& ! statement.contains("import")
					&& ! statement.contains("private")
					&& ! statement.contains("static")
					&& ! statement.contains("public")
					&& ! statement.contains("void")
					&& ! statement.contains("//");
		}
	    
	}
}
