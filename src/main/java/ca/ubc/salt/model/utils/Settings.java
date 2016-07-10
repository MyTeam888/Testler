package ca.ubc.salt.model.utils;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Settings
{

    public final static Logger consoleLogger = LogManager.getRootLogger();
//    public final static Logger fileLogger = LogManager.getLogger("FileLogger");
//    public static final String LIBRARY_JAVA = "/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/rt.jar";
    public static final String LIBRARY_JAVA = "/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home/jre/lib/rt.jar";
    public static final String PROJECT_PATH = "/Users/Arash/Research/repos/commons-math";
//    public static final String PROJECT_PATH = "/Users/arash/Documents/workspace-mars/Calculator";
    public static final String PROJECT_INSTRUMENTED_PATH = PROJECT_PATH + "-instrumented";
    public static final String PROJECT_MERGED_PATH = PROJECT_PATH + "-merged";
    public final static String tracePaths = PROJECT_INSTRUMENTED_PATH + "/traces";
    public final static String classFileMappingPath = "classFileMapping.txt";
    
    
    
    public static final String TEST_CLASS = "/Users/arash/Research/repos/commons-math/src/test/java/org/apache/commons/math4/transform/FastFourierTransformerTest.java";
    public static final String PROD_TEST_CLASS = "/Users/Arash/Research/repos/commons-math/src/main/java/org/apache/commons/math4/complex/Complex.java";
//    public static final String TEST_CLASS = "/Users/Arash/Research/repos/commons-math/src/test/java/org/apache/commons/math4/fraction/FractionTest.java";
    
    
    
    
    
    public static String getInstrumentedCodePath(String oldPath)
    {
	return oldPath.replaceFirst(PROJECT_PATH, PROJECT_INSTRUMENTED_PATH);
    }
    
    
}
