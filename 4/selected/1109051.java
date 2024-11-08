package org.dbe.kb.smman;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.dbe.kb.rdb.DBObject;
import java.io.StringReader;
import java.sql.ResultSet;
import java.util.Vector;

public class SMmanager {

    private ByteArrayInputStream _bin;

    private final String SMSQL = "insert into DBESM.SERVICEMANIFEST (SMID, SERVICEMANIFEST, SDLMODELID, SSLMODELID, SSLDATAID, SCMMODELID, DOCUMENTATION) values (?,?,?,?,?,?,?)";

    private final String RESSDLSQL = "select sm.SMID, res.Rank, sm.DOCUMENTATION  from DBESM.ServiceManifest as sm, DBESDL.Definitions as def , DBE_SYS.Results as res where res.ID = def.NAME and sm.SDLMODELID = res.ID";

    private final String RESSSLSQL = "select sm.SMID, res.Rank, sm.DOCUMENTATION  from DBESM.ServiceManifest as sm, DBE_SYS.Results as res where sm.SSLMODELID = sp.SEMANTICPACKAGEID";

    private final String RESDATASQL = "select sm.SMID, res.Rank, sm.DOCUMENTATION from DBESSL.SERVICEPROFILEINSTANCE as sp, DBESM.SERVICEMANIFEST as sm, DBE_SYS.Results as res where sp.serviceprofileinstanceid = res.ID and sp.semanticpackageinstanceid = sm.ssldataid";

    public SMmanager() {
    }

    public void processSM(InputStream in) throws Exception {
    }

    public InputStream getBMLmodel() throws Exception {
        return _bin;
    }

    public InputStream getSDLmodel() throws Exception {
        return _bin;
    }

    public InputStream getBMLdata() throws Exception {
        return _bin;
    }

    public String storeSM(String sslID, String sdlID, String scmID, String sslDataID) throws Exception, java.sql.SQLException {
        DBObject dbo = new DBObject();
        ResultSet rs = dbo.submitQuery("select max(SMID) from DBESM.SERVICEMANIFEST");
        rs.next();
        int smid;
        if ((smid = rs.getInt(1)) != 0) smid += 10; else smid = 10010;
        dbo.createNewStatement(this.SMSQL);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        System.out.println(bout.toString());
        String buffer = new String(bout.toByteArray());
        StringReader sr = new StringReader(buffer);
        dbo.setString(1, "" + smid);
        dbo.setCharacterStream(2, sr, bout.size());
        dbo.setString(3, sdlID);
        dbo.setString(4, sslID);
        dbo.setString(5, sslDataID);
        dbo.setString(6, scmID);
        if (sslID != null && sslID.toLowerCase().indexOf("sms") != -1) dbo.setString(7, "SMS Messaging Service"); else if (sslID != null && sslID.toLowerCase().indexOf("email") != -1) dbo.setString(7, "Electronic Mail Service"); else if (sslID != null && sslID.indexOf("Taxi") != -1) dbo.setString(7, "Taxi Reservation Service"); else if (buffer != null && buffer.indexOf("Kydon") != -1) dbo.setString(7, "Kydon Hotel. Chania Crete"); else if (buffer != null && buffer.indexOf("Samaria") != -1) dbo.setString(7, "Samaria Hotel. Chania Crete"); else if (buffer != null && buffer.indexOf("Omalos") != -1) dbo.setString(7, "Omalos Hotel. Chania Crete"); else dbo.setString(7, "Description not available..");
        System.out.println("model:" + sslID.toLowerCase());
        dbo.executeUpdate();
        return "" + smid;
    }

    public void getSM(String smID, OutputStream out) throws Exception, java.sql.SQLException {
        System.out.println("SR: Get SM with id=" + smID);
        DBObject dbo = new DBObject();
        dbo.createNewStatement("SELECT SERVICEMANIFEST FROM DBESM.SERVICEMANIFEST WHERE SMID=?");
        dbo.setInt(1, Integer.parseInt(smID));
        ResultSet rs = dbo.executeQuery();
        if (rs.next() == false) {
            System.out.println("SR: SM not found .. Returning empty SM.");
            dbo.close();
            return;
        }
        InputStream in = rs.getAsciiStream(1);
        int k;
        while ((k = in.read()) != -1) out.write(k);
        out.flush();
        dbo.close();
    }

    public void deleteSM(String smID, OutputStream out) throws java.io.IOException, java.sql.SQLException {
        System.out.println("DeleteSM:" + smID);
        DBObject dbo = new DBObject();
        dbo.createNewStatement("delete from DBESM.SERVICEMANIFEST where SMID=?");
        dbo.setString(1, smID);
        dbo.executeUpdate();
        if (out != null) {
            out.write("<SM_DELETED/>".getBytes());
            out.flush();
        }
        dbo.close();
    }

    public void formulateQueryResults(String modelName, OutputStream out) throws Exception, java.sql.SQLException {
        DBObject dbo = new DBObject();
        ResultSet rs = null;
        System.out.println("Formulate Results for " + modelName);
        if (modelName.equals("SSL")) {
            rs = dbo.submitQuery(this.RESDATASQL);
        } else if (modelName.equals("sdl")) {
            rs = dbo.submitQuery(this.RESSDLSQL);
        }
        Vector v = new Vector();
        while (rs.next()) {
            String[] selem = new String[3];
            selem[0] = rs.getString(1);
            selem[1] = rs.getString(2);
            selem[2] = rs.getString(3);
            System.out.println("Result=" + selem[0] + ", " + selem[1]);
            v.addElement(selem);
        }
        java.io.ObjectOutputStream oout = new java.io.ObjectOutputStream(out);
        oout.writeObject(v);
        oout.flush();
    }
}
