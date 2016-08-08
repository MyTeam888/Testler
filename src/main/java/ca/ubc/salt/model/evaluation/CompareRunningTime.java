package ca.ubc.salt.model.evaluation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ubc.salt.model.merger.MergingResult;
import evaluation.TimeResult;

public class CompareRunningTime
{

    static final String originalResultsPath = "results/timeResultsOriginal.txt";
    static final String mergedResultsPath = "results/timeResultsMerged.txt";
    static final String mergedInfoPath = "results/mergingResult.txt";

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException
    {
	ObjectInputStream in = new ObjectInputStream(new FileInputStream(originalResultsPath));
	List<TimeResult> timesOriginal = (List<TimeResult>) in.readObject();
	in.close();

	in = new ObjectInputStream(new FileInputStream(mergedResultsPath));
	List<TimeResult> timesMerged = (List<TimeResult>) in.readObject();
	in.close();

	in = new ObjectInputStream(new FileInputStream(mergedInfoPath));
	List<MergingResult> mergingResults = (List<MergingResult>) in.readObject();
	in.close();

	Formatter fr = new Formatter("timeCompare.csv");

	Map<String, TimeResult> timesMergedMap = getTheMap(timesMerged);
	Map<String, TimeResult> timesOriginalMap = getTheMap(timesOriginal);

	for (MergingResult mr : mergingResults)
	{
	    long original = 0;

	    for (String testCase : mr.getMergedTestCases())
	    {
		TimeResult testCaseResult = timesOriginalMap.get(testCase);
		original += testCaseResult.getTime();
	    }

	    String mergedTestCaseName = mr.getMergedClassName() + "." + mr.getMergedTestCaseName();
	    TimeResult mergedResult = timesMergedMap.get(mergedTestCaseName);
	    if (mergedResult != null)
	    {
		long merged = mergedResult.getTime();
		if (mergedResult.getStatus().equals("succeeded") && merged < original && mr.couldntsatisfy == false && mr.fatalError == false)
		    fr.format("%s,%s,%d,%d,%d\n", mr.getMergedTestCases().toString().replace(",", " "), mergedTestCaseName, original,
			    merged, original - merged);
	    }
	}

	fr.flush();
	fr.close();
	System.out.println("done!");

    }

    public static Map<String, TimeResult> getTheMap(List<TimeResult> times)
    {
	Map<String, TimeResult> map = new HashMap<String, TimeResult>();
	for (TimeResult tr : times)
	{
	    int index = tr.getTestClassName().lastIndexOf('.');
	    String key = tr.getTestClassName().substring(index + 1);
	    key = key + "." + tr.getTestCaseName();

	    map.put(key, tr);
	}

	return map;
    }

}
