package ca.ubc.salt.model.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;

import ca.ubc.salt.model.composer.TestCaseComposer;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Utils;

public class TestStatement extends TestModelNode
{
    Set<TestState> compatibleStates;
    TestState start, end;
    String name;
    public Statement statement;
    public Statement refactoredStatement;
    String methodCall;
    String input;
    Collection<String> sideEffects;

    public long time = 1000;

    public void initSideEffects(List<String> testCases)
    {
	sideEffects = new ArrayList<String>();
	try
	{
	    Map<String, String> before = TestCaseComposer.nameValuePairs.get(this.name);
	    String nextState = Utils.nextOrPrevState(testCases, this.name, true);
	    Map<String, String> after = TestCaseComposer.nameValuePairs.get(nextState);

	    for (Entry<String, String> entry : before.entrySet())
	    {
		String varName = entry.getKey();
		String varValBefore = entry.getValue();
		String varValAfter = after.get(varName);
		if (!varValBefore.equals(varValAfter))
		    sideEffects.add(varName);
	    }
	} catch (ExecutionException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public TestStatement(TestState start, TestState end, String name)
    {
	this.start = start;
	this.end = end;
	this.name = name;
	compatibleStates = new HashSet<TestState>();

    }

    public Set<TestState> getCompatibleStates()
    {
	return compatibleStates;
    }

    public void setCompatibleStates(Set<TestState> compatibleStates)
    {
	this.compatibleStates = compatibleStates;
    }

    public TestState getStart()
    {
	return start;
    }

    public void setStart(TestState start)
    {
	this.start = start;
    }

    public TestState getEnd()
    {
	return end;
    }

    public void setEnd(TestState end)
    {
	this.end = end;
    }

    @Override
    public String toString()
    {
	// TODO Auto-generated method stub
	if (this.refactoredStatement != null)
	    return this.refactoredStatement.toString();
	if (this.statement != null)
	    return this.statement.toString();
	else
	    return this.name;
    }

    public String getName()
    {
	return name;
    }

    public void setName(String name)
    {
	this.name = name;
    }

    @Override
    public boolean equals(Object obj)
    {
	if (obj instanceof TestStatement)
	{
	    TestStatement tst = (TestStatement) obj;
	    if (tst.start.equals(this.start) && tst.end.equals(this.end) // &&
									 // tst.methodCall.equals(this.methodCall)
	    )
		return true;
	}
	return false;
    }

    @Override
    public TestStatement clone() throws CloneNotSupportedException
    {
	// TODO Auto-generated method stub

	TestStatement clone = new TestStatement(this.start, this.end, this.name);
	clone.statement = this.statement;
	clone.refactoredStatement = this.refactoredStatement;
	clone.sideEffects = this.sideEffects;
	clone.time = this.time;
	clone.parent.putAll(this.parent);
	clone.distFrom.putAll(this.distFrom);
	return clone;

    }

    public Collection<String> getSideEffects()
    {
	return sideEffects;
    }

    // @Override
    // public int hashCode()
    // {
    //
    // return this.start.hashCode() * 13 + this.end.hashCode() * 17 +
    // this.methodCall == null ? 0 : this.methodCall.hashCode();
    //
    // }

}
