package dbaccess.dmsp;

import java.sql.*;
import java.util.*;
import java.awt.*;
import dbaccess.util.*;

/*** This class retrieves Section information from the DMSP database and* provides navigation methods to move to other sections.* <p>* <ul>* <li>Section and orbit times are <i>DateTime</i> objects.* <li>Each section may contain several sensor/channel codes which are* defined in the "Channel" class.* <li>The mean lat/lon point is an <i>LatLon</i> object and represents* the coordinates of the section's center point and is also a valid* grid point.* </ul>* The constructor will create the object and store connection information.* Use the <i>get()</i> method to search for the Section in the* database.  Use the <i>isFound()</i> method to determine if the* information was found or not after the call to <i>get()</i>.* <p>* Use the <i>print()</i> and <i>display()</i> methods to print Section* information to stdout or to display it to an AWT text area, respectively.* The <i>printHdr()</i> and <i>displayHdr()</i> print or display a* message telling whether or not the Section info was found.* <p>* Once found, use the <i>next()</i> and <i>prev</i> methods to move to* adjacent sections (north or south) in the orbit.  Use <i>nextOrbit()</i>* and <i>prevOrbit()</i> methods to move to the section (west or east) in* the adjacent orbit.  Use <i>nextPass()</i> and <i>prevPass()</i> to * move to approximately the same spot on the next or previous pass of the* satellite.* <p>* Note that the navigation methods do not create new objects, but change* the section information in the calling section object.* <p>* The static method <i>getSectionList()</i> retrieves a set of sections* based on a lat/lon and date/time range.  It returns a vector of section* objects.  Method <i>getSectionListAllYears()</i> does the same thing* except uses the date/time range without the year to select data allowing* you to find sections during the same time period accross several years.*/
public class Section {

    /** Image file name */
    public String name;

    /** Satellite (F10,F11,F12,F13) */
    public String satellite;

    /** Section Number (1-8) */
    public short section;

    /** Orbit start time for this section */
    public DateTime orbitTime;

    /** Section start time */
    public DateTime sectionTime;

    /** Array containing online and availability codes for each of 12 channels.   * The channels are assigned as follows:   * @see Channel   */
    public Channel[] channel = new Channel[MAX_CHANNELS];

    /** Scan Percent of image that is good */
    public float scanPercent;

    /** Decimal part of the orbit start time (6 decimals - portion of a second) */
    public float orbitPartSec;

    /** Decimal part of the section start time (6 decimals - portion of a second) */
    public float sectionPartSec;

    /** lat/lon (approximate) of image center point (1 decimal) */
    public LatLon meanll;

    /** minimum lat/lon of the image boundries */
    public LatLon minll;

    /** maximum lat/lon of the image boundries */
    public LatLon maxll;

    Statement stmt;

    boolean found;

    static final int OrbitLength = 6060;

    static final int SectionLength = OrbitLength / 8;

    static final int HRS6 = 21600;

    static final int HRS12 = 43200;

    static final int MAX_CHANNELS = 12;

    static final int LOCBYR = 1993;

    static final int LOCEYR = 2002;

    static final int MAXATTEMPTS = 3;

    static final String LOCTAB = "sectionLoc";

    /**     * Create the section class     * @param c DBConnect object for JDBC     */
    public Section(DBConnect c) {
        stmt = c.getStatement();
        found = false;
    }

    /**     * Combines the get() method with the constructor.     * @param c DBConnect object for JDBC     * @param satellite Satellite name     * @param ll Latitude/Longitude point to search for     * @param srchtm Date/time to search for     */
    public Section(DBConnect c, String satellite, LatLon ll, DateTime srchtm) {
        stmt = c.getStatement();
        found = false;
        found = get(satellite, ll, srchtm);
    }

    /**     * Set the class variables from the database result set.  This     * constructor expects that the Section rows have been previously     * retrieved.     * @param st JDBC Statement object     * @param rs JDBC ResultSet object     */
    public Section(Statement st, ResultSet rs) {
        stmt = st;
        getSectionData(rs);
        found = true;
    }

    public boolean isFound() {
        return found;
    }

    /**     * Get a dmsp section that contains the grid point lat/lon     * for a satellite and is near date/time.     * <ul>     * <li>To find the first section of the day set hr=6.     * <li>To find the last section of the day set hr=18.     * </ul>     * @param satellite Satellite name     * @param ll Latitude/Longitude point to search for     * @param srchtm Date/time to search for     * @return True if section found; False otherwise     */
    public boolean get(String satellite, LatLon ll, DateTime srchtm) {
        DateTime imgtm, tmptm;
        String begtm, endtm;
        found = false;
        String locTab = LOCTAB + srchtm.get(Calendar.YEAR);
        tmptm = (DateTime) srchtm.clone();
        tmptm.add(Calendar.HOUR, -6);
        begtm = tmptm.toYMDString();
        tmptm = (DateTime) srchtm.clone();
        tmptm.add(Calendar.HOUR, 6);
        endtm = tmptm.toYMDString();
        imgtm = new DateTime(1900, 1, 1, 0, 0, 0);
        DateTimeCompare dtc = new DateTimeCompare(srchtm);
        String query = "SELECT sectionTime FROM " + locTab;
        query += " WHERE satellite='" + satellite;
        query += "' and lat=" + new Format("%5.2f").form(ll.getLat());
        query += " and lon=" + new Format("%6.2f").form(ll.getLon());
        query += " and sectionTime>" + begtm;
        query += " and sectionTime<" + endtm;
        try {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                tmptm = new DateTime(rs.getString("sectionTime"));
                if (dtc.isCloser(tmptm)) imgtm = tmptm;
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println(" *** " + e.getSQLState());
            e.printStackTrace();
        }
        query = "SELECT * FROM sections";
        query += " WHERE satellite='" + satellite + "' and sectionTime=" + imgtm.toYMDString();
        try {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                found = true;
                getSectionData(rs);
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return found;
    }

    /**     * Get a dmsp section containing the grid point lat/lon and with     * a section center point closest to lat/lon for a satellite and     * date/time.  Use a wide +/- time when searching by time and      * then pick the closest section to the lat/lon given.     * @param satellite Satellite name     * @param ll Latitude/Longitude point to search for     * @param srchtm Date/time to search for     * @return True if section found; False otherwise     */
    protected boolean getSectionByLocation(String satellite, LatLon ll, DateTime srchtm) {
        DateTime tmptm;
        String begtm, endtm;
        found = false;
        String locTab = LOCTAB + srchtm.get(Calendar.YEAR);
        tmptm = (DateTime) srchtm.clone();
        tmptm.add(Calendar.HOUR, -6);
        begtm = tmptm.toYMDString();
        tmptm = (DateTime) srchtm.clone();
        tmptm.add(Calendar.HOUR, 6);
        endtm = tmptm.toYMDString();
        LatLonCompare lld = new LatLonCompare(ll);
        String query = "SELECT sections.* FROM " + locTab + ",sections";
        query += " WHERE " + locTab + ".satellite='" + satellite;
        query += "' and lat=" + new Format("%5.2f").form(ll.getLat());
        query += " and lon=" + new Format("%6.2f").form(ll.getLon());
        query += " and " + locTab + ".sectionTime>" + begtm;
        query += " and " + locTab + ".sectionTime<" + endtm;
        query += " and " + locTab + ".satellite=sections.satellite";
        query += " and " + locTab + ".sectionTime=sections.sectionTime";
        try {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                found = true;
                LatLon tmpll = new LatLon(rs.getFloat("meanLat"), rs.getFloat("meanLon"));
                if (lld.isCloser(tmpll)) getSectionData(rs);
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return found;
    }

    /**     * Get a section by section time.     * @param satellite Satellite name.     * @param srchtm Time to use in search.     * @param varsec +/- variance from search time (in seconds) allowed.     * @return True if section found; False otherwise     */
    public boolean getSectionByTime(String satellite, DateTime srchtm, int varsec) {
        DateTime imgtm, tmptm;
        String begtm, endtm;
        found = false;
        String locTab = LOCTAB + srchtm.get(Calendar.YEAR);
        tmptm = (DateTime) srchtm.clone();
        tmptm.add(Calendar.SECOND, -varsec);
        begtm = tmptm.toYMDString();
        tmptm = (DateTime) srchtm.clone();
        tmptm.add(Calendar.SECOND, varsec);
        endtm = tmptm.toYMDString();
        DateTimeCompare dtc = new DateTimeCompare(srchtm);
        String query = "SELECT * FROM sections";
        query += " WHERE satellite='" + satellite;
        query += "' and sectionTime>" + begtm;
        query += " and sectionTime<" + endtm;
        try {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                found = true;
                tmptm = new DateTime(rs.getString("sectionTime"));
                if (dtc.isCloser(tmptm)) getSectionData(rs);
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return (found);
    }

    /**     * Get the next section for this satellite.     * @return True if found; False otherwise     */
    public boolean nextSection() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, SectionLength);
        found = getSectionByTime(satellite, srchtm, SectionLength / 2);
        return found;
    }

    /**     * Get the previous section for this satellite.     * @return True if found; False otherwise     */
    public boolean prevSection() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, -SectionLength);
        found = getSectionByTime(satellite, srchtm, SectionLength / 2);
        return found;
    }

    /**     * Get the next orbit section for this satellite.     * @return True if found; False otherwise     */
    public boolean nextOrbit() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, OrbitLength);
        found = getSectionByTime(satellite, srchtm, SectionLength / 2);
        return found;
    }

    /**     * Get the previous orbit section for this satellite.     * @return True if found; False otherwise     */
    public boolean prevOrbit() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, -OrbitLength);
        found = getSectionByTime(satellite, srchtm, SectionLength / 2);
        return found;
    }

    /**     * Get the section at the current location on the next pass     * for this satellite.     * @return True if found; False otherwise     */
    public boolean nextPass() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, OrbitLength * 7);
        found = getSectionByLocation(satellite, meanll, srchtm);
        return found;
    }

    /**     * Get the section at the current location on the previous pass     * for this satellite.     * @return True if found; False otherwise     */
    public boolean prevPass() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, -OrbitLength * 7);
        found = getSectionByLocation(satellite, meanll, srchtm);
        return found;
    }

    public String getName() {
        return name;
    }

    public String getSatellite() {
        return satellite;
    }

    public short getSection() {
        return section;
    }

    public DateTime getSectionTime() {
        return sectionTime;
    }

    public DateTime getOrbitTime() {
        return orbitTime;
    }

    public float getSectionPartSec() {
        return sectionPartSec;
    }

    public float getOrbitPartSec() {
        return orbitPartSec;
    }

    public float getScanPercent() {
        return scanPercent;
    }

    public Channel[] getChannel() {
        return channel;
    }

    public LatLon getMeanll() {
        return meanll;
    }

    public float getMeanLat() {
        return meanll.getLat();
    }

    public float getMeanLon() {
        return meanll.getLon();
    }

    public LatLon getMinll() {
        return minll;
    }

    public float getMinLat() {
        return minll.getLat();
    }

    public float getMinLon() {
        return minll.getLon();
    }

    public LatLon getMaxll() {
        return maxll;
    }

    public float getMaxLat() {
        return maxll.getLat();
    }

    public float getMaxLon() {
        return maxll.getLon();
    }

    protected void getSectionData(ResultSet rs) {
        try {
            name = rs.getString("name");
            satellite = rs.getString("satellite");
            section = rs.getShort("section");
            orbitTime = new DateTime(rs.getString("orbitTime"));
            orbitPartSec = rs.getFloat("orbitPartialSeconds");
            sectionTime = new DateTime(rs.getString("sectionTime"));
            sectionPartSec = rs.getFloat("sectionPartialSeconds");
            scanPercent = rs.getFloat("scanPercent");
            meanll = new LatLon(rs.getFloat("meanLat"), rs.getFloat("meanLon"));
            minll = new LatLon(rs.getFloat("minLat"), rs.getFloat("minLon"));
            maxll = new LatLon(rs.getFloat("maxLat"), rs.getFloat("maxLon"));
            for (int i = 0; i < MAX_CHANNELS; i++) {
                String tmp = rs.getString("channel" + String.valueOf(i + 1));
                channel[i] = new Channel(i + 1, tmp.charAt(0), tmp.charAt(1));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void print() {
        if (!found) {
            System.out.println("*** Section not found\n");
            System.out.flush();
            return;
        }
        System.out.println("           *** Section Info ***");
        System.out.println("Name=" + name + "  Satellite=" + satellite + "  Section=" + section);
        System.out.println("            Channel/Sensor Info");
        for (int i = 0; i < MAX_CHANNELS; i++) channel[i].print();
        System.out.println("  Orbit Time=  " + orbitTime + " (" + orbitPartSec + ")");
        System.out.println("  Section Time=" + sectionTime + " (" + sectionPartSec + ")");
        System.out.println("  Scan=" + scanPercent + "%  Mean Lat,Lon=" + meanll);
        System.out.println("  Min Lat,Lon=" + minll + "  Max Lat,Lon=" + maxll + "\n");
        System.out.flush();
    }

    public void printHdr() {
        if (!found) {
            System.out.println("*** Section not found\n");
            System.out.flush();
            return;
        }
        System.out.println("Name=" + name + "  Satellite=" + satellite + "  Section=" + section + " " + sectionTime + "\n");
        System.out.flush();
    }

    public static void printList(Vector v) {
        int l = v.size();
        if (l < 1) {
            System.out.println("*** No sections found\n");
            return;
        }
        System.out.println("\n      Name         Sat Sect    SectionTime      Mean Lat/Lon");
        for (int i = 0; i < l; i++) {
            Section s = (Section) v.elementAt(i);
            System.out.println("[" + i + "] " + s.name + "  " + s.satellite + "  " + s.section + "  " + s.sectionTime + "  " + s.meanll);
        }
    }

    public void display(TextArea t) {
        if (!found) {
            t.append("*** Section not found\n");
            return;
        }
        t.append("\n           *** Section Info ***\n");
        t.append("Name=" + name + "  Satellite=" + satellite + "  Section=" + section + "\n");
        t.append("            Channel/Sensor Info\n");
        for (int i = 0; i < MAX_CHANNELS; i++) channel[i].display(t);
        t.append("  Orbit Time=  " + orbitTime + " (" + orbitPartSec + ")\n");
        t.append("  Section Time=" + sectionTime + " (" + sectionPartSec + ")\n");
        t.append("  Scan_percent=" + scanPercent + "%  Mean Lat,Lon=" + meanll);
        t.append("  Min Lat,Lon=" + minll + "  Max Lat,Lon=" + maxll + "\n");
    }

    public void displayHdr(TextArea t) {
        if (!found) {
            t.append("*** Section not found\n");
            return;
        }
        t.append("Name=" + name + "  Sat=" + satellite + "  Sect=" + section + " " + sectionTime + "\n");
    }

    public static void displayList(TextArea t, Vector v) {
        int l = v.size();
        if (l < 1) {
            t.append("*** No sections found\n");
            return;
        }
        t.append("\n            Name               Sat  Sect    SectionTime      Mean Lat Lon\n");
        for (int i = 0; i < l; i++) {
            Section s = (Section) v.elementAt(i);
            t.append("[" + i + "] " + s.name + "  " + s.satellite + "  " + s.section + "  " + s.sectionTime + "  " + s.meanll + "\n");
        }
    }
}
