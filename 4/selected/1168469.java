package net.sourceforge.jcoupling2.dao.simple;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.jcoupling2.exception.JCouplingException;
import net.sourceforge.jcoupling2.persistence.DataMapper;
import net.sourceforge.jcoupling2.persistence.Property;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestProperty {

    private static final Logger logger = Logger.getLogger(TestProperty.class);

    private DataMapper dMapper;

    @Before
    public void setUp() throws Exception {
        dMapper = new DataMapper();
    }

    @After
    public void tearDown() throws Exception {
        dMapper = null;
    }

    @Test
    public void testAdd() throws JCouplingException {
        List<String> ColumnNames = Arrays.asList("_name", "channelid", "_type", "description", "xpathpropexpression");
        List<String> ColumnValues = Arrays.asList("TestProperty1", "48", "varchar2", "some description", "/JCMesage/Message/TestProperty1/text()");
        logger.debug("Adding the property ...");
        logger.debug("Property " + dMapper.addProperty(ColumnNames, ColumnValues) + " added!");
        logger.debug("Done");
    }

    @Test
    public void testRemove() throws JCouplingException {
        String propertyName = "TestProperty1";
        logger.debug("Removing the property ...");
        dMapper.removeProperty(propertyName);
        logger.debug("Done");
    }

    @Test
    public void testRetrieveChannelName() throws JCouplingException {
        logger.debug("Retrieving the property ...");
        Property property = null;
        Iterator<Property> PropertyIterator = (dMapper.retrieveProperty("TestChannel1")).iterator();
        while (PropertyIterator.hasNext()) {
            property = PropertyIterator.next();
            logger.debug("=================================================");
            logger.debug("PropertyID: " + property.getID());
            logger.debug("PropertyName: " + property.getName());
            logger.debug("Channel: " + property.getChannelID());
            logger.debug("Description: " + property.getDescription());
            logger.debug("XPathExpression: " + property.getXpathExpression());
            logger.debug("Resulttype: " + property.getDataType());
            logger.debug("Done");
        }
    }

    @Test
    public void testRetrieveChannelID() throws JCouplingException {
        logger.debug("Retrieving the property ...");
        Property property = null;
        Iterator<Property> PropertyIterator = (dMapper.retrieveProperty(13)).iterator();
        while (PropertyIterator.hasNext()) {
            property = PropertyIterator.next();
            logger.debug("=================================================");
            logger.debug("PropertyID: " + property.getID());
            logger.debug("PropertyName: " + property.getName());
            logger.debug("Channel: " + property.getChannelID());
            logger.debug("Description: " + property.getDescription());
            logger.debug("XPathExpression: " + property.getXpathExpression());
            logger.debug("Resulttype: " + property.getDataType());
            logger.debug("Done");
        }
    }

    @Test
    public void testAlterDescription() throws JCouplingException {
        logger.debug("Altering the property ...");
        dMapper.alterPropertyDescription("Patient", "Name des Patienten");
        logger.debug("Done");
    }

    @Test
    public void testAlterXPath() throws JCouplingException {
        logger.debug("Altering the property ...");
        dMapper.alterPropertyXPath("TestProperty1", "blub");
        logger.debug("Done");
    }
}
