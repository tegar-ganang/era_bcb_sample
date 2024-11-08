package net.sourceforge.jcoupling2.dao.obsolete;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.sourceforge.jcoupling2.dao.ChannelRepository;
import org.apache.log4j.Logger;

/**
 * A <code>channel</code> object is a technology-agnostic abstraction of a "conduit". It serves to pass messages between
 * communication endpoints. For a <code>channel</code> to become persistent it must first be added to the
 * {@link ChannelRepository ChannelRepository}.
 */
public class Channel implements Comparable<Channel> {

    private static Logger log = Logger.getLogger(Channel.class);

    private static Integer channelID;

    private static String channelName;

    private static String fingerprintTableName;

    private static String spaceCoupling;

    private static String middlewareAdapterId;

    private static String isTimeDecoupled;

    private static String isWSDLBacked;

    private static String msgSchemaIn;

    private static String supportsInbound;

    private static String supportsInvoke;

    private static String wsdlChannelOperationName;

    private static String wsdlChannelPortType;

    private static String wsdlChannelUrl;

    private static String channelMsgSchemaOut;

    /**
	 * 
	 * @param channelName
	 */
    public Channel(String chName) {
        try {
            getChannelByName(chName);
        } catch (SQLException e) {
            e.printStackTrace();
            log.error(e.toString());
        }
    }

    public Channel(Integer chID) {
        try {
            getChannelByID(chID);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Integer getChannelID() {
        return channelID;
    }

    public static void setChannelID(Integer channelID) {
        Channel.channelID = channelID;
    }

    public static String getFingerprintTableName() {
        return fingerprintTableName;
    }

    public static void setFingerprintTableName(String fingerprintTableName) {
        Channel.fingerprintTableName = fingerprintTableName;
    }

    public static String getSpaceCoupling() {
        return spaceCoupling;
    }

    public static void setSpaceCoupling(String spaceCoupling) {
        Channel.spaceCoupling = spaceCoupling;
    }

    public static String getMiddlewareAdapterId() {
        return middlewareAdapterId;
    }

    public static void setMiddlewareAdapterId(String middlewareAdapterId) {
        Channel.middlewareAdapterId = middlewareAdapterId;
    }

    public static String getIsTimeDecoupled() {
        return isTimeDecoupled;
    }

    public static void setIsTimeDecoupled(String isTimeDecoupled) {
        Channel.isTimeDecoupled = isTimeDecoupled;
    }

    public static String getIsWSDLBacked() {
        return isWSDLBacked;
    }

    public static void setIsWSDLBacked(String isWSDLBacked) {
        Channel.isWSDLBacked = isWSDLBacked;
    }

    public static String getMsgSchemaIn() {
        return msgSchemaIn;
    }

    public static void setMsgSchemaIn(String msgSchemaIn) {
        Channel.msgSchemaIn = msgSchemaIn;
    }

    public static String getSupportsInbound() {
        return supportsInbound;
    }

    public static void setSupportsInbound(String supportsInbound) {
        Channel.supportsInbound = supportsInbound;
    }

    public static String getSupportsInvoke() {
        return supportsInvoke;
    }

    public static void setSupportsInvoke(String supportsInvoke) {
        Channel.supportsInvoke = supportsInvoke;
    }

    public static String getWsdlChannelOperationName() {
        return wsdlChannelOperationName;
    }

    public static void setWsdlChannelOperationName(String wsdlChannelOperationName) {
        Channel.wsdlChannelOperationName = wsdlChannelOperationName;
    }

    public static String getWsdlChannelPortType() {
        return wsdlChannelPortType;
    }

    public static void setWsdlChannelPortType(String wsdlChannelPortType) {
        Channel.wsdlChannelPortType = wsdlChannelPortType;
    }

    public static String getWsdlChannelUrl() {
        return wsdlChannelUrl;
    }

    public static void setWsdlChannelUrl(String wsdlChannelUrl) {
        Channel.wsdlChannelUrl = wsdlChannelUrl;
    }

    public static String getChannelMsgSchemaOut() {
        return channelMsgSchemaOut;
    }

    public static void setChannelMsgSchemaOut(String channelMsgSchemaOut) {
        Channel.channelMsgSchemaOut = channelMsgSchemaOut;
    }

    public void addProperty(Property property) {
        String tableName = null;
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + property;
    }

    private void getChannelByName(String chName) throws SQLException {
        String tableName = null;
        String sql = "SELECT CHANNELID,FINGERPRINTTABLENAME,SPACECOUPLING," + "MIDDLEWAREADAPTERID,ISWSDLBACKED,ISTIMEDECOUPLED,SUPPORTSINBOUND," + "SUPPORTSINVOKE,MSGSCHEMAIN,WSDLCHANNELURL,WSDLCHANNELPORTTYPE," + "WSDLCHANNELOPERATIONNAME,CHANNELMSGSCHEMAOUT FROM " + tableName + " WHERE channelName = ?";
        PreparedStatement psth = null;
        psth.setString(1, chName);
        ResultSet rs = psth.executeQuery();
        if (rs.next()) {
            this.setChannelID(rs.getInt("channelID"));
            this.setFingerprintTableName(rs.getString("FINGERPRINTTABLENAME"));
            this.setSpaceCoupling(rs.getString("SPACECOUPLING"));
            this.setMiddlewareAdapterId(rs.getString("MIDDLEWAREADAPTERID"));
            this.setIsWSDLBacked(rs.getString("ISWSDLBACKED"));
            this.setIsTimeDecoupled(rs.getString("ISTIMEDECOUPLED"));
            this.setSupportsInbound(rs.getString("SUPPORTSINBOUND"));
            this.setSupportsInvoke(rs.getString("SUPPORTSINVOKE"));
            this.setMsgSchemaIn(rs.getString("MSGSCHEMAIN"));
            this.setWsdlChannelUrl(rs.getString("WSDLCHANNELURL"));
            this.setWsdlChannelPortType(rs.getString("WSDLCHANNELPORTTYPE"));
            this.setWsdlChannelOperationName(rs.getString("WSDLCHANNELOPERATIONNAME"));
            this.setChannelMsgSchemaOut(rs.getString("CHANNELMSGSCHEMAOUT"));
            this.channelName = chName;
        }
        rs.close();
        psth.close();
    }

    private void getChannelByID(Integer chID) throws SQLException {
        String tableName = null;
        String sql = "SELECT CHANNELNAME,FINGERPRINTTABLENAME,SPACECOUPLING," + "MIDDLEWAREADAPTERID,ISWSDLBACKED,ISTIMEDECOUPLED,SUPPORTSINBOUND," + "SUPPORTSINVOKE,MSGSCHEMAIN,WSDLCHANNELURL,WSDLCHANNELPORTTYPE," + "WSDLCHANNELOPERATIONNAME,CHANNELMSGSCHEMAOUT FROM " + tableName + " WHERE channelID = ?";
        PreparedStatement psth = null;
        psth.setInt(1, chID);
        ResultSet rs = psth.executeQuery();
        if (rs.next()) {
            this.channelName = rs.getString("channelName");
            this.setFingerprintTableName(rs.getString("FINGERPRINTTABLENAME"));
            this.setSpaceCoupling(rs.getString("SPACECOUPLING"));
            this.setMiddlewareAdapterId(rs.getString("MIDDLEWAREADAPTERID"));
            this.setIsWSDLBacked(rs.getString("ISWSDLBACKED"));
            this.setIsTimeDecoupled(rs.getString("ISTIMEDECOUPLED"));
            this.setSupportsInbound(rs.getString("SUPPORTSINBOUND"));
            this.setSupportsInvoke(rs.getString("SUPPORTSINVOKE"));
            this.setMsgSchemaIn(rs.getString("MSGSCHEMAIN"));
            this.setWsdlChannelUrl(rs.getString("WSDLCHANNELURL"));
            this.setWsdlChannelPortType(rs.getString("WSDLCHANNELPORTTYPE"));
            this.setWsdlChannelOperationName(rs.getString("WSDLCHANNELOPERATIONNAME"));
            this.setChannelMsgSchemaOut(rs.getString("CHANNELMSGSCHEMAOUT"));
        }
        rs.close();
        psth.close();
    }

    public int compareTo(Channel arg0) {
        return 0;
    }
}
