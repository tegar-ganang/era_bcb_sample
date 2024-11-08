package dbaccess.dmsp;

import java.sql.*;
import java.util.*;
import java.awt.*;
import dbaccess.util.*;

/**
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

    /** Array containing online and availability codes for each of 12 channels.
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

    /**
    public Section(DBConnect c) {
        stmt = c.getStatement();
        found = false;
    }

    /**
    public Section(DBConnect c, String satellite, LatLon ll, DateTime srchtm) {
        stmt = c.getStatement();
        found = false;
        found = get(satellite, ll, srchtm);
    }

    /**
    public Section(Statement st, ResultSet rs) {
        stmt = st;
        getSectionData(rs);
        found = true;
    }

    public boolean isFound() {
        return found;
    }

    /**
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

    /**
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

    /**
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

    /**
    public boolean nextSection() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, SectionLength);
        found = getSectionByTime(satellite, srchtm, SectionLength / 2);
        return found;
    }

    /**
    public boolean prevSection() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, -SectionLength);
        found = getSectionByTime(satellite, srchtm, SectionLength / 2);
        return found;
    }

    /**
    public boolean nextOrbit() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, OrbitLength);
        found = getSectionByTime(satellite, srchtm, SectionLength / 2);
        return found;
    }

    /**
    public boolean prevOrbit() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, -OrbitLength);
        found = getSectionByTime(satellite, srchtm, SectionLength / 2);
        return found;
    }

    /**
    public boolean nextPass() {
        if (!found) return found;
        DateTime srchtm = (DateTime) sectionTime.clone();
        srchtm.add(Calendar.SECOND, OrbitLength * 7);
        found = getSectionByLocation(satellite, meanll, srchtm);
        return found;
    }

    /**
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