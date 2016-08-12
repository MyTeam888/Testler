package ca.ubc.salt.model.instrumenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyStore.ProtectionParameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;
import evaluation.TimeResult;

@RunWith(value = Parameterized.class)
public class Instrumenter
{

    private static final String PARENT_CLASS_DEPENDENCY_FILE = "parentClassDependency.txt";
    private static final String PARAMETERIZED_CLASSES_FILE = "parameterizedClasses.txt";
    static HashMap<String, String> classFileMapping = new HashMap<String, String>();
    public static HashMap<String, Set<String>> parentClassDependency = new HashMap<String, Set<String>>();
    public static Set<String> parameterizedClasses = new HashSet<String>();

    public static void loadStructs()
    {
	try
	{
	    ObjectInputStream in = new ObjectInputStream(new FileInputStream(PARENT_CLASS_DEPENDENCY_FILE));
	    parentClassDependency = (HashMap<String, Set<String>>) in.readObject();
	    in.close();

	    in = new ObjectInputStream(new FileInputStream(PARAMETERIZED_CLASSES_FILE));
	    parameterizedClasses = (Set<String>) in.readObject();
	    in.close();
	} catch (Exception e)
	{
	    e.printStackTrace();
	}
    }

    public static void main(String[] args)
    {
	Utils.copyProject(Settings.PROJECT_PATH, Settings.PROJECT_INSTRUMENTED_PATH);
	loadStructs();
	try
	{
	    instrumentClass(Settings.PROJECT_PATH);
	    XStream xstream = new XStream(new StaxDriver());
	    FileWriter fw = new FileWriter(Settings.classFileMappingPath);
	    fw.write(xstream.toXML(classFileMapping));
	    fw.close();

	    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(PARAMETERIZED_CLASSES_FILE));
	    out.writeObject(parameterizedClasses);
	    out.flush();
	    out.close();

	    out = new ObjectOutputStream(new FileOutputStream(PARENT_CLASS_DEPENDENCY_FILE));
	    out.writeObject(parentClassDependency);
	    out.flush();
	    out.close();

	} catch (IllegalArgumentException | MalformedTreeException | IOException | BadLocationException
		| CoreException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static void instrumentClass(String classPath)
	    throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	File fClass = new File(classPath);
	if (fClass.isFile() && fClass.getAbsolutePath().endsWith("java"))
	{

	    String source = FileUtils.readFileToString(fClass);
	    Document document = new Document(source);
	    List<ClassModel> classes = ClassModel.getClasses(document.get(), true, classPath,
		    new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });

	    if (!Utils.isTestClass(fClass))
	    {
		Settings.consoleLogger.error(String.format("prod : %s", classPath));
		if (classes.size() > 0)
		{
		    ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());
		    for (ClassModel clazz : classes)
			ProductionClassInstrumenter.instrumentClass(clazz, null, document, rewriter);

		    Document newDocument = new Document(document.get());
		    TextEdit edits = rewriter.rewriteAST(document, null);
		    edits.apply(newDocument);

		    ProductionClassInstrumenter.addImports(newDocument);
		    document = newDocument;
		}

	    } else
	    {
		Settings.consoleLogger.error(String.format("test : %s", classPath));
		if (classes.size() == 0)
		    return;
		ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());
		for (ClassModel clazz : classes)
		{

		    updateStructs(clazz);

		    if (clazz.isInstrumentable())
		    {
			classFileMapping.put(clazz.name, fClass.getAbsolutePath());

			TestClassInstrumenter.instrumentClass(clazz, null, clazz.typeDec.getName().toString(),
				rewriter);
		    }
		}

		TextEdit edits = rewriter.rewriteAST(document, null);
		edits.apply(document);

		Utils.addImports(document, Arrays
			.asList(new String[] { "instrument.InstrumentClassGenerator", "instrument.NullValueType" }));

		// ImportRewrite importRewrite = ImportRewrite.create(, true);
		//
		// importRewrite.addImport("java.io.FileWriter");
		// importRewrite.addImport("java.io.IOException");
		// importRewrite.addImport("java.io.ObjectOutputStream");
		// importRewrite.addImport("com.thoughtworks.xstream.XStream");
		// importRewrite.addImport("com.thoughtworks.xstream.io.xml.StaxDriver");
		//
		// edits = importRewrite.rewriteImports(null);

	    }

	    Utils.writebackSourceCode(document, Settings.getInstrumentedCodePath(classPath));

	} else if (fClass.isDirectory())
	{
	    File[] listOfFiles = fClass.listFiles();
	    for (int i = 0; i < listOfFiles.length; i++)
	    {
		instrumentClass(listOfFiles[i].getAbsolutePath());
	    }
	}

    }

    public static void updateStructs(ClassModel clazz)
    {
	
	List modifs = clazz.getTypeDec().modifiers();

	for (Object obj : modifs)
	{
	    if (obj instanceof SingleMemberAnnotation)
	    {
		SingleMemberAnnotation mod = (SingleMemberAnnotation) obj;
		String typeName = mod.getTypeName().getFullyQualifiedName();
		String value = mod.getValue().toString();
		if (typeName.contains("RunWith") && value.contains("Parameterized"))
		    parameterizedClasses.add(clazz.name);

	    }else if (obj instanceof NormalAnnotation)
	    {
		NormalAnnotation mod = (NormalAnnotation) obj;
		String typeName = mod.getTypeName().getFullyQualifiedName();
		String value = mod.values().toString();
		if (typeName.contains("RunWith") && value.contains("Parameterized"))
		    parameterizedClasses.add(clazz.name);
	    }
	}
	Type parent = clazz.typeDec.getSuperclassType();
	if (parent != null)
	{
	    ITypeBinding binding = parent.resolveBinding();
	    String parentName = binding.getQualifiedName();
	    Utils.addToTheSetInMap(parentClassDependency, parentName, clazz.name);
	}
    }

}
