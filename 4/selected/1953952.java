package COM.winserver.wildcat;

import java.io.IOException;

public class TSetLastRead_Request extends TWildcatRequest {

    public int conference;

    public int lastread;

    public static final int SIZE = TWildcatRequest.SIZE + 4 + 4;

    public TSetLastRead_Request() {
        type = WildcatRequest.wrSetLastRead;
    }

    public TSetLastRead_Request(byte[] x) {
        fromByteArray(x);
    }

    protected void writeTo(WcOutputStream out) throws IOException {
        super.writeTo(out);
        out.writeInt(conference);
        out.writeInt(lastread);
    }

    protected void readFrom(WcInputStream in) throws IOException {
        super.readFrom(in);
        conference = in.readInt();
        lastread = in.readInt();
    }
}
