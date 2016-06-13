package ca.ubc.salt.model.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.Statement;

public class TestStatement extends TestModelNode
{
    Set<TestState> compatibleStates;
    TestState start, end;
    String name;
    Statement statement;
    String methodCall;
    String input;
    public long time = 1;
    
    
    
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
	    if (tst.start.equals(this.start) && tst.end.equals(this.end) && tst.methodCall.equals(this.methodCall))
		return true;
	}
	return false;
    }

//    @Override
//    public int hashCode()
//    {
//
//	return this.start.hashCode() * 13 + this.end.hashCode() * 17 + this.methodCall == null ? 0 : this.methodCall.hashCode();
//
//    }

}
