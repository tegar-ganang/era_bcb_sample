package gov.fnal.mcas.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DataSourceUtilities {

    /**
	 * create the name of the transformation (e.g. XSLT) doc for this Data Source.
	 * This name is used in the XMLDB
	 * @param dataSourceName the name of the data source
	 * @return a String containing the name of the transformation doc (e.g. in XSLT format)
	 */
    public static String composeTransformationDocName(String dataSourceName) {
        return dataSourceName + "-transformation";
    }

    /**
	 * create the name of the validation (e.g. XSD) doc for this Data Source.
	 * This name is used in the XMLDB
	 * @param dataSourceName the name of the data source
	 * @return a String containing the name of the validation doc (e.g. in XSD format)
	 */
    public static String composeValidationDocName(String dataSourceName) {
        return dataSourceName + "-validation";
    }

    /**
	 * create the absolute path of the transformation (e.g. XSLT) doc in the local store for this Data Source.
	 * @param dataSourceName the name of the data source
	 * @param config the tool configuration 
	 * @return a File containing the absolute path of the transformation doc in the local store 
	 */
    public static File composeTransformationDocInStoreFile(String dataSourceName, String storePath) {
        return new File(storePath + "/" + composeTransformationDocName(dataSourceName));
    }

    /**
	 * create the absolute path of the validation (e.g. XSD) doc in the local store for this Data Source.
	 * @param dataSourceName the name of the data source
	 * @param config the tool configuration 
	 * @return a File containing the absolute path of the validation doc in the local store 
	 */
    public static File composeValidationDocInStoreFile(String dataSourceName, String storePath) {
        return new File(storePath + "/" + composeValidationDocName(dataSourceName));
    }

    /**
	 * create the name of the Data Source database file that will hold the data source data
	 * @param dataSourceName the name of the data source
	 * @param dataSourceProject the name of the data source project
	 * @return a String containing the name of database for the given data source
	 */
    public static String composeDataSourceDatabaseName(String dataSourceName) {
        return dataSourceName;
    }

    /**
	 * Reads the content of a file and returns it as a string
	 * @param file the file object that needs to be read
	 * @return a String containing the content of the file
	 */
    public static String readTextFile(File file) throws IOException {
        StringBuffer sb = new StringBuffer(1024);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        char[] chars = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(chars)) > -1) {
            sb.append(String.valueOf(chars));
        }
        reader.close();
        return sb.toString().trim();
    }

    /**
	 * Copy a source file to a destination 
	 */
    public static void copyFile(File src, File dst) throws IOException {
        File inputFile = src;
        File outputFile = dst;
        FileReader in = new FileReader(inputFile);
        FileWriter out = new FileWriter(outputFile);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
    }
}
