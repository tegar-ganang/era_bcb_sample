package net.sourceforge.jcoupling2.dao.simple;

import java.util.ArrayList;
import java.util.Iterator;
import net.sourceforge.jcoupling2.exception.JCouplingException;
import net.sourceforge.jcoupling2.persistence.Channel;
import net.sourceforge.jcoupling2.persistence.DataMapper;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestChannel {

    private static final Logger logger = Logger.getLogger(TestChannel.class);

    private Channel channel = null;

    private DataMapper dMapper;

    @Before
    public void setUp() throws JCouplingException {
        dMapper = new DataMapper();
    }

    @After
    public void tearDown() {
        channel = null;
    }

    @Test
    public void testAddConstructor() throws JCouplingException {
        logger.debug("Adding the channel ...");
        Channel c1 = new Channel("TestChannel4", 1);
        logger.debug("Channel with ID " + c1.getChannelID() + " added!");
        logger.debug("Done");
    }

    @Test
    public void testAddChannel() throws JCouplingException {
        channel = new Channel();
        channel.setChannelName("TestChannel2");
        channel.setMiddlewareAdapterID(new Integer(1));
        channel.setWsdlChannelUrl("Test1");
        channel.setWsdlChannelPortType("Test2");
        channel.setWsdlChannelOperationName("Test3");
        logger.debug("Adding the channel ...");
        logger.debug("Channel with ID " + dMapper.addChannel(channel) + " added!");
        logger.debug("Done");
    }

    @Test
    public void testRemove() throws JCouplingException {
        String channelname = "TestChannel4";
        logger.debug("Removing the channel ...");
        dMapper.removeChannel(channelname);
        logger.debug("Done");
    }

    @Test
    public void testRetrieveAllChannels() throws JCouplingException {
        logger.debug("Retrieving all the channels ...");
        ArrayList<Channel> channels = dMapper.retrieveAllChannels();
        Iterator<Channel> ChannelIterator = channels.iterator();
        Channel channel = null;
        while (ChannelIterator.hasNext()) {
            channel = ChannelIterator.next();
            logger.debug("|=============================|");
            logger.debug("ChannelID: " + channel.getChannelID());
            logger.debug("ChannelName: " + channel.getChannelName());
            logger.debug("MiddlewareAdapterID: " + channel.getMiddlewareAdapterID());
            logger.debug("IsTimeDecoupled: " + channel.getIsTimeDecoupled());
        }
        logger.debug("Done");
    }

    @Test
    public void testRetrieveChannelID() throws JCouplingException {
        Integer channelID = new Integer(13);
        logger.debug("Retrieving the channel ...");
        Channel channel = dMapper.retrieveChannel(channelID);
        logger.debug("ChannelID: " + channel.getChannelID());
        logger.debug("ChannelName: " + channel.getChannelName());
        logger.debug("MiddlewareAdapterID: " + channel.getMiddlewareAdapterID());
        logger.debug("IsTimeDecoupled: " + channel.getIsTimeDecoupled());
        logger.debug("IsWsdlBacked: " + channel.getIsWSDLBacked());
        logger.debug("MessageSchemaIn: " + channel.getMsgSchemaIn());
        logger.debug("MessageSchemaOut: " + channel.getMsgSchemaOut());
        logger.debug("SupportsInvoke: " + channel.supportsInvoke());
        logger.debug("SupportsInbound: " + channel.supportsInbound());
        logger.debug("Done");
    }

    @Test
    public void testRetrieveChannelName() throws JCouplingException {
        String channelName = "TestChannel1";
        logger.debug("Retrieving the channel ...");
        Channel channel = dMapper.retrieveChannel(channelName);
        logger.debug("ChannelID: " + channel.getChannelID());
        logger.debug("ChannelName: " + channel.getChannelName());
        logger.debug("MiddlewareAdapterID: " + channel.getMiddlewareAdapterID());
        logger.debug("IsTimeDecoupled: " + channel.getIsTimeDecoupled());
        logger.debug("IsWsdlBacked: " + channel.getIsWSDLBacked());
        logger.debug("MessageSchemaIn: " + channel.getMsgSchemaIn());
        logger.debug("MessageSchemaOut: " + channel.getMsgSchemaOut());
        logger.debug("SupportsInvoke: " + channel.supportsInvoke());
        logger.debug("SupportsInbound: " + channel.supportsInbound());
        logger.debug("Done");
    }
}
