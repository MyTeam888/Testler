package ca.ubc.salt.model.utils;

public class Triple<A, B, C> implements Comparable<Triple>
{
    A first;
    B second;
    C third;

    
    
    public C getThird()
    {
        return third;
    }

    public void setThird(C third)
    {
        this.third = third;
    }

    public Triple()
    {
	// TODO Auto-generated constructor stub
    }

    public Triple(A first, B second, C third)
    {
	this.first = first;
	this.second = second;
	this.third = third;
    }

    public A getFirst()
    {
	return first;
    }

    public void setFirst(A first)
    {
	this.first = first;
    }

    public B getSecond()
    {
	return second;
    }

    public void setSecond(B second)
    {
	this.second = second;
    }

    @Override
    public int compareTo(Triple o)
    {
	// TODO Auto-generated method stub
	if (this.first instanceof Comparable)
	{
	    Comparable a = (Comparable) this.first;
	    Comparable b = (Comparable) o.first;
	    return a.compareTo(b);

	}

	return 0;
    }

    @Override
    public String toString()
    {
	// TODO Auto-generated method stub
	return "(" + first.toString() + ", " + second.toString() + ")";
    }

}
