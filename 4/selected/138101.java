package com.ideo.ria.plugins;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Goal which allows to copy some ressources.
 *
 * @goal copyResources
 * @phase initialize
 *
 * @author Nicolas Jozwiak
 */
public class CopyResourcesPlugin extends AbstractMojo {

    private static final String SVN = ".svn";

    /**
	* Project directory.
	* @parameter expression="${basedir}"
	*/
    private String pathResources;

    /**
	* Project build directory.
	* @parameter expression="${project.build.directory}"
	*/
    private String buildDirectory;

    /**
	* Artifact ID.
	* @parameter expression="${project.artifactId}"
	*/
    private String artifactId;

    /**
	* Project version.
	* @parameter expression="${project.version}"
	*/
    private String version;

    /**
	* Project localRepository.
	* @parameter expression="${settings.localRepository}"
	*/
    private String localRepository;

    /**
	 * SweetDEV-RIA ressources.groupId.
	 *
	 * @parameter expression="${ressources.groupId}"
	 */
    private static String ressources_groupId;

    /**
	 * SweetDEV-RIA ressources.artifactId.
	 *
	 * @parameter expression="${ressources.artifactId}"
	 */
    private static String ressources_artifactId;

    /**
	 * SweetDEV-RIA ressources.version.
	 *
	 * @parameter expression="${ressources.version}"
	 */
    private static String ressources_version;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (ressources_groupId == null) {
                String path = pathResources.substring(0, pathResources.lastIndexOf(File.separator));
                copyFiles(path + File.separator + "sweetdev-ria-core\\src\\main\\resources\\SweetDevRIA", buildDirectory + File.separator + artifactId + "-" + version + "\\resources");
                copyFiles(path + File.separator + "sweetdev-ria-core\\target\\BuildDir", buildDirectory + File.separator + artifactId + "-" + version + "\\resources\\js");
                getLog().info("copy succeeded !!");
            } else {
                String path = localRepository + File.separator + ressources_groupId + File.separator + ressources_artifactId + File.separator + ressources_version + File.separator + ressources_artifactId + "-" + ressources_version + ".zip";
                String exportPath = buildDirectory + File.separator + artifactId + "-" + version;
                getLog().info("sweetdev-ria-resources path:" + path);
                getLog().info("sweetdev-ria-resources export path:" + exportPath);
                String pathProjetWebapp = pathResources + "\\src\\main\\webapp";
                getLog().info("pathProjetWebapp:" + pathProjetWebapp);
                boolean success = deleteDirectory(pathProjetWebapp + "\\resources");
                if (success) {
                    unzipFile(path, pathProjetWebapp);
                    unzipFile(path, exportPath);
                    getLog().info("copy succeeded !!");
                } else {
                    getLog().error("Some files could not be deleted!!!");
                }
            }
        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    /**
	 * Use to unzip a file in a directory 	
	 * @param filePath   path of the file to unzip
	 * @param exportPath directory where file will be unzipped 
	 * @throws IOException
	 */
    public static void unzipFile(String filePath, String exportPath) throws IOException {
        int BUFFER = 2048;
        ZipFile zipfile = new ZipFile(filePath);
        Enumeration e = zipfile.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            if (entry.isDirectory()) {
                File directory = new File(exportPath + File.separator + entry.getName());
                directory.mkdir();
            } else {
                File file = new File(exportPath + File.separator + entry.getName());
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
                BufferedInputStream is = new BufferedInputStream(zipfile.getInputStream(entry));
                int count;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new FileOutputStream(exportPath + File.separator + entry.getName());
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = is.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
                is.close();
            }
        }
        zipfile.close();
    }

    public static void main(String[] args) {
        try {
            String path = "C:/project/repository\\sweetdev-ria\\sweetdev-ria-resources\\1.1-RC3\\sweetdev-ria-resources-1.1-RC3.zip";
            String exportPath = "C:\\project\\subversion\\sweetdev-ria-demo\\target\\sweetdev-ria-demo-1.1-RC3\\resources";
            deleteDirectory(exportPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Use to delete a directory
	 * @throws IOException
	 */
    public static boolean deleteDirectory(String path) throws IOException {
        File directory = new File(path);
        boolean operationSucces = true;
        if (directory.exists() && directory.isDirectory()) {
            Collection listeFiles = getListeFile(new File(path));
            Iterator it = listeFiles.iterator();
            while (it.hasNext()) {
                File file = (File) it.next();
                if (!file.delete()) {
                    operationSucces = false;
                }
            }
            Collection listeDirectory = getListeDirectory(new File(path));
            it = listeDirectory.iterator();
            while (it.hasNext()) {
                File file = (File) it.next();
                if (!file.delete()) {
                    operationSucces = false;
                }
            }
            if (!directory.delete()) {
                operationSucces = false;
            }
        }
        return operationSucces;
    }

    /**
	 * Return the list of files which are contained into a directory and sub-directory
	 * @param path     path of the directory	 	 
	 * @throws IOException
	 */
    public static Collection getListeFile(File path) throws IOException {
        Collection listeFiles = new ArrayList();
        String[] listePathFile = null;
        FileFilter directoryFilter = new FileFilter() {

            public boolean accept(File file) {
                return file.isDirectory();
            }
        };
        File[] listeDirectory = path.listFiles(directoryFilter);
        for (int j = 0; j < listeDirectory.length; j++) {
            File subDirectory = listeDirectory[j];
            listeFiles.addAll(getListeFile(subDirectory));
        }
        listePathFile = path.list();
        for (int i = 0; i < listePathFile.length; i++) {
            File file = new File(path.getAbsolutePath() + File.separator + listePathFile[i]);
            if (!file.isDirectory()) {
                listeFiles.add(file);
            }
        }
        return listeFiles;
    }

    /**
	 * Return teh list of sub-directory in order to suppress them
	 * @param path path of the directory	 
	 * @throws IOException
	 */
    public static Collection getListeDirectory(File path) throws IOException {
        Collection allDirectory = new ArrayList();
        FileFilter directoryFilter = new FileFilter() {

            public boolean accept(File file) {
                return file.isDirectory();
            }
        };
        File[] listeDirectory = path.listFiles(directoryFilter);
        for (int j = 0; j < listeDirectory.length; j++) {
            File subDirectory = listeDirectory[j];
            allDirectory.addAll(getListeDirectory(subDirectory));
            allDirectory.add(subDirectory);
        }
        return allDirectory;
    }

    public static void copyFiles(String strPath, String dstPath) throws IOException {
        File src = new File(strPath);
        File dest = new File(dstPath);
        if (src.isDirectory()) {
            dest.mkdirs();
            String list[] = src.list();
            for (int i = 0; i < list.length; i++) {
                if (list[i].lastIndexOf(SVN) != -1) {
                    if (!SVN.equalsIgnoreCase(list[i].substring(list[i].length() - 4, list[i].length()))) {
                        String dest1 = dest.getAbsolutePath() + "\\" + list[i];
                        String src1 = src.getAbsolutePath() + "\\" + list[i];
                        copyFiles(src1, dest1);
                    }
                } else {
                    String dest1 = dest.getAbsolutePath() + "\\" + list[i];
                    String src1 = src.getAbsolutePath() + "\\" + list[i];
                    copyFiles(src1, dest1);
                }
            }
        } else {
            FileInputStream fin = new FileInputStream(src);
            FileOutputStream fout = new FileOutputStream(dest);
            int c;
            while ((c = fin.read()) >= 0) fout.write(c);
            fin.close();
            fout.close();
        }
    }
}
