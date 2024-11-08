package valentino.rejaxb.test;

import java.io.File;
import java.util.List;
import valentino.rejaxb.XmlBinderFactory;
import valentino.rejaxb.common.rss1.Rdf;
import valentino.rejaxb.common.rss1.RdfChannel;
import valentino.rejaxb.common.rss1.RdfChannelImage;
import valentino.rejaxb.common.rss1.RdfImage;
import valentino.rejaxb.common.rss1.RdfItem;
import valentino.rejaxb.common.rss1.RdfLi;
import valentino.rejaxb.common.rss2.Channel;
import valentino.rejaxb.common.rss2.Item;
import valentino.rejaxb.common.rss2.Rss2;
import junit.framework.TestCase;

public class UnitTest extends TestCase {

    private void assertBinderValid(BinderTestDTO binder) throws Exception {
        assertEquals("John", binder.firstName);
        assertEquals("Valentino", binder.lastName);
        assertEquals("John Valentino", binder.creator);
        assertEquals("Duck", binder.randomNode);
        assertEquals(456, binder.favoriteNumber);
        assertEquals(23, binder.shortTest);
        assertEquals(true, binder.booleanTest);
        assertEquals('a', binder.charTest);
        assertEquals(3.1234f, binder.floatTest);
        assertEquals(1.23456, binder.doubleTest);
        assertEquals(123456789l, binder.longTest);
        List<AddressDTO> addresses = binder.addresses;
        assertEquals(2, addresses.size());
        AddressDTO vo1 = addresses.get(0);
        assertEquals("123 Fake", vo1.street);
        assertEquals("NRH", vo1.city);
        assertEquals("TX", vo1.state);
        List<String> friends = vo1.friends;
        assertEquals(2, friends.size());
        assertEquals("Ryan", friends.get(0));
        assertEquals("Kyle", friends.get(1));
        AddressDTO vo2 = addresses.get(1);
        assertEquals("456 Fake", vo2.street);
        assertEquals("Hurst", vo2.city);
        List<String> phoneNumbers = binder.phoneNumbers;
        assertEquals(2, phoneNumbers.size());
        assertEquals("555-555-5555", phoneNumbers.get(0));
        assertEquals("333-555-5555", phoneNumbers.get(1));
        List<Integer> testInts = binder.testInts;
        assertEquals(2, testInts.size());
        assertEquals(0, testInts.get(0).intValue());
        assertEquals(1, testInts.get(1).intValue());
        assertEquals(0, binder.testShorts.get(0).shortValue());
        List<Person> persons = binder.persons;
        assertEquals(1, persons.size());
        assertEquals("John", persons.get(0).name);
        assertEquals(1.234f, binder.testFloats.get(0));
        assertEquals(1.0, binder.testDoubles.get(0));
        assertEquals(123456L, binder.testLongs.get(0).longValue());
    }

    private void assertBindersEquals(BinderTestDTO binderA, BinderTestDTO binderB) throws Exception {
        assertEquals(binderA.firstName, binderB.firstName);
        assertEquals(binderA.lastName, binderB.lastName);
        assertEquals(binderA.creator, binderB.creator);
        assertEquals(binderA.randomNode, binderB.randomNode);
        assertEquals(binderA.favoriteNumber, binderB.favoriteNumber);
        assertEquals(binderA.shortTest, binderB.shortTest);
        assertEquals(binderA.booleanTest, binderB.booleanTest);
        assertEquals(binderA.charTest, binderB.charTest);
        assertEquals(binderA.floatTest, binderB.floatTest);
        assertEquals(binderA.doubleTest, binderB.doubleTest);
        assertEquals(binderA.longTest, binderB.longTest);
        List<AddressDTO> addressesB = binderB.addresses;
        List<AddressDTO> addressesA = binderA.addresses;
        assertEquals(addressesA.size(), addressesB.size());
        AddressDTO vo1B = addressesB.get(0);
        AddressDTO vo1A = addressesA.get(0);
        assertEquals(vo1A.street, vo1B.street);
        assertEquals(vo1A.city, vo1B.city);
        assertEquals(vo1A.state, vo1B.state);
        List<String> friendsB = vo1B.friends;
        List<String> friendsA = vo1A.friends;
        assertEquals(friendsA.size(), friendsB.size());
        assertEquals(friendsA.get(0), friendsB.get(0));
        assertEquals(friendsA.get(1), friendsB.get(1));
        AddressDTO vo2B = addressesB.get(1);
        AddressDTO vo2A = addressesA.get(1);
        assertEquals(vo2A.street, vo2B.street);
        assertEquals(vo2A.city, vo2B.city);
        List<String> phoneNumbersB = binderB.phoneNumbers;
        List<String> phoneNumbersA = binderA.phoneNumbers;
        assertEquals(phoneNumbersA.size(), phoneNumbersB.size());
        assertEquals(phoneNumbersA.get(0), phoneNumbersB.get(0));
        assertEquals(phoneNumbersA.get(1), phoneNumbersB.get(1));
        List<Integer> testIntsB = binderB.testInts;
        List<Integer> testIntsA = binderA.testInts;
        assertEquals(testIntsA.size(), testIntsB.size());
        assertEquals(testIntsA.get(0).intValue(), testIntsB.get(0).intValue());
        assertEquals(testIntsA.get(1).intValue(), testIntsB.get(1).intValue());
        assertEquals(binderA.testShorts.get(0).shortValue(), binderB.testShorts.get(0).shortValue());
        List<Person> personsB = binderB.persons;
        List<Person> personsA = binderA.persons;
        assertEquals(personsA.size(), personsB.size());
        assertEquals(personsA.get(0).name, personsB.get(0).name);
        assertEquals(binderA.testFloats.get(0), binderB.testFloats.get(0));
        assertEquals(binderA.testDoubles.get(0), binderB.testDoubles.get(0));
        assertEquals(binderA.testLongs.get(0).longValue(), binderB.testLongs.get(0).longValue());
    }

    public void testGeneralBinding() {
        try {
            File file = new File("test/BinderTest.xml");
            BinderTestDTO binderA = new BinderTestDTO();
            XmlBinderFactory factory = XmlBinderFactory.newInstance();
            factory.bind(binderA, file);
            assertEquals(0, factory.getWarnings().size());
            factory = XmlBinderFactory.newInstance();
            String xml = factory.toXML(binderA);
            assertEquals(0, factory.getWarnings().size());
            BinderTestDTO binderB = new BinderTestDTO();
            factory = XmlBinderFactory.newInstance();
            factory.bind(binderB, xml);
            assertEquals(0, factory.getWarnings().size());
            assertBindersEquals(binderA, binderB);
            this.assertBinderValid(binderA);
            this.assertBinderValid(binderB);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void assertRssValid(Rss2 rss) throws Exception {
        assertEquals("2.0", rss.getVersion());
        List<Channel> channels = rss.getChannels();
        assertEquals(1, channels.size());
        Channel channel = channels.get(0);
        assertEquals("Lift Off News", channel.getTitle());
        assertEquals("http://liftoff.msfc.nasa.gov/", channel.getLink());
        assertEquals("Liftoff to Space Exploration.", channel.getDescription());
        assertEquals("en-us", channel.getLanguage());
        assertEquals("Tue, 10 Jun 2003 04:00:00 GMT", channel.getPubDate());
        assertEquals("http://blogs.law.harvard.edu/tech/rss", channel.getDocs());
        assertEquals("Weblog Editor 2.0", channel.getGenerator());
        assertEquals("editor@example.com", channel.getManagingEditor());
        assertEquals("webmaster@example.com", channel.getWebMaster());
        assertEquals(5, channel.getTtl());
        List<Item> items = channel.getItems();
        assertEquals(4, items.size());
        Item item1 = items.get(0);
        Item item2 = items.get(1);
        Item item3 = items.get(2);
        Item item4 = items.get(3);
        assertEquals("Star City", item1.getTitle());
        assertEquals("http://liftoff.msfc.nasa.gov/news/2003/news-starcity.asp", item1.getLink());
        assertEquals("Tue, 03 Jun 2003 09:39:21 GMT", item1.getPubDate());
        assertEquals("http://liftoff.msfc.nasa.gov/2003/06/03.html#item573", item1.getGuid());
        assertEquals("How do Americans get ready to work with Russians aboard the " + "International Space Station? They take a crash course in culture, language " + "and protocol at Russia's Star City.", item1.getDescription());
        assertEquals("Space Exploration", item2.getTitle());
        assertEquals("http://liftoff.msfc.nasa.gov/", item2.getLink());
        assertEquals("Fri, 30 May 2003 11:06:42 GMT", item2.getPubDate());
        assertEquals("http://liftoff.msfc.nasa.gov/2003/05/30.html#item572", item2.getGuid());
        assertEquals("The Engine That Does More", item3.getTitle());
        assertEquals("http://liftoff.msfc.nasa.gov/news/2003/news-VASIMR.asp", item3.getLink());
        assertEquals("Tue, 27 May 2003 08:37:32 GMT", item3.getPubDate());
        assertEquals("http://liftoff.msfc.nasa.gov/2003/05/27.html#item571", item3.getGuid());
        assertEquals("Astronauts' Dirty Laundry", item4.getTitle());
        assertEquals("http://liftoff.msfc.nasa.gov/news/2003/news-laundry.asp", item4.getLink());
        assertEquals("Tue, 20 May 2003 08:56:02 GMT", item4.getPubDate());
        assertEquals("http://liftoff.msfc.nasa.gov/2003/05/20.html#item570", item4.getGuid());
    }

    public void testRss2() {
        try {
            Rss2 rss = new Rss2();
            XmlBinderFactory factory = XmlBinderFactory.newInstance();
            factory.bind(rss, new File("test/Rss2Test.xml"));
            assertEquals(0, factory.getWarnings().size());
            assertRssValid(rss);
            String xml = XmlBinderFactory.newInstance().toXML(rss);
            factory = XmlBinderFactory.newInstance();
            assertEquals(0, factory.getWarnings().size());
            Rss2 rssB = new Rss2();
            factory = XmlBinderFactory.newInstance();
            factory.bind(rssB, xml);
            assertEquals(0, factory.getWarnings().size());
            assertRssValid(rssB);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void assertRss1Valid(Rdf rdf) throws Exception {
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", rdf.getXmlnsRdf());
        assertEquals("http://purl.org/rss/1.0/", rdf.getXmlns());
        RdfChannel channel = rdf.getChannels().get(0);
        assertEquals("http://www.xml.com/xml/news.rss", channel.getAbout());
        assertEquals("XML.com", channel.getTitle());
        assertEquals("XML.com features a rich mix of information and services " + "for the XML community.", channel.getDescription());
        assertEquals("http://xml.com/pub", channel.getLink());
        RdfChannelImage rImage = channel.getImage();
        assertEquals("http://xml.com/universal/images/xml_tiny.gif", rImage.getRdfResource());
        List<RdfLi> listItems = channel.getItems().getRdfSeq().getListItems();
        assertEquals(2, listItems.size());
        assertEquals("http://xml.com/pub/2000/08/09/xslt/xslt.html", listItems.get(0).getRdfResource());
        assertEquals("http://xml.com/pub/2000/08/09/rdfdb/index.html", listItems.get(1).getRdfResource());
        assertEquals("http://search.xml.com", channel.getTextinput().getRdfResource());
        RdfImage image = rdf.getImage();
        assertEquals("http://xml.com/universal/images/xml_tiny.gif", image.getAbout());
        assertEquals("XML.com", image.getTitle());
        assertEquals("http://www.xml.com", image.getLink());
        assertEquals("http://xml.com/universal/images/xml_tiny.gif", image.getUrl());
        List<RdfItem> items = rdf.getItems();
        assertEquals(2, items.size());
        RdfItem item1 = items.get(0);
        RdfItem item2 = items.get(1);
        assertEquals("http://xml.com/pub/2000/08/09/xslt/xslt.html", item1.getAbout());
        assertEquals("Processing Inclusions with XSLT", item1.getTitle());
        assertEquals("http://xml.com/pub/2000/08/09/xslt/xslt.html", item1.getLink());
        assertEquals("Processing document inclusions with general XML tools can be " + "problematic. This article proposes a way of preserving inclusion " + "information through SAX-based processing.", item1.getDescription());
        assertEquals("http://xml.com/pub/2000/08/09/rdfdb/index.html", item2.getAbout());
        assertEquals("Putting RDF to Work", item2.getTitle());
        assertEquals("http://xml.com/pub/2000/08/09/rdfdb/index.html", item2.getLink());
        assertEquals("Tool and API support for the Resource Description Framework " + "is slowly coming of age. Edd Dumbill takes a look at RDFDB, " + "one of the most exciting new RDF toolkits.", item2.getDescription());
    }

    public void testRss1() {
        try {
            Rdf rdf = new Rdf();
            XmlBinderFactory factory = XmlBinderFactory.newInstance();
            factory.bind(rdf, new File("test/Rss1Test.xml"));
            assertEquals(0, factory.getWarnings().size());
            assertRss1Valid(rdf);
            factory = XmlBinderFactory.newInstance();
            String xml = factory.toXML(rdf);
            assertEquals(0, factory.getWarnings().size());
            Rdf rdfB = new Rdf();
            factory = XmlBinderFactory.newInstance();
            factory.bind(rdfB, xml);
            assertEquals(0, factory.getWarnings().size());
            assertRss1Valid(rdfB);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
