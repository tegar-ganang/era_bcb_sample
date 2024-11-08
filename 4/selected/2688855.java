package com.patientis.client.deploy;

import java.io.File;
import org.apache.commons.io.FileUtils;
import com.patientis.framework.utility.FileSystemUtil;

/**
 * One line class description
 *
 * 
 * <br/>  
 */
public class Mirth {

    public static final String[] jbossServiceFiles = { "/server/default/deploy/ejb3.deployer/META-INF/jboss-service.xml", "/server/default/deploy/jboss-web.deployer/server.xml", "/server/default/deploy/jbossws.sar/jbossws.beans/META-INF/jboss-beans.xml", "/server/default/deploy/http-invoker.sar/META-INF/jboss-service.xml", "/server/default/conf/jboss-service.xml" };

    /**
	 * Update the ports
	 * @param serverDir
	 * @throws Exception
	 */
    public static void updatePorts(String targetServerDir, int startPort) throws Exception {
        for (String f : jbossServiceFiles) {
            String xmlFile = targetServerDir + f;
            File file = new File(xmlFile);
            if (file.exists()) {
                String oldContents = FileSystemUtil.getTextContents(new File(xmlFile));
                String newContents = oldContents.replace("8080", String.valueOf(startPort)).replace("1099", String.valueOf(startPort + 1)).replace("1098", String.valueOf(startPort + 2));
                FileSystemUtil.createFile(xmlFile, newContents);
            } else {
                System.err.println("unable to find " + file.getAbsolutePath());
            }
        }
    }

    public static void doNotUseHostName(String targetServerDir) throws Exception {
        String filename = targetServerDir + "/server/default/deploy/http-invoker.sar/META-INF/jboss-service.xml";
        String oldContents = FileSystemUtil.getTextContents(new File(filename));
        String newContents = oldContents.replace("<attribute name=\"UseHostName\">true</attribute>", "<attribute name=\"UseHostName\">false</attribute>");
        FileSystemUtil.createFile(filename, newContents);
    }

    /**
	 * Update JBoss
	 * 
	 * @param targetServerDir
	 * @param setupDir
	 * @throws Exception
	 */
    public static void updateJbossStandard(String targetServerDir, String setupDir, String sourceDeployment) throws Exception {
        String jbossStandardXML = targetServerDir + "/server/default/conf/standardjboss.xml";
        String jbossStandardCopy = setupDir + "/standardjboss.xml.http";
        String jbossStandardOriginal = setupDir + "/standardjboss.xml.orig";
        FileSystemUtil.createDirectory(setupDir);
        FileUtils.copyFile(new File(jbossStandardXML), new File(jbossStandardCopy));
        FileUtils.copyFile(new File(jbossStandardXML), new File(jbossStandardOriginal));
        String insert = FileSystemUtil.getTextContents(new File(sourceDeployment + "/configure/standardjboss_httpinvoker.txt"));
        String jbossStandardContents = FileSystemUtil.getTextContents(new File(jbossStandardCopy));
        jbossStandardContents = jbossStandardContents.replace("<invoker-proxy-bindings>", "<invoker-proxy-bindings>\n" + insert + "\n");
        int statelessSessionBeanStartsAt = jbossStandardContents.indexOf("<container-name>Standard Stateless SessionBean</container-name>");
        if (statelessSessionBeanStartsAt > 0) {
            String replaceText = "<invoker-proxy-binding-name>";
            String replaceTextEnd = "</invoker-proxy-binding-name>";
            int replaceStart = jbossStandardContents.indexOf(replaceText, statelessSessionBeanStartsAt);
            int replaceEnd = jbossStandardContents.indexOf(replaceTextEnd, statelessSessionBeanStartsAt) + replaceTextEnd.length();
            jbossStandardContents = jbossStandardContents.substring(0, replaceStart) + "<invoker-proxy-binding-name>stateless-http-invoker</invoker-proxy-binding-name>" + jbossStandardContents.substring(replaceEnd);
        } else {
            throw new Exception("cant find Standard Stateless SessionBean");
        }
        FileSystemUtil.createFile(jbossStandardCopy, jbossStandardContents);
        FileSystemUtil.deleteFile(setupDir + "/standardjboss_httpinvoker.txt");
    }
}
