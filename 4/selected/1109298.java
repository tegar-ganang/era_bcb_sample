package com.webhiker.dreambox.api.bouquet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.w3c.dom.Element;
import com.webhiker.dreambox.api.Utils;
import com.webhiker.dreambox.api.services.Service;

/**
 * The Class Bouquet lists the user configured bouquet information for the Dreambox.
 */
public class Bouquet {

    /** The channels. */
    private List<Channel> channels;

    /** The bref. */
    private String name, bref;

    /**
	 * Instantiates a new bouquet.
	 * 
	 * @param name the name
	 * @param bref the bref
	 */
    public Bouquet(String name, String bref) {
        setName(removeQuotes(name));
        setBref(removeQuotes(bref));
    }

    public Bouquet(Element element) {
        setName(Utils.getElement(element, "name", 0).getTextContent());
        setBref(Utils.getElement(element, "reference", 0).getTextContent());
        List<Service> services = new ArrayList<Service>();
        Service service;
        for (int i = 0; i < Utils.getSize(element, "service"); i++) {
            services.add(service = new Service(Utils.getElement(element, "service", i)));
            System.out.println("service>>>>" + service.getName());
        }
    }

    /**
	 * Removes the quotes.
	 * 
	 * @param s the s
	 * 
	 * @return the string
	 */
    public static String removeQuotes(String s) {
        if (s.indexOf('"') < 0) return s;
        return s.substring(1, s.length() - 1);
    }

    /**
	 * Gets the channels.
	 * 
	 * @return the channels
	 */
    public List<Channel> getChannels() {
        return channels;
    }

    /**
	 * Sets the channels.
	 * 
	 * @param c the new channels
	 */
    public void setChannels(List<Channel> c) {
        channels = c;
    }

    /**
	 * Sets the bouquets.
	 * 
	 * @param is the is
	 * 
	 * @return the list< bouquet>
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public static List<Bouquet> setBouquets(InputStream is) throws IOException {
        List<Bouquet> bouquets = new ArrayList<Bouquet>();
        String bstr = "var bouquets = new Array(";
        String brstr = "var bouquetRefs = new Array(";
        StringBuffer sb = Utils.getStringBuffer(is);
        is.close();
        int index = sb.indexOf(bstr);
        int bindex = sb.indexOf(brstr);
        StringTokenizer tok = new StringTokenizer(sb.substring(index + bstr.length(), sb.indexOf(");", index + bstr.length())), ",");
        StringTokenizer reftok = new StringTokenizer(sb.substring(bindex + brstr.length(), sb.indexOf(");", bindex + brstr.length())), ",");
        String token;
        String rtoken;
        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if (reftok.hasMoreTokens()) {
                rtoken = reftok.nextToken();
            } else {
                rtoken = "";
            }
            Bouquet bouquet;
            bouquets.add(bouquet = new Bouquet(token.trim(), rtoken.trim()));
            bouquet.setChannels(Channel.setChannels(sb.substring(bindex + 28), bouquets.size() - 1));
        }
        return bouquets;
    }

    /**
	 * Gets the name.
	 * 
	 * @return the name
	 */
    public String getName() {
        return name;
    }

    /**
	 * Sets the name.
	 * 
	 * @param name the new name
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * Gets the bref.
	 * 
	 * @return the bref
	 */
    public String getBref() {
        return bref;
    }

    /**
	 * Sets the bref.
	 * 
	 * @param bref the new bref
	 */
    public void setBref(String bref) {
        this.bref = bref;
    }

    public String toString() {
        return getName();
    }
}
