package de.gamobi.jkariam.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import de.gamobi.jkariam.exceptions.BuildingNotFoundException;
import de.gamobi.jkariam.simpletypes.Position;

public final class City implements Comparable<City> {

    private String id;

    private String name;

    private Position position;

    private String resource;

    private int totalPopulation;

    private int worker;

    private int miner;

    private int scientists;

    private int priests;

    private ArrayList<Building> buildings = new ArrayList<Building>();

    private File cityFile;

    public City(File city) throws JDOMException, IOException {
        this.cityFile = city;
        this.loadAll();
    }

    public City(File city, boolean test) throws JDOMException, IOException {
        this.cityFile = city;
        if (!test) this.loadAll();
    }

    public static City createNewCity(String accountName, String cityName) throws IOException, JDOMException {
        String id = getHighestID(accountName);
        String src = "examples" + File.separator + "city.xml";
        String dest = "saves" + File.separator + accountName + File.separator + id + " - " + cityName + ".xml";
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);
        copy(fis, fos);
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(dest);
        Element city = doc.getRootElement();
        city.getAttribute("name").setValue(cityName);
        XMLOutputter xmlOut = new XMLOutputter();
        xmlOut.output(doc, new BufferedOutputStream(new FileOutputStream(dest)));
        return new City(new File(dest));
    }

    @Override
    public int compareTo(City arg0) {
        return this.id.compareTo(arg0.id);
    }

    public void loadAll() throws JDOMException, IOException {
        this.loadAttributes();
        this.loadBuildings();
    }

    public void loadAttributes() throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(this.cityFile);
        Element city = doc.getRootElement();
        this.id = this.cityFile.getName().split(" ")[0];
        this.name = city.getAttributeValue("name");
        this.position = new Position(city.getAttributeValue("position"));
        this.resource = city.getAttributeValue("resource");
        this.totalPopulation = Integer.valueOf(city.getChild("population").getAttributeValue("total"));
        this.worker = Integer.valueOf(city.getChild("population").getAttributeValue("worker"));
        this.miner = Integer.valueOf(city.getChild("population").getAttributeValue("miner"));
        this.scientists = Integer.valueOf(city.getChild("population").getAttributeValue("scientists"));
        this.priests = Integer.valueOf(city.getChild("population").getAttributeValue("priests"));
    }

    public void loadBuildings() throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(this.cityFile);
        Element city = doc.getRootElement();
        for (String building : new File("buildings").list()) {
            String bname = building.replaceAll(".xml", "");
            if (bname.startsWith(".")) continue;
            try {
                Building tmp = new Building(bname);
                tmp.setLevel(Integer.valueOf(city.getChild("buildings").getChild(bname.toLowerCase()).getAttributeValue("level")));
                this.buildings.add(tmp);
            } catch (BuildingNotFoundException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(this.buildings);
    }

    public void saveAll(String accountName) throws JDOMException, IOException {
        this.saveAttributes(accountName);
        this.saveBuildings(accountName);
    }

    public void saveAttributes(String accountName) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(this.cityFile);
        Element city = doc.getRootElement();
        city.getAttribute("position").setValue(this.position.toString());
        city.getChild("population").getAttribute("total").setValue(String.valueOf(this.totalPopulation));
        city.getChild("population").getAttribute("worker").setValue(String.valueOf(this.worker));
        city.getChild("population").getAttribute("miner").setValue(String.valueOf(this.miner));
        city.getChild("population").getAttribute("scientists").setValue(String.valueOf(this.scientists));
        city.getChild("population").getAttribute("priests").setValue(String.valueOf(this.priests));
        XMLOutputter xmlOut = new XMLOutputter();
        xmlOut.output(doc, new BufferedOutputStream(new FileOutputStream("saves" + File.separator + accountName + File.separator + id + " - " + name + ".xml")));
    }

    public void saveBuildings(String accountName) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(this.cityFile);
        Element city = doc.getRootElement();
        for (Building building : this.buildings) {
            String bname = building.getName().toLowerCase();
            city.getChild("buildings").getChild(bname).getAttribute("level").setValue(String.valueOf(building.getLevel()));
        }
        XMLOutputter xmlOut = new XMLOutputter();
        xmlOut.output(doc, new BufferedOutputStream(new FileOutputStream("saves" + File.separator + accountName + File.separator + id + " - " + name + ".xml")));
    }

    public void setAttributeValue(String attributeName, Object aValue) {
        if (attributeName.equals("Gesamtbevölkerung")) {
            this.totalPopulation = Integer.valueOf(aValue.toString());
        } else if (attributeName.equals("Holzfäller")) {
            this.worker = Integer.valueOf(aValue.toString());
        } else if (attributeName.equals("Arbeiter")) {
            this.miner = Integer.valueOf(aValue.toString());
        } else if (attributeName.equals("Forscher")) {
            this.scientists = Integer.valueOf(aValue.toString());
        } else if (attributeName.equals("Priester")) {
            this.priests = Integer.valueOf(aValue.toString());
        } else {
            for (Building building : this.buildings) {
                if (building.getName().equals(attributeName) && !aValue.toString().equals("")) {
                    building.setLevel(Integer.valueOf(aValue.toString()));
                }
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[0xFFFF];
        for (int len; (len = in.read(buffer)) != -1; ) out.write(buffer, 0, len);
    }

    private static String getHighestID(String dir) {
        String[] files = new File("saves" + File.separator + dir).list();
        int highestID = 0;
        for (String file : files) {
            if (file.matches(".. - .*")) {
                int tmp = Integer.valueOf(file.split(" ")[0]);
                if (tmp > highestID) highestID = tmp;
            }
        }
        highestID++;
        if (highestID < 10) {
            return "0" + String.valueOf(highestID);
        } else {
            return String.valueOf(highestID);
        }
    }

    public int getIncome() {
        return (this.totalPopulation - this.miner - this.worker - this.priests - this.scientists) * 3 - (this.priests + this.scientists) * 3;
    }

    /**
	 * @return the position
	 */
    public Position getPosition() {
        return position;
    }

    /**
	 * @param position the position to set
	 */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
	 * @return the resource
	 */
    public String getResource() {
        return resource;
    }

    /**
	 * @param resource the resource to set
	 */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
	 * @return the totalPopulation
	 */
    public int getTotalPopulation() {
        return totalPopulation;
    }

    /**
	 * @param totalPopulation the totalPopulation to set
	 */
    public void setTotalPopulation(int totalPopulation) {
        this.totalPopulation = totalPopulation;
    }

    /**
	 * @return the worker
	 */
    public int getWorker() {
        return worker;
    }

    /**
	 * @param worker the worker to set
	 */
    public void setWorker(int worker) {
        this.worker = worker;
    }

    /**
	 * @return the miner
	 */
    public int getMiner() {
        return miner;
    }

    /**
	 * @param miner the miner to set
	 */
    public void setMiner(int miner) {
        this.miner = miner;
    }

    /**
	 * @return the scientists
	 */
    public int getScientists() {
        return scientists;
    }

    /**
	 * @param scientists the scientists to set
	 */
    public void setScientists(int scientists) {
        this.scientists = scientists;
    }

    /**
	 * @return the priests
	 */
    public int getPriests() {
        return priests;
    }

    /**
	 * @param priests the priests to set
	 */
    public void setPriests(int priests) {
        this.priests = priests;
    }

    /**
	 * @return the buildings
	 */
    public ArrayList<Building> getBuildings() {
        return buildings;
    }

    /**
	 * @param buildings the buildings to set
	 */
    public void setBuildings(ArrayList<Building> buildings) {
        this.buildings = buildings;
    }

    /**
	 * This method returns a building by name.
	 * @param name The building to look for.
	 * @return The building or null if no building with this name was found.
	 */
    public Building getBuildingByName(String name) {
        for (Building b : this.buildings) {
            if (b.getName().equals(name)) return b;
        }
        return null;
    }

    /**
	 * @return the id
	 */
    public String getId() {
        return id;
    }

    /**
	 * @return the name
	 */
    public String getName() {
        return name;
    }
}
