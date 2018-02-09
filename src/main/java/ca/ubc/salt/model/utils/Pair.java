package ca.ubc.salt.model.utils;

public class Pair<A, B> implements Comparable<Pair>
{
    A first;
    B second;

    public Pair()
    {
	// TODO Auto-generated constructor stub
    }

    public Pair(A first, B second)
    {
	this.first = first;
	this.second = second;
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
    public int compareTo(Pair o)
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
