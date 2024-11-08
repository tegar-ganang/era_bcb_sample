package org.charvolant.tmsnet.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * An EPG for a specific channel.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ChannelEPG extends ContextHolder<PVRState> {

    /** The channel index */
    private int channel;

    /** The events for that channel */
    private List<Event> events;

    /**
   * Construct a ChannelEPG.
   *
   */
    public ChannelEPG() {
        this(null, 0);
    }

    /**
   * Construct a ChannelEPG.
   *
   * @param context The context
   * @param channel The channel for this EPG
   */
    public ChannelEPG(PVRState context, int channel) {
        super(context);
        this.channel = channel;
        this.events = new ArrayList<Event>(128);
    }

    /**
   * Get the channel index.
   *
   * @return the channel index
   */
    public int getChannel() {
        return this.channel;
    }

    /**
   * Get the events.
   *
   * @return the events
   */
    public List<Event> getEvents() {
        return this.events;
    }

    /**
   * Insert a new list of events into an existing list of events.
   * <p>
   * The segment in the existing list of events that falls between
   * the new list of events is cut out  and replaced.
   * 
   * @param channel The channel to update
   * @param update The new list of events
   */
    public void updateEvents(List<Event> update) {
        List<Event> updated;
        Date newStart, newFinish;
        int pos;
        for (Event event : update) event.setContext(this);
        if (this.events == null || this.events.isEmpty()) {
            this.events = update;
            return;
        }
        newStart = update.get(0).getStart();
        newFinish = update.get(update.size() - 1).getFinish();
        if (!newStart.before(this.events.get(this.events.size() - 1).getFinish())) {
            this.events.addAll(update);
            return;
        }
        if (!newFinish.after(this.events.get(0).getStart())) {
            update.addAll(this.events);
            this.events = update;
            return;
        }
        updated = new ArrayList<Event>(Math.max(this.events.size(), update.size()) + 16);
        for (pos = 0; pos < this.events.size(); pos++) {
            Event evt = this.events.get(pos);
            if (evt.getFinish().after(newStart)) break; else updated.add(evt);
        }
        updated.addAll(update);
        for (; pos < this.events.size() && this.events.get(pos).getStart().before(newFinish); pos++) ;
        for (; pos < this.events.size(); pos++) updated.add(this.events.get(pos));
        this.events = updated;
    }

    /**
   * Clean the EPG of any events that end before a given date.
   * 
   * @param expiry The expiry date
   */
    public void cleanEPG(Date expiry) {
        List<Event> updated;
        if (this.events.isEmpty()) return;
        if (this.events.get(0).getFinish().after(expiry)) return;
        updated = new ArrayList<Event>(this.events.size());
        for (Event event : this.events) if (event.getFinish().after(expiry)) updated.add(event);
        this.events = updated;
    }

    /**
   * Choose the closest event to another entity.
   * <p>
   * The closest event is chosen by seeing what event most overlaps the given time-span.
   * 
   * @param coverable The object to compare against
   * 
   * @return The matching event or null for not found
   */
    public Event findEvent(Coverable coverable) {
        long overlap = 0, ol;
        Event best = null;
        for (Event event : this.events) {
            ol = event.getOverlap(coverable);
            if (ol > overlap) {
                overlap = ol;
                best = event;
            }
        }
        return best;
    }

    /**
   * Choose the event that corresponds to a specific time.
   * 
   * @param time The time
   * 
   * @return The matching event or null for not found
   */
    public Event findEvent(Date time) {
        for (Event event : this.events) {
            if (!time.before(event.getStart()) && !time.after(event.getFinish())) return event;
        }
        return null;
    }

    /**
   * Find the list of events that occur within the given time range.
   * <p>
   * Events that straddle the range are included.
   * 
   * @param start The start date
   * @param finish The end date
   * 
   * @return The event list (empty for none)
   */
    public List<Event> findEvents(Date start, Date finish) {
        List<Event> events = new ArrayList<Event>(128);
        for (Event event : this.events) if ((!event.getStart().before(start) && event.getStart().before(finish)) || (event.getFinish().after(start) && !event.getFinish().after(finish)) || (event.getStart().before(start) && event.getFinish().after(finish))) events.add(event);
        return events;
    }
}
