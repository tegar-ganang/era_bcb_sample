package net.sf.jvdr.data.comparator;

import java.util.Comparator;
import org.hampelratte.svdrp.responses.highlevel.Channel;

public class ChannelComparator {

    public enum CompTyp {

        Number
    }

    ;

    public ChannelComparator() {
    }

    public Comparator<Channel> getComparator() {
        return getComparator(CompTyp.Number);
    }

    public Comparator<Channel> getComparator(CompTyp ct) {
        Comparator<Channel> c = null;
        switch(ct) {
            default:
                c = new ChannelComparatorNumber();
                break;
        }
        return c;
    }

    public class ChannelComparatorNumber implements Comparator<Channel> {

        public int compare(Channel a, Channel b) {
            long diff = a.getChannelNumber() - b.getChannelNumber();
            if (diff < 0) {
                return -1;
            } else if (diff == 0) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}
