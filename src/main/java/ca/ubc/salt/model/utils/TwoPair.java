package ca.ubc.salt.model.utils;

public class TwoPair<A, B, C, D> implements Comparable<TwoPair>
{
    A first;
    B second;
    C third;
    D forth;
    public TwoPair()
    {
	// TODO Auto-generated constructor stub
    }

    public TwoPair(A first, B second, C third, D forth)
    {
	this.first = first;
	this.second = second;
	this.third = third;
	this.forth = forth;
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
    

    public C getThird()
    {
        return third;
    }

    public void setThird(C third)
    {
        this.third = third;
    }

    @Override
    public int compareTo(TwoPair o)
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

    
    
    public D getForth()
    {
        return forth;
    }

    public void setForth(D forth)
    {
        this.forth = forth;
    }

    @Override
    public String toString()
    {
	// TODO Auto-generated method stub
	return "(" + first.toString() + ", " + second.toString() + ")";
    }

}
