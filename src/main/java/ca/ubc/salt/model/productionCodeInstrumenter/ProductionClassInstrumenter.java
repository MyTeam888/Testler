package ca.ubc.salt.model.productionCodeInstrumenter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.Method;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class ProductionClassInstrumenter
{
    
    public static void main(String[] args)
	    throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	instrumentClass(Settings.PROD_TEST_CLASS);
    }
    
    
    public static void instrumentClass(String testClassPath)
	    throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	File testClass = new File(testClassPath);
	if (testClass.isFile())
	{
	    
	    if (Utils.isTestClass(testClass))
		return;

	    String source = FileUtils.readFileToString(testClass);
	    Document document = new Document(source);
	    List<ClassModel> classes = ClassModel.getClasses(document.get());

	    for (ClassModel clazz : classes)
		document = instrumentClass(clazz, null, document);
	    
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
    
    
    

    public static Document instrumentClass(ClassModel srcClass, List<ClassModel> loadedClasses, Document document)
	    throws IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	List<Method> methods = srcClass.getMethods();

	Document newDocument = new Document(document.get());
	ASTRewrite rewriter = ASTRewrite.create(srcClass.getCu().getAST());
	for (Method method : methods)
	{
		method.instrumentProductionMethod(rewriter, document, null, !method.getMethodDec().isConstructor());
	}
	TextEdit edits = rewriter.rewriteAST(document, null);
	edits.apply(newDocument);

	addImports(newDocument);
	
	return newDocument;

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

	String[] imports = new String[] { "instrument.InstrumentClassGenerator" };
	for (String name : imports)
	    addImport(cu, name);

	TextEdit edits = cu.rewrite(document, null);

	edits.apply(document);


    }

    private static void addImport(CompilationUnit cu, String name)
    {
	AST ast = cu.getAST();
	ImportDeclaration imp = ast.newImportDeclaration();
	imp.setName(ast.newName(name));
	cu.imports().add(imp);
    }
    
    public static ASTNode generateInstrumentationHeader(String methodName, List<String> varNames)
    {
	StringBuilder sb = new StringBuilder();
	sb.append(String.format("InstrumentClassGenerator.traceMethodCall(\"%s\"", methodName));

	sb.append(',');
	for(String var : varNames)
	{
	    sb.append(var);
	    sb.append(',');
	}
	sb.setLength(sb.length()-1);
	sb.append(");");
	
	
	return Utils.createBlockWithText(sb.toString());

    }
    
}
