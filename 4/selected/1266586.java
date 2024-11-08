package servlet.administration;

/**
 *
 * @author  Administrator
 */
public class LocationHandler {

    private ejb.bprocess.util.NewGenXMLGenerator newGenXMLGenerator = null;

    private ejb.bprocess.util.Utility utility = null;

    private ejb.bprocess.util.HomeFactory homeFactory = null;

    /** Creates a new instance of LocationHandler */
    public LocationHandler() {
    }

    public String getViewNames(org.jdom.Document doc) {
        String retxml = "";
        utility = ejb.bprocess.util.Utility.getInstance(null);
        homeFactory = ejb.bprocess.util.HomeFactory.getInstance();
        newGenXMLGenerator = ejb.bprocess.util.NewGenXMLGenerator.getInstance(null);
        String xmlstr = (new org.jdom.output.XMLOutputter()).outputString(doc);
        java.util.Hashtable ht = newGenXMLGenerator.parseXMLDocument(xmlstr);
        java.util.Enumeration e1 = ht.keys();
        String libID = "";
        while (e1.hasMoreElements()) {
            String key = e1.nextElement().toString();
            libID = ht.get(key).toString();
        }
        try {
            retxml = ((ejb.bprocess.administration.LocationSessionHome) ejb.bprocess.util.HomeFactory.getInstance().getRemoteHome("LocationSession")).create().getDetails(libID);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return retxml;
    }

    public String updateLocationDetails(org.jdom.Document doc) {
        utility = ejb.bprocess.util.Utility.getInstance(null);
        homeFactory = ejb.bprocess.util.HomeFactory.getInstance();
        newGenXMLGenerator = ejb.bprocess.util.NewGenXMLGenerator.getInstance(null);
        String xmlstr = (new org.jdom.output.XMLOutputter()).outputString(doc);
        java.util.Hashtable ht = newGenXMLGenerator.parseXMLDocument(xmlstr);
        Integer libID = new Integer("" + ht.get("LibraryId"));
        String entryID = "" + ht.get("EntryId");
        String etdate = String.valueOf(ejb.bprocess.util.Utility.getInstance(null).getTimestamp().getTime());
        ht.put("EntryDate", etdate);
        java.sql.Timestamp entryDate = utility.getTimestamp("" + ht.get("EntryDate"));
        String location = ht.get("Location").toString();
        String coordinates = "";
        if (ht.get("Coordinates").toString() != null) {
            coordinates = ht.get("Coordinates").toString();
        }
        utility = ejb.bprocess.util.Utility.getInstance(null);
        homeFactory = ejb.bprocess.util.HomeFactory.getInstance();
        newGenXMLGenerator = ejb.bprocess.util.NewGenXMLGenerator.getInstance(null);
        String retxml = "";
        try {
            retxml = ((ejb.bprocess.administration.LocationSessionHome) ejb.bprocess.util.HomeFactory.getInstance().getRemoteHome("LocationSession")).create().updateLocatioDetails(libID, location, coordinates, entryID, entryDate);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        java.util.Hashtable ht2 = new java.util.Hashtable();
        ht2.put("Response", retxml);
        retxml = "";
        retxml = ejb.bprocess.util.NewGenXMLGenerator.getInstance(null).buildXMLDocument(ht2);
        return retxml;
    }

    public String modifyLocationDetails(org.jdom.Document doc) {
        utility = ejb.bprocess.util.Utility.getInstance(null);
        homeFactory = ejb.bprocess.util.HomeFactory.getInstance();
        newGenXMLGenerator = ejb.bprocess.util.NewGenXMLGenerator.getInstance(null);
        String xmlstr = (new org.jdom.output.XMLOutputter()).outputString(doc);
        java.util.Hashtable ht = newGenXMLGenerator.parseXMLDocument(xmlstr);
        Integer libID = new Integer(ht.get("LibraryId").toString());
        Integer locID = new Integer(ht.get("LocationId").toString());
        String entryID = ht.get("EntryId").toString();
        String etdate = String.valueOf(ejb.bprocess.util.Utility.getInstance(null).getTimestamp().getTime());
        ht.put("EntryDate", etdate);
        java.sql.Timestamp entryDate = utility.getTimestamp("" + ht.get("EntryDate"));
        String location = ht.get("Location").toString();
        String coordinates = ht.get("Coordinates").toString();
        String retxml = "";
        try {
            retxml = ((ejb.bprocess.administration.LocationSessionHome) ejb.bprocess.util.HomeFactory.getInstance().getRemoteHome("LocationSession")).create().modifyLocationDetails(libID, locID, location, coordinates, entryID, entryDate);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        java.util.Hashtable ht2 = new java.util.Hashtable();
        ht2.put("Response", retxml);
        retxml = "";
        retxml = ejb.bprocess.util.NewGenXMLGenerator.getInstance(null).buildXMLDocument(ht2);
        return retxml;
    }

    public String deleteLocation(org.jdom.Document doc) {
        String retxml = "";
        utility = ejb.bprocess.util.Utility.getInstance(null);
        homeFactory = ejb.bprocess.util.HomeFactory.getInstance();
        newGenXMLGenerator = ejb.bprocess.util.NewGenXMLGenerator.getInstance(null);
        String xmlstr = (new org.jdom.output.XMLOutputter()).outputString(doc);
        java.util.Hashtable ht = newGenXMLGenerator.parseXMLDocument(xmlstr);
        Integer libID = new Integer(ht.get("LibraryID").toString());
        Integer locID = new Integer(ht.get("LocationId").toString());
        try {
            retxml = ((ejb.bprocess.administration.LocationSessionHome) ejb.bprocess.util.HomeFactory.getInstance().getRemoteHome("LocationSession")).create().deleteLocation(libID, locID);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return retxml;
    }

    public String getMapNames(org.jdom.Document doc) {
        String libraryId = doc.getRootElement().getChildTextTrim("LibraryID");
        String filepath = ejb.bprocess.util.NewGenLibRoot.getRoot() + java.util.ResourceBundle.getBundle("server").getString("Maps");
        filepath += "/LIB_" + libraryId;
        java.io.File actualfile = new java.io.File(filepath);
        if (actualfile.exists()) {
            System.out.println("directory exists");
        } else {
            if (actualfile.mkdir()) System.out.println("directory created"); else System.out.println("directory does not created");
        }
        String[] s = actualfile.list();
        org.jdom.Element root = new org.jdom.Element("Response");
        if (s != null) {
            for (int i = 0; i < s.length; i++) {
                String filename = s[i];
                org.jdom.Element file = new org.jdom.Element("FileName");
                file.setText(filename);
                root.addContent(file);
            }
        }
        org.jdom.Document doc1 = new org.jdom.Document(root);
        String xmlstr = (new org.jdom.output.XMLOutputter()).outputString(doc1);
        return xmlstr;
    }

    public byte[] getMapFile(org.jdom.Document doc) {
        byte[] byx = new byte[100];
        try {
            String libraryId = doc.getRootElement().getChildTextTrim("LibraryID");
            String filename = doc.getRootElement().getChildTextTrim("FileName");
            String filepath = ejb.bprocess.util.NewGenLibRoot.getRoot() + java.util.ResourceBundle.getBundle("server").getString("Maps");
            filepath += "/LIB_" + libraryId + "/" + filename;
            java.io.File actualfile = new java.io.File(filepath);
            java.nio.channels.FileChannel fc = (new java.io.FileInputStream(actualfile)).getChannel();
            int fileLength = (int) fc.size();
            java.nio.MappedByteBuffer bb = fc.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fileLength);
            byx = new byte[bb.capacity()];
            bb.get(byx);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return byx;
    }
}
