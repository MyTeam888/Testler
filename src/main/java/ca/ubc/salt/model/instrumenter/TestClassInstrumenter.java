package ca.ubc.salt.model.instrumenter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.compiler.ast.NumberLiteral;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class TestClassInstrumenter
{
    public static void instrumentClass(String testClassPath)
	    throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	File testClass = new File(testClassPath);
	if (testClass.isFile())
	{
	    if (!Utils.isTestClass(testClass))
		return;

	    String source = FileUtils.readFileToString(testClass);
	    Document document = new Document(source);
	    List<ClassModel> classes = ClassModel.getClasses(document.get());

	    for (ClassModel clazz : classes)
		document = instrumentClass(clazz, null, document, clazz.typeDec.getName().toString());

	    System.out.println(document.get());

	} else if (testClass.isDirectory())
	{
	    File[] listOfFiles = testClass.listFiles();
	    for (int i = 0; i < listOfFiles.length; i++)
	    {
		instrumentClass(listOfFiles[i].getAbsolutePath());
	    }
	}

    }

    public static Document instrumentClass(ClassModel srcClass, List<ClassModel> loadedClasses, Document document,
	    String fileName)
	    throws IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	List<Method> methods = srcClass.getMethods();

	Document newDocument = new Document(document.get());
	ASTRewrite rewriter = ASTRewrite.create(srcClass.cu.getAST());
	for (Method method : methods)
	{
	    if (method.isTestMethod() && !Settings.blackListSet.contains(method.getFullMethodName()))
		method.instrumentTestMethod(rewriter, document, null, fileName, !method.getMethodDec().isConstructor());
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

	Utils.addImports(newDocument, Arrays.asList(new String[] { "instrument.InstrumentClassGenerator" }));

	return newDocument;

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

    public static void main(String[] args)
	    throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	// instrumentClass(
	// "/Users/arash/Documents/workspace-mars/assertionmodel/src/main/java/ca/ubc/ca/salt/assertionModel/instrumenter/Visitor.java");
	// instrumentClass(
	// "/Users/arash/Library/Mobile
	// Documents/com~apple~CloudDocs/Research/Calculator/src/calc/CalculatorTest.java");
	instrumentClass(Settings.TEST_CLASS);
    }

    public static ASTNode generateInstrumentationHeader(ClassModel clazz, int randomNumber, String fileName, String methodName)
    {
	
	StringBuilder sb = new StringBuilder();
	sb.append(String.format(
		"InstrumentClassGenerator.init(\"%s.%s\");InstrumentClassGenerator.initTestStatement(0);",
		fileName, methodName));
	sb.append(getTextForInstrumentation(clazz.getVarDecsOfFields(), 1));
	return Utils.createBlockWithText(sb.toString());

    }

    public static ASTNode generateFooterBlock(int randomNumber)
    {
	return Utils.createBlockWithText("InstrumentClassGenerator.close()");
    }

    public static ASTNode generateInstrumentationBlock(int randomNumber,
	    LinkedList<VariableDeclarationFragment> varDecs, String methodName, int counter)
    {

	String text = getTextForInstrumentation(varDecs, counter);
	
	return Utils.createBlockWithText(text);

    }
    // public static ASTNode generateInstrumentationHeader(int randomNumber,
    // String methodName)
    // {
    // StringBuilder sb = new StringBuilder();
    // sb.append(String.format("XStream xstream_%d = new XStream(new
    // StaxDriver());", randomNumber));
    // sb.append(String.format("try{FileWriter fw_%d = new
    // FileWriter(\"traces/%s-%d.xml\");", randomNumber,
    // methodName, 1));
    // sb.append(String.format("fw_%d.append(\"<vars></vars>\\n\");",
    // randomNumber));
    // sb.append(String.format("ObjectOutputStream out_%d =
    // xstream_%d.createObjectOutputStream(fw_%d);", randomNumber,
    // randomNumber, randomNumber));
    // sb.append(String.format("out_%d.close();}catch (IOException
    // e){e.printStackTrace();}", randomNumber));
    // return Utils.createBlockWithText(sb.toString());
    //
    // }
    //
    // public static ASTNode generateFooterBlock(int randomNumber)
    // {
    // String str = String.format("out_%d.close();", randomNumber);
    // return Utils.createBlockWithText(str);
    // }
    //
    // public static ASTNode generateInstrumentationBlock(int randomNumber,
    // LinkedList<VariableDeclarationFragment> varDecs, String methodName, int
    // counter)
    // {
    //
    // StringBuilder sb = new StringBuilder();
    // sb.append(String.format("try{FileWriter fw_%d = new
    // FileWriter(\"traces/%s-%d.xml\");", randomNumber,
    // methodName, counter));
    // sb.append(String.format("fw_%d.append(\"<vars>\");", randomNumber));
    // for (VariableDeclarationFragment var : varDecs)
    // sb.append(String.format("fw_%d.append(\"<var>%s</var>\");", randomNumber,
    // var.getName()));
    // sb.append(String.format("fw_%d.append(\"</vars>\\n\");", randomNumber));
    //
    // sb.append(String.format("ObjectOutputStream out_%d =
    // xstream_%d.createObjectOutputStream(fw_%d);", randomNumber,
    // randomNumber, randomNumber));
    // for (VariableDeclarationFragment var : varDecs)
    // sb.append(String.format("out_%d.writeObject(%s);", randomNumber,
    // var.getName()));
    // sb.append(String.format("out_%d.close();}catch (IOException
    // e){e.printStackTrace();}", randomNumber));
    // return Utils.createBlockWithText(sb.toString());
    //
    // }

    public static String getTextForInstrumentation(List<VariableDeclarationFragment> list, int counter)
    {
	StringBuilder sb = new StringBuilder();
	sb.append(String.format("InstrumentClassGenerator.traceTestStatementExecution("));
	for (VariableDeclarationFragment var : list)
	{
	    sb.append("\"");
	    sb.append(var.getName());
	    sb.append("\"");
	    sb.append(',');
	}
	if (list.size() > 0)
	    sb.setLength(sb.length() - 1);
	sb.append(");");

	sb.append("InstrumentClassGenerator.writeObjects(");
	for (VariableDeclarationFragment var : list)
	{
	    sb.append(var.getName());
	    sb.append(',');
	}
	if (list.size() > 0)
	    sb.setLength(sb.length() - 1);
	sb.append(");");
	sb.append(String.format("InstrumentClassGenerator.initTestStatement(%d);", counter));

	String text = sb.toString();
	return text;
    }

}
