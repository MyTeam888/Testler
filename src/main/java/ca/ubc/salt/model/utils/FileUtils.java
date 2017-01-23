package ca.ubc.salt.model.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.w3c.dom.NodeList;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import Comparator.NaturalOrderComparator;

public class FileUtils {
	static LoadingCache<List<String>, ArrayList<String>> sortedAllStates;
	static {
		sortedAllStates = CacheBuilder.newBuilder().maximumSize(100)
				.build(new CacheLoader<List<String>, ArrayList<String>>() {
					@Override
					public ArrayList<String> load(List<String> testCases) throws Exception {
						ArrayList<String> sortedTestStates = FileUtils.getStatesForTestCase(testCases);

						Collections.sort(sortedTestStates, new NaturalOrderComparator());

						return sortedTestStates;
					}
				});
	}

	public static HashSet<String> readSet(String path) throws FileNotFoundException {

		Scanner prodSc = new Scanner(new File(path));
		HashSet<String> set = new HashSet<String>();
		while (prodSc.hasNext()) {
			set.add(prodSc.next());
		}
		return set;
	}

	public static String readFile(File file) throws FileNotFoundException {
		Scanner sc = new Scanner(file);
		StringBuffer sb = new StringBuffer();
		while (sc.hasNextLine())
			sb.append(sc.nextLine());
		sc.close();
		return sb.toString();
	}

	public static void removeList(Set<String> a, Set<String> b) {
		for (Iterator<String> iterator = a.iterator(); iterator.hasNext();)
			if (b.contains(iterator.next()))
				iterator.remove();

	}

	public static String readFileToString(String filePath) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));

		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
		}

		reader.close();

		return fileData.toString();
	}

	public static String readFileToString(File file) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(file));

		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
		}

		reader.close();

		return fileData.toString();
	}

	public static String getState(String stateName) {

		return getState(new File(Settings.tracePaths + "/" + stateName));
	}

	public static String getState(File file) {
		String str;
		try {
			str = readFileToString(file);
			int end = str.indexOf("</vars>");
			if (end == -1)
				return "";
			try {
				return str.substring(end + 8);
			} catch (StringIndexOutOfBoundsException e) {
				return "";
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	// public static String getState(File file)
	// {
	// List<String> lines = FileUtils.getLines(file);
	// if (lines.size() > 0)
	// {
	// String state = lines.get(lines.size() - 1);
	// if (state.startsWith("<?xml"))
	// return state;
	// else
	// {
	// for (ListIterator<String> it = lines.listIterator(); it.hasNext();)
	// {
	// String line = it.next();
	// if (line.startsWith("<vars>"))
	// return it.next();
	// }
	// return "";
	// }
	// } else
	// return "";
	// }

	public static String getVars(String stateName) {
		String str;
		try {
			str = readFileToString(Settings.tracePaths + "/" + stateName);
			int begin = str.indexOf("<vars>");
			int end = str.indexOf("</vars>");
			if (begin == -1 || end == -1)
				return null;
			return str.substring(begin, end + 8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	// public static String getVars(String stateName)
	// {
	// List<String> lines = FileUtils.getLines(new File(Settings.tracePaths +
	// "/" + stateName));
	// if (lines == null)
	// return null;
	// if (lines.size() < 2)
	// return null;
	//
	// String vars = lines.get(lines.size() - 2);
	// if (vars.startsWith("<vars>"))
	// return vars;
	// else
	// {
	// for (String line : lines)
	// {
	// if (line.startsWith("<vars>"))
	// return line;
	// }
	// }
	// return null;
	// }

	public static String getMethodCalled(String stateName) {
		return getMethodCalled(new File(Settings.tracePaths + "/" + stateName));
	}

	static List<String> getLines(File fileName) {

		try {
			Scanner sc = new Scanner(fileName);
			List<String> lines = new LinkedList<String>();
			while (sc.hasNextLine())
				lines.add(sc.nextLine());
			sc.close();
			return lines;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static String getMethodCalled(File stateFile) {
		String str;
		try {
			str = readFileToString(stateFile);
			int index = str.indexOf("<vars>");
			if (index == 0)
				return null;
			if (index == -1)
				return str;
			return str.substring(0, index);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	public static ArrayList<String> getStatesForTestCase(List<String> testCases) {
		ArrayList<String> states = new ArrayList<String>();
		for (String testCase : testCases) {
			getStatesForTestCase(testCase, states);
		}
		return states;
	}

	public static List<String> getStatesForTestCase(String testCase, List<String> states) {
		File[] stateFiles = getStateFilesForTestCase(testCase);
		if (states == null)
			states = new LinkedList<String>();
		for (File file : stateFiles) {
			// String name = file.getName();
			// int index = name.lastIndexOf('.');
			// String state = name.substring(0, index);
			// states.add(state);
			states.add(file.getName());
		}

		return states;
	}

	public static File[] getStateFilesForTestCase(final String testCase) {
		File folder = new File(Settings.tracePaths);
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(testCase + "-");
			}
		};

		File[] traces = folder.listFiles(filter);
		return traces;
	}

	public static String[] getStatesForTestCase(final String testCase) {
		File folder = new File(Settings.tracePaths);
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(testCase + "-");
			}
		};

		String[] traces = folder.list(filter);
		return traces;
	}

	public static Map<String, String> getNameValuePairs(String stateName) {
		Map<String, String> nameValuePair = new HashMap<String, String>();

		String varXML = FileUtils.getVars(stateName);

		// TODO double check what to return
		if (varXML == null)
			return nameValuePair;

		List<String> stateVarNames = XMLUtils.getVars(varXML);

		int index = 0;
		NodeList nodeList = XMLUtils.getNodeList(stateName);
		for (String stateVar : stateVarNames) {
			nameValuePair.put(stateVar, XMLUtils.getXMLString(nodeList.item(index)));
			index++;
		}
		if (index != stateVarNames.size())
			Settings.consoleLogger.error("XStream error with " + stateName);

		return nameValuePair;
	}

}
