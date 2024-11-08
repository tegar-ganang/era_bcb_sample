package tools;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import engine.EngineDriver;
import tools.files.FileUtils;
import user.*;

public class Store {

    /**
     * Default path for a store icon.  Used if none is specified.
     */
    private final String DEFAULT_ICON_PATH = "ENCOM/images/default_store_icon.jpg";

    /**
     * Display-able name of Store object.
     */
    private String name;

    /**
     * Path of the Store's Icon.
     */
    private String iconPath;

    /**
	 * Icon used to represent this store.
	 */
    private ImageIcon icon;

    /**
	 * ID of the owner of this store.
	 */
    private String ownerID;

    /**
     * ID of this store. Will be used as key.
     */
    private String id;

    /**
	 * Used to retain the gross profit of the store object.
	 */
    private double income = 0;

    /**
     * Class constructor.
     * @param c StoreOwner Reference of the owner of this Store
     * @param n String Name of this Store object
     */
    public Store(StoreOwner c, String n) {
        ownerID = c.getID();
        name = n;
        iconPath = DEFAULT_ICON_PATH;
        id = getIDcode();
    }

    /**
     * Class constructor.
     * @param c Customer Reference to owner of this Store
     * @param n String Name of this store object
     */
    public Store(Customer c, String n) {
        ownerID = c.getID();
        name = n;
        iconPath = DEFAULT_ICON_PATH;
        id = getIDcode();
        buildIcon();
    }

    /**
     * Class constructor.
     * @param ownerid String ID of the owner of this store
     * @param n String Owner of this store
     */
    public Store(String ownerid, String n) {
        this.ownerID = ownerid;
        name = n;
        iconPath = DEFAULT_ICON_PATH;
        id = getIDcode();
        buildIcon();
    }

    /** <h3>Store</h3>CLB
     * 	
     *  <p>public void <strong>Store(String ownerid,String storename,String storeid,String path)</strong><p>
     * 	<p>&nbsp;&nbsp;This is for LOADING Store Objects (Store Objects that where already made into Store Objects at one time.</p> 
     *   
     * @param ownerid			-Pass in StoreOwner ID
     * @param storename			-Pass in name of Store
     * @param storeid			-Pass in store ID
     * @param path				-Pass in Icon path
     * @deprecated              -For loading only 
     */
    public Store(String ownerid, String storename, double bal, String storeid, String path) {
        ownerID = ownerid;
        name = storename;
        income = bal;
        iconPath = DEFAULT_ICON_PATH;
        iconPath = path;
        id = storeid;
        buildIcon();
    }

    /**
     * Class constructor.
     * @param c StoreOwner Reference to the owner of this store
     * @param n String Name of this Store object
     * @param path String path of the location of the icon for this store
     */
    public Store(StoreOwner c, String n, String path) {
        ownerID = c.getID();
        name = n;
        iconPath = path;
        id = getIDcode();
        buildIcon();
    }

    /**
     * Class constructor.
     * @param c Customer Reference to the owner of this store
     * @param n String Name of this store object
     * @param path String path of the location of the icon for this store
     */
    public Store(Customer c, String n, String path) {
        ownerID = c.getID();
        name = n;
        iconPath = path;
        id = getIDcode();
        buildIcon();
    }

    /**
     * Sets the Name of the Store object.
     * @param n String Name to be set as the Store object's name.
     */
    public void setName(String n) {
        name = n;
    }

    /**
     * Sets the icon of the store via a file object.  The path will be extracted.
     * @param f File File object to be used at the icon.
     */
    public void setIcon(File f) {
        if (f != null) iconPath = f.getPath(); else iconPath = this.DEFAULT_ICON_PATH;
        buildIcon();
    }

    /**
     * Sets the icon of the store via a String that represents the path
     * of where the image file is located.  (Should be located in ENCOM/images/)
     * @param f String String representation of the relative path of image file
     */
    public void setIcon(String f) {
        if (f != null) iconPath = f; else iconPath = this.DEFAULT_ICON_PATH;
        buildIcon();
    }

    /**
     * Links the Store object to an owner via the StoreOwner's id.  This is set
     * inside the object to make sure it is linked to an owner.
     * @param id String The owner's ID string passed to create a link between store &
     * owner.
     */
    public void setOwnerID(String id) {
        ownerID = id;
    }

    /**
	 * Sets the ID of the store.  This is used mostly by the engine to handle the
	 * object.  It also functions as the object's primary key in the database.
	 * @param i String ID to be set as the store's id. 
	 */
    public void setID(String i) {
        id = i;
    }

    /**
	 * Sets the income of the store.
	 * @param income
	 */
    public void setIncome(double income) {
        this.income = income;
    }

    /**
	 * Adds a double to the income of the store.
	 * @param i double Number to be added to the store's income.
	 */
    public void addIncome(double i) {
        income += i;
    }

    /**
	 * Private method to generate ID codes for each store instance.
	 * @return String ID to be set as ID code for object.
	 */
    private String getIDcode() {
        return EngineDriver.generateID(EngineDriver.STORE_ID);
    }

    /**
	 * Private method that attempts to build an ImageIcon object to set in the
	 * Store objects.  Will print to the report logs if failure occurs.
	 */
    private void buildIcon() {
        if (iconPath != null) {
            if (iconPath.endsWith(".png") || iconPath.endsWith(".gif") || iconPath.endsWith(".jpg") || iconPath.endsWith(".jpeg")) {
                if (FileUtils.checkImgEncomDirectory(new File(iconPath))) {
                    icon = new ImageIcon(iconPath, name);
                }
                if (FileUtils.checkImgEncomDirectory(new File(iconPath))) icon = new ImageIcon(iconPath, name); else {
                    File source = new File(iconPath);
                    File dest = new File("/ENCOM/images" + source.getName());
                    try {
                        if (FileUtils.copyFile(source, dest)) {
                            icon = new ImageIcon(source.getPath(), name);
                        } else {
                            iconPath = this.DEFAULT_ICON_PATH;
                        }
                    } catch (IOException e) {
                        Logger.getLogger(Store.class.getPackage().getName()).log(Level.WARNING, "IOException occured during icon creation of " + name, e);
                    }
                }
            } else {
                iconPath = this.DEFAULT_ICON_PATH;
                buildIcon();
            }
        }
    }

    /**
     * Returns the readable name of the Store object.
     * @return String Name of the Store.
     */
    public String getName() {
        return name;
    }

    /**
	 * Returns the ID of the StoreOwner linked to this store object.
	 * @return String ID of Owner.
	 */
    public String getOwnerID() {
        return ownerID;
    }

    /**
	 * Returns the ID/Primary Key of the Store object.
	 * @return String ID of the Store.
	 */
    public String getID() {
        return id;
    }

    /**
	 * Returns the income of the store
	 * @return double Store's gross income.
	 */
    public double getIncome() {
        return income;
    }

    /**
	 * Returns the string representation of the relative path of the store's icon.
	 * @return String Relative path of the Store's logo.
	 */
    public String getIconPath() {
        return iconPath;
    }

    /**
     * Returns the ImageIcon of the store, linking to the built icon.
     * @return ImageIcon Icon of the Store
     */
    public ImageIcon getIcon() {
        return icon;
    }
}
