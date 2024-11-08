package com.simplerss.handler;

import com.simplerss.dataobject.*;
import com.simplerss.helper.DateHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class RSSHandler extends ChainedHandler {

    protected Channel channel = null;

    protected ArrayList items = null;

    protected CloudHandler cloudHandler = null;

    protected ItemHandler itemHandler = null;

    protected ImageHandler imageHandler = null;

    protected TextInputHandler textInputHandler = null;

    protected SkipHoursHandler skipHoursHandler = null;

    protected SkipDaysHandler skipDaysHandler = null;

    public RSSHandler(XMLReader reader) {
        super(reader);
        cloudHandler = new CloudHandler(this);
        itemHandler = new ItemHandler(this);
        imageHandler = new ImageHandler(this);
        textInputHandler = new TextInputHandler(this);
        skipHoursHandler = new SkipHoursHandler(this);
        skipDaysHandler = new SkipDaysHandler(this);
        items = new ArrayList();
    }

    public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
        super.startElement(uri, name, qName, atts);
        String tag = qName.toLowerCase();
        if (("cloud").equals(tag)) {
            cloudHandler.startHandlingEvents(tag, atts);
        } else if (("image").equals(tag)) {
            imageHandler.startHandlingEvents(tag, atts);
        } else if (("textinput").equals(tag)) {
            textInputHandler.startHandlingEvents(tag, atts);
        } else if (("skiphours").equals(tag)) {
            skipHoursHandler.startHandlingEvents(tag, atts);
        } else if (("skipdays").equals(tag)) {
            skipDaysHandler.startHandlingEvents(tag, atts);
        } else if (("item").equals(tag)) {
            itemHandler.startHandlingEvents(tag, atts);
        } else if (("channel").equals(tag)) {
            channel = createChannel();
        }
    }

    protected Channel createChannel() {
        return new Channel();
    }

    public void setAttribute(String aName, Object aObject) throws SAXException {
        if (("cloud").equals(aName)) {
            channel.setCloud((Cloud) aObject);
        } else if (("image").equals(aName)) {
            channel.setImage((Image) aObject);
        } else if (("textinput").equals(aName)) {
            channel.setTextInput((TextInput) aObject);
        } else if (("skiphours").equals(aName)) {
            channel.setSkipHours((SkipHours) aObject);
        } else if (("skipdays").equals(aName)) {
            channel.setSkipDays((SkipDays) aObject);
        } else if (("item").equals(aName)) {
            items.add(aObject);
        }
    }

    public void endElement(String uri, String name, String qName) throws SAXException {
        String tag = qName.toLowerCase();
        if (("title").equals(tag)) {
            channel.setTitle(mText);
        } else if (("link").equals(tag)) {
            try {
                channel.setLink(new URL(mText));
            } catch (MalformedURLException mue) {
            }
        } else if (("description").equals(tag)) {
            channel.setDescription(mText);
        } else if (("language").equals(tag)) {
            channel.setLanguage(mText);
        } else if (("copyright").equals(tag)) {
            channel.setCopyright(mText);
        } else if (("managingeditor").equals(tag)) {
            channel.setManagingEditor(mText);
        } else if (("webmaster").equals(tag)) {
            channel.setWebMaster(mText);
        } else if (("pubdate").equals(tag)) {
            channel.setPubDate(DateHelper.parseDate(mText));
        } else if (("lastbuilddate").equals(tag)) {
            channel.setLastBuildDate(DateHelper.parseDate(mText));
        } else if (("category").equals(tag)) {
            channel.setCategory(mText);
        } else if (("generator").equals(tag)) {
            channel.setGenerator(mText);
        } else if (("docs").equals(tag)) {
            try {
                channel.setDocs(new URL(mText));
            } catch (MalformedURLException mue) {
            }
        } else if (("ttl").equals(tag)) {
            channel.setTtl(Integer.parseInt(mText));
        } else if (("rating").equals(tag)) {
            channel.setRating(mText);
        } else if (("channel").equals(tag)) {
            channel.setItem((Item[]) items.toArray(new Item[items.size()]));
        }
    }

    public Channel getChannel() {
        return channel;
    }
}
