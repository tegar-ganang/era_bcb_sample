package org.rapla.storage.dbsql.tests;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.rapla.components.util.DateTools;
import org.rapla.components.util.IOUtil;
import org.rapla.entities.Category;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.RefEntity;
import org.rapla.framework.RaplaException;
import org.rapla.storage.dbsql.DBOperator;
import org.rapla.storage.tests.AbstractOperatorTest;

public class SQLOperatorTest extends AbstractOperatorTest {

    public SQLOperatorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        IOUtil.copy("webapp/WEB-INF/rapla-hsqldb.properties", TEST_FOLDER_NAME + "/rapla-hsqldb.properties");
        IOUtil.copy("webapp/WEB-INF/rapla-hsqldb.script", TEST_FOLDER_NAME + "/rapla-hsqldb.script");
        super.setUp();
        ((DBOperator) operator).removeAll();
        operator.disconnect();
        operator.connect();
    }

    public static Test suite() {
        return new TestSuite(SQLOperatorTest.class);
    }

    /** exposes a bug in 1.1 
     * @throws RaplaException */
    public void testPeriodInfitiveEnd() throws RaplaException {
        facade.login("homer", "duffs".toCharArray());
        Reservation event = facade.newReservation();
        Appointment appointment = facade.newAppointment(new Date(), new Date());
        event.getClassification().setValue("name", "test");
        appointment.setRepeatingEnabled(true);
        appointment.getRepeating().setEnd(null);
        event.addAppointment(appointment);
        facade.store(event);
        operator.refresh();
        Reservation event1 = (Reservation) operator.getPersistant(event);
        Repeating repeating = event1.getAppointments()[0].getRepeating();
        assertNotNull(repeating);
        assertNull(repeating.getEnd());
        assertEquals(-1, repeating.getNumber());
    }

    public void testPeriodStorage() throws RaplaException {
        facade.login("homer", "duffs".toCharArray());
        Date start = DateTools.cutDate(new Date());
        Date end = new Date(start.getTime() + DateTools.MILLISECONDS_PER_WEEK);
        Period period = facade.newPeriod();
        period.setName("TEST PERIOD");
        period.setStart(start);
        period.setEnd(end);
        facade.store(period);
        operator.refresh();
        Period period1 = (Period) operator.getPersistant(period);
        assertEquals(period1.getStart(), period.getStart());
        assertEquals(period1.getEnd(), period1.getEnd());
    }

    public void testCategoryChange() throws RaplaException {
        facade.login("homer", "duffs".toCharArray());
        {
            Category category1 = facade.newCategory();
            Category category2 = facade.newCategory();
            category1.setKey("users1");
            category2.setKey("users2");
            Category groups = (Category) facade.edit(facade.getUserGroupsCategory());
            groups.addCategory(category1);
            groups.addCategory(category2);
            facade.store(groups);
            assertEquals("users1", facade.getUserGroupsCategory().getCategories()[3].getKey());
            assertEquals("users2", facade.getUserGroupsCategory().getCategories()[4].getKey());
            operator.disconnect();
            operator.connect();
            facade.refresh();
        }
        assertEquals("users1", facade.getUserGroupsCategory().getCategories()[3].getKey());
        assertEquals("users2", facade.getUserGroupsCategory().getCategories()[4].getKey());
    }

    public void testDynamicTypeChange() throws Exception {
        facade.login("homer", "duffs".toCharArray());
        DynamicType type = (DynamicType) facade.edit(facade.getDynamicType("event"));
        Object id = ((RefEntity) type).getId();
        Attribute att = facade.newAttribute(AttributeType.STRING);
        att.setKey("test-att");
        type.addAttribute(att);
        facade.store(type);
        facade.logout();
        printTypeIds();
        operator.disconnect();
        facade.login("homer", "duffs".toCharArray());
        DynamicType typeAfterEdit = facade.getDynamicType("event");
        Object idAfterEdit = ((RefEntity) typeAfterEdit).getId();
        assertEquals(id, idAfterEdit);
    }

    private void printTypeIds() throws RaplaException, SQLException {
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

    protected String getStorageName() {
        return "sql";
    }

    protected String getFacadeName() {
        return "sql-facade";
    }
}
