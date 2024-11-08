package XML;

import Entities.Feature;
import Entities.QualityHistory;
import Utility.DatabaseHandle;
import Utility.Person;
import Utility.TestObjects.PreInitializedObjects;
import Utility.UserHandle;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pontuslp
 */
public class XMLParserTest {

    PreInitializedObjects pio;

    static String dbPath = "./test/TestLastUsedDatabase.xml";

    static String dbRewritePath = "./test/TestLastUsedDatabaseRewrite.xml";

    static String userPath = "./test/TestUserInformation.xml";

    static String userRewritePath = "./test/TestUserInformationRewrite.xml";

    XMLParser parsedDB;

    XMLParser parsedUser;

    public XMLParserTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        PreInitializedObjects pio = new PreInitializedObjects();
        pio.sq.getDatabase().writeDatabase(dbPath);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        pio = new PreInitializedObjects();
        parsedDB = new XMLParser(dbPath);
        parsedUser = new XMLParser(userPath);
    }

    @After
    public void tearDown() {
    }

    /**
	 * Test of hasUser method, of class XMLParser.
	 */
    @Test
    public void testHasUser() {
        System.out.println("hasUser");
        assertFalse(parsedDB.hasUser());
        assertTrue(parsedUser.hasUser());
    }

    /**
	 * Test of hasFeatures method, of class XMLParser.
	 */
    @Test
    public void testHasFeatures() {
        System.out.println("hasFeatures");
        assertTrue(parsedDB.hasFeatures());
        assertFalse(parsedUser.hasFeatures());
    }

    /**
	 * Test of getUser method, of class XMLParser.
	 */
    @Test
    public void testGetUser() {
        System.out.println("getUser");
        assertEquals("Pontus Lindberg Parker", parsedUser.getUser().toString());
    }

    /**
	 * Test of getFeatures method, of class XMLParser.
	 */
    @Test
    public void testGetFeatures() {
        System.out.println("getFeatures");
        assertEquals("Startup", parsedDB.getFeatures().firstElement().toString());
        assertEquals("Screen", parsedDB.getFeatures().lastElement().toString());
    }

    /**
	 * Test that conversion to xml returns equal objects on parse
	 */
    @Test
    public void testToXMLAndBack() {
        System.out.println("toXMLAndBack");
        XMLBuilder xmlb = new XMLBuilder();
        DatabaseHandle database = new DatabaseHandle(pio.features);
        database.writeDatabase(dbRewritePath);
        database.readDatabase(dbRewritePath);
        assertTrue(database.featuresEquals(pio.features));
        UserHandle user = new UserHandle(pio.sq, pio.p);
        user.writeUser(userRewritePath);
        user.readUser(userRewritePath);
        assertTrue(pio.p.equals(user.getUser()));
    }

    /**
	 * Test that the same string appears after parsing and re-generating XML.
	 */
    @Test
    public void testToObjectAndBack() {
        System.out.println("toObjectAndBack");
        String original = "";
        XMLBuilder xmlb;
        try {
            File f = new File(userPath);
            BufferedReader in = new BufferedReader(new FileReader(f));
            original = in.readLine();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        xmlb = new XMLBuilder();
        xmlb.append("<User>");
        parsedUser.getUser().toXML(xmlb);
        xmlb.append("</User>");
        assertEquals(original, xmlb.toString());
        try {
            File f = new File(dbPath);
            BufferedReader in = new BufferedReader(new FileReader(f));
            original = in.readLine();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        xmlb = new XMLBuilder();
        xmlb.append("<sequence id=\"features\" >");
        for (Feature f : parsedDB.getFeatures()) {
            f.toXML(xmlb);
        }
        xmlb.append("</sequence>");
        assertEquals(original, xmlb.toString());
    }
}
