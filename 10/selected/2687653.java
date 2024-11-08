package avoware.intchat.server.servlet;

import avoware.intchat.server.misc.Tools;
import avoware.intchat.server.db.IntChatDatabaseOperations;
import avoware.intchat.server.session.IntChatSessionManager;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import avoware.intchat.shared.IntChatConstants;
import avoware.intchat.server.IntChatServerDefaults;
import java.sql.ResultSet;
import avoware.intchat.shared.xml.IntChatMessage;
import java.util.HashMap;
import java.net.URLEncoder;
import java.net.URLDecoder;
import avoware.intchat.server.io.IntChatServletInputStream;
import avoware.intchat.server.io.IntChatServletOutputStream;
import java.sql.Timestamp;
import java.util.TimeZone;
import avoware.intchat.server.db.IntChatSequence;
import avoware.intchat.server.RuntimeParameters;
import avoware.intchat.server.RuntimeParameters.ParameterNames;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.mortbay.jetty.RetryRequest;

/**
 *
 * @author Andrew Orlov
 */
public class File extends _Auth {

    private static final int CHUNK_SIZE = 65535;

    private IntChatSequence ic_messages_id_seq;

    /** Creates a new instance of File */
    public File(IntChatSessionManager iccsm, IntChatSequence ic_messages_id_seq) throws IllegalArgumentException {
        super(iccsm);
        if (ic_messages_id_seq != null) {
            this.ic_messages_id_seq = ic_messages_id_seq;
        } else throw new IllegalArgumentException("Input argument cannot be null");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (doAuth(request, response)) {
            Connection conn = null;
            try {
                int UID = icsm.getIntChatSession(request).getUID();
                long id = Long.parseLong(request.getParameter("id"));
                conn = getJDBCConnection(icsm.getHeavyDatabaseConnectionPool(), request, response, HttpServletResponse.SC_MOVED_TEMPORARILY);
                if (conn == null) return;
                ResultSet rs = IntChatDatabaseOperations.executeQuery(conn, "SELECT ic_messages.mhead, ic_messages.mbody " + "FROM ic_messages, ic_recipients WHERE ic_recipients.mid=ic_messages.id AND ic_recipients.rid=" + UID + " AND ic_messages.id=" + id);
                if (rs.next()) {
                    long length = Long.parseLong(rs.getString("mbody"));
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/octet-stream");
                    response.addHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(rs.getString("mhead"), IntChatServerDefaults.ENCODING) + "\"");
                    response.setHeader(IntChatConstants.HEADER_FILELENGTH, Long.toString(length));
                    IntChatServletOutputStream out = new IntChatServletOutputStream(request, response);
                    rs.getStatement().close();
                    rs = null;
                    if (!getBLOB(conn, out, id)) {
                        Tools.writeFileInfo(conn, UID, id, false, IntChatConstants.FileOperations.FILE_NOT_FOUND);
                        response.reset();
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    }
                    out.flush();
                    out.finish();
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                if (rs != null) {
                    rs.getStatement().close();
                    rs = null;
                }
            } catch (RetryRequest rr) {
                throw rr;
            } catch (NumberFormatException nfe) {
                Tools.makeErrorResponse(request, response, HttpServletResponse.SC_BAD_REQUEST, nfe);
            } catch (Exception e) {
                Tools.makeErrorResponse(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            } finally {
                try {
                    if (conn != null) icsm.getHeavyDatabaseConnectionPool().releaseConnection(conn);
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (doAuth(request, response)) {
            Connection conn = null;
            try {
                int UID = icsm.getIntChatSession(request).getUID();
                conn = getJDBCConnection(icsm.getHeavyDatabaseConnectionPool(), request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                if (conn == null) return;
                ResultSet rs = IntChatDatabaseOperations.executeQuery(conn, "SELECT id FROM ic_messagetypes WHERE templatename='" + IntChatConstants.MessageTemplates.IC_FILES + "' LIMIT 1");
                if (rs.next()) {
                    int fileTypeID = rs.getInt("id");
                    String recipients = request.getHeader(IntChatConstants.HEADER_FILERECIPIENTS);
                    rs.getStatement().close();
                    rs = null;
                    if (recipients != null) {
                        HashMap<String, String> hm = Tools.parseMultiparamLine(request.getHeader("Content-Disposition"));
                        String fileName = URLDecoder.decode(hm.get("filename"), IntChatServerDefaults.ENCODING);
                        long fileLength = (request.getHeader("Content-Length") != null ? Long.parseLong(request.getHeader("Content-Length")) : -1);
                        fileLength = (request.getHeader(IntChatConstants.HEADER_FILELENGTH) != null ? Long.parseLong(request.getHeader(IntChatConstants.HEADER_FILELENGTH)) : fileLength);
                        long maxFileSize = RuntimeParameters.getIntValue(ParameterNames.MAX_FILE_SIZE) * 1048576;
                        if (maxFileSize > 0 && fileLength > maxFileSize) {
                            request.getInputStream().close();
                            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                            return;
                        }
                        long now = System.currentTimeMillis();
                        long nextid = ic_messages_id_seq.nextval();
                        IntChatServletInputStream in = new IntChatServletInputStream(request);
                        IntChatMessage icm = null;
                        conn.setAutoCommit(false);
                        try {
                            PreparedStatement ps = conn.prepareStatement("INSERT INTO ic_messages (id, tid, mhead, mbody, mdate, sid) VALUES (?, ?, ?, ?, ?, ?)");
                            ps.setLong(1, nextid);
                            ps.setInt(2, fileTypeID);
                            ps.setString(3, fileName);
                            ps.setString(4, Long.toString(fileLength));
                            ps.setLong(5, now);
                            ps.setInt(6, UID);
                            ps.executeUpdate();
                            ps.close();
                            if (!insertBLOB(conn, in, fileLength, nextid, maxFileSize)) {
                                conn.rollback();
                                return;
                            }
                            icm = new IntChatMessage(false, fileTypeID, null, null);
                            String[] id = recipients.split(",");
                            int id1;
                            for (int i = 0; i < id.length; i++) {
                                id1 = Integer.parseInt(id[i].trim());
                                IntChatDatabaseOperations.executeUpdate(conn, "INSERT INTO ic_recipients (mid, rid) VALUES ('" + nextid + "', '" + id1 + "')");
                                icm.addTo(id1);
                            }
                            conn.commit();
                        } catch (Exception e) {
                            conn.rollback();
                            throw e;
                        } finally {
                            conn.setAutoCommit(true);
                        }
                        if (icm != null) {
                            icm.setID(nextid);
                            icm.setDate(new Timestamp(now - TimeZone.getDefault().getOffset(now)));
                            icm.setFrom(UID);
                            icm.setHeadText(fileName);
                            icm.setBodyText(Long.toString(fileLength));
                            icsm.onClientSentMessage(icm);
                        }
                        response.setStatus(HttpServletResponse.SC_OK);
                    } else {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                if (rs != null) {
                    rs.getStatement().close();
                    rs = null;
                }
            } catch (RetryRequest rr) {
                throw rr;
            } catch (Exception e) {
                Tools.makeErrorResponse(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            } finally {
                try {
                    if (conn != null) icsm.getHeavyDatabaseConnectionPool().releaseConnection(conn);
                } catch (Exception e) {
                }
            }
        }
    }

    private static int fillBuffer(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        int l;
        while ((buffer.length - offset) > 0 && (l = in.read(buffer, offset, buffer.length - offset)) != -1) {
            in.available();
            offset += l;
        }
        return offset;
    }

    public static boolean insertBLOB(Connection conn, InputStream in, long streamLength, long messageId, long maxFileSize) throws SQLException, IOException {
        byte[] buffer = new byte[CHUNK_SIZE];
        long chunk_id = 0;
        long bytesRead = 0;
        int l;
        PreparedStatement ps = conn.prepareStatement("INSERT INTO ic_messages_binarydata (mid, chunk_id, chunk_data) VALUES (?, ?, ?)");
        while ((l = fillBuffer(in, buffer)) > 0 && (maxFileSize > 0 ? bytesRead < maxFileSize : true)) {
            ps.setLong(1, messageId);
            ps.setLong(2, chunk_id);
            ps.setBinaryStream(3, new ByteArrayInputStream(buffer), l);
            ps.executeUpdate();
            chunk_id++;
            bytesRead += l;
        }
        ps.close();
        if (bytesRead < streamLength) {
            return false;
        }
        if (maxFileSize > 0 && bytesRead > maxFileSize) {
            throw new SQLException("MAX_FILE_SIZE limit is exceeded");
        }
        return true;
    }

    private boolean getBLOB(Connection conn, OutputStream out, long messageId) throws SQLException, IOException, InterruptedException {
        boolean found = false;
        ResultSet rs = null;
        conn.setAutoCommit(false);
        try {
            switch(IntChatDatabaseOperations.getDialect()) {
                case IntChatDatabaseOperations.DIALECT_POSTGRESQL:
                    rs = IntChatDatabaseOperations.executeQuery(conn, "SELECT chunk_data FROM ic_messages_binarydata WHERE mid=" + messageId + " ORDER BY chunk_id");
                    break;
                case IntChatDatabaseOperations.DIALECT_MYSQL:
                    rs = IntChatDatabaseOperations.executeQuery(conn, "SELECT chunk_data FROM ic_messages_binarydata WHERE mid=" + messageId + " ORDER BY chunk_id", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, Integer.MIN_VALUE);
                    break;
                default:
                    throw new SQLException("Unsupported SQL dialect");
            }
            while (rs.next()) {
                found = true;
                byte[] buffer = rs.getBytes(1);
                out.write(buffer, 0, buffer.length);
                buffer = null;
            }
        } finally {
            if (rs != null) {
                rs.getStatement().close();
                rs = null;
            }
            conn.setAutoCommit(true);
        }
        return found;
    }
}
