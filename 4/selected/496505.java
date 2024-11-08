package ua.snuk182.asia.core.dataentity;

import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import ua.snuk182.asia.services.HistorySaver;
import ua.snuk182.asia.view.conversations.ConversationsView;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

public class Buddy implements Parcelable, Comparable<Buddy> {

    /**
	 * 
	 */
    private static final long serialVersionUID = 3234673071630264609L;

    public static final byte ST_OFFLINE = 0;

    public static final byte ST_ONLINE = 1;

    public static final byte ST_AWAY = 2;

    public static final byte ST_NA = 3;

    public static final byte ST_BUSY = 4;

    public static final byte ST_DND = 5;

    public static final byte ST_FREE4CHAT = 6;

    public static final byte ST_INVISIBLE = 7;

    public static final byte ST_HOME = 8;

    public static final byte ST_WORK = 9;

    public static final byte ST_DINNER = 10;

    public static final byte ST_DEPRESS = 11;

    public static final byte ST_ANGRY = 12;

    public static final byte ST_OTHER = 13;

    public static final byte VIS_PERMITTED = 1;

    public static final byte VIS_DENIED = 2;

    public static final byte VIS_IGNORED = 3;

    public static final byte VIS_REGULAR = 0;

    public static final byte VIS_NOT_AUTHORIZED = 4;

    public static final byte VIS_GROUPCHAT = 5;

    public int id;

    public String serviceName;

    public String name;

    public String protocolUid;

    public String ownerUid;

    public String iconHash;

    public byte serviceId;

    public byte status;

    public byte xstatus = -1;

    public String xstatusName;

    public String xstatusDescription;

    public String externalIP;

    public int onlineTime;

    public Date signonTime;

    public byte visibility;

    public byte unread = 0;

    public int groupId;

    public boolean canFileShare = false;

    public String clientId = null;

    public boolean waitsForInfo = false;

    public String authRequest = null;

    private HistorySaver historySaver;

    public static final String BUDDYICON_FILEEXT = ".ico";

    public String getName() {
        return name != null ? name : protocolUid;
    }

    @Override
    public String toString() {
        return getName() + " (" + protocolUid + ")";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private Buddy(Parcel in) {
        readFromParcel(in);
    }

    public Buddy(String protocolUid, AccountView account) {
        this(protocolUid, account.protocolUid, account.protocolName, account.serviceId);
    }

    public Buddy(String protocolUid, String ownerUid, String serviceName, byte serviceId) {
        this.protocolUid = protocolUid;
        this.ownerUid = ownerUid;
        this.serviceName = serviceName;
        this.serviceId = serviceId;
    }

    public void readFromParcel(Parcel in) {
        id = in.readInt();
        serviceName = in.readString();
        name = in.readString();
        protocolUid = in.readString();
        ownerUid = in.readString();
        serviceId = in.readByte();
        status = in.readByte();
        xstatus = in.readByte();
        xstatusName = in.readString();
        xstatusDescription = in.readString();
        externalIP = in.readString();
        onlineTime = in.readInt();
        long sig = in.readLong();
        signonTime = sig > -1 ? new Date(sig) : null;
        visibility = in.readByte();
        unread = in.readByte();
        canFileShare = in.readByte() != 0;
        groupId = in.readInt();
        iconHash = in.readString();
        clientId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(serviceName);
        dest.writeString(name);
        dest.writeString(protocolUid);
        dest.writeString(ownerUid);
        dest.writeByte(serviceId);
        dest.writeByte(status);
        dest.writeByte(xstatus);
        dest.writeString(xstatusName);
        dest.writeString(xstatusDescription);
        dest.writeString(externalIP);
        dest.writeInt(onlineTime);
        dest.writeLong(signonTime != null ? signonTime.getTime() : -1);
        dest.writeByte(visibility);
        dest.writeByte(unread);
        dest.writeByte((byte) (canFileShare ? 1 : 0));
        dest.writeInt(groupId);
        dest.writeString(iconHash);
        dest.writeString(clientId);
    }

    public HistorySaver getHistorySaver() {
        if (historySaver == null) {
            historySaver = new HistorySaver(this);
        }
        return historySaver;
    }

    public static final Parcelable.Creator<Buddy> CREATOR = new Parcelable.Creator<Buddy>() {

        @Override
        public Buddy createFromParcel(Parcel source) {
            return new Buddy(source);
        }

        @Override
        public Buddy[] newArray(int size) {
            return new Buddy[size];
        }
    };

    public List<TextMessage> getLastHistory(Context context, boolean getAll) {
        unread = 0;
        return getHistorySaver().getLastHistory(context, getAll);
    }

    public void merge(Buddy origin) {
        if (origin == null || origin == this) {
            return;
        }
        serviceName = origin.serviceName;
        name = origin.name;
        protocolUid = origin.protocolUid;
        ownerUid = origin.ownerUid;
        serviceId = origin.serviceId;
        status = origin.status;
        xstatus = origin.xstatus;
        xstatusName = origin.xstatusName;
        xstatusDescription = origin.xstatusDescription;
        externalIP = origin.externalIP;
        onlineTime = origin.onlineTime;
        signonTime = origin.signonTime;
        visibility = origin.visibility;
        unread = origin.unread;
        canFileShare = origin.canFileShare;
        groupId = origin.groupId;
        iconHash = origin.iconHash;
    }

    public String getOwnerAccountId() {
        return ownerUid + " " + serviceName;
    }

    public Buddy() {
    }

    @Override
    public int compareTo(Buddy another) {
        if (status != another.status) {
            if (status == Buddy.ST_OFFLINE) {
                return -1;
            }
            if (another.status == Buddy.ST_OFFLINE) {
                return 1;
            }
        }
        return name.compareToIgnoreCase(another.name);
    }

    public String getFilename() {
        return getOwnerAccountId() + " " + protocolUid;
    }

    public static synchronized Bitmap getIcon(Context context, String filename) {
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(filename + BUDDYICON_FILEEXT);
        } catch (Exception e) {
        }
        if (fis == null) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        options.inScaled = false;
        options.inPurgeable = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(fis, null, options);
    }

    public String getChatTag() {
        return ConversationsView.class.getSimpleName() + " " + serviceId + " " + protocolUid;
    }
}
