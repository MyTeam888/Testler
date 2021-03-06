package ca.ubc.salt.model.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import Comparator.NaturalOrderComparator;
import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.Instrumenter;

public class Utils {

	public static Map<String, String> classFileMapping;
	static {
		classFileMapping = getClassFileMapping();
	}

	public static LoadingCache<String, ClassModel> classes;

	static {
		classes = CacheBuilder.newBuilder().maximumSize(100).build(new CacheLoader<String, ClassModel>() {
			@Override
			public ClassModel load(String className) throws Exception {
				// make the expensive call
				return getTheModelForTheClass(className);
			}
		});
	}

	public static List<ClassModel> getAllClasses(List<String> classesNames) {
		
		List<ClassModel> classesModel = new LinkedList<ClassModel>();
		
		for (String testClassName : classesNames) {
			try {
				ClassModel theClass;
				theClass = Utils.getTheModelForTheClass(testClassName);
				if (theClass != null)
					classesModel.add(theClass);
				else {
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return classesModel;
	}

	private static ClassModel getTheModelForTheClass(String testClassName) {
		try {
			String testClassPath = null;
			
			if (testClassName.indexOf('.') == -1) {
				testClassName = Utils.getTestCaseName(testClassName);
				testClassPath = Instrumenter.classFileMappingShortName.get(testClassName);
			} else
				testClassPath = Utils.classFileMapping.get(testClassName);
			
			if(testClassPath == null){
				System.out.println("[ERROR]\tUtils.getTheModelForTheClass()::Model not found for class " + testClassName);
				return null;
			}
			
			String source = FileUtils.readFileToString(testClassPath);
			Document document = new Document(source);
			List<ClassModel> classes = ClassModel.getClasses(document.get(), true, testClassPath,
					new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });

			ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());

			for (ClassModel clazz : classes) {

				if (clazz.name.contains(testClassName)) {
					return clazz;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getNamePrecision(String className, int precision) {
		for (int i = className.length() - 1; i >= 0; i--) {
			if (className.charAt(i) == '.') {
				precision--;
				if (precision == 0)
					return className.substring(i + 1);
			}
		}
		return "";
	}

	public static <T> Set intersection(List<Set<T>> sets) {
		Set common = new HashSet();
		if (sets.size() != 0) {
			ListIterator<Set<T>> iterator = sets.listIterator();
			common.addAll(iterator.next());
			while (iterator.hasNext()) {
				common.retainAll(iterator.next());
			}
		}
		return common;
	}

	/**
	 * checks whether the file is a test (just by looking at the filesystem
	 * path)
	 * 
	 * @param classFile
	 * @return
	 */
	public static boolean isTestClass(File classFile) {
		if (classFile.getAbsolutePath().contains("src/test/") || classFile.getAbsolutePath().contains("src/it/"))
			return true;
		else
			return false;
	}

	/**
	 * remove files that can cause problems to merging phase
	 */
	public static void cleanProjectBeforeMerging() {
		
		String fileToDelete = System.getProperty("user.dir") + "/" + Settings.SUBJECT + "-components.xml";
		
		String[] cmdRM = new String[] { "rm", fileToDelete };
		try {
			if(new File(fileToDelete).exists()){
				System.out.println(runCommand(cmdRM, "/"));
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		fileToDelete = System.getProperty("user.dir") + "/" + Settings.SUBJECT + "-unique.xml";
		
		cmdRM = new String[] { "rm", fileToDelete };
		try {
			if(new File(fileToDelete).exists()){
				System.out.println(runCommand(cmdRM, "/"));
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		fileToDelete = System.getProperty("user.dir") + "/" + Settings.SUBJECT + "-mergingStat.csv";
		
		cmdRM = new String[] { "rm", fileToDelete };
		try {
			if(new File(fileToDelete).exists()){
				System.out.println(runCommand(cmdRM, "/"));
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		fileToDelete = System.getProperty("user.dir") + "/" + Settings.SUBJECT + "-stat.csv";
		
		cmdRM = new String[] { "rm", fileToDelete };
		try {
			if(new File(fileToDelete).exists()){
				System.out.println(runCommand(cmdRM, "/"));
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * creates a separate copy of the project for the instrumentation phase
	 * 
	 * @param from
	 * @param to
	 */
	public static void copyProject(String from, String to) {

		// copies the project
		String[] cmdCP = new String[] { "cp", "-r", from, to };
		try {
			System.out.println(runCommand(cmdCP, "/"));
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		String traces = System.getProperty("user.dir") + "/InstrumentationHelper/traces";
		String instrument = System.getProperty("user.dir") + "/InstrumentationHelper/instrument";

		// adds the traces directory
		cmdCP = new String[] { "cp", "-r", traces, to };
		try {
			System.out.println(runCommand(cmdCP, "/"));
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// adds the instrument directory
		String todeep = to.concat("/src/main/java");
		cmdCP = new String[] { "cp", "-r", instrument, todeep };
		try {
			System.out.println(runCommand(cmdCP, "/"));
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Programmatically modifies the pom file to add the Xstream library
		// dependency

		// Reading pom
		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model = null;
		try {
			model = reader.read(new FileReader(new File(to, "/pom.xml")));
		} catch (IOException | XmlPullParserException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Editing pom
		Dependency xstream = new Dependency();
		xstream.setGroupId("com.thoughtworks.xstream");
		xstream.setArtifactId("xstream");
		xstream.setVersion("1.4.9");

		boolean present = false;

		for (int i = 0; i < model.getDependencies().size(); i++) {
			Dependency d = (Dependency) model.getDependencies().get(i);
			if (d.getArtifactId().equals(xstream.getArtifactId())) {
				present = true;
				break;
			}
		}

		if (!present) {
			model.addDependency(xstream);
		}

		// Writing pom
		MavenXpp3Writer writer = new MavenXpp3Writer();
		try {
			writer.write(new FileWriter(new File(to, "/pom.xml")), model);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static ASTNode createBlockWithText(String str) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_STATEMENTS);
		Map pOptions = JavaCore.getOptions();
		pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(pOptions);

		parser.setSource(str.toCharArray());
		ASTNode cu = parser.createAST(null);

		return cu;
	}

	public static ASTNode createExpWithText(String str) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_EXPRESSION);
		Map pOptions = JavaCore.getOptions();
		pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(pOptions);

		parser.setSource(str.toCharArray());
		ASTNode cu = parser.createAST(null);

		return cu;
	}

	public static String runCommand(String[] commands, String path) throws IOException, InterruptedException {
		StringBuffer result = new StringBuffer();

		ProcessBuilder builder = new ProcessBuilder(commands);
		builder.redirectErrorStream(true);
		builder.directory(new File(path));
		Process process = builder.start();

		Scanner scInput = new Scanner(process.getInputStream());
		while (scInput.hasNext()) {
			String line = scInput.nextLine();
			result.append(line);
			result.append("\n");
		}
		process.waitFor();
		return result.toString();

	}

	public static Map<String, String> getClassFileMapping() {
		try {
			XStream xstream = new XStream(new StaxDriver());
			return (Map<String, String>) xstream.fromXML(new File(Settings.classFileMappingPath));
		} catch (Exception e) {
			Settings.consoleLogger.error("Class file mapping is missing. Creating a new one");
			return new HashMap<String, String>();
		}
	}

	public static Map<String, String> getClassFileMappingShortName() {
		try {
			XStream xstream = new XStream(new StaxDriver());
			return (Map<String, String>) xstream.fromXML(new File(Settings.shortClassFileMappingPath));
		} catch (Exception e) {
			Settings.consoleLogger.error("Class file short mapping is missing. Creating a new one");
			return new HashMap<String, String>();
		}
		
	}

	public static String getClassFile(String className) {
		return Utils.classFileMapping.get(className);
	}

	public static String getClassFileForProjectPath(String className, String projectPath) {
		return Utils.classFileMapping.get(className).replace(Settings.PROJECT_PATH, projectPath);
	}

	public static String getTestCaseFile(String testCase) {
		String className = getTestClassNameFromTestCase(testCase);
		return Utils.classFileMapping.get(className);
	}

	public static String getTestClassNameFromTestCase(String testCase) {
		int index = testCase.lastIndexOf('.');
		String className = testCase.substring(0, index);
		return className;
	}

	public static <K, V> void addToTheListInMap(Map<K, List<V>> map, K key, V value) {
		List<V> list = map.get(key);
		if (list == null) {
			list = new LinkedList<V>();
			map.put(key, list);
		}
		list.add(value);
	}

	public static <K, V> void addToTheSetInMap(Map<K, Set<V>> map, K key, V value) {
		Set<V> list = map.get(key);
		if (list == null) {
			list = new HashSet<V>();
			map.put(key, list);
		}
		list.add(value);
	}

	public static <K, V> void addAllTheSetInMap(Map<K, Set<V>> map, K key, Set<V> value) {
		Set<V> list = map.get(key);
		if (value == null)
			return;
		if (list == null) {
			list = new HashSet<V>();
			map.put(key, list);
		}
		list.addAll(value);
	}

	public static <K, V> void addAllTheListInMap(Map<K, List<V>> map, K key, List<V> value) {
		List<V> list = map.get(key);
		if (value == null)
			return;
		if (list == null) {
			list = new ArrayList<V>();
			map.put(key, list);
		}
		list.addAll(value);
	}

	public static <K, V> boolean containsInSetInMap(Map<K, Set<V>> map, K key, V value) {
		Set<V> set = map.get(key);
		if (set == null) {
			return false;
		}
		return set.contains(value);
	}

	public static <K1, K2, V> void addToTheMapInMap(Map<K1, Map<K2, V>> map, K1 key1, K2 key2, V value) {
		Map<K2, V> m = map.get(key1);
		if (m == null) {
			m = new TreeMap<K2, V>(new NaturalOrderComparator());
			map.put(key1, m);
		}
		m.put(key2, value);
	}

	public static <K, V> void removeFromTheSetInMap(Map<K, Set<V>> map, K key, V value) {
		Set<V> list = map.get(key);
		if (list != null) {
			list.remove(value);
		} else
			map.remove(key);
	}

	public static String getTestCaseName(String testCase) {
		if (testCase == null)
			return "";
		int index = testCase.lastIndexOf('.');
		return testCase.substring(index + 1);
	}

	public static String getTestCaseNameFromTestStatementWithoutClassName(String testStatement) {
		String testCase = getTestCaseNameFromTestStatement(testStatement);
		return testCase.substring(testCase.lastIndexOf('.') + 1);
	}

	public static String getTestCaseNameFromTestStatement(String testStatement) {
		int index = testStatement.lastIndexOf('-');
		if (index == -1)
			return null;
		return testStatement.substring(0, index);
	}

	public static Map<String, Set<String>> getTestClassMapFromTestCases(Set<String> testCases) {
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		for (String testCase : testCases) {
			String testClass = Utils.getTestClassNameFromTestCase(testCase);
			Utils.addToTheSetInMap(map, testClass, testCase);
		}
		return map;
	}

	public static String nextOrPrevState(List<String> testCases, String state, boolean next) {

		ArrayList<String> sortedTestStates;
		try {
			sortedTestStates = FileUtils.sortedAllStates.get(testCases);
			return nextOrPrevState(state, sortedTestStates, next);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

	public static List<String> getAdjStates(List<String> testCases, String state, int n) {

		List<String> adjState = new ArrayList<String>();
		ArrayList<String> sortedTestStates;
		try {
			sortedTestStates = FileUtils.sortedAllStates.get(testCases);
			int index = sortedTestStates.indexOf(state);
			for (int i = Math.max(0, index - n); i < Math.min(sortedTestStates.size(), index + n); i++)
				adjState.add(sortedTestStates.get(i));
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return adjState;
	}

	public static String nextOrPrevState(String state, ArrayList<String> sortedTestStatements, boolean next) {
		// int index = Collections.binarySearch(sortedTestStatements, state, new
		// NaturalOrderComparator());
		int index = sortedTestStatements.indexOf(state);
		if (index == -1 || (next && index == sortedTestStatements.size() - 1) || (!next && index == 0))
			return "";

		String[] split = Utils.splitState(state);

		if (split.length != 2)
			return "";

		String nextState = sortedTestStatements.get(index + (next ? 1 : -1));
		String[] splitNext = Utils.splitState(nextState);

		if (splitNext.length != 2)
			return "";

		if (split[0].equals(splitNext[0]))
			return nextState;
		else
			return "";
	}

	public static String[] splitState(String state) {
		state = state.substring(0, state.lastIndexOf('.'));
		String[] split = state.split("-");
		return split;
	}

	public static String getTestClassNameFromTestStatement(String testStatement) {
		try {
			String testCaseName = getTestCaseNameFromTestStatement(testStatement);
			int index = testCaseName.lastIndexOf('.');
			return testStatement.substring(0, index);
		} catch (Exception e) {
//			Settings.consoleLogger.error("there is a state name " + testStatement);
			return "";
		}
	}

	public static Set<String> getNames(Collection<MethodInvocation> methodCalls) {
		Set<String> nameSet = new HashSet<String>();
		for (MethodInvocation mi : methodCalls) {
			nameSet.add(mi.getName().getIdentifier());
		}

		return nameSet;
	}

	public static Set<String> getNameSet(Collection<SimpleName> readVars) {
		Set<String> varNames = new HashSet<String>();
		for (SimpleName var : readVars) {
			IBinding nodeBinding = var.resolveBinding();
			IVariableBinding ivb = (IVariableBinding) nodeBinding;
			if (ivb != null)
				varNames.add(ivb.getName());
			else {
				// Settings.consoleLogger.error("binding is null for " + var);
				varNames.add(var.getIdentifier());
			}
		}
		return varNames;
	}

	public static void writebackSourceCode(Document document, String newPath) {
		try {
			FileWriter fw = new FileWriter(newPath);
			fw.write(document.get());
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void addImports(Document document, Collection<String> imports) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map<String, String> pOptions = JavaCore.getOptions();
		pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(pOptions);

		parser.setSource(document.get().toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		addImports(document, imports, cu);

	}

	public static void addImports(Document document, Map<String, ImportDeclaration> imports) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map<String, String> pOptions = JavaCore.getOptions();
		pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(pOptions);

		parser.setSource(document.get().toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		addImports(document, imports, cu);

	}

	private static void addImports(Document document, Collection<String> imports, CompilationUnit cu) {
		cu.recordModifications();

		// String[] imports = new String[] { "java.io.FileWriter",
		// "java.io.IOException", "java.io.ObjectOutputStream",
		// "com.thoughtworks.xstream.XStream",
		// "com.thoughtworks.xstream.io.xml.StaxDriver" };
		for (String name : imports)
			addImport(cu, name);

		TextEdit edits = cu.rewrite(document, null);

		try {
			edits.apply(document);
		} catch (MalformedTreeException | BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void addImports(Document document, Map<String, ImportDeclaration> imports, CompilationUnit cu) {
		cu.recordModifications();

		// String[] imports = new String[] { "java.io.FileWriter",
		// "java.io.IOException", "java.io.ObjectOutputStream",
		// "com.thoughtworks.xstream.XStream",
		// "com.thoughtworks.xstream.io.xml.StaxDriver" };
		for (Entry<String, ImportDeclaration> entry : imports.entrySet())
			addImport(cu, entry.getKey(), entry.getValue());

		TextEdit edits = cu.rewrite(document, null);

		try {
			edits.apply(document);
		} catch (MalformedTreeException | BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void addImport(CompilationUnit cu, String name) {
		AST ast = cu.getAST();
		ImportDeclaration imp = ast.newImportDeclaration();
		String[] split = name.split(" ");
		if (split.length == 2 && split[0].equals("static")) {
			imp.setName(ast.newName(split[1]));
			imp.setStatic(true);
			name = split[1];
		} else {
			imp.setName(ast.newName(name));
			imp.setStatic(false);
		}
		boolean isNew = true;
		for (Object obj : cu.imports()) {
			ImportDeclaration imdec = (ImportDeclaration) obj;
			if (imdec.getName().getFullyQualifiedName().equals(name)) {
				isNew = false;
				break;
			}
		}
		if (isNew)
			cu.imports().add(imp);
	}

	public static void addImport(CompilationUnit cu, String name, ImportDeclaration imp) {
		AST ast = cu.getAST();
		if (imp == null) {
			imp = ast.newImportDeclaration();
			String[] split = name.split(" ");
			if (split.length == 2 && split[0].equals("static")) {
				imp.setName(ast.newName(split[1]));
				imp.setStatic(true);
				name = split[1];
			} else {
				imp.setName(ast.newName(name));
				imp.setStatic(false);
			}
		} else
			imp = (ImportDeclaration) ASTNode.copySubtree(ast, imp);
		boolean isNew = true;
		for (Object obj : cu.imports()) {
			ImportDeclaration imdec = (ImportDeclaration) obj;
			if (imdec.getName().getFullyQualifiedName().equals(name)) {
				isNew = false;
				break;
			}
		}
		if (isNew)
			cu.imports().add(imp);
	}

	// could be improved later !
	public static String getTestClassWithMaxNumberOfTestCases(Map<String, Set<String>> map) {
		int max = Integer.MIN_VALUE;
		String maxTestClass = null;
		for (Entry<String, Set<String>> entry : map.entrySet()) {
			if (max < entry.getValue().size()) {
				max = entry.getValue().size();
				maxTestClass = entry.getKey();
			}
		}

		return maxTestClass;
	}

	public static Map<String, List<String>> cloneListInMap(Map<String, List<String>> toBeClonedMap) {
		Map<String, List<String>> uncoveredStmts = new TreeMap<String, List<String>>(new NaturalOrderComparator());
		for (Entry<String, List<String>> entry : toBeClonedMap.entrySet()) {
			addAllTheListInMap(uncoveredStmts, entry.getKey(), entry.getValue());

		}
		return uncoveredStmts;
	}

	public static List<String> getAllParentsPaths(String child) {
		List<String> parentNames = getAllParents(child);
		List<String> paths = new ArrayList<String>();
		for (String parent : parentNames) {
			String path = Instrumenter.classFileMappingShortName.get(parent);
			if (path != null)
				paths.add(path);
		}
		return paths;
	}

	public static List<String> getAllParents(String child) {
		List<String> parents = new ArrayList<String>();
		String parent = Instrumenter.childClassDependency.get(child);
		while (parent != null) {
			try {
				parents.add(parent);
				child = parent;
				parent = Instrumenter.childClassDependency.get(child);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return parents;
	}

	public static String getUnitName(String path) {
		String projectPath = Settings.PROJECT_PATH;
		int index = projectPath.lastIndexOf('/');
		projectPath = projectPath.substring(0, index);
		String reminder = path.replace(projectPath, "");
		return reminder;
	}

}
