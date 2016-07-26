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
    // public final static Logger fileLogger =
    // LogManager.getLogger("FileLogger");
    public static final String LIBRARY_JAVA = "/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/rt.jar";
    // public static final String LIBRARY_JAVA =
    // "/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home/jre/lib/rt.jar";
    public static final String PROJECT_PATH = "/Users/arash/Research/repos/commons-math";
    // public static final String PROJECT_PATH =
    // "/Users/arash/Documents/workspace-mars/Calculator";
    public static final String PROJECT_INSTRUMENTED_PATH = PROJECT_PATH + "-instrumented";
    public static final String PROJECT_MERGED_PATH = PROJECT_PATH + "-merged";
    public final static String tracePaths = PROJECT_INSTRUMENTED_PATH + "/traces";
    public final static String classFileMappingPath = "classFileMapping.txt";

    public final static String[] methodBlackList = new String[] { "QRDecompositionTest.testHTrapezoidal",
	    "GillStepInterpolatorTest.serialization", "PiecewiseBicubicSplineInterpolatingFunctionTest.testParabaloid",
	    "CorrelatedRandomVectorGeneratorTest.testSampleWithZeroCovariance",
	    "SphericalPolygonsSetTest.testConcentricSubParts", "ContinuousOutputModelTest.testRandomAccess",
	    "AkimaSplineInterpolatorTest.testInterpolateParabola", "UniformCrossoverTest.testCrossover",
	    "BlockFieldMatrixTest.testOperatePremultiplyLarge", "QRDecompositionTest.testDimensions",
	    "BlockFieldMatrixTest.testGetSetColumnVectorLarge", "QRDecompositionTest.testAEqualQR",
	    "BlockRealMatrixTest.testOperatePremultiplyLarge", "BlockRealMatrixTest.testCopyFunctions",
	    "SingularValueDecompositionTest.testStability2", "BicubicInterpolatorTest.testParaboloid",
	    "RRQRDecompositionTest.testHTrapezoidal", "SingularValueDecompositionTest.testStability1",
	    "RRQRSolverTest.testOverdetermined", "MultivariateSummaryStatisticsTest.testEqualsAndHashCode",
	    "HighamHall54StepInterpolatorTest.serialization", "RRQRSolverTest.testUnderdetermined",
	    "SimplexSolverTest.testLargeModel", "CertifiedDataTest.testDescriptiveStatistics",
	    "GraggBulirschStoerStepInterpolatorTest.serialization", "QRSolverTest.testUnderdetermined",
	    "EnumeratedRealDistributionTest.testSample", "RRQRDecompositionTest.testQOrthogonal",
	    "ContinuousOutputFieldModelTest.testModelsMerging", "DfpMathTest.testSin",
	    "CertifiedDataTest.testSummaryStatistics", "QRDecompositionTest.testRUpperTriangular",
	    "BlockFieldMatrixTest.testGetSetRowLarge", "BlockRealMatrixTest.testGetSetMatrixLarge",
	    "LutherStepInterpolatorTest.serialization", "PiecewiseBicubicSplineInterpolatingFunctionTest.testPlane",
	    "RRQRDecompositionTest.testRUpperTriangular", "TriDiagonalTransformerTest.testMatricesValues5",
	    "ThreeEighthesStepInterpolatorTest.serialization",
	    "KohonenTrainingTaskTest.testTravellerSalesmanSquareTourParallelSolver",
	    "DormandPrince54StepInterpolatorTest.serialization", "EulerStepInterpolatorTest.serialization",
	    "EventFilterTest.testHistoryIncreasingForward", "BlockFieldMatrixTest.testSeveralBlocks",
	    "QRSolverTest.testOverdetermined", "TricubicInterpolatingFunctionTest.testWave", "commons-math",
	    "ArrayFieldVectorTest.testSerial", "BlockFieldMatrixTest.testGetSetMatrixLarge",
	    "CovarianceTest.testOneColumn", "BicubicInterpolatorTest.testPlane",
	    "ContinuousOutputModelTest.testBoundaries", "SimplexSolverTest.testMath930",
	    "AdamsBashforthIntegratorTest.backward", "eclipse-inst", "EventFilterTest.testHistoryDecreasingForward",
	    "EventFilterTest.testHistoryDecreasingBackward", "AkimaSplineInterpolatorTest.testInterpolateCubic",
	    "ClassicalRungeKuttaStepInterpolatorTest.serialization", "AdamsNordsieckTransformerTest.testTransformExact",
	    "BlockRealMatrixTest.testOperateLarge", "MidpointStepInterpolatorTest.serialization",
	    "BlockFieldMatrixTest.testGetSetRowVectorLarge", "RRQRDecompositionTest.testDimensions",
	    "NordsieckStepInterpolatorTest.serialization", "FieldMatrixImplTest.testWalk",
	    "PolynomialCurveFitterTest.testRedundantSolvable", "PolyhedronsSetTest.testCross",
	    "MultivariateNormalDistributionTest.testSampling", "HermiteTest.testNormalVariance",
	    "ContinuousOutputFieldModelTest.testRandomAccess", "TricubicInterpolatingFunctionTest.testPlane",
	    "BlockFieldMatrixTest.testWalk", "ContinuousOutputFieldModelTest.testBoundaries",
	    "RRQRDecompositionTest.testAPEqualQR", "EnumeratedRealDistributionTest.testIsSupportConnected",
	    "EigenDecompositionTest.testBigMatrix", "ValueServerTest.testNextDigest",
	    "MultivariateNormalMixtureModelDistributionTest.testNonUnitWeightSum",
	    "BlockFieldMatrixTest.testGetSetRowMatrixLarge", "BlockFieldMatrixTest.testGetSetColumnLarge",
	    "BlockFieldMatrixTest.testGetSetColumnMatrixLarge", "EventFilterTest.testHistoryIncreasingBackward",
	    "BicubicInterpolatingFunctionTest.testParaboloid", "BicubicInterpolatingFunctionTest.testPlane",
	    "PolygonsSetTest.testIssue880Complete", "DormandPrince853StepInterpolatorTest.serialization",
	    "KMeansPlusPlusClustererTest.testSmallDistances", "LegendreHighPrecisionTest.testInverse",
	    "Euclidean2DTest.testDimension", "ValueServerTest.testFixedSeed",
	    "EvaluationTest.testComputeValueAndJacobian", "QRDecompositionTest.testQOrthogonal",
	    "BlockFieldMatrixTest.testCopyFunctions", "EnumeratedIntegerDistributionTest.testSample",
	    "TricubicInterpolatingFunctionTest.testQuadric", "AkimaSplineInterpolatorTest.testInterpolateLine",
	    "BlockFieldMatrixTest.testOperateLarge" };
    public static final Set<String> blackListSet = new HashSet<String>(Arrays.asList(methodBlackList));

    public static final String TEST_CLASS = "/Users/arash/Research/repos/commons-math/src/test/java/org/apache/commons/math4/transform/FastFourierTransformerTest.java";
    public static final String PROD_TEST_CLASS = "/Users/Arash/Research/repos/commons-math/src/main/java/org/apache/commons/math4/complex/Complex.java";
    // public static final String TEST_CLASS =
    // "/Users/Arash/Research/repos/commons-math/src/test/java/org/apache/commons/math4/fraction/FractionTest.java";

    public static String getInstrumentedCodePath(String oldPath)
    {
	return oldPath.replaceFirst(PROJECT_PATH, PROJECT_INSTRUMENTED_PATH);
    }

}
