# TestModel
This tool creates a test model from existing test suites for various test refactoring purposes

In order to create the test model first you need to instrument the project.

**Instruction to Build the Test Model**

1. To instrument the project specify the project path in `Settings.PROJECT_PATH`
2. run `java ca.ubc.salt.model.instrumenter.Instrumenter`
3. You need to run the instrumented project by calling `mvn test` in the project path 
4. Now you can get the model by calling `TestMerger.createModelForTestCases(testCases)` and as input provide a list of testcase names that you want to build the model for

**Merging Usecase**

To merge test cases you can call `java ca.ubc.salt.model.merger.TestMerger` it gives list of paths for merged testcases
