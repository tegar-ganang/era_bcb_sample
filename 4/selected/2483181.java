package com.ideo.ria.plugins;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import com.ideo.ria.util.FileUtils;
import com.ideo.ria.util.JsMin;

/**
 * Goal which allows to generate some JS docs.
 * 
 * @goal jsmin
 * @phase install
 * @author Nicolas Jozwiak, Sebastien Revel
 *
 */
public class BuildJSMinPlugin extends AbstractMojo {

    /**
	 * Directory where the Build will put the files.
	 *
	 * @parameter expression="${build.site.path}"
	 */
    private static String m_buildSite;

    /**
	 * Directory where the Build will put the files.
	 *
	 * @parameter expression="${build.jsdocs.path}"
	 */
    private static String m_buildJsdocs;

    /**
	 * Build's directory.
	 *
	 * @parameter expression="${build.doc.path}"
	 */
    private static String m_buildDir;

    /**
	 * Directory where the resources will be copied.
	 *
	 * @parameter expression="${copy.resources.dir}"
	 */
    private static String m_saveResources;

    /**
	 * Tab which contains the js.
	 *
	 * @parameter expression="${jsNames}"
	 */
    private static String[] jsNames;

    /**
	 * Tab which contains the externe js.
	 *
	 * @parameter expression="${jsNamesExtern}"
	 */
    private static String[] jsNamesExtern;

    /**
	 * SweetDEV-RIA FileName.
	 *
	 * @parameter expression="${complete.filename}"
	 */
    private static String m_completeFileName;

    /**
	 * SweetDEV-RIA FileName after JSMin.
	 *
	 * @parameter expression="${js.final}"
	 */
    private static String m_finalFileName;

    /**
	 * Tab which contains the extensions.
	 *
	 * @parameter expression="${extensions}"
	 */
    private static String[] extensions;

    /**
	 * Directory which contains the js resources.
	 *
	 * @parameter expression="${js.resources.dir}"
	 */
    private static String m_jsResources;

    /**
	 * Directory of JSLint.
	 *
	 * @parameter expression="${utils.jslint.dir.exe}"
	 */
    private static String m_jsLintExe;

    /**
	 * Directory of JSLint.
	 *
	 * @parameter expression="${utils.jslint.dir.conf}"
	 */
    private static String m_jsLintConf;

    /**
	 * Path to licence file added by default on minimized file.
	 * 
	 * @parameter expression="${licence.path}
	 */
    private static String m_licencePath;

    public void execute() throws MojoExecutionException {
        getLog().info("*********************************************** ");
        getLog().info("*                 BUILD JSMIN                 * ");
        getLog().info("*********************************************** ");
        build();
    }

    /**
	 * This static method :
	 * <ul>
	 *	<li>- deletes directories "BuildDir" and "Doc",</li>
	 *	<li>- creates directories "Doc", "BuildDir" and "BuildDir/Resources",</li>
	 *	<li>- copy all JS files in resources dir,</li>
	 *	<li>- build JsDoc,</li>
	 *	<li>- check JavaScript syntax,</li>
	 *	<li>- concat all JavaScript file into a single one called 'SweetDevRIAComplete.js',</li>
	 *	<li>- launch JsMin on this file to produce the minimized result file 'SweetDevRIA.js' whithout any comment, tabs or space.</li>
	 *	</ul>
	 */
    public static void build() {
        createDirectories();
        List listExtension = ArrayToList(extensions);
        FileUtils.recursiveFileCopyToDir(m_jsResources, m_saveResources, listExtension);
        checkAllSyntax();
        FileUtils.concatAllFiles(FileUtils.concat(jsNamesExtern, jsNames), m_completeFileName, m_saveResources);
        JsMin.minifierFile(m_completeFileName, m_finalFileName);
        FileUtils.concatFiles(m_licencePath, m_finalFileName);
        FileUtils.recursiveDelete(new File(m_saveResources));
    }

    /**
	 * Create directory structure.
	 */
    public static void createDirectories() {
        File root = new File(m_buildSite);
        File buildDir = new File(m_buildDir);
        if (!root.exists()) {
            System.out.println("Root directory [" + m_buildSite + "] doesn't exist ! So, I create it... ");
            root.mkdir();
            buildDir.mkdir();
        }
        File buildJsdoc = new File(m_buildJsdocs);
        if (FileUtils.recursiveDelete(buildDir)) {
            File resourcesDir = new File(m_saveResources);
            FileUtils.recursiveCreate(buildDir);
            FileUtils.recursiveCreate(buildJsdoc);
            FileUtils.recursiveCreate(resourcesDir);
        }
    }

    /**
	 * Check JavaScript syntax on all JavaScript files using JsLint.
	 */
    private static void checkAllSyntax() {
        File resources = new File(m_saveResources);
        File[] listFiles = resources.listFiles();
        for (int i = 0; i < listFiles.length; i++) {
            File file = listFiles[i];
            if (!arrayContainsValue(jsNamesExtern, file.getName())) {
                checkSyntax(m_saveResources + File.separator + file.getName());
            }
        }
    }

    /**
	 * Check JavaScript syntax for file _fileName using JsLint.
	 * @param _fileName Input JavaScript file to check syntax
	 */
    private static void checkSyntax(final String _fileName) {
        System.out.println("Check JavaScript syntax on file [" + _fileName + "]");
        String[] commands = new String[] { m_jsLintExe, "conf", m_jsLintConf, "process", _fileName };
        try {
            Process proc = Runtime.getRuntime().exec(commands);
            redirectProcToOut(proc);
        } catch (IOException e) {
            System.err.println("Error while checking JavaScript syntax on file [" + _fileName + "]");
            e.printStackTrace();
        }
    }

    /**
	 * Redirect output to console
	 *
	 * @param _proc process.
	 */
    private static void redirectProcToOut(final Process _proc) {
        BufferedInputStream in = new BufferedInputStream(_proc.getInputStream());
        final BufferedReader br = new BufferedReader(new InputStreamReader(in));
        try {
            System.out.write(br.readLine().getBytes());
            String s;
            while ((s = br.readLine()) != null) {
                System.out.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Transform an array into List.
	 *
	 * @param _array Array to transform to list
	 * @return List for the _array object
	 */
    private static List ArrayToList(final Object[] _array) {
        List res = new ArrayList();
        for (int i = 0; i < _array.length; i++) {
            res.add(_array[i]);
        }
        return res;
    }

    /**
	 * Check if a value is contained into the array.
	 *
	 * @param _array Array with values to test
	 * @param _obj object to look up
	 * @return boolean true if the object _obj is inside the array, false else.
	 */
    private static boolean arrayContainsValue(final Object[] _array, final Object _obj) {
        List list = ArrayToList(_array);
        return list.contains(_obj);
    }
}
