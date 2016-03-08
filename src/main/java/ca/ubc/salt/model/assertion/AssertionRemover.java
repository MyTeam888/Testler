package ca.ubc.salt.model.assertion;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;

import japa.parser.ParseException;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.visitor.VoidVisitorAdapter;

public class AssertionRemover
{

    static String projectPath = "/Users/arash/Desktop/Research/Repos/commons-math";
    static File testFolder = new File("/Users/arash/Desktop/Research/Repos/commons-math/src/test");
    // static File testFolder = new
    // File("/Users/arash/Desktop/Research/Repos/commons-math/src/test/java/org/apache/commons/math4/dfp/DfpDecTest.java");

    public static void main(String[] args) throws ParseException, IOException, InterruptedException
    {
	removeAssertionsAndRun(testFolder);
    }

    public static void removeAssertionsAndRun(File testClass) throws ParseException, IOException, InterruptedException
    {
	// Scanner sc = new Scanner(System.in);

	if (testClass.isFile())
	{

	    String extention = FilenameUtils.getExtension(testClass.getName());
	    if (extention.equals("java") && testClass.getName().toLowerCase().contains("test"))
	    {
		System.out.println(String.format("removing assertions of %s\n", testClass.getAbsolutePath()));
		TestSuiteComposer composer = new TestSuiteComposer(testClass);

		File originalTestClass = new File(testClass.getAbsolutePath() + ".original");
		Files.copy(testClass.toPath(), originalTestClass.toPath(), StandardCopyOption.REPLACE_EXISTING);

		HashSet<Integer> assertionToRemove = new HashSet<Integer>();
		for (int i = 0; i < composer.getTotalPossibleNumberOfAssertions(); i++)
		{

		    assertionToRemove.add(i);

		    composer.compose(assertionToRemove);

		    String[] cmd = { "mvn", "-Dtest=", "-DfailIfNoTests=false", "test" };
		    cmd[1] = cmd[1] + FilenameUtils.getBaseName(testClass.getName());

		    System.out.println(String.format("running command %s\n", Arrays.toString(cmd)));

		    String result = runCommand(cmd, projectPath);

		    if (!result.contains("BUILD SUCCESS"))
			System.out.println(
				String.format("removing %d assertion in line %s in file %s results in failure!\n", i,
					composer.removedAssertionLines.toString(), testClass.getCanonicalPath()));

		    assertionToRemove.remove(i);
		    // sc.next();
		    Files.copy(originalTestClass.toPath(), testClass.toPath(), StandardCopyOption.REPLACE_EXISTING);
		    composer.totalPossibleNumberOfAssertions = composer.serialNumber;
		}

		originalTestClass.delete();
	    }

	} else if (testClass.isDirectory())
	{
	    File[] listOfFiles = testClass.listFiles();
	    for (int i = 0; i < listOfFiles.length; i++)
	    {
		removeAssertionsAndRun(listOfFiles[i]);
	    }
	}

    }

    public static String runCommand(String[] commands, String path) throws IOException, InterruptedException
    {
	StringBuffer result = new StringBuffer();

	ProcessBuilder builder = new ProcessBuilder(commands);
	builder.redirectErrorStream(true);
	builder.directory(new File(path));
	Process process = builder.start();

	Scanner scInput = new Scanner(process.getInputStream());
	if (!process.waitFor(45, TimeUnit.MINUTES))
	{
	    process.destroy();
	}

	while (scInput.hasNext())
	{
	    String line = scInput.nextLine();
	    result.append(line);
	    result.append("\n");
	    // System.out.println(line);

	}

	return result.toString();

    }
}
