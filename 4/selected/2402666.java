package ictk.boardgame.chess.net.ics;

import ictk.boardgame.chess.net.ics.event.*;
import java.util.HashMap;

/** Routes ICSEvent messages to ICSEventListeners. It is possible to
 *  use one router for many live connections to different servers. At least
 *  that's an intended goal.
 */
public class ICSEventRouter {

    /** key offset for integers so they can be put in the hash */
    protected static int OFFSET = 1000;

    /** the default listener receives all events or those not sent to a 
       ** exclusive listener (depending on set options). */
    ICSEventListener defaultListener;

    /** subscribers to ICSEvents */
    protected ICSEventListener[][] subscribers;

    /** subscribers to individual channels.
       ** the key is an int[2] with [0] set to the ChannelType (shout, TCh etc)
       ** and the [1] set the the channel number,
       ** the value is a ICSEventListener[] */
    protected HashMap chSubscribers, chExclusive;

    /** a list of which events are exclusively listed to by the 
       ** subscriber (event if null) instead of also being sent 
       ** to the defaultListener */
    protected boolean[] exclusive;

    public ICSEventRouter() {
        subscribers = new ICSEventListener[ICSEvent.NUM_EVENTS][];
        chSubscribers = new HashMap();
        chExclusive = new HashMap();
        exclusive = new boolean[ICSEvent.NUM_EVENTS];
    }

    /** This listener receives all message that are not exclusively
    *  processed by some other listener.  Uncategorized output is also
    *  sent to the default listener.
    */
    public void setDefaultListener(ICSEventListener eh) {
        defaultListener = eh;
    }

    /** This listener receives all message that are not exclusively
    *  processed by some other listener.  Uncategorized output is also
    *  sent to the default listener.
    */
    public ICSEventListener getDefaultListener() {
        return defaultListener;
    }

    /** an event sent to this method will be relayed to all the listeners
    *  who are interested in it.
    */
    public void dispatch(ICSEvent evt) {
        Integer key = null;
        int type = evt.getEventType(), i = 0;
        ICSEventListener[] listeners = null;
        boolean done = false, done2 = false;
        switch(type) {
            case ICSEvent.CHANNEL_EVENT:
            case ICSEvent.TOURNAMENT_CHANNEL_EVENT:
            case ICSEvent.SHOUT_EVENT:
                key = new Integer(type * OFFSET + ((ICSChannelEvent) evt).getChannel());
                listeners = (ICSEventListener[]) chSubscribers.get(key);
                done = (listeners != null) && isChannelExclusive(type, ((ICSChannelEvent) evt).getChannel());
                break;
            case ICSEvent.BOARD_UPDATE_EVENT:
            case ICSEvent.KIBITZ_EVENT:
            case ICSEvent.WHISPER_EVENT:
            case ICSEvent.BOARD_SAY_EVENT:
            default:
                break;
        }
        if (listeners != null) for (i = 0; i < listeners.length; i++) listeners[i].icsEventDispatched(evt);
        if (!done && (listeners = subscribers[type]) != null) {
            for (i = 0; i < listeners.length; i++) listeners[i].icsEventDispatched(evt);
            done2 = true;
        }
        if (!exclusive[type] && defaultListener != null) defaultListener.icsEventDispatched(evt);
    }

    /** tells the router that this listener would like to hear a particular
    *  type of event.
    *
    *  @param icsEventNumber an ICSEvent.<FOO>_EVENT
    */
    public void addEventListener(int icsEventNumber, ICSEventListener eh) {
        subscribers[icsEventNumber] = _addListener(subscribers[icsEventNumber], eh);
    }

    /** should the event be routed only to listeners subscribed to this
    *  event, or also to the default listener.  
    *
    *  @param t if true then the default listener will not receive the 
    *           event even if there are no listeners for this event.
    */
    public void setExclusive(int eventType, boolean t) {
        exclusive[eventType] = t;
    }

    public boolean isExclusive(int eventType) {
        return exclusive[eventType];
    }

    /** adding this type of listener will subscribe the listener to
    *  the following types of events for this board number:<br>
    *  type 1: board updates, moves forward and back, resignations<br>
    *  type 2: takeback requests, draw offers, adjourn and pause requests<br>
    *  type 3: kibitzes, whispers, and board says<br>
    *  NOTE: all types must be registered independently
    */
    public void addBoardListener(ICSEventListener eh, int boardNumber, int type) {
    }

    /** adds a listener to a particular channel.  This is useful if you want
    *  to log particular channels, or send them to different display 
    *  areas.  If a listener wish to listen to all channel events then
    *  it would be better to subscribe via addEventListener().
    *
    *  @param channelType is the EventType for this sort of channel.  For
    *                     example: ICSEvent.CHANNEL_EVENT is for normal
    *                     channel tells, ICSEvent.SHOUT_EVENT is for
    *                     shouts.
    *  @param channelNumber is number of the channel, or in the case of 
    *                     shouts is the type of shout.
    */
    public void addChannelListener(int channelType, int channelNumber, ICSEventListener eh) {
        Integer key = new Integer(channelType * OFFSET + channelNumber);
        ICSEventListener[] list;
        list = (ICSEventListener[]) chSubscribers.get(key);
        list = _addListener(list, eh);
        chSubscribers.put(key, list);
    }

    /** removes a listener to a particular channel.
    *
    *  @param channelType is the EventType for this sort of channel.  For
    *                     example: ICSEvent.CHANNEL_EVENT is for normal
    *                     channel tells, ICSEvent.SHOUT_EVENT is for
    *                     shouts.
    *  @param channelNumber is number of the channel, or in the case of 
    *                     shouts is the type of shout.
    */
    public void removeChannelListener(int channelType, int channelNumber, ICSEventListener eh) {
        Integer key = new Integer(channelType * OFFSET + channelNumber);
        ICSEventListener[] list;
        list = (ICSEventListener[]) chSubscribers.get(key);
        list = _removeListener(list, eh);
        if (list == null) chSubscribers.remove(key); else chSubscribers.put(key, list);
    }

    /** are channel events for this channel only routed to this channel's
    *  listener(s), or are they also send to the CHANNEL_EVENT listener.
    *  This setting has no bearing on whether the defaultListener
    *  receives the event or not.
    */
    public void setChannelExclusive(int channelType, int channelNumber, boolean t) {
        Integer key = new Integer(channelType * OFFSET + channelNumber);
        chExclusive.put(key, ((t) ? Boolean.TRUE : Boolean.FALSE));
    }

    /** are channel events for this channel only routed to this channel's
    *  listener(s), or are they also send to the CHANNEL_EVENT listener.
    *  This setting has no bearing on whether the defaultListener
    *  receives the event or not.
    */
    public boolean isChannelExclusive(int channelType, int channelNumber) {
        Integer key = new Integer(channelType * OFFSET + channelNumber);
        Boolean b = null;
        return ((b = (Boolean) chExclusive.get(key)) != null) && b == Boolean.TRUE;
    }

    protected ICSEventListener[] _addListener(ICSEventListener[] list, ICSEventListener evt) {
        ICSEventListener[] tmp = null;
        if (list == null) {
            tmp = new ICSEventListener[1];
            tmp[0] = evt;
        } else {
            tmp = new ICSEventListener[list.length + 1];
            System.arraycopy(list, 0, tmp, 0, list.length);
            tmp[list.length] = evt;
        }
        return tmp;
    }

    protected ICSEventListener[] _removeListener(ICSEventListener[] list, ICSEventListener evt) {
        ICSEventListener[] tmp = null;
        if (list != null && list.length > 1) {
            tmp = new ICSEventListener[list.length - 1];
            int count = 0;
            for (int i = 0; i < list.length; i++) if (list[i] != evt) tmp[count++] = list[i];
        }
        return tmp;
    }
}
