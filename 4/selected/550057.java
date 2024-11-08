package de.fraunhofer.isst.axbench.test;

import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * @brief Class for the generation of write test cases.
 * 
 * The write test cases read a model form a given file, using the AXLReader,
 * then write it to a file, using the AXLWriter, and read the written model again.
 * Then the write test case compares both models to each other, by comparing the
 * number of children. A number of expected errors can be given, for the determination
 * of test success or test failure. Giving 0 as expected error count results in a test
 * case, expecting that reading and writing the model does not generate errors, and that 
 * both read models are not null and contain the equal amount of elements. Giving a positive
 * number of expected errors, results in a test case, expecting the given number of errors
 * to occur during read operation.
 * @author nschult
 * @since 0.7.2
 *
 */
public class WriteTestGenerator {

    /**
	 * @brief The entry point for the write test case generator. Creates a new file and calls the write functions.
	 * @param className The name of the test case.
	 * @param fileName The path to the model file.
	 * @param expErrors number of expected errors during read operation.
	 */
    public static void writeWriteModelTestCase(String className, String fileName, int expErrors) {
        System.out.println("Writing WriteModelTestCase \"" + className + "\" for file " + fileName);
        try {
            FileWriter filestream = new FileWriter("../aXLangModel/src/de/fraunhofer/isst/axbench/test/autoTests/" + className + ".java");
            BufferedWriter out = new BufferedWriter(filestream);
            out.write(writeImportBlock() + writeHeaderBlock(className) + writeSetUpMethod() + writeTearDownMethod() + writeReadFileMethod(expErrors) + writeWriteFileMethod() + writeCheckMethod() + writeTestMethod(fileName) + writeEndOfFile());
            out.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
	 * @brief Writes an static import block.
	 * @return Returns a string containing a import block code piece.
	 */
    public static String writeImportBlock() {
        return "// This file was automatically generated.\n\n" + "package de.fraunhofer.isst.axbench.test.autoTests;\n\n" + "import java.io.BufferedInputStream;\n" + "import java.io.File;\n" + "import java.io.FileInputStream;\n" + "import java.io.FileOutputStream;\n" + "import java.io.IOException;\n" + "import java.io.InputStream;\n" + "import java.io.OutputStream;\n" + "import junit.framework.TestCase;\n" + "import org.junit.After;\n" + "import org.junit.Before;\n" + "import org.junit.Test;\n" + "import java.util.Collection;\n" + "import de.fraunhofer.isst.axbench.axlang.api.IAXLangElement;\n" + "import de.fraunhofer.isst.axbench.api.AXLMessage;\n" + "import de.fraunhofer.isst.axbench.api.converter.AXLFile;\n" + "import de.fraunhofer.isst.axbench.axlang.elements.Model;\n" + "import de.fraunhofer.isst.axbench.converter.AXLReader;\n" + "import de.fraunhofer.isst.axbench.converter.AXLWriter;\n" + "import de.fraunhofer.isst.axbench.operations.checker.ValidityChecker;\n\n";
    }

    /**
	 * @brief Writes the class declaration and global variables.
	 * @param className The name of the class to be tested extended with test.
	 * @return String containing class declaration and global declarations.
	 */
    public static String writeHeaderBlock(String className) {
        return "public class " + className + " extends TestCase{\n" + "\tModel axlModel1;\n" + "\tModel axlModel2;\n" + "\tAXLWriter theWriter;\n" + "\tValidityChecker theChecker;\n" + "\tInputStream stmIn;\n\n";
    }

    /**
	 * @brief Writes the set up method for the test case.
	 * @return String containing a static set up block.
	 */
    public static String writeSetUpMethod() {
        return "\t@Override\n" + "\t@Before\n" + "\tpublic void setUp() throws Exception {\n" + "\t\taxlModel1 = null;\n" + "\t\taxlModel2 = null;\n" + "\t\ttheWriter = new AXLWriter();\n" + "\t\ttheChecker = new ValidityChecker();\n" + "\t\tstmIn = null;\n" + "\t}\n\n";
    }

    /**
	 * @brief Writes the tear down method for the test case.
	 * @return String containing a static tear down block.
	 */
    public static String writeTearDownMethod() {
        return "\t@Override\n" + "\t@After\n" + "\tpublic void tearDown() throws Exception {\n" + "\t\taxlModel1 = null;\n" + "\t\taxlModel2 = null;\n" + "\t\ttheWriter = null;\n" + "\t\ttheChecker = null;\n" + "\t\tstmIn.close();\n" + "\t\tstmIn = null;\n" + "\t}\n\n";
    }

    /**
	 * @brief Writes the actual read method, which read from a given file and tests the result.
	 * @param expErrors Number of expected errors.
	 * @return A string containing the method statement 
	 */
    public static String writeReadFileMethod(int expErrors) {
        return "\tpublic Model readFromFile(File fleIn){\n" + "\t\tAXLReader theReader = new AXLReader();\n" + "\t\tModel axlModel = null;\n" + "\t\ttry{\n" + "\t\t\tAXLFile theAXLFile = new AXLFile(fleIn.getPath());\n" + "\t\t\tFile fleIn1 = new File(theAXLFile.getPathAndFile());\n" + "\t\t\tstmIn = new BufferedInputStream(new FileInputStream(fleIn1));\n" + "\t\t\taxlModel = (Model) theReader.readAXLangElement(theAXLFile, stmIn);\n" + "\t\t\tString errorString = \"\";\n" + "\t\t\tfor(AXLMessage theError : theReader.getErrors()){\n" + "\t\t\t\terrorString += theError.getText();\n" + "\t\t\t}\n" + "\t\t\tif(theReader.getErrorCount() != " + expErrors + ") fail(\"The following errors occured while reading the model:\" +errorString);\n" + "\t\t\ttheReader = null;\n" + "\t\t}catch (IOException e){\n" + "\t\t\ttheReader = null;\n" + "\t\t\tfail(\"Caught unexpected exception while reading file. \"+e.getMessage());\n" + "\t\t}\n" + "\t\treturn axlModel;\n" + "\t}\n\n";
    }

    /**
	 * @brief Writes the write method. Test fails automatically, when errors occur.
 	 * @return Returns a string containing a write method.
	 */
    public static String writeWriteFileMethod() {
        return "\tpublic void writeToFile(AXLFile theAXLFile){\n" + "\t\ttry{\n" + "\t\t\tFile fleOut = new File(theAXLFile.getPathAndFile());\n" + "\t\t\tOutputStream stmOut = new FileOutputStream(fleOut);\n" + "\t\t\ttheWriter.writeAXLangElement(theAXLFile.getParent(), theAXLFile, stmOut);\n" + "\t\t\tstmOut.close();\n" + "\t\t}catch (IOException e){\n" + "\t\t\tfail(\"Caught unexpected exception while writing file. \"+e.getMessage());\n" + "\t\t}\n" + "\t}\n\n";
    }

    /**
	 * @brief Method which compares to given models to each other by comparing the number of children and whether both models contain the same children.
	 * @return Returns a string representation of the comparation method.
	 */
    public static String writeCheckMethod() {
        return "\tpublic void checkModels(Model axlModel1, Model axlModel2){\n" + "\t\tassertNotNull(\"axlModel1 was null, but expected it not to be.\",axlModel1);\n" + "\t\tassertNotNull(\"axlModel2 was null, but expected it not to be.\",axlModel2);\n" + "\t\tassertEquals(\"The amount of model children differ. \",axlModel1.getAllChildren().size(),axlModel2.getAllChildren().size());\n" + "\t\tfor(Collection<IAXLangElement> axlElement1 : axlModel1.getChildren().values()){\n" + "\t\t\tfor(IAXLangElement axlElement2 : axlElement1){\n" + "\t\t\t\tboolean found = false;\n" + "\t\t\t\tfor(Collection<IAXLangElement> axlElement3 : axlModel2.getChildren().values()){\n" + "\t\t\t\t\tfor(IAXLangElement axlElement4 : axlElement3){\n" + "\t\t\t\t\t\tif(axlElement2.compareTo(axlElement4) == 0) found = true;\n" + "\t\t\t\t\t}\n" + "\t\t\t\t}\n" + "\t\t\t\tif(!found) fail(\"axlModel1 element \"+axlElement2.getIdentifier()+\" was not found in axlModel2\");\n" + "\t\t\t}\n" + "\t\t}\n" + "\t\tfor(Collection<IAXLangElement> axlElement1 : axlModel2.getChildren().values()){\n" + "\t\t\tfor(IAXLangElement axlElement2 : axlElement1){\n" + "\t\t\t\tboolean found = false;\n" + "\t\t\t\tfor(Collection<IAXLangElement> axlElement3 : axlModel1.getChildren().values()){\n" + "\t\t\t\t\tfor(IAXLangElement axlElement4 : axlElement3){\n" + "\t\t\t\t\t\tif(axlElement2.compareTo(axlElement4) == 0) found = true;\n" + "\t\t\t\t\t}\n" + "\t\t\t\t}\n" + "\t\t\t\tif(!found) fail(\"axlModel1 element \"+axlElement2.getIdentifier()+\" was not found in axlModel2\");\n" + "\t\t\t}\n" + "\t\t}\n" + "\t}\n\n";
    }

    /**
	 * @brief Writes a test method, which calls the read, write and check methods accordingly.
	 * @param fileName The path to the model file.
	 * @return Returns a string representation of the test method.
	 */
    public static String writeTestMethod(String fileName) {
        return "\t@Test\n" + "\tpublic void testReadModel(){\n" + "\t\tFile fleIn = new File(\"" + fileName + "\");\n" + "\t\taxlModel1 = readFromFile(fleIn);\n" + "\t\tassertNotNull(\"axlModel1 was null, but expected it not to be.\",axlModel1);\n" + "\t\tAXLFile theAXLFile = new AXLFile(new File(\"../aXLangModel/src/de/fraunhofer/isst/axbench/test/autoTests/generated.axl\").getPath(), axlModel1);\n" + "\t\twriteToFile(theAXLFile);\n" + "\t\tfleIn = new File(\"../aXLangModel/src/de/fraunhofer/isst/axbench/test/autoTests/generated.axl\");\n" + "\t\taxlModel2 = readFromFile(fleIn);\n" + "\t\tcheckModels(axlModel1, axlModel2);\n" + "\t}\n\n";
    }

    /**
	 * @brief Writes the end of file statement.
	 * @return string with closed bracket and end of file.
	 */
    public static String writeEndOfFile() {
        return "} //<EOF>\n";
    }
}
