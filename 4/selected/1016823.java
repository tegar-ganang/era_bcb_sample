package org.openejb.admin.web.cmp.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import javax.ejb.Handle;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import org.exolab.castor.jdo.conf.Database;
import org.exolab.castor.jdo.conf.Driver;
import org.exolab.castor.jdo.conf.Jndi;
import org.exolab.castor.jdo.conf.Mapping;
import org.exolab.castor.jdo.conf.Param;
import org.exolab.castor.xml.ValidationException;
import org.openejb.admin.web.HttpRequest;
import org.openejb.admin.web.HttpResponse;
import org.openejb.admin.web.WebAdminBean;
import org.openejb.core.EnvProps;
import org.openejb.util.FileUtils;

/**
 * @author <a href="mailto:tim_urberg@yahoo.com">Tim Urberg</a>
 */
public class CMPMappingBean extends WebAdminBean {

    /** the handle file name */
    private static final String HANDLE_FILE = System.getProperty("file.separator") + "configurationHandle.obj";

    /** Creates a new instance of HomeBean */
    public void ejbCreate() {
        this.section = "CMPMapping";
    }

    /** called before any content is written to the browser
	 * @param request the http request
	 * @param response the http response
	 * @throws IOException if an exception is thrown
	 */
    public void preProcess(HttpRequest request, HttpResponse response) throws IOException {
    }

    /** called after all content is written to the browser
	 * @param request the http request
	 * @param response the http response
	 * @throws IOException if an exception is thrown
	 */
    public void postProcess(HttpRequest request, HttpResponse response) throws IOException {
    }

    /** Write the TITLE of the HTML document.  This is the part
	 * that goes into the <code>&lt;head&gt;&lt;title&gt;
	 * &lt;/title&gt;&lt;/head&gt;</code> tags
	 *
	 * @param body the output to write to
	 * @exception IOException of an exception is thrown
	 *
	 */
    public void writeHtmlTitle(PrintWriter body) throws IOException {
        body.println(HTML_TITLE);
    }

    /** Write the title of the page.  This is displayed right
	 * above the main block of content.
	 * 
	 * @param body the output to write to
	 * @exception IOException if an exception is thrown
	 */
    public void writePageTitle(PrintWriter body) throws IOException {
        body.println("Container Managed Persistance Mapping");
    }

    /** Write the sub items for this bean in the left navigation bar of
	 * the page.  This should look somthing like the one below:
	 *
	 *      <code>
	 *      &lt;tr&gt;
	 *       &lt;td valign="top" align="left"&gt;
	 *        &lt;a href="system?show=deployments"&gt;&lt;span class="subMenuOff"&gt;
	 *        &nbsp;&nbsp;&nbsp;Deployments
	 *        &lt;/span&gt;
	 *        &lt;/a&gt;&lt;/td&gt;
	 *      &lt;/tr&gt;
	 *      </code>
	 *
	 * Alternately, the bean can use the method formatSubMenuItem(..) which
	 * will create HTML like the one above
	 *
	 * @param body the output to write to
	 * @exception IOException if an exception is thrown
	 *
	 */
    public void writeSubMenuItems(PrintWriter body) throws IOException {
    }

    /** 
	 * writes the main body content to the broswer.  This content is inside 
	 * a <code>&lt;p&gt;</code> block 
	 * 
	 * @param body the output to write to
	 * @exception IOException if an exception is thrown
	 */
    public void writeBody(PrintWriter body) throws IOException {
        body.println("Coming soon...");
    }

    /**
	 * takes care of the submission of database information
	 */
    private boolean submitDatabaseInformation(PrintWriter body, CMPMappingDataObject dataObject, String handleFile) throws IOException {
        DatabaseData databaseData = new DatabaseData();
        databaseData.setDbEngine(request.getFormParameter(CMPMappingWriter.FORM_FIELD_DB_ENGINE));
        databaseData.setDriverClass(request.getFormParameter(CMPMappingWriter.FORM_FIELD_DRIVER_CLASS));
        databaseData.setDriverUrl(request.getFormParameter(CMPMappingWriter.FORM_FIELD_DRIVER_URL));
        databaseData.setFileName(request.getFormParameter(CMPMappingWriter.FORM_FIELD_FILE_NAME));
        databaseData.setJndiName(request.getFormParameter(CMPMappingWriter.FORM_FIELD_JNDI_NAME));
        databaseData.setPassword(request.getFormParameter(CMPMappingWriter.FORM_FIELD_PASSWORD));
        databaseData.setUsername(request.getFormParameter(CMPMappingWriter.FORM_FIELD_USERNAME));
        try {
            databaseData.validate();
        } catch (ValidationException e) {
            CMPMappingWriter.printDBInfo(body, e.getMessage(), databaseData, handleFile);
            return false;
        }
        String path = FileUtils.getBase().getDirectory("conf").getAbsolutePath() + System.getProperty("file.separator") + databaseData.getFileName();
        String localDBFileName = path + ".cmp_local_database.xml";
        String globalDBFileName = path + ".cmp_global_database.xml";
        String mappingFileName = path + ".cmp_or_mapping.xml";
        Database globalDatabase = new Database();
        Database localDatabase = new Database();
        globalDatabase.setName(EnvProps.GLOBAL_TX_DATABASE);
        globalDatabase.setEngine(databaseData.getDbEngine());
        localDatabase.setName(EnvProps.LOCAL_TX_DATABASE);
        localDatabase.setEngine(databaseData.getDbEngine());
        Mapping mapping = new Mapping();
        mapping.setHref(mappingFileName);
        globalDatabase.addMapping(mapping);
        localDatabase.addMapping(mapping);
        Jndi jndi = new Jndi();
        jndi.setName(databaseData.getJndiName());
        globalDatabase.setJndi(jndi);
        Driver driver = new Driver();
        Param userNameParam = new Param();
        Param passwordParam = new Param();
        userNameParam.setName("user");
        userNameParam.setValue(databaseData.getUsername());
        passwordParam.setName("password");
        passwordParam.setValue(databaseData.getPassword());
        driver.setClassName(databaseData.getDriverClass());
        driver.setUrl(databaseData.getDriverUrl());
        driver.addParam(userNameParam);
        driver.addParam(passwordParam);
        localDatabase.setDriver(driver);
        try {
            localDatabase.validate();
            globalDatabase.validate();
        } catch (ValidationException e) {
            CMPMappingWriter.printDBInfo(body, e.getMessage(), databaseData, handleFile);
            return false;
        }
        File jdbcDriverSource = new File(request.getFormParameter(CMPMappingWriter.FORM_FIELD_JDBC_DRIVER));
        String libDir = FileUtils.getBase().getDirectory("lib").getAbsolutePath() + System.getProperty("file.separator") + jdbcDriverSource.getName();
        File destFile = new File(libDir);
        if (jdbcDriverSource.isFile()) {
            if (!destFile.exists() && !destFile.createNewFile()) {
                throw new IOException("Could not create file: " + libDir);
            }
            FileUtils.copyFile(destFile, jdbcDriverSource);
        }
        dataObject.setGlobalDatabase(globalDatabase);
        dataObject.setGlobalDatabaseFileName(globalDBFileName);
        dataObject.setLocalDatabase(localDatabase);
        dataObject.setLocalDatabaseFileName(localDBFileName);
        dataObject.setMappingRootFileName(mappingFileName);
        return true;
    }

    /** 
	 * gets an object reference and handle 
	 * 
	 * @param mappingData the object to create a handle from
	 * @return an absolute path of the handle file
	 * @throws IOException if the file cannot be created
	 */
    private String createHandle(CMPMappingDataObject mappingData) throws IOException {
        File myHandleFile = new File(FileUtils.createTempDirectory().getAbsolutePath() + HANDLE_FILE);
        if (!myHandleFile.exists()) {
            myHandleFile.createNewFile();
        }
        ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(myHandleFile));
        objectOut.writeObject(mappingData.getHandle());
        objectOut.flush();
        objectOut.close();
        return myHandleFile.getAbsolutePath();
    }

    /** 
	 * creates a new CMPMappingDataObject 
	 * 
	 * @return a new CMPMappingDataObject
	 * @throws IOException if the object cannot be created
	 */
    private CMPMappingDataObject getCMPMappingDataObject() throws IOException {
        Properties p = new Properties();
        p.put(Context.INITIAL_CONTEXT_FACTORY, "org.openejb.core.ivm.naming.InitContextFactory");
        try {
            InitialContext ctx = new InitialContext(p);
            Object obj = ctx.lookup("mapping/webadmin/CMPMappingData");
            CMPMappingDataHome home = (CMPMappingDataHome) PortableRemoteObject.narrow(obj, CMPMappingDataHome.class);
            return home.create();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /** 
	 * this method gets the handle 
	 * 
	 * @param handleFile the handle to the object
	 * @return the configuration data object
	 * @throws IOException if the file is not found
	 */
    private CMPMappingDataObject getHandle(String handleFile) throws IOException {
        File myHandleFile = new File(handleFile);
        ObjectInputStream objectIn = new ObjectInputStream(new FileInputStream(myHandleFile));
        Handle mappingHandle;
        try {
            mappingHandle = (Handle) objectIn.readObject();
            return (CMPMappingDataObject) mappingHandle.getEJBObject();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}
