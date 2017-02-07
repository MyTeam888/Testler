package ca.ubc.salt.model.instrumenter;

import java.io.File;
import java.io.IOException;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

@RunWith(value = Parameterized.class)
public class InstrumentedTestSuiteExecutor {

	public static void main(String[] args) throws Exception {

		Settings.consoleLogger.error("Running instrumented project");
		
		deleteTraces();
		
//		runInstrumentedProject();

	}

	private static void deleteTraces() throws Exception {
		String traceDir = Settings.PROJECT_INSTRUMENTED_PATH + "/traces";

		// clear the traces
		File f = new File(traceDir);

		if (!f.isDirectory()) {
			throw new Exception("Traces directory not found");
		} else {
			File[] traces = f.listFiles();
			for (File trace : traces) {
				if (!trace.getName().equals("init.init-.xml") && !trace.getName().equals("staticLoading.xml")) {
					if (trace.delete()) {
						// System.out.println("Deleted trace " +
						// trace.getName());
					}
				}

			}
		}
	}

	private static void runInstrumentedProject() {
		// run the instrumented project
		String[] cmdRun = new String[] { "/usr/local/Cellar/maven/3.3.9/bin/mvn", "test" };
		try {
			System.out.println(Utils.runCommand(cmdRun, Settings.PROJECT_INSTRUMENTED_PATH + "/"));
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}
