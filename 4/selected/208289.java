package COM.winserver.wildcat;

import java.io.IOException;

public class TWildcatSMTP extends WcRecord {

    public int dwVersion;

    public int dwRevision;

    public int port;

    public int sendthreads;

    public int acceptthreads;

    public int VerboseLogging;

    public boolean acceptonly;

    public int retries;

    public int retrywait;

    public String smarthost;

    public int sizelimit;

    public boolean localonly;

    public boolean MAPSRBL;

    public String MAPSRBLServer;

    public boolean ESMTP;

    public boolean reqauth;

    public boolean VRFY;

    public boolean AllowRelay;

    public boolean CheckRCPT;

    public boolean EnableBadFilter;

    public boolean RequireMX;

    public boolean RequireHostMatch;

    public static final int SIZE = 0 + 2 + 2 + 4 + 2 + 2 + 4 + 4 + 4 + 4 + 52 + 4 + 4 + 4 + 52 + 4 + 4 + 4 + 4 + 4 + 4 + 4 + 4;

    public TWildcatSMTP() {
    }

    public TWildcatSMTP(byte[] x) {
        fromByteArray(x);
    }

    protected void writeTo(WcOutputStream out) throws IOException {
        super.writeTo(out);
        out.writeShort(dwVersion);
        out.writeShort(dwRevision);
        out.writeInt(port);
        out.writeShort(sendthreads);
        out.writeShort(acceptthreads);
        out.writeInt(VerboseLogging);
        out.writeBoolean(acceptonly);
        out.writeInt(retries);
        out.writeInt(retrywait);
        out.writeString(smarthost, 52);
        out.writeInt(sizelimit);
        out.writeBoolean(localonly);
        out.writeBoolean(MAPSRBL);
        out.writeString(MAPSRBLServer, 52);
        out.writeBoolean(ESMTP);
        out.writeBoolean(reqauth);
        out.writeBoolean(VRFY);
        out.writeBoolean(AllowRelay);
        out.writeBoolean(CheckRCPT);
        out.writeBoolean(EnableBadFilter);
        out.writeBoolean(RequireMX);
        out.writeBoolean(RequireHostMatch);
    }

    protected void readFrom(WcInputStream in) throws IOException {
        super.readFrom(in);
        dwVersion = in.readUnsignedShort();
        dwRevision = in.readUnsignedShort();
        port = in.readInt();
        sendthreads = in.readUnsignedShort();
        acceptthreads = in.readUnsignedShort();
        VerboseLogging = in.readInt();
        acceptonly = in.readBoolean();
        retries = in.readInt();
        retrywait = in.readInt();
        smarthost = in.readString(52);
        sizelimit = in.readInt();
        localonly = in.readBoolean();
        MAPSRBL = in.readBoolean();
        MAPSRBLServer = in.readString(52);
        ESMTP = in.readBoolean();
        reqauth = in.readBoolean();
        VRFY = in.readBoolean();
        AllowRelay = in.readBoolean();
        CheckRCPT = in.readBoolean();
        EnableBadFilter = in.readBoolean();
        RequireMX = in.readBoolean();
        RequireHostMatch = in.readBoolean();
    }
}
