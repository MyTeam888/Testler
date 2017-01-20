package evaluation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Formatter;
import java.util.List;
import java.util.Set;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

import ca.ubc.salt.model.merger.MergingResult;

public class SingleTestRunner {

	static final String originalResultsPath = "results/timeResultsOriginal.txt";
	static final String mergedResultsPath = "results/timeResultsMerged.txt";
	static final String mergedInfoPath = "/Users/arash/Google Drive/Sync/Projects/TestModel/results/mergingResult.txt";

	static final int TIMES = 10;

	public static Formatter formatter;

	public static void main(String... args) throws ClassNotFoundException, FileNotFoundException, IOException {

		formatter = new Formatter("runningTimes.csv");
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(mergedInfoPath));
		List<MergingResult> mergingResults = (List<MergingResult>) in.readObject();
		in.close();

		ClassLoader classLoader = SingleTestRunner.class.getClassLoader();

		runTestsBeforeMerge(mergingResults, classLoader);

		runTestsAfterMerge(mergingResults, classLoader);

		for (MergingResult mr : mergingResults) {
			// ExtendedRunner.skipIgnored = false;
			long before = runTests(mr.getMergedTestCases(), classLoader);
			// ExtendedRunner.skipIgnored = true;
			long after = runTest(mr.getMergedClassName(), mr.getMergedTestCaseName(), classLoader);
			if (before != -1 && after != -1)
				formatter.format("%s,%d,%d,%d,%s,%d,%d,%d\n", mr.getMergedTestCases().toString().replaceAll(",", " "),
						mr.getMergedTestCases().size(), mr.getBefore() - mr.getMergedTestCases().size(), mr.getAfter(),
						mr.getMergedClassName() + "." + mr.getMergedTestCaseName(), before, after, before - after);
		}

		formatter.flush();
		formatter.close();
	}

	private static void runTestsAfterMerge(List<MergingResult> mergingResults, ClassLoader classLoader) {
		for (MergingResult mr : mergingResults) {
			runTest(mr.getMergedClassName(), mr.getMergedTestCaseName(), classLoader);
		}
	}

	private static void runTestsBeforeMerge(List<MergingResult> mergingResults, ClassLoader classLoader) {
		for (MergingResult mr : mergingResults) {

			runTests(mr.getMergedTestCases(), classLoader);

		}
	}

	public static long runTests(Set<String> testCases, ClassLoader classLoader) {
		long nano = 0;
		for (String testCase : testCases) {
			long runTime = runTest(Utils.getTestClassNameFromTestCase(testCase), Utils.getTestCaseName(testCase),
					classLoader);
			if (runTime == -1)
				return -1;
			nano += runTime;
		}
		return nano;
	}

	public static long runTest(String className, String testName, ClassLoader classLoader) {

		System.out.println("running test " + className + "." + testName);
		long nano = 0;
		try {
			Class aClass = classLoader.loadClass(className);
			Request request = Request.method(aClass, testName);

			for (int i = 0; i < TIMES; i++) {
				long runTime = System.nanoTime();
				Result result = new JUnitCore().run(request);
				runTime = System.nanoTime() - runTime;

				if (result.wasSuccessful() == false || result.getIgnoreCount() != 0)
					return -1;
				nano += runTime;
			}
			// formatter.format("%s,%d\n", className + "." + testName,
			// System.nanoTime() - nano);
			// System.out.println(result.wasSuccessful() ? 0 : 1);

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return nano;

	}
}