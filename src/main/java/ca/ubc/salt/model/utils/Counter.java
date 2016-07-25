package ca.ubc.salt.model.utils;

import java.util.HashMap;
import java.util.Map;

public class Counter <K>
{
    Map<K, Integer> counts = new HashMap<K, Integer>();
    
    public void increment(K key)
    {
	increment(key, 1);
    }
    
    public void increment(K key, int val)
    {
	Integer count = counts.get(key);
	if (count == null)
	{
	    count = 0;
	}
	counts.put(key, count + val);
    }
    
    public int get(K key)
    {
	return counts.get(key);
    }
}
