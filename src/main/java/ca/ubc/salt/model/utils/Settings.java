package ca.ubc.salt.model.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Settings {

	public final static Logger consoleLogger = LogManager.getRootLogger();

	public static final String LIBRARY_JAVA = "/Library/Java/JavaVirtualMachines/jdk1.8.0_111.jdk/Contents/Home/jre/lib/rt.jar";

	public static final String FILESYSTEM_PATH = "/Users/tsigalko18/Desktop/toTry/";
//	public static final String FILESYSTEM_PATH = "/Users/astocco/Desktop/";
	
//	public static final String SUBJECT = "assertj-core";
//	public static final String SUBJECT = "commons-math";
//	public static final String SUBJECT = "math-simple-complete";
//	public static final String SUBJECT = "commons-lang-master";
//	public static final String SUBJECT = "pmd-core";
//	public static final String SUBJECT = "pmd-java";
//	public static final String SUBJECT = "checkstyle-master";
//	public static final String SUBJECT = "Tudu-Lists-master";
//	public static final String SUBJECT = "lambdaj-master";
//	public static final String SUBJECT = "truth-master"; // small results (7%)
//	public static final String SUBJECT = "jfreechart-pulito";
//	public static final String SUBJECT = "java-library-master";
//	public static final String SUBJECT = "checkstyle-6.14";
	public static final String SUBJECT = "log4j-core";
	
//	public static final String SUBJECT = "crunch-core-normal";
//	public static final String SUBJECT = "crunch-kafka";
//	public static final String SUBJECT = "tika-xmp";
//	public static final String SUBJECT = "xmlsec-2.0.8";
//	public static final String SUBJECT = "commons-email";
//	public static final String SUBJECT = "log4j-core";

	public static final String PROJECT_PATH = FILESYSTEM_PATH + SUBJECT;

	public static final String PROJECT_INSTRUMENTED_PATH = 	PROJECT_PATH + "-instrumented";
	public static final String PROJECT_MERGED_PATH = 		PROJECT_PATH + "-merged";
	public static final String PROJECT_STOPWATCH_PATH = 	PROJECT_PATH + "-stopwatched";
	public final static String tracePaths = 				PROJECT_INSTRUMENTED_PATH + "/traces";
	public final static String classFileMappingPath = 		SUBJECT + "-classFileMapping.xml";
	public final static String shortClassFileMappingPath = 	SUBJECT + "-shortClassFileMapping.xml";
	
	// tests timeout in ms
	public final static String TIMEOUT = "180000";

	public final static String[] methodBlackList = new String[] {""};

	public static final Set<String> blackListSet = new HashSet<String>(Arrays.asList(methodBlackList));
	
	public static final String TEST_CLASS = "/Users/arash/Research/repos/commons-math/src/test/java/org/apache/commons/math4/transform/FastFourierTransformerTest.java";
    public static final String PROD_TEST_CLASS = "/Users/Arash/Research/repos/commons-math/src/main/java/org/apache/commons/math4/complex/Complex.java";

	public static String getInstrumentedCodePath(String oldPath) {
		return oldPath.replaceFirst(PROJECT_PATH, PROJECT_INSTRUMENTED_PATH);
	}

	public static String getTimedCodePath(String oldPath) {
		return oldPath.replaceFirst(PROJECT_PATH, PROJECT_STOPWATCH_PATH);
	}

}
