package servlet.util.osmania;

/**
 *
 * @author  Administrator
 */
public class PatronDetailsHandler {

    private ejb.bprocess.util.NewGenXMLGenerator newGenXMLGenerator = null;

    private ejb.bprocess.util.Utility utility = null;

    private ejb.bprocess.util.HomeFactory homeFactory = null;

    /** Creates a new instance of PatronDetailsHandler */
    public PatronDetailsHandler() {
    }

    public String getDetails(org.jdom.Document doc) {
        String str = "";
        java.util.Hashtable ht1 = new java.util.Hashtable();
        utility = ejb.bprocess.util.Utility.getInstance(null);
        homeFactory = ejb.bprocess.util.HomeFactory.getInstance();
        newGenXMLGenerator = ejb.bprocess.util.NewGenXMLGenerator.getInstance(null);
        String xmlstr = (new org.jdom.output.XMLOutputter()).outputString(doc);
        java.util.Hashtable ht = newGenXMLGenerator.parseXMLDocument(xmlstr);
        java.util.Vector v1 = new java.util.Vector();
        String libid = ht.get("LibraryID").toString();
        String patid = ht.get("PatronId").toString();
        java.lang.Integer patlibid = new java.lang.Integer(libid);
        try {
            System.out.println("sending patronid=" + patid);
            System.out.println("sending libid=" + patlibid);
            v1 = ((ejb.bprocess.util.NewGenHome) ejb.bprocess.util.HomeFactory.getInstance().getRemoteHome("NewGen")).create().getPatronDetailsForIdCard(null, patid, patlibid);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        System.out.println("vector size in handler=" + v1.size());
        org.jdom.Element root = new org.jdom.Element("Response");
        if (v1.size() > 0) {
            org.jdom.Element ele1 = new org.jdom.Element("Name");
            ele1.setText(v1.elementAt(0).toString());
            root.addContent(ele1);
            org.jdom.Element root1 = new org.jdom.Element("Present");
            java.util.Vector v2 = (java.util.Vector) v1.elementAt(1);
            org.jdom.Element ele21 = new org.jdom.Element("Adress1");
            ele21.setText(v2.elementAt(0).toString());
            root1.addContent(ele21);
            org.jdom.Element ele22 = new org.jdom.Element("Adress2");
            ele22.setText(v2.elementAt(1).toString());
            root1.addContent(ele22);
            org.jdom.Element ele23 = new org.jdom.Element("City");
            ele23.setText(v2.elementAt(2).toString());
            root1.addContent(ele23);
            root.addContent(root1);
            org.jdom.Element root2 = new org.jdom.Element("Parmenent");
            java.util.Vector v3 = (java.util.Vector) v1.elementAt(2);
            org.jdom.Element ele31 = new org.jdom.Element("Adress1");
            ele31.setText(v3.elementAt(0).toString());
            root2.addContent(ele31);
            org.jdom.Element ele32 = new org.jdom.Element("Adress2");
            ele32.setText(v3.elementAt(1).toString());
            root2.addContent(ele32);
            org.jdom.Element ele33 = new org.jdom.Element("City");
            ele33.setText(v3.elementAt(2).toString());
            root2.addContent(ele33);
            root.addContent(root2);
            org.jdom.Element ele4 = new org.jdom.Element("Department");
            ele4.setText(v1.elementAt(3).toString());
            root.addContent(ele4);
            org.jdom.Element ele5 = new org.jdom.Element("Course");
            ele5.setText(v1.elementAt(4).toString());
            root.addContent(ele5);
        } else {
            org.jdom.Element ele5 = new org.jdom.Element("Results");
            ele5.setText("notfound");
            root.addContent(ele5);
        }
        org.jdom.Document doc2 = new org.jdom.Document(root);
        str = (new org.jdom.output.XMLOutputter()).outputString(doc2);
        return str;
    }

    public java.util.Vector getPhotos(org.jdom.Document doc) {
        java.util.Vector v1 = new java.util.Vector();
        java.util.Hashtable ht1 = new java.util.Hashtable();
        utility = ejb.bprocess.util.Utility.getInstance(null);
        homeFactory = ejb.bprocess.util.HomeFactory.getInstance();
        newGenXMLGenerator = ejb.bprocess.util.NewGenXMLGenerator.getInstance(null);
        String xmlstr = (new org.jdom.output.XMLOutputter()).outputString(doc);
        java.util.Hashtable ht = newGenXMLGenerator.parseXMLDocument(xmlstr);
        String libid = ht.get("LibraryID").toString();
        String patid = ht.get("PatronId").toString();
        String fileSeperator = System.getProperties().get("file.separator").toString();
        try {
            java.io.File patpho = new java.io.File(ejb.bprocess.util.NewGenLibRoot.getRoot() + "/PatronPhotos/" + "LIB_" + libid + "/" + "PAT_" + patid + ".jpg");
            System.out.println("patronId : " + patid);
            v1.addElement("PAT_" + patid + ".jpg");
            java.nio.channels.FileChannel fc = (new java.io.FileInputStream(patpho)).getChannel();
            int fileLength = (int) fc.size();
            System.out.println("fileLength : " + fileLength);
            java.nio.MappedByteBuffer bb = fc.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fileLength);
            byte[] byx = new byte[bb.capacity()];
            System.out.println(byx.length);
            System.out.println(bb.hasArray());
            fc.close();
            bb.get(byx);
            System.out.println(byx.length);
            v1.addElement(byx);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return v1;
    }
}
