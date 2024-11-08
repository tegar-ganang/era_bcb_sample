package uk.org.windswept.feedreader;

import java.io.File;

/**
 * @version    : $Revision: 86 $
 * @author     : $Author: satkinson $
 * Last Change : $Date: 2010-11-09 16:00:22 -0500 (Tue, 09 Nov 2010) $
 * URL         : $HeadURL: http://javafeedreader.svn.sourceforge.net/svnroot/javafeedreader/trunk/src/main/java/uk/org/windswept/feedreader/BasicConfiguration.java $
 * ID          : $Id: BasicConfiguration.java 86 2010-11-09 21:00:22Z satkinson $
 */
public class BasicConfiguration {

    private File workingDirectory;

    private static BasicConfiguration instance = new BasicConfiguration();

    private BasicConfiguration() {
        workingDirectory = new File(System.getProperty("user.home"), Constants.WORKING_DIRECTORY);
        if (!workingDirectory.exists()) {
            if (!workingDirectory.mkdirs()) {
                throw new RuntimeException("Unable to create working directory : " + workingDirectory);
            }
        }
        if (!workingDirectory.isDirectory()) {
            throw new RuntimeException("Working directory is not directory : " + workingDirectory);
        }
        if (!workingDirectory.canRead() || !workingDirectory.canWrite()) {
            throw new RuntimeException("Need read and write access to working directory : " + workingDirectory);
        }
        File log4jFile = new File(workingDirectory, Constants.LOG4J_PROPERTIES);
        System.setProperty("log4j.configuration", "file:" + log4jFile.getAbsolutePath());
    }

    public static BasicConfiguration getInstance() {
        return instance;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }
}
