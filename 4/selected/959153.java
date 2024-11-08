package org.oobench.ejb.common.deploy;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import java.io.*;

public class JBoss implements DeployerInterface {

    private String sourceJar = null;

    private String jbossDeployDir = null;

    private Task task = null;

    public JBoss(Task theTask, String theSourceJar, String theJBossDeployDir) {
        task = theTask;
        sourceJar = theSourceJar;
        jbossDeployDir = theJBossDeployDir;
    }

    public void deploy() throws BuildException {
        System.out.println("*** Deploying: " + sourceJar);
        System.out.println("*** JBoss dir: " + jbossDeployDir);
        File source = new File(sourceJar);
        File dest = new File(jbossDeployDir + "/deploy/" + (new File(sourceJar).getName()));
        task.log("*** Copying: " + source.getAbsolutePath() + " to " + dest.getAbsolutePath());
        try {
            FileUtils.copyFile(source, dest, true);
        } catch (Exception e) {
            throw new BuildException("Error copying file " + sourceJar + " to " + jbossDeployDir + ": " + e.toString());
        }
        task.log("*** Waiting 10s for JBoss to deploy " + "new jar file...");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ie) {
        }
        System.out.println();
    }
}
