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
    public final static Logger fileLogger = LogManager.getLogger("FileLogger");
    public final static String tracePaths = "/Users/Arash/Research/repos/commons-math/traces";
    public static final String LIBRARY_JAVA = "/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/rt.jar";
    public static final String PROJECT_PATH = "/Users/Arash/Research/repos/commons-math/src";
    
}
