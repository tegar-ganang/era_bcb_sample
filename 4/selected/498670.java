package ua.snuk182.asia.core.dataentity;

import android.os.Parcel;
import android.os.Parcelable;

public class TextMessage extends Message {

    public String writerUid = null;

    public String to;

    public TextMessage(Parcel arg0) {
        super(arg0);
        readFromParcel(arg0);
    }

    public TextMessage(String from) {
        this(from, from);
    }

    public TextMessage(String writerUid, String from) {
        super(from);
        this.writerUid = writerUid;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(to);
        dest.writeString(writerUid);
    }

    @Override
    public void readFromParcel(Parcel in) {
        super.readFromParcel(in);
        to = in.readString();
        writerUid = in.readString();
    }

    public static final Parcelable.Creator<TextMessage> CREATOR = new Parcelable.Creator<TextMessage>() {

        @Override
        public TextMessage createFromParcel(Parcel arg0) {
            return new TextMessage(arg0);
        }

        @Override
        public TextMessage[] newArray(int size) {
            return new TextMessage[size];
        }
    };
}
