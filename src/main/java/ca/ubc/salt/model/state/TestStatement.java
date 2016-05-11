package ca.ubc.salt.model.state;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.Statement;

public class TestStatement
{
    Set<TestState> compatibleStates;
    TestState start, end;
    String name;
    Statement statement;
    String methodCall;
    String input;

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

}
