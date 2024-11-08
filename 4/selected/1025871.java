package net.sourceforge.olduvai.lrac.drawer.strips;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.sourceforge.olduvai.lrac.LiveRAC;
import net.sourceforge.olduvai.lrac.drawer.templates.Template;
import net.sourceforge.olduvai.lrac.logging.LogEntry;
import net.sourceforge.olduvai.lrac.util.Util;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 *
 */
public class StripHandler {

    /**
	 * Name of the file containing strip channels
	 */
    private static final String CHANNELNAMEFILE = LiveRAC.SYSTEMPREFSFOLDER + "channels.txt";

    /**
	 * Default strip definition file distributed with LiveRAC
	 */
    public static final String DEFAULTSTRIPFILE = "strips.xml";

    /**
     * List of strips.  This is a LinkedList to allow us to perform 
     * arbitrary inserts and because there will be a relatively small number
     * of strips (less than 100 typical).  
     */
    LinkedList<Strip> stripList;

    ArrayList<StripChannel> completeChannelList = null;

    HashMap<String, StripChannelGroup> channelGroupMap = null;

    /**
     * This contains a map of each channels defined in the strip
     * to their respective palettes.  The list is required because it is possible for an alarm
     * or metric to be monitored in more than one column.   
     */
    HashMap<String, HashSet<Strip>> channelToStripMap = new HashMap<String, HashSet<Strip>>();

    Document doc = null;

    Element rootElement;

    /**
     * Used for associating strips to templates
     */
    HashMap<String, Template> templateList;

    /**
	 * 
	 */
    public StripHandler(InputStream stripInputStream, HashMap<String, Template> templateList) {
        this.templateList = templateList;
        makeCompleteChannelList();
        SAXBuilder builder = new SAXBuilder();
        try {
            doc = builder.build(stripInputStream);
        } catch (JDOMException e1) {
            System.err.println("StripHandler: Error parsing strip file.");
            e1.printStackTrace();
            return;
        } catch (IOException e1) {
            System.err.println("StripHandler: I/O error parsing strip file.");
            e1.printStackTrace();
            return;
        }
        stripList = new LinkedList<Strip>();
        rootElement = doc.getRootElement();
        List strips = rootElement.getChildren("strip");
        Iterator it = strips.iterator();
        Element e;
        while (it.hasNext()) {
            e = (Element) it.next();
            createStrip(e);
        }
        rebuildChannelStripMap();
    }

    /**
	 * creates a new palette with default settings
	 *
	 */
    public Strip newStrip() {
        Element stripTitle = new Element(Strip.TITLETAG);
        stripTitle.setText("New Strip");
        Element stripActive = new Element(Strip.ACTIVETAG);
        stripActive.setText(Boolean.toString(false));
        Element newStrip = new Element(Strip.ROOTTAG);
        newStrip.addContent(stripTitle);
        newStrip.addContent(stripActive);
        rootElement.addContent(newStrip);
        return createStrip(newStrip);
    }

    private Strip createStrip(Element e) {
        Strip p = new Strip(e, templateList, this);
        stripList.add(p);
        HashSet<Strip> singletonStrip = new HashSet<Strip>(1);
        singletonStrip.add(p);
        channelToStripMap.put(p.getStripTitle(), singletonStrip);
        return p;
    }

    public boolean deleteStrip(Strip p) {
        if (!stripList.contains(p)) return false;
        rootElement.removeContent(p.rootElement);
        return stripList.remove(p);
    }

    /**
	 * Returns an ArrayList of all the strips in the ordering that 
	 * they will be displayed by the rendering thread.  
	 * 
	 * @return ArrayList of strips in order
	 */
    public ArrayList<Strip> getStripList() {
        ArrayList<Strip> result = new ArrayList<Strip>(stripList.size());
        Iterator<Strip> it = stripList.iterator();
        while (it.hasNext()) {
            Strip s = it.next();
            if (s.isActive()) result.add(s);
        }
        return result;
    }

    /**
	 * Returns an ArrayList of all the strips in the ordering that 
	 * they will be displayed by the rendering thread.  
	 * 
	 * @return ArrayList of strips in order
	 */
    public List<Strip> getCompleteStripList() {
        return stripList;
    }

    public void saveStrips(File fileName, List<Strip> stripList) {
        Element newRoot = new Element("root");
        Document newDoc = new Document(newRoot);
        LiveRAC.makeLogEntry(LogEntry.STRIPS_SAVED, "Saving strips", stripList);
        Iterator<Strip> it = stripList.iterator();
        while (it.hasNext()) {
            Strip p = it.next();
            p.rootElement.detach();
            newRoot.addContent(p.rootElement);
        }
        XMLOutputter outputter = new XMLOutputter();
        try {
            outputter.output(newDoc, new FileWriter(fileName));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("StripHandler: Error writing template XML file");
        }
    }

    /**
	 * When a change occurs we need to rebuild the map between channels and strips
	 * 
	 */
    protected void rebuildChannelStripMap() {
        HashMap<String, HashSet<Strip>> newMap = new HashMap<String, HashSet<Strip>>();
        Iterator<Strip> stripIt = stripList.iterator();
        while (stripIt.hasNext()) {
            final Strip strip = stripIt.next();
            Iterator<StripChannel> channelIt = strip.getChannelList().iterator();
            while (channelIt.hasNext()) {
                final StripChannel channel = channelIt.next();
                final String channelId = channel.getChannelID();
                HashSet<Strip> newStripSet = newMap.get(channelId);
                if (newStripSet == null) {
                    newStripSet = new HashSet<Strip>();
                    newMap.put(channelId, newStripSet);
                }
                newStripSet.add(strip);
            }
        }
        channelToStripMap.clear();
        channelToStripMap.putAll(newMap);
    }

    /**
	 * Retrieves the mapping from a channel to a list of strips that use 
	 * the specified channel. 
	 * @return
	 */
    public HashMap<String, HashSet<Strip>> getChannelToStripMap() {
        return channelToStripMap;
    }

    /**
	 * Retrieve a file stream for either palette defaults or user defined palette settings
	 * @param cl
	 * @return
	 */
    public static InputStream getStripFileStream(ClassLoader cl) {
        final String defaultSystemPaletteFile = LiveRAC.getSelectedProfileSystemPath() + StripHandler.DEFAULTSTRIPFILE;
        final String defaultUserPaletteFile = LiveRAC.getSelectedProfileUserPath() + StripHandler.DEFAULTSTRIPFILE;
        return Util.getFileStream(cl, defaultSystemPaletteFile, defaultUserPaletteFile, LiveRAC.loadProfileDefault);
    }

    private void makeCompleteChannelList() {
        final ClassLoader cl = this.getClass().getClassLoader();
        InputStream listStream = cl.getResourceAsStream(CHANNELNAMEFILE);
        BufferedReader b = new BufferedReader(new InputStreamReader(listStream));
        completeChannelList = new ArrayList<StripChannel>();
        try {
            while (b.ready()) {
                String line = b.readLine();
                completeChannelList.add(StripChannel.createChannel(line));
            }
        } catch (IOException e) {
            System.err.println("StripHandler: I/O exception loading channels");
        }
        channelGroupMap = StripChannelGroup.createChannelGroupMap(completeChannelList);
        Iterator<StripChannelGroup> it = channelGroupMap.values().iterator();
        while (it.hasNext()) {
            final StripChannelGroup scg = it.next();
            final StripChannel sc = new StripChannel(StripChannel.PREFIX, scg.getPrefix(), scg.getPrefix() + StripChannelGroup.GROUPDESCRIPTION);
            completeChannelList.add(sc);
        }
        Collections.sort(completeChannelList, new Comparator<StripChannel>() {

            public int compare(StripChannel o1, StripChannel o2) {
                return o1.getChannelID().compareTo(o2.getChannelID());
            }
        });
    }

    public List<StripChannel> getCompleteChannelList() {
        return completeChannelList;
    }

    public HashMap<String, StripChannelGroup> getChannelGroupMap() {
        return channelGroupMap;
    }
}
