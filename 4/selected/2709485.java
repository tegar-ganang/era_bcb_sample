package com.ideo.jso.junit;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.apache.struts.mock.MockHttpSession;
import org.apache.struts.mock.MockPageContext;
import org.apache.struts.mock.MockServletConfig;
import org.apache.struts.mock.MockServletContext;
import org.junit.Ignore;
import com.ideo.jso.conf.AbstractGroupBuilder;
import com.ideo.jso.conf.Group;
import com.ideo.jso.junit.mock.JSOMockPageContext;
import com.ideo.jso.junit.mock.JSOMockServletContext;
import com.ideo.jso.junit.printImport.StaticFiles;
import com.ideo.jso.tag.InclusionController;
import com.ideo.jso.util.Initializer;
import com.ideo.jso.util.URLConf;

public abstract class JSOUnit extends TestCase {

    public void testFake() throws Exception {
    }

    /**
	 * 
	 * @return
	 */
    protected PageContext createMockPageContext(String mockSiteUrl) {
        MockServletContext servlet = new JSOMockServletContext(mockSiteUrl);
        MockServletConfig config = new MockServletConfig(servlet);
        MockHttpSession session = new MockHttpSession(servlet);
        MockHttpServletRequest request = new MockHttpServletRequest("/JSOTest", "/JSOUnit", null, null, session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockPageContext pageContext = new JSOMockPageContext(config, request, response);
        return pageContext;
    }

    /**
	 * 
	 * @return
	 */
    protected Writer createWriter() {
        Writer writer = new StringWriter();
        return writer;
    }

    /**
	 * 
	 * @param jsoFilePath
	 * @return
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
    protected Properties initJSO(String jsoFilePath) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Properties properties = getJSOConfig(jsoFilePath);
        String realPath = Thread.currentThread().getContextClassLoader().getResource("").getFile();
        Initializer.initialize(properties, null, realPath);
        return properties;
    }

    /**
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
    protected Properties getJSOConfig(String filePath) throws IOException {
        Properties properties = new Properties();
        properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath));
        return properties;
    }

    /**
	 * Return a collection of URL of files found in jars and class-path corresponding to 'configFileName' path.
	 * @param configFileName
	 * @return
	 * @throws IOException
	 */
    protected Collection getConfigResources(String configFileName) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration result = cl.getResources(configFileName);
        LinkedList resources = new LinkedList();
        while (result.hasMoreElements()) {
            URL url = (URL) result.nextElement();
            resources.addLast(url);
        }
        return resources;
    }

    /**
     * 
     * @param jsoPropertiesFile
     * @param occurences
     * @param properties
     * @throws IOException
     */
    protected void checkConfigFileName(String configFileName, int occurences, Properties properties) throws IOException {
        assertTrue("jso.configFileName property presents.", properties.containsKey("jso.configFileName"));
        assertNotNull("jso file property not null.", properties.getProperty("jso.configFileName"));
        assertTrue("jso.configFileName contains " + configFileName, properties.getProperty("jso.configFileName").trim().indexOf(configFileName.trim()) >= 0);
        Collection configFiles = getConfigResources(configFileName);
        assertEquals("Number of configuration jso file is " + occurences, occurences, configFiles.size());
    }

    /**
	 * 
	 * @param groups
	 */
    protected void printGroups(Map groups) {
        Iterator iGroup = groups.values().iterator();
        while (iGroup.hasNext()) {
            Group g = (Group) iGroup.next();
            System.out.println(g.toString() + "\n\n");
        }
    }

    /**
	 * 
	 * @param loadedGroups
	 * @param groupName
	 */
    protected void testGroupPresents(Map loadedGroups, String groupName) {
        assertTrue("Group " + groupName + " exists.", loadedGroups.containsKey(groupName));
    }

    /**
	 * 
	 * @param group
	 * @param files
	 */
    protected void checkCssFilesList(Group group, String[] files) {
        assertNotNull("checkCssFilesList called with a null group", group);
        String name = group.getName();
        if (files == null) {
            assertNotNull("Group " + name + " has no css files but css file list not null.", group.getCssNames());
            assertEquals("Group " + name + " has no css file but css file list not null.", group.getCssNames().size(), 0);
        } else {
            assertEquals("Correct number of css files", files.length, group.getCssNames().size());
            Iterator iterator = group.getCssNames().iterator();
            assertNotNull("Group " + name + " constains css file declaration.", group.getCssNames());
            for (int i = 0; i < files.length; i++) {
                assertTrue("Group " + name + "'s css files list contains at least " + i + " element(s).", iterator.hasNext());
                String file = (String) iterator.next();
                assertEquals("Group " + name + "'s css files list contains file " + file + " at position " + i + ".", files[i], file);
            }
        }
    }

    /**
	 * 
	 * @param group
	 * @param files
	 */
    protected void checkJsFilesList(Group group, String[] files) {
        assertNotNull("checkJsFilesList called with a null group", group);
        String name = group.getName();
        if (files == null) {
            assertNotNull("Group " + name + " has no js files but js file list not null.", group.getJsNames());
            assertEquals("Group " + name + " has no js file but js file list not null.", group.getJsNames().size(), 0);
        } else {
            assertEquals("Correct number of js files", files.length, group.getJsNames().size());
            Iterator iterator = group.getJsNames().iterator();
            assertNotNull("Group " + name + " constains js file declaration.", group.getJsNames());
            for (int i = 0; i < files.length; i++) {
                assertTrue("Group " + name + "'s js files list contains at least " + i + " element(s).", iterator.hasNext());
                String file = (String) iterator.next();
                assertEquals("Group " + name + "'s js files list contains file " + file + " at position " + i + ".", files[i], file);
            }
        }
    }

    /**
	 * 
	 * @param group
	 * @param subgroups
	 */
    protected void checkSubGroupList(Group group, String[] subgroups) {
        assertNotNull("checkSubGroupList called with a null group", group);
        String name = group.getName();
        if (subgroups == null) {
            assertNotNull("Group " + name + " has no sub-groups but sub-groups list not null.", group.getSubgroups());
            assertEquals("Group " + name + " has no sub-groups but sub-groups list not null.", 0, group.getSubgroups().size());
        } else {
            assertEquals("Correct number of sub groups", subgroups.length, group.getSubgroups().size());
            Iterator iterator = group.getSubgroups().iterator();
            assertNotNull("Group " + name + " constains sub-groups declaration.", group.getSubgroups());
            for (int i = 0; i < subgroups.length; i++) {
                assertTrue("Group " + name + "'s sub-groups list contains at least " + i + " element(s).", iterator.hasNext());
                Group subgroup = (Group) iterator.next();
                assertEquals("Group " + name + "'s sub-groups list contains file " + subgroup.getName() + " at position " + i + ".", subgroups[i], subgroup.getName());
            }
        }
    }

    protected void checkGroupLocation(Group group, String expectedLocation) {
        assertEquals("Group " + group.getName() + " location is OK", expectedLocation, group.getLocation());
    }

    protected void checkGroupTimestampPolicy(Group group, String expectedTimestampPolicy) {
        assertEquals("Group " + group.getName() + " TimestampPolicy is OK", expectedTimestampPolicy, group.getTimeStampPolicy());
    }

    protected void checkJSFileTimestamp(Group group, ServletContext sc, long maxtimestamp) throws Exception {
        long jstimestamp = group.computeMaxJSTimestamp(sc);
        assertEquals("Check group " + group.getName() + " JS Files have max timestamp of " + jstimestamp, jstimestamp, maxtimestamp);
    }

    protected void checkCSSFileTimestamp(Group group, ServletContext sc, long maxtimestamp) throws Exception {
        long csstimestamp = group.computeMaxCSSTimestamp(sc);
        assertEquals("Check group " + group.getName() + " JS Files have max timestamp of " + csstimestamp, csstimestamp, maxtimestamp);
    }

    /**
	 * check if group generate HTML links to js and css links in the correct order definied in StaticFiles files
	 * parameter. 
	 * @param group
	 * @param files
	 * @param exploded This links generation in exploded mode.
	 * @throws Exception
	 */
    protected void checkImportOk(Group group, StaticFiles files, boolean exploded) throws Exception {
        PageContext pageContext = createMockPageContext(group.getLocation());
        StringWriter out = new StringWriter();
        String name = group.getName();
        InclusionController ic = InclusionController.getInstance();
        ic.printImports(pageContext, out, name, exploded);
        String importResult = "";
        importResult += files.computeJSScriptCalls(exploded, group.getBestLocation(null));
        importResult += files.computeCSSCalls(group.getBestLocation(null));
        System.out.println(name + " excepted import:\n");
        System.out.println(importResult);
        System.out.println(name + " returned import:\n");
        System.out.println(out.toString());
        assertEquals("Check import on group " + name, importResult, out.toString());
    }

    protected String getFileContentAsString(String filePath, String encoding) throws IOException {
        URL testURL = Thread.currentThread().getContextClassLoader().getResource(filePath);
        InputStream input = null;
        StringWriter sw = new StringWriter();
        try {
            if (testURL != null) {
                input = testURL.openStream();
            } else {
                input = new FileInputStream(filePath);
            }
            IOUtils.copy(input, sw, encoding);
        } finally {
            if (input != null) {
                input.close();
            }
        }
        return sw.toString();
    }

    protected void checkMergedJSFilesAreEmpty(Group group, String encoding) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AbstractGroupBuilder.getInstance().buildGroupJsIfNeeded(group, baos, null);
        baos.flush();
        baos.close();
        System.out.println("--- Group " + group.getName() + ":");
        System.out.println(baos.toString(encoding));
        System.out.println("---");
        assertEquals("", baos.toString(encoding));
    }

    protected String getRealPathForJarFile(String classPath) {
        URL urlProperties = URLConf.getClassPathUrlResource(classPath);
        if (urlProperties != null) {
            String urlExternForm = urlProperties.toExternalForm();
            if (urlExternForm != null && urlExternForm.startsWith("file:/")) {
                return urlExternForm.substring("file:/".length());
            }
        }
        return classPath;
    }
}
