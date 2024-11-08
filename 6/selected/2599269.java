package org.rapla.storage.dbsql.tests;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.rapla.ServerTest;
import org.rapla.components.util.IOUtil;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeAnnotations;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.storage.CachableStorageOperator;
import org.rapla.storage.ImportExportManager;
import org.rapla.storage.dbsql.DBOperator;

public class SQLOperatorRemoteTest extends ServerTest {

    public SQLOperatorRemoteTest(String name) {
        super(name);
    }

    protected String getStorageName() {
        return "storage-sql";
    }

    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite("SQLOperatorRemoteTest");
        suite.addTest(new SQLOperatorRemoteTest("testExport"));
        suite.addTest(new SQLOperatorRemoteTest("testNewAttribute"));
        suite.addTest(new SQLOperatorRemoteTest("testAttributeChange"));
        suite.addTest(new SQLOperatorRemoteTest("testChangeDynamicType"));
        suite.addTest(new SQLOperatorRemoteTest("testChangeGroup"));
        suite.addTest(new SQLOperatorRemoteTest("testCreateResourceAndRemoveAttribute"));
        return suite;
    }

    protected void setUp() throws Exception {
        IOUtil.copy("webapp/WEB-INF/rapla-hsqldb.properties", WEBAPP_INF_FOLDER_NAME + "/rapla-hsqldb.properties");
        IOUtil.copy("webapp/WEB-INF/rapla-hsqldb.script", WEBAPP_INF_FOLDER_NAME + "/rapla-hsqldb.script");
        super.setUp();
    }

    public void testExport() throws Exception {
        RaplaContext sm = getContext();
        ImportExportManager conv = (ImportExportManager) sm.lookup(ImportExportManager.ROLE);
        conv.doExport();
        CachableStorageOperator operator = (CachableStorageOperator) getContext().lookup(CachableStorageOperator.ROLE + "/sql");
        operator.disconnect();
        Thread.sleep(1000);
        operator.connect();
        operator.getVisibleEntities(null);
        operator.disconnect();
    }

    /** exposes a bug in the 0.12.1 Version of Rapla */
    public void testAttributeChange() throws Exception {
        ClientFacade facade = (ClientFacade) getContext().lookup(ClientFacade.ROLE + "/sql-facade");
        facade.login("admin", "".toCharArray());
        changeEventType(facade);
        facade.logout();
        CachableStorageOperator operator = (CachableStorageOperator) getContext().lookup(CachableStorageOperator.ROLE + "/sql");
        operator.disconnect();
        testTypeIds();
        operator.connect();
        changeEventType(facade);
        testTypeIds();
    }

    private void changeEventType(ClientFacade facade) throws RaplaException {
        DynamicType eventType = (DynamicType) facade.edit(facade.getDynamicType("event"));
        Attribute attribute = eventType.getAttribute("description");
        attribute.setType(AttributeType.CATEGORY);
        attribute.setConstraint(ConstraintIds.KEY_ROOT_CATEGORY, facade.getSuperCategory().getCategory("department"));
        facade.store(eventType);
    }

    private void testTypeIds() throws RaplaException, SQLException {
        CachableStorageOperator operator = (CachableStorageOperator) getContext().lookup(CachableStorageOperator.ROLE + "/sql");
        Connection connection = ((DBOperator) operator).createConnection();
        String sql = "SELECT * from DYNAMIC_TYPE";
        try {
            Statement statement = connection.createStatement();
            ResultSet set = statement.executeQuery(sql);
            while (!set.isLast()) {
                set.next();
                String idString = set.getString("ID");
                String key = set.getString("TYPE_KEY");
                System.out.println("id " + idString + " key " + key);
            }
        } catch (SQLException ex) {
            throw new RaplaException(ex);
        } finally {
            connection.close();
        }
    }

    public void testNewAttribute() throws Exception {
        ClientFacade facade = (ClientFacade) getContext().lookup(ClientFacade.ROLE + "/sql-facade");
        facade.login("admin", "".toCharArray());
        DynamicType roomType = (DynamicType) facade.edit(facade.getDynamicType("room"));
        Attribute attribute = facade.newAttribute(AttributeType.STRING);
        attribute.setKey("color");
        attribute.setAnnotation(AttributeAnnotations.KEY_EDIT_VIEW, AttributeAnnotations.VALUE_NO_VIEW);
        roomType.addAttribute(attribute);
        facade.store(roomType);
        roomType = (DynamicType) facade.getPersistant(roomType);
        Allocatable[] allocatables = facade.getAllocatables(new ClassificationFilter[] { roomType.newClassificationFilter() });
        Allocatable allocatable = (Allocatable) facade.edit(allocatables[0]);
        allocatable.getClassification().setValue("color", "665532");
        String name = (String) allocatable.getClassification().getValue("name");
        facade.store(allocatable);
        facade.logout();
        CachableStorageOperator operator = (CachableStorageOperator) getContext().lookup(CachableStorageOperator.ROLE + "/sql");
        operator.disconnect();
        operator.connect();
        facade.login("admin", "".toCharArray());
        allocatables = facade.getAllocatables(new ClassificationFilter[] { roomType.newClassificationFilter() });
        allocatable = (Allocatable) facade.edit(allocatables[0]);
        assertEquals(name, allocatable.getClassification().getValue("name"));
    }

    public void testCreateResourceAndRemoveAttribute() throws RaplaException {
        Allocatable newResource = facade1.newResource();
        newResource.setClassification(facade1.getDynamicType("room").newClassification());
        newResource.getClassification().setValue("name", "test-resource");
        facade1.store(newResource);
        DynamicType typeEdit3 = (DynamicType) facade1.edit(facade1.getDynamicType("room"));
        typeEdit3.removeAttribute(typeEdit3.getAttribute("belongsto"));
        facade1.store(typeEdit3);
    }

    public void tearDown() throws Exception {
        CachableStorageOperator operator = (CachableStorageOperator) getContext().lookup(CachableStorageOperator.ROLE + "/sql");
        operator.disconnect();
        Thread.sleep(200);
        operator.connect();
        operator.getVisibleEntities(null);
        operator.disconnect();
        Thread.sleep(100);
        super.tearDown();
        Thread.sleep(500);
    }
}
