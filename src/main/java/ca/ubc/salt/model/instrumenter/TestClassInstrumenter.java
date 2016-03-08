package ca.ubc.salt.model.instrumenter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;

public class TestClassInstrumenter
{
    public static void instrumentClass(String testClassPath)
	    throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	File testClass = new File(testClassPath);
	if (testClass.isFile())
	{
	    if (!isTestClass(testClass))
		return;

	    String source = FileUtils.readFileToString(testClass);
	    Document document = new Document(source);
	    List<ClassModel> classes = ClassModel.getClasses(document.get());

	    for (ClassModel clazz : classes)
		instrumentClass(clazz, null, document);

	} else if (testClass.isDirectory())
	{
	    File[] listOfFiles = testClass.listFiles();
	    for (int i = 0; i < listOfFiles.length; i++)
	    {
		instrumentClass(listOfFiles[i].getAbsolutePath());
	    }
	}

    }

    public static void instrumentClass(ClassModel srcClass, List<ClassModel> loadedClasses, Document document)
	    throws IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	List<Method> methods = srcClass.getMethods();

	Document newDocument = new Document(document.get());
	ASTRewrite rewriter = ASTRewrite.create(srcClass.cu.getAST());
	for (Method method : methods)
	{
	    if (isTestMethod(method))
		method.instrumentMethod(rewriter, document, null);
	}
	TextEdit edits = rewriter.rewriteAST(document, null);
	edits.apply(newDocument);

	// ImportRewrite importRewrite = ImportRewrite.create(, true);
	//
	// importRewrite.addImport("java.io.FileWriter");
	// importRewrite.addImport("java.io.IOException");
	// importRewrite.addImport("java.io.ObjectOutputStream");
	// importRewrite.addImport("com.thoughtworks.xstream.XStream");
	// importRewrite.addImport("com.thoughtworks.xstream.io.xml.StaxDriver");
	//
	// edits = importRewrite.rewriteImports(null);

	edits.apply(newDocument);

	addImports(newDocument);

    }

    public static void addImports(Document document) throws MalformedTreeException, BadLocationException
    {
	ASTParser parser = ASTParser.newParser(AST.JLS8);
	parser.setKind(ASTParser.K_COMPILATION_UNIT);
	Map pOptions = JavaCore.getOptions();
	pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
	pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
	pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
	parser.setCompilerOptions(pOptions);

	parser.setSource(document.get().toCharArray());
	CompilationUnit cu = (CompilationUnit) parser.createAST(null);

	cu.recordModifications();

	String[] imports = new String[] { "java.io.FileWriter", "java.io.IOException", "java.io.ObjectOutputStream",
		"com.thoughtworks.xstream.XStream", "com.thoughtworks.xstream.io.xml.StaxDriver" };
	for (String name : imports)
	    addImport(cu, name);

	TextEdit edits = cu.rewrite(document, null);

	edits.apply(document);

	System.out.println(document.get());

    }

    private static void addImport(CompilationUnit cu, String name)
    {
	AST ast = cu.getAST();
	ImportDeclaration imp = ast.newImportDeclaration();
	imp.setName(ast.newName(name));
	cu.imports().add(imp);
    }

    public static boolean isTestMethod(Method method)
    {

	if (method.methodDec.getName().toString().toLowerCase().contains("test"))
	    return true;
	for (Object obj : method.methodDec.modifiers())
	{
	    if (obj instanceof MarkerAnnotation)
	    {
		MarkerAnnotation ma = (MarkerAnnotation) obj;
		if (ma.getTypeName().getFullyQualifiedName().contains("Test"))
		    return true;
	    }
	}

	return false;
    }
    // public static void instrumentClass2(String testClassPath) throws
    // IOException
    // {
    // File testClass = new File(testClassPath);
    // if (testClass.isFile())
    // {
    //
    // String extention = FilenameUtils.getExtension(testClass.getName());
    // // if (extention.equals("java") &&
    // // testClass.getName().toLowerCase().contains("test"))
    // {
    // Settings.consoleLogger.info(String.format("instrumenting %s\n",
    // testClass.getName()));
    //
    // ASTParser parser = ASTParser.newParser(AST.JLS8);
    // parser.setKind(ASTParser.K_COMPILATION_UNIT);
    // Map pOptions = JavaCore.getOptions();
    // pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
    // pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
    // JavaCore.VERSION_1_8);
    // pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
    // parser.setCompilerOptions(pOptions);
    //
    // String source = FileUtils.readFileToString(testClassPath);
    // parser.setSource(source.toCharArray());
    // CompilationUnit cu = (CompilationUnit) parser.createAST(null);
    //
    // List typeDeclarationList = cu.types();
    //
    // for (Object type : typeDeclarationList)
    // {
    // // get methods list
    // // System.out.println(((AbstractTypeDeclaration)type).getName());
    // if (type instanceof TypeDeclaration)
    // {
    // TypeDeclaration typeDec = (TypeDeclaration) type;
    // MethodDeclaration[] methodList = typeDec.getMethods();
    //
    // for (MethodDeclaration method : methodList)
    // {
    // System.out.println(method.getName());
    //// Visitor visitor = new Visitor();
    //// method.accept(visitor);
    //// for (VariableDeclarationFragment var : visitor.varDecs)
    //// System.out.println(" " + var.getName());
    //
    //
    // Block block = method.getBody();
    // for (Object obj : block.statements())
    // {
    // Statement stmt = (Statement) obj;
    //// System.out.println("*");
    //// System.out.println(stmt);
    // }
    //
    // }
    //
    // FieldDeclaration[] fields = typeDec.getFields();
    //
    // for (FieldDeclaration field : fields)
    // {
    // if (Modifier.isStatic(field.getModifiers()))
    // System.out.println("static");
    // for (Object o : field.fragments())
    // {
    // if (o instanceof VariableDeclarationFragment)
    // {
    // VariableDeclarationFragment var = ((VariableDeclarationFragment) o);
    // String s = var.getName().toString();
    // System.out.println("-------------field: " + s);
    // } else
    // System.out.println("hehe");
    //
    // }
    // }
    //
    // }
    // }
    //
    // }
    //
    // } else if (testClass.isDirectory())
    // {
    // File[] listOfFiles = testClass.listFiles();
    // for (int i = 0; i < listOfFiles.length; i++)
    // {
    // instrumentClass(listOfFiles[i].getAbsolutePath());
    // }
    // }
    //
    // }

    public static boolean isTestClass(File classFile)
    {
	return true;
    }

    public static void main(String[] args)
	    throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	// instrumentClass(
	// "/Users/arash/Documents/workspace-mars/assertionmodel/src/main/java/ca/ubc/ca/salt/assertionModel/instrumenter/Visitor.java");
	// instrumentClass(
	// "/Users/arash/Library/Mobile
	// Documents/com~apple~CloudDocs/Research/Calculator/src/calc/CalculatorTest.java");
	instrumentClass(
		"/Users/arash/Desktop/Research/Repos/commons-math/src/test/java/org/apache/commons/math4/fraction/FractionTest.java");
    }

    public static ASTNode generateInstrumentationHeader(int randomNumber, String methodName)
    {
	String str = String.format("XStream xstream_%d = new XStream(new StaxDriver());", randomNumber);
	str += String.format(
		"try{ObjectOutputStream out_%d = xstream_%d.createObjectOutputStream(new FileWriter(\"traces/%s-%d.xml\"));",
		randomNumber, randomNumber, methodName, 0);
	str += String.format("out_%d.close();}catch (IOException e){e.printStackTrace();}", randomNumber);
	return createBlockWithText(str);

    }

    public static ASTNode generateFooterBlock(int randomNumber)
    {
	String str = String.format("out_%d.close();", randomNumber);
	return createBlockWithText(str);
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

    public static ASTNode generateInstrumentationBlock(int randomNumber,
	    LinkedList<VariableDeclarationFragment> varDecs, String methodName, int counter)
    {
	StringBuilder sb = new StringBuilder();
	sb.append(String.format(
		"try{ObjectOutputStream out_%d = xstream_%d.createObjectOutputStream(new FileWriter(\"traces/%s-%d.xml\"));",
		randomNumber, randomNumber, methodName, counter));
	for (VariableDeclarationFragment var : varDecs)
	    sb.append(String.format("out_%d.writeObject(%s);", randomNumber, var.getName()));
	sb.append(String.format("out_%d.close();}catch (IOException e){e.printStackTrace();}", randomNumber));
	return createBlockWithText(sb.toString());

    }

}
