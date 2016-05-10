package ca.ubc.salt.model.instrumenter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;

import ca.ubc.salt.model.productionCodeInstrumenter.ProductionClassInstrumenter;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class Instrumenter
{
    
    
    public static void main(String[] args)
    {
//	Utils.copyProject();
	try
	{
	    instrumentClass(Settings.PROJECT_PATH);
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
	    List<ClassModel> classes = ClassModel.getClasses(document.get());

	    if (!Utils.isTestClass(fClass))
	    {
		Settings.consoleLogger.debug(String.format("prod : %s", classPath));
		for (ClassModel clazz : classes)
		    document = ProductionClassInstrumenter.instrumentClass(clazz, null, document);
	    }
	    else
	    {
		Settings.consoleLogger.debug(String.format("test : %s", classPath));
		for (ClassModel clazz : classes)
		    document = TestClassInstrumenter.instrumentClass(clazz, null, document);
	    }
	    
	    writebackInstrumentedCode(document, Settings.getInstrumentedCodePath(classPath));

	} else if (fClass.isDirectory())
	{
	    File[] listOfFiles = fClass.listFiles();
	    for (int i = 0; i < listOfFiles.length; i++)
	    {
		instrumentClass(listOfFiles[i].getAbsolutePath());
	    }
	}

    }

    public static void writebackInstrumentedCode(Document document, String newPath)
    {
	try
	{
	    FileWriter fw = new FileWriter(newPath);
	    fw.write(document.get());
	    fw.close();
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }


}
