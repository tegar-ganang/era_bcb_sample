package de.gamobi.jkariam.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class Account {

    /** This is the name of the player or his account. */
    private String player;

    /** This is the server on which this account is running. */
    private String server;

    /** This is the list of cities the user owns. */
    private ArrayList<City> cities = new ArrayList<City>();

    /**
	 * This list contains the order of the buildings for the user interface. Although this is part of the view
	 * (Model-View-Controller) this attribute is located here because it is defined in the user's save file. 
	 */
    private LinkedList<String> buildingOrder = this.loadBuildingOrder();

    public Account(String account) throws JDOMException, IOException {
        this.loadAccount(account);
    }

    public static Account createNewAccount(String accountName, String server) throws IOException, JDOMException {
        new File("saves" + File.separator + accountName).mkdir();
        String src = "examples" + File.separator + "account.xml";
        String dest = "saves" + File.separator + accountName + File.separator + accountName + ".xml";
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);
        copy(fis, fos);
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(dest);
        Element city = doc.getRootElement();
        city.getAttribute("name").setValue(accountName);
        city.getAttribute("server").setValue(server);
        XMLOutputter xmlOut = new XMLOutputter();
        xmlOut.output(doc, new BufferedOutputStream(new FileOutputStream(dest)));
        return new Account(accountName);
    }

    /**
	 * This method tries to load an account from the file system. It takes the name of the account as a parameter, and searches
	 * in the "saves" folder for it. If the account exists the corresponding XML file and all the city files will be opened
	 * and processed.
	 * 
	 * @param account
	 * @throws JDOMException
	 * @throws IOException
	 */
    public void loadAccount(String account) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build("saves" + File.separator + account + File.separator + account + ".xml");
        Element acc = doc.getRootElement();
        this.player = acc.getAttributeValue("name");
        this.server = acc.getAttributeValue("server");
        File dir = new File("saves" + File.separator + account);
        String[] files = dir.list();
        Arrays.sort(files);
        for (String file : files) {
            if (!file.equals(this.getPlayer() + ".xml") && !file.startsWith(".")) {
                this.cities.add(new City(new File(dir + File.separator + file)));
            }
        }
        Collections.sort(this.cities);
    }

    public void saveAccount() throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build("saves" + File.separator + this.player + File.separator + this.player + ".xml");
        Element acc = doc.getRootElement();
        acc.getAttribute("name").setValue(this.player);
        acc.getAttribute("server").setValue(this.server);
        XMLOutputter xmlOut = new XMLOutputter();
        xmlOut.output(doc, new BufferedOutputStream(new FileOutputStream("saves" + File.separator + this.player + File.separator + this.player + ".xml")));
        for (City city : this.cities) {
            city.saveAll(this.player);
        }
    }

    public LinkedList<String> loadBuildingOrder() {
        LinkedList<String> buildingOrder = new LinkedList<String>();
        String[] buildings = new File("buildings").list();
        for (String b : buildings) {
            if (!b.startsWith(".")) buildingOrder.add(b.split("[.]")[0]);
        }
        Collections.sort(buildingOrder);
        return buildingOrder;
    }

    public City getCityByName(String name) {
        for (City c : this.cities) {
            if (c.getName().equals(name)) return c;
        }
        return null;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[0xFFFF];
        for (int len; (len = in.read(buffer)) != -1; ) out.write(buffer, 0, len);
    }

    /**
	 * @return the cities
	 */
    public ArrayList<City> getCities() {
        return cities;
    }

    /**
	 * @param cities the cities to set
	 */
    public void setCities(ArrayList<City> cities) {
        this.cities = cities;
    }

    /**
	 * @return the player
	 */
    public String getPlayer() {
        return player;
    }

    /**
	 * @return the server
	 */
    public String getServer() {
        return server;
    }

    /**
	 * @return the buildingOrder
	 */
    public LinkedList<String> getBuildingOrder() {
        return buildingOrder;
    }

    /**
	 * @param buildingOrder the buildingOrder to set
	 */
    public void setBuildingOrder(LinkedList<String> buildingOrder) {
        this.buildingOrder = buildingOrder;
    }
}
