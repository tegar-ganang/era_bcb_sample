package com.ibm.tuningfork.infra.feed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import com.ibm.tuningfork.infra.Logging;
import com.ibm.tuningfork.infra.event.TypedEvent;

/**
 * A logical position in the feed and the corresponding points in the feedlets.
 */
public final class FeedPosition {

    private int feedEventIndex;

    private boolean ready;

    private static final class InternalFeedletPosition {

        static final long NOT_READY_TIME = -1;

        static final long NO_MORE_TIME = -2;

        private int feedletIndex;

        private int feedletEventIndex;

        private TypedEvent event;

        private long currentTime;

        private final Feedlet feedlet;

        public void write(ObjectOutputStream out) throws IOException {
            out.writeInt(feedletIndex);
            out.writeInt(feedletEventIndex);
        }

        public InternalFeedletPosition(ObjectInputStream in, FeedSeekIndex feedIndex) throws IOException, ClassNotFoundException {
            feedletIndex = in.readInt();
            feedletEventIndex = in.readInt();
            feedlet = feedIndex.getFeed().getFeedletByIndex(feedletIndex);
            currentTime = InternalFeedletPosition.NOT_READY_TIME;
            event = null;
        }

        InternalFeedletPosition(InternalFeedletPosition src) {
            this.feedletIndex = src.feedletIndex;
            feedletEventIndex = src.feedletEventIndex;
            feedlet = src.feedlet;
            currentTime = InternalFeedletPosition.NOT_READY_TIME;
            event = null;
        }

        InternalFeedletPosition(Feedlet feedlet, int feedletIndex, int eventIndex) {
            this.feedletIndex = feedletIndex;
            feedletEventIndex = eventIndex;
            this.feedlet = feedlet;
            currentTime = InternalFeedletPosition.NOT_READY_TIME;
            event = null;
        }

        final void advance() {
            feedletEventIndex++;
        }

        final boolean refresh() {
            event = feedlet.getEvent(feedletEventIndex);
            if (event == null) {
                if (feedlet.isClosed() && feedletEventIndex >= feedlet.getPhysicalLength()) {
                    currentTime = InternalFeedletPosition.NO_MORE_TIME;
                } else {
                    currentTime = InternalFeedletPosition.NOT_READY_TIME;
                    return false;
                }
            } else {
                currentTime = event.getTime();
            }
            return true;
        }
    }

    InternalFeedletPosition[] feedletPositions;

    private transient FeedSeekIndex feedIndex;

    public void write(ObjectOutputStream out) throws IOException {
        out.writeInt(feedEventIndex);
        out.writeBoolean(ready);
        out.writeInt(feedletPositions.length);
        for (int i = 0; i < feedletPositions.length; i++) {
            feedletPositions[i].write(out);
        }
    }

    public FeedPosition(FeedSeekIndex feedIndex, ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.feedIndex = feedIndex;
        feedEventIndex = in.readInt();
        ready = in.readBoolean();
        int len = in.readInt();
        feedletPositions = new InternalFeedletPosition[len];
        for (int i = 0; i < len; i++) {
            feedletPositions[i] = new InternalFeedletPosition(in, feedIndex);
        }
    }

    public FeedPosition(FeedPosition src) {
        this.feedIndex = src.feedIndex;
        this.feedEventIndex = src.feedEventIndex;
        this.feedletPositions = new InternalFeedletPosition[src.feedletPositions.length];
        for (int i = 0; i < feedletPositions.length; i++) {
            feedletPositions[i] = new InternalFeedletPosition(src.feedletPositions[i]);
        }
        this.ready = false;
    }

    public String toString() {
        String result = "Feed Position " + feedEventIndex + (ready ? " is ready: " : " is not ready: ");
        for (int i = 0; i < feedletPositions.length; i++) {
            result += "  (" + i + ") " + feedletPositions[i].feedletEventIndex + "@" + feedletPositions[i].currentTime;
        }
        return result;
    }

    public final int getEventIndex() {
        return feedEventIndex;
    }

    public boolean expand(Feedlet[] allFeedlets) {
        if (allFeedlets.length <= feedletPositions.length) {
            return false;
        }
        for (int i = 0; i < allFeedlets.length; i++) {
            if (allFeedlets[i] == null) {
                throw new NullPointerException();
            }
        }
        InternalFeedletPosition[] newFeedletPositions = new InternalFeedletPosition[allFeedlets.length];
        System.arraycopy(feedletPositions, 0, newFeedletPositions, 0, feedletPositions.length);
        for (int i = feedletPositions.length; i < allFeedlets.length; i++) {
            newFeedletPositions[i] = new InternalFeedletPosition(allFeedlets[i], i, 0);
        }
        feedletPositions = newFeedletPositions;
        ready = false;
        return true;
    }

    public FeedPosition(FeedSeekIndex feedIndex) {
        this.feedIndex = feedIndex;
        this.feedEventIndex = 0;
        feedletPositions = new InternalFeedletPosition[0];
        ready = false;
    }

    private void refreshTimes() {
        if (feedletPositions.length == 0) {
            return;
        }
        for (int i = 0; i < feedletPositions.length; i++) {
            if (!feedletPositions[i].refresh()) {
                ready = false;
                return;
            }
        }
        ready = true;
    }

    private InternalFeedletPosition getEarliestInternalFeedletPosition() {
        InternalFeedletPosition result = null;
        for (int i = 0; i < feedletPositions.length; i++) {
            InternalFeedletPosition cur = feedletPositions[i];
            long t = cur.currentTime;
            if (t > 0) {
                if (result == null || t < result.currentTime) {
                    result = cur;
                }
            }
        }
        return result;
    }

    public TypedEvent generateEvent() {
        if (feedletPositions.length == 0) {
            return null;
        }
        if (!ready) {
            refreshTimes();
            if (!ready) {
                return null;
            }
        }
        InternalFeedletPosition ifp = getEarliestInternalFeedletPosition();
        if (ifp == null) {
            return null;
        }
        TypedEvent e = ifp.event;
        ifp.advance();
        if (e == null) {
            Logging.errorln("FeedletPosition.generateEvent got a null event feedlet " + ifp.feedlet + " eventIndex = " + (ifp.feedletEventIndex - 1) + " time = " + ifp.currentTime);
        }
        feedEventIndex++;
        if (!ifp.refresh()) {
            ready = false;
        }
        return e;
    }
}
