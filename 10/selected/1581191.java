package com.knowgate.hipermail;

import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.BodyPart;
import javax.mail.Part;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;
import java.util.Vector;
import com.knowgate.dataobjs.DB;
import com.knowgate.debug.DebugFile;

/**
 * @author Sergio Montoro Ten
 * @version 1.0
 */
public class DBMimeMultipart extends Multipart {

    private Vector aParts = new Vector();

    private Part oParent;

    public DBMimeMultipart(Part oMessage) {
        oParent = oMessage;
    }

    public Part getParent() {
        return oParent;
    }

    public void addBodyPart(MimePart part) throws MessagingException {
        aParts.add(part);
    }

    public int getCount() {
        return aParts.size();
    }

    public BodyPart getBodyPart(int index) throws MessagingException {
        BodyPart oRetVal = null;
        try {
            oRetVal = (BodyPart) aParts.get(index);
        } catch (ArrayIndexOutOfBoundsException aiob) {
            throw new MessagingException("Invalid message part index", aiob);
        }
        return oRetVal;
    }

    public BodyPart getBodyPart(String cid) throws MessagingException {
        Object oPart;
        if (cid == null) return null;
        final int iParts = aParts.size();
        for (int p = 0; p < iParts; p++) {
            oPart = aParts.get(p);
            if (cid.equals(((MimePart) oPart).getContentID())) return (BodyPart) oPart;
        }
        return null;
    }

    public void removeBodyPart(int iPart) throws MessagingException, ArrayIndexOutOfBoundsException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMultipart.removeBodyPart(" + String.valueOf(iPart) + ")");
            DebugFile.incIdent();
        }
        DBMimeMessage oMsg = (DBMimeMessage) getParent();
        DBFolder oFldr = ((DBFolder) oMsg.getFolder());
        Statement oStmt = null;
        ResultSet oRSet = null;
        String sDisposition = null, sFileName = null;
        boolean bFound;
        try {
            oStmt = oFldr.getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(SELECT " + DB.id_disposition + "," + DB.file_name + " FROM " + DB.k_mime_parts + " WHERE " + DB.gu_mimemsg + "='" + oMsg.getMessageGuid() + "' AND " + DB.id_part + "=" + String.valueOf(iPart) + ")");
            oRSet = oStmt.executeQuery("SELECT " + DB.id_disposition + "," + DB.file_name + " FROM " + DB.k_mime_parts + " WHERE " + DB.gu_mimemsg + "='" + oMsg.getMessageGuid() + "' AND " + DB.id_part + "=" + String.valueOf(iPart));
            bFound = oRSet.next();
            if (bFound) {
                sDisposition = oRSet.getString(1);
                if (oRSet.wasNull()) sDisposition = "inline";
                sFileName = oRSet.getString(2);
            }
            oRSet.close();
            oRSet = null;
            oStmt.close();
            oStmt = null;
            if (!bFound) {
                if (DebugFile.trace) DebugFile.decIdent();
                throw new MessagingException("Part not found");
            }
            if (!sDisposition.equals("reference") && !sDisposition.equals("pointer")) {
                if (DebugFile.trace) DebugFile.decIdent();
                throw new MessagingException("Only parts with reference or pointer disposition can be removed from a message");
            } else {
                if (sDisposition.equals("reference")) {
                    try {
                        File oRef = new File(sFileName);
                        if (oRef.exists()) oRef.delete();
                    } catch (SecurityException se) {
                        if (DebugFile.trace) DebugFile.writeln("SecurityException " + sFileName + " " + se.getMessage());
                        if (DebugFile.trace) DebugFile.decIdent();
                        throw new MessagingException("SecurityException " + sFileName + " " + se.getMessage(), se);
                    }
                }
                oStmt = oFldr.getConnection().createStatement();
                if (DebugFile.trace) DebugFile.writeln("Statement.executeUpdate(DELETE FROM " + DB.k_mime_parts + " WHERE " + DB.gu_mimemsg + "='" + oMsg.getMessageGuid() + "' AND " + DB.id_part + "=" + String.valueOf(iPart) + ")");
                oStmt.executeUpdate("DELETE FROM " + DB.k_mime_parts + " WHERE " + DB.gu_mimemsg + "='" + oMsg.getMessageGuid() + "' AND " + DB.id_part + "=" + String.valueOf(iPart));
                oStmt.close();
                oStmt = null;
                oFldr.getConnection().commit();
            }
        } catch (SQLException sqle) {
            if (oRSet != null) {
                try {
                    oRSet.close();
                } catch (Exception ignore) {
                }
            }
            if (oStmt != null) {
                try {
                    oStmt.close();
                } catch (Exception ignore) {
                }
            }
            try {
                oFldr.getConnection().rollback();
            } catch (Exception ignore) {
            }
            if (DebugFile.trace) DebugFile.decIdent();
            throw new MessagingException(sqle.getMessage(), sqle);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMultipart.removeBodyPart()");
        }
    }

    public void writeTo(OutputStream os) throws IOException, MessagingException {
        throw new UnsupportedOperationException("Method writeTo() not implemented for DBMimeMultipart");
    }
}
