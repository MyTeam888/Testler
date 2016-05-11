package instrument;

import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class InstrumentClassGenerator
{
    static boolean firstCall;
    static String methodName;
    static String fileName;
    static XStream xstream = new XStream(new StaxDriver());
    static FileWriter fw = null;
    static ObjectOutputStream out;

    public static void init(String methodName)
    {
	InstrumentClassGenerator.methodName = methodName;
	firstCall = true;

    }

    public static void traceMethodCall(String methodName, Object... input)
    {
	if (firstCall == true)
	{
	    try
	    {
		fw.append("<methodCall>");
		fw.append(String.format("<method>%s</method>\n", methodName));
		fw.append("<input>");
		writeObjects(input);
		fw.append("</input>");
		fw.append("</methodCall>\n");
	    } catch (IOException e)
	    {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    firstCall = false;
	}

    }

    public static void close()
    {
	try
	{
	    fw.close();
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static void initTestStatement(int lineNumber)
    {

	fileName = String.format("traces/%s-%d.xml", methodName, lineNumber);

	try
	{
	    if (fw != null)
		fw.close();
	    fw = new FileWriter(fileName);
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static void traceTestStatementExecution(String... vars)
    {
	firstCall = true;
	try
	{
	    fw.append("<vars>");
	    for (String var : vars)
	    {
		fw.append(String.format("<var>%s</var>", var));
	    }
	    fw.append("</vars>\n");
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }

    public static void writeObjects(Object... input)
    {
	try
	{
	    out = xstream.createObjectOutputStream(fw);
	    for (Object obj : input)
	    {
		out.writeObject(obj);
	    }
	    out.close();
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }

}
