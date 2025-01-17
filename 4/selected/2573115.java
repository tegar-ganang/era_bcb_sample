package COM.winserver.wildcat;

import java.io.IOException;

public class TFileRecord5 extends WcRecord {

    public int Status;

    public int Area;

    public String Name;

    public String Description;

    public String Password;

    public int Size;

    public long FileTime;

    public long LastAccessed;

    public boolean NeverOverwrite;

    public boolean NeverDelete;

    public boolean FreeFile;

    public boolean CopyBeforeDownload;

    public boolean Offline;

    public boolean FailedScan;

    public boolean FreeTime;

    public int Downloads;

    public int Cost;

    public TUserInfo Uploader;

    public int UserInfo;

    public boolean HasLongDescription;

    public static final int SIZE = 0 + 4 + 4 + 16 + 76 + 32 + 4 + 4 + 8 + 8 + 4 + 4 + 4 + 4 + 4 + 4 + 4 + 4 + 4 + TUserInfo.SIZE + 4 + 4 + 44 * 1;

    public TFileRecord5() {
    }

    public TFileRecord5(byte[] x) {
        fromByteArray(x);
    }

    protected void writeTo(WcOutputStream out) throws IOException {
        super.writeTo(out);
        out.writeInt(Status);
        out.writeInt(Area);
        out.writeString(Name, 16);
        out.writeString(Description, 76);
        out.writeString(Password, 32);
        out.write(new byte[4]);
        out.writeInt(Size);
        out.writeLong(FileTime);
        out.writeLong(LastAccessed);
        out.writeBoolean(NeverOverwrite);
        out.writeBoolean(NeverDelete);
        out.writeBoolean(FreeFile);
        out.writeBoolean(CopyBeforeDownload);
        out.writeBoolean(Offline);
        out.writeBoolean(FailedScan);
        out.writeBoolean(FreeTime);
        out.writeInt(Downloads);
        out.writeInt(Cost);
        Uploader.writeTo(out);
        out.writeInt(UserInfo);
        out.writeBoolean(HasLongDescription);
        out.write(new byte[44 * 1]);
    }

    protected void readFrom(WcInputStream in) throws IOException {
        super.readFrom(in);
        Status = in.readInt();
        Area = in.readInt();
        Name = in.readString(16);
        Description = in.readString(76);
        Password = in.readString(32);
        in.skip(4);
        Size = in.readInt();
        FileTime = in.readLong();
        LastAccessed = in.readLong();
        NeverOverwrite = in.readBoolean();
        NeverDelete = in.readBoolean();
        FreeFile = in.readBoolean();
        CopyBeforeDownload = in.readBoolean();
        Offline = in.readBoolean();
        FailedScan = in.readBoolean();
        FreeTime = in.readBoolean();
        Downloads = in.readInt();
        Cost = in.readInt();
        Uploader = new TUserInfo();
        Uploader.readFrom(in);
        UserInfo = in.readInt();
        HasLongDescription = in.readBoolean();
        in.skip(44 * 1);
    }
}
