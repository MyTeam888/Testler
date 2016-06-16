package ca.ubc.salt.model.composer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.Statement;

import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.Method;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Utils;

public class TestCaseComposer
{

    public String composeTestCase(List<TestStatement> path)
    {

    }

    public void populateStateField(List<TestStatement> path)
    {
	Map<String, List<TestStatement>> fileTestStatementMapping = new HashMap<String, List<TestStatement>>();
	for (TestStatement stmt : path)
	{

	    String testCaseStr = getTestCaseName(stmt);
	    String filePath = Utils.getTestCaseFile(testCaseStr);
	    Utils.addToTheCollectionInMap(fileTestStatementMapping, filePath, stmt);
	}

	for (Entry<String, List<TestStatement>> entry : fileTestStatementMapping.entrySet())
	{

	}

    }

    private String getTestCaseName(TestStatement stmt)
    {
	String stmtStr = stmt.getName();
	int index = stmtStr.lastIndexOf('-');
	String testCaseStr = stmtStr.substring(0, index);
	return testCaseStr;
    }

    public void populateForFile(String filePath, List<TestStatement> statements)
    {
	try
	{
	    List<ClassModel> clazzs = ClassModel.getClasses(FileUtils.readFileToString(filePath));

	    for (TestStatement stmt : statements)
	    {
		for (ClassModel clazz : clazzs)
		{
		    List<Method> methods = clazz.getMethods();

		    for (Method m : methods)
		    {
			if (m.getMethodDec().getName().toString().equals(Utils.getTestCaseName(stmt.getName())))
			{
			    
			    stmt.statement = (Statement) m.getMethodDec().getBody().statements().get(getTestStatementNumber(stmt.getName()));
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
    
    
    public int getTestStatementNumber(String xmlTestStatementStr)
    {
	int index = xmlTestStatementStr.lastIndexOf('-');
	int endIndex = xmlTestStatementStr.lastIndexOf('.');
	return Integer.valueOf(xmlTestStatementStr.substring(index + 1, endIndex));
    }

}
