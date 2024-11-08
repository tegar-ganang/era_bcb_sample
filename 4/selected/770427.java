package org.charvolant.tmsnet.resources.networks;

import java.util.Arrays;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.charvolant.tmsnet.resources.networks.ChannelIdentification;
import org.charvolant.tmsnet.resources.networks.NetworkIdentification;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for network identification
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public class NetworkIdentificationTest {

    private NetworkIdentification network;

    /**
   * @throws java.lang.Exception
   */
    @Before
    public void setUp() throws Exception {
    }

    /**
   * @throws java.lang.Exception
   */
    @After
    public void tearDown() throws Exception {
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.resources.networks.ChannelIdentification#getLogicalChannelNumber()}.
   */
    @Test
    public void testLoad1() throws Exception {
        JAXBContext context = JAXBContext.newInstance("org.charvolant.tmsnet.resources.networks");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        this.network = (NetworkIdentification) unmarshaller.unmarshal(this.getClass().getResource("network1.xml"));
        Assert.assertEquals("Test", this.network.getName());
        Assert.assertEquals("large.png", this.network.getLargeIcon().toString());
        Assert.assertEquals("small.png", this.network.getSmallIcon().toString());
        Assert.assertEquals(4119, this.network.getOriginalNetworkId());
        Assert.assertEquals("Please", this.network.getOperator());
        Assert.assertTrue(this.network.getChannels().isEmpty());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.resources.networks.ChannelIdentification#getLogicalChannelNumber()}.
   */
    @Test
    public void testLoad2() throws Exception {
        JAXBContext context = JAXBContext.newInstance("org.charvolant.tmsnet.resources.networks");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ChannelIdentification info;
        this.network = (NetworkIdentification) unmarshaller.unmarshal(this.getClass().getResource("network2.xml"));
        Assert.assertEquals("Test", this.network.getName());
        Assert.assertEquals("large.png", this.network.getLargeIcon().toString());
        Assert.assertEquals("small.png", this.network.getSmallIcon().toString());
        Assert.assertEquals(4119, this.network.getOriginalNetworkId());
        Assert.assertEquals(Arrays.asList(4120), this.network.getNetworkIds());
        Assert.assertEquals("Please", this.network.getOperator());
        Assert.assertEquals(1, this.network.getChannels().size());
        info = this.network.getChannels().get(2);
        Assert.assertNotNull(info);
        Assert.assertEquals("Channel1", info.getName());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.resources.networks.ChannelIdentification#getLogicalChannelNumber()}.
   */
    @Test
    public void testLoad3() throws Exception {
        JAXBContext context = JAXBContext.newInstance("org.charvolant.tmsnet.resources.networks");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ChannelIdentification info;
        this.network = (NetworkIdentification) unmarshaller.unmarshal(this.getClass().getResource("network3.xml"));
        Assert.assertEquals(2, this.network.getChannels().size());
        info = this.network.getChannels().get(2);
        Assert.assertNotNull(info);
        Assert.assertEquals("Channel1", info.getName());
        Assert.assertEquals(this.network.getLargeIcon(), info.getLargeIcon());
        Assert.assertEquals(this.network.getSmallIcon(), info.getSmallIcon());
        info = this.network.getChannels().get(20);
        Assert.assertNotNull(info);
        Assert.assertEquals("Channel2", info.getName());
        Assert.assertEquals(this.network.getLargeIcon(), info.getLargeIcon());
        Assert.assertFalse(this.network.getSmallIcon().equals(info.getSmallIcon()));
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.resources.networks.ChannelIdentification#getLogicalChannelNumber()}.
   */
    @Test
    public void testLoad4() throws Exception {
        JAXBContext context = JAXBContext.newInstance("org.charvolant.tmsnet.resources.networks");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ChannelIdentification info;
        this.network = (NetworkIdentification) unmarshaller.unmarshal(this.getClass().getResource("network4.xml"));
        info = this.network.getChannel(2);
        Assert.assertNotNull(info);
        Assert.assertEquals("Channel1", info.getName());
        Assert.assertEquals(this.network.getLargeIcon(), info.getLargeIcon());
        Assert.assertEquals(this.network.getSmallIcon(), info.getSmallIcon());
        info = this.network.getChannel(20);
        Assert.assertNotNull(info);
        Assert.assertEquals("ChannelE1", info.getName());
        Assert.assertEquals(this.network.getLargeIcon(), info.getLargeIcon());
        Assert.assertEquals(this.network.getSmallIcon(), info.getSmallIcon());
    }
}
