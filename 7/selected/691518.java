package unitee.server;

import java.util.Vector;
import unitee.*;
import unitee.common.*;

/**
 * Test suite is testable too. Testing a TestSuite means to run
 * all test cases and nested TestSuites of the test suite. The test suite has
 * two states: construction time and production time. Construction time is
 * when the suite is being built (on system start up) and production is
 * thereafter. Most of the suite data is managed by the states.
 * 
 * @author Ishai Asa
 * @author Oded Blayer
 * <B>NOTE</B>: This class is internal to unitee's framework.
 */
public class DefaultTestSuite extends DefaultTest implements TestSuite {

    private SuiteState state;

    public DefaultTestSuite() {
        setState(new ConstructSuiteState());
    }

    public DefaultTestSuite(String name) {
        this();
        setName(name);
    }

    private void setState(SuiteState newState) {
        state = newState;
    }

    /**
	 * Ends construction mode. This method is called after the suite was
	 * loaded and set up (by calling addTest()s) from persistent storage.
	 */
    public void endConstructionMode() {
        state.endConstructionMode();
    }

    /**
	 * Adds a test into this suite.
	 */
    public void addTest(Test test) {
        if (state.findTest(test.getName()) == null) {
            state.addTest(test);
            test.setParent(this);
        } else throw new IllegalArgumentException("test \"" + test.getName() + "\"already exists");
    }

    /**
	 * Deletes a test from this suite.
	 */
    public void removeTest(Test test) {
        state.removeTest(test);
    }

    /**
	 * Returns all tests (both test case holders and test suites).
	 */
    public Test[] getTests() {
        return state.getTests();
    }

    /**
	 * Initializes all tests within this suite.
	 */
    public void init() throws ParameterException {
        Test[] tests = getTests();
        for (int i = 0; i < tests.length; i++) tests[i].init();
    }

    /**
	 * Tries to transform this class into TestSuite.
	 * @returns this.
	 */
    public TestSuite asSuite() {
        return this;
    }

    /**
	 * Calls test() for every test within this suite.
	 * 
	 * Deprecated behavior:
	 * This method should capture all subtests (both TestCaseHolders and 
	 * TestSuites) and create a clone on which the tests will be run. By
	 * this , clients of the test suite can add or remove tests while a test()
	 * is in progress. Or , better yet , any requests to the suite will be
	 * logged and executed when the test finishes.
	 */
    public void test(TestReport report) {
        Test[] tests = getTests();
        for (int i = 0; i < tests.length; i++) tests[i].test(report);
    }

    /**
	 * Relocates a test within this test suite and shrinks the array. 
	 * <code><qouteblock>
	 * If your array is - [ a b c d e f ] , and you run relocate(2,5) 
	 * You will get -->   [ a c d e b f ].
	 * If your array is - [ a b c d e f ] , and you run relocate(5,2) 
	 * You will get -->   [ a e b c d f ].
	 *  do nothing.
	 * </qouteblock></code>
	 * @throw IllegalArgumentException if the indexes are out of range
	 */
    public void relocate(int oldIndex, int newIndex) {
        state.relocate(oldIndex, newIndex);
    }

    /**
	 * Searches for a test in this test suite and in the child suites.
	 * @return the found test , or null if the qualified name 
	 * does not exist in this suite.
	 */
    public Test findTest(String qualifiedName) {
        String currentQualify = getQualifiedName();
        Test retVal = null;
        if (currentQualify.equals(qualifiedName)) retVal = this; else if (qualifiedName != null) {
            String name;
            if (qualifiedName.startsWith(currentQualify)) {
                name = qualifiedName.substring(currentQualify.length() + 1);
            } else {
                throw new IllegalArgumentException("you must enter a fully qualified test name");
            }
            retVal = state.findTest(name);
            if (retVal == null) {
                if (name.indexOf('/') != -1) {
                    TestSuite suite = findSuite(name.substring(0, (name.indexOf('/'))));
                    if (suite != null) retVal = suite.findTest(qualifiedName);
                }
            }
        }
        return retVal;
    }

    private TestSuite findSuite(String suiteName) {
        Test test = state.findTest(suiteName);
        if (test == null) return null; else return test.asSuite();
    }

    /**
	 * Defines the state of a test suite
	 */
    private interface SuiteState {

        public Test[] getTests();

        public void addTest(Test test);

        /**
		 * Finds a test by its name.
		 * @return the found test or null on failure.
		 */
        public Test findTest(String name);

        public void endConstructionMode();

        public void removeTest(Test test);

        public void relocate(int oldIndex, int newIndex);
    }

    /**
	 * The state of suite when it is constructed. Everything about this state
	 * is "fluid" as it is a temporary state for the time the system is
	 * starting up.
	 */
    private class ConstructSuiteState implements SuiteState {

        private static final int initialTestsSize = 10;

        private Vector tests;

        public ConstructSuiteState() {
            tests = new Vector(initialTestsSize);
        }

        public Test[] getTests() {
            Test[] retTests = new Test[tests.size()];
            tests.toArray(retTests);
            return retTests;
        }

        public void addTest(Test test) {
            tests.add(test);
        }

        public void endConstructionMode() {
            DefaultTestSuite.this.setState(new NormalSuiteState(this));
        }

        public Test findTest(String name) {
            for (int i = 0; i < tests.size(); i++) {
                Test t = (Test) tests.get(i);
                if (t.getName().equals(name)) return t;
            }
            return null;
        }

        public void removeTest(Test test) {
            throw new UnsupportedOperationException("not implemented");
        }

        public void relocate(int oldIndex, int newIndex) {
            throw new UnsupportedOperationException("not implemented");
        }
    }

    /**
	 * A production state. The suite changes to this state for the remaining
	 * of the program execution.
	 */
    private class NormalSuiteState implements SuiteState {

        private Test tests[];

        public NormalSuiteState(SuiteState prevState) {
            tests = prevState.getTests();
        }

        public Test[] getTests() {
            return tests;
        }

        public void addTest(Test test) {
            Test[] resized = new Test[tests.length + 1];
            System.arraycopy(tests, 0, resized, 0, tests.length);
            tests = resized;
            tests[tests.length - 1] = test;
        }

        public void endConstructionMode() {
            throw new IllegalStateException("already in production mode");
        }

        public Test findTest(String name) {
            int index = findTestIndex(name);
            if (index == -1) return null; else return tests[index];
        }

        private int findTestIndex(String name) {
            for (int i = 0; i < tests.length; i++) {
                if (tests[i].getName().equals(name)) {
                    return i;
                }
            }
            return -1;
        }

        public void removeTest(Test test) {
            int index = findTestIndex(test.getName());
            Test[] newTests = new Test[tests.length - 1];
            System.arraycopy(tests, 0, newTests, 0, index);
            System.arraycopy(tests, index + 1, newTests, 0, tests.length - index - 1);
            tests = newTests;
        }

        public void relocate(int oldIndex, int newIndex) {
            if (oldIndex < 0 || oldIndex > tests.length - 1) {
                throw new java.lang.IllegalArgumentException();
            }
            if (newIndex < 0 || newIndex > tests.length - 1) {
                throw new java.lang.IllegalArgumentException();
            } else {
                if (newIndex > oldIndex) {
                    Test temp = tests[oldIndex];
                    for (int i = oldIndex; i < newIndex; i++) tests[i] = tests[i + 1];
                    tests[newIndex] = temp;
                } else if (oldIndex > newIndex) {
                    Test temp = tests[oldIndex];
                    for (int j = oldIndex; j > newIndex; j--) tests[j] = tests[j - 1];
                    tests[newIndex] = temp;
                }
            }
        }
    }
}
