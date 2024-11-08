package com.neoworks.jukex.sqlimpl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import com.neoworks.jukex.DatabaseObject;
import com.neoworks.jukex.Track;
import com.neoworks.jukex.TrackStore;
import com.neoworks.jukex.TrackStoreFactory;
import com.neoworks.jukex.Playlist;
import com.neoworks.jukex.tracksource.TrackSourcePipelineElementSkeleton;
import com.neoworks.jukex.tracksource.TrackSourcePipeline;
import com.neoworks.util.StringDecorator;
import org.apache.log4j.Category;
import com.neoworks.connectionpool.PoolManager;
import java.sql.*;

/**
 * Implementation of a Playlist
 *
 * @author Nigel Atkinson (<a href="mailto:nigel@neoworks.com">nigel@neoworks.com</a>)
 * @author Nick Vincent (<a href="mailto:nick@neoworks.com">nick@neoworks.com</a>)
 */
public class JukeXPlaylist extends TrackSourcePipelineElementSkeleton implements Playlist {

    private static final Category log = Category.getInstance(JukeXPlaylist.class.getName());

    private static final boolean logDebugEnabled = log.isDebugEnabled();

    private static final boolean logInfoEnabled = log.isInfoEnabled();

    private long id = -1;

    private LinkedList ll = null;

    /**
	 * Default constructor
	 */
    public JukeXPlaylist() {
        super();
        ll = new LinkedList();
    }

    /**
	 * Creates a new instance of JukeXPlaylist
	 *
	 * @param name The Playlist name
	 * @param id The database id of the Playlist
	 */
    public JukeXPlaylist(String name, long id) {
        super(name);
        ll = new LinkedList();
        this.id = id;
        readTrackListing();
    }

    /**
	 * Public constructor
	 *
	 * @param tsp The Pipeline whose bitch this Element is
	 */
    public JukeXPlaylist(TrackSourcePipeline tsp) {
        super(tsp);
        ll = new LinkedList();
    }

    /**
	 * Private constructor
	 *
	 * @param name The name of the Playlist
	 * @param list
	 * @param id The database id of the Playlist
	 */
    private JukeXPlaylist(String name, List list, long id) {
        super(name);
        ll = new LinkedList(list);
    }

    /**
	 * Get the database id for this playlist
	 *
	 * @return The id
	 */
    protected long getId() {
        return this.id;
    }

    /**
	 * Get the next track from the head of the list
	 *
	 * @return the next Track or null
	 */
    public Track getNextTrack() {
        Track retVal = null;
        if (this.size() > 0) {
            retVal = getTrack(0);
        } else {
            if (logDebugEnabled) log.debug("I'm spent, delegating...");
            retVal = delegateGetNextTrack();
        }
        return retVal;
    }

    /**
	 * Get a track by index
	 *
	 * @param index The track index to get
	 * @return The Track at the specified index
	 */
    public Track getTrack(int index) {
        Track retVal = (Track) ll.remove(index);
        persist();
        return retVal;
    }

    /**
	 * Peek at the upcoming tracks
	 *
	 * @param count The number of tracks to peek ahead at
	 * @return A List of Track objects
	 */
    public List peekTracks(int count) {
        int rem = count - ll.size();
        if (logDebugEnabled) log.debug("Peeking ahead for " + count + "tracks, remainder " + rem);
        List retVal = new ArrayList();
        if (ll.size() > 0) {
            for (int i = 0; i < Math.min(ll.size(), count); i++) {
                retVal.add(ll.get(i));
            }
        }
        if (rem > 0) {
            retVal.addAll(super.delegatePeekTracks(rem));
        }
        return retVal;
    }

    /**
	 * Read the track listing from the database
	 */
    private synchronized boolean readTrackListing() {
        Connection conn = null;
        boolean retVal = false;
        try {
            PoolManager pm = PoolManager.getInstance();
            TrackStore trackStore = TrackStoreFactory.getTrackStore();
            conn = pm.getConnection(JukeXTrackStore.DB_NAME);
            PreparedStatement ps = conn.prepareStatement("SELECT trackid,position FROM PlaylistEntry WHERE playlistid=? ORDER BY position");
            ps.setLong(1, this.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.add(trackStore.getTrack(rs.getLong(1)));
            }
            ps.close();
            retVal = true;
        } catch (SQLException se) {
            log.error("Failed due to an exception reading a Track listing into a playlist", se);
        } catch (Exception e) {
            log.warn("Encountered exception while reading track listing: ", e);
        } finally {
            try {
                conn.close();
            } catch (SQLException ignore) {
            }
        }
        return retVal;
    }

    /**
	 * Synchronise the database with the List using the power of almighty bodge.
	 */
    private synchronized void persist() {
        Connection conn = null;
        try {
            PoolManager pm = PoolManager.getInstance();
            conn = pm.getConnection(JukeXTrackStore.DB_NAME);
            conn.setAutoCommit(false);
            Statement state = conn.createStatement();
            state.executeUpdate("DELETE FROM PlaylistEntry WHERE playlistid=" + this.id);
            if (this.size() > 0) {
                StringBuffer sql = new StringBuffer();
                sql.append("INSERT INTO PlaylistEntry ( playlistid , trackid , position ) VALUES ");
                int location = 0;
                Iterator i = ll.iterator();
                while (i.hasNext()) {
                    long currTrackID = ((DatabaseObject) i.next()).getId();
                    sql.append('(').append(this.id).append(',').append(currTrackID).append(',').append(location++).append(')');
                    if (i.hasNext()) sql.append(',');
                }
                state.executeUpdate(sql.toString());
            }
            conn.commit();
            conn.setAutoCommit(true);
            state.close();
        } catch (SQLException se) {
            try {
                conn.rollback();
            } catch (SQLException ignore) {
            }
            log.error("Encountered an error persisting a playlist", se);
        } finally {
            try {
                conn.close();
            } catch (SQLException ignore) {
            }
        }
    }

    /** 
	 * Set the state of this PipelineElement from a Map of Objects
	 *
	 * @param state Map representing the state of this Playlist
	 */
    public boolean setState(Map state) {
        this.name = (String) state.get("name");
        this.id = ((Long) state.get("id")).longValue();
        return readTrackListing();
    }

    /**
	 * Get the state of this PipelineElement as a Map of Objects
	 */
    public Map getState() {
        Map retVal = new HashMap();
        retVal.put("name", name);
        retVal.put("id", new Long(id));
        return retVal;
    }

    /**
	 * Add a Track to the Playlist at a specific position
	 *
	 * @param index The position to insert at
	 * @param element The Track to add
	 * @exception ClassCastException If the object is not a Track
	 */
    public void add(int index, Object element) {
        if (element instanceof Track) {
            ll.add(index, element);
            persist();
        } else {
            throw new ClassCastException("Cannot add a non Track object to a Playlist");
        }
    }

    /**
	 * Add a Track to the Playlist
	 *
	 * @param element The Track to add
	 * @return success
	 * @exception ClassCastException If the object is not a Track
	 */
    public boolean add(Object element) {
        if (element instanceof Track) {
            boolean retval = ll.add(element);
            persist();
            return retval;
        } else {
            throw new ClassCastException("Cannot add a non Track object to Playlist");
        }
    }

    /**
	 * Add a Collection of Tracks to the Playlist
	 *
	 * @param c The Collection of Track objects
	 * @return success
	 * @exception ClassCastException If any of the objects are not Tracks
	 */
    public boolean addAll(Collection c) {
        if (isOnlyTracks(c)) {
            boolean retval = ll.addAll(c);
            persist();
            return retval;
        } else {
            throw new ClassCastException("Cannot add a non Track object to Playlist");
        }
    }

    /**
	 * Add a Collection of Tracks to the Playlist at a specific position
	 *
	 * @param index The position to insert at
	 * @param c The Collection of Track objects
	 * @return success
	 * @exception ClassCastException If any of the objects are not Tracks
	 */
    public boolean addAll(int index, Collection c) {
        if (isOnlyTracks(c)) {
            boolean retval = ll.addAll(index, c);
            persist();
            return retval;
        } else {
            throw new ClassCastException("Cannot add a non Track object to Playlist");
        }
    }

    /**
	 * Verify that a Collection contains only Track objects
	 *
	 * @param c The Collection to check
	 * @return success
	 */
    private boolean isOnlyTracks(Collection c) {
        Iterator i = c.iterator();
        while (i.hasNext()) {
            Object obj = i.next();
            if (!(obj instanceof Track)) return false;
        }
        return true;
    }

    /**
	 * Clear the list
	 */
    public void clear() {
        ll.clear();
        persist();
    }

    /**
	 * Get the size of the list
	 *
	 * @return the size
	 */
    public int size() {
        return ll.size();
    }

    /**
	 * Check whether the List contains any elements
	 *
	 * @return result
	 */
    public boolean isEmpty() {
        return ll.isEmpty();
    }

    /**
	 * Check whether the List contains all of the specific objects in a Collection
	 *
	 * @param c The Collection of objects to check for
	 * @return result
	 */
    public boolean containsAll(Collection c) {
        return ll.containsAll(c);
    }

    /**
	 * Check whether the Playlist contains a specific object
	 *
	 * @param element The Object to check for
	 * @return result
	 */
    public boolean contains(Object element) {
        return ll.contains(element);
    }

    /**
	 * Equality operator
	 *
	 * @param o The object to compare
	 * @return result
	 */
    public boolean equals(Object o) {
        return ((o instanceof JukeXPlaylist) && this.getId() == ((JukeXPlaylist) o).getId());
    }

    /**
	 * Remove the Object at index from the List
	 *
	 * @param index The index of the comdemned object
	 * @return the removed Object
	 */
    public Object remove(int index) {
        Object retval = ll.remove(index);
        persist();
        return retval;
    }

    /**
	 * Remove an Object from the List
	 *
	 * @param o The Object to remove
	 * @return success
	 */
    public boolean remove(Object o) {
        boolean retval = ll.remove(o);
        persist();
        return retval;
    }

    /** 
	 * Remove all of the Objects in a Collection from the List
	 *
	 * @param c The Collection of condemned Objects
	 * @return success
	 */
    public boolean removeAll(Collection c) {
        boolean retval = ll.removeAll(c);
        persist();
        return retval;
    }

    /**
	 * Retains only the elements in the List that are contained in a Collection
	 *
	 * @param c The Collection of objects to retain
	 * @return success
	 */
    public boolean retainAll(Collection c) {
        boolean retval = ll.retainAll(c);
        persist();
        return retval;
    }

    /**
	 * Get a listIterator on the List at a particular position
	 *
	 * @param index The position of the Iterator in the List
	 * @return A listIterator at the position specified
	 */
    public ListIterator listIterator(int index) {
        return ll.listIterator(index);
    }

    /**
	 * Get a listIterator on the List
	 *
	 * @return A listIterator
	 */
    public ListIterator listIterator() {
        return ll.listIterator();
    }

    /**
	 * Get an Iterator on the List
	 *
	 * @return An Iterator
	 */
    public Iterator iterator() {
        return ll.iterator();
    }

    /**
	 * Return the index of the last occurence of an object in the List or -1 if it does not appear in the List
	 *
	 * @param o The object to get the index of
	 * @return The index of the last occurence of an Object or -1 if it does not appear in the List
	 */
    public int lastIndexOf(Object o) {
        return ll.lastIndexOf(o);
    }

    /**
	 * Return the index of an object in the List or -1 if it does not appear in the List
	 *
	 * @param o The object to get the index of
	 * @return The index of the Object or -1 if it does not appear in the List
	 */
    public int indexOf(Object o) {
        return ll.indexOf(o);
    }

    /**
	 * Returns an array containing all of the elements in the List in proper sequence;
	 * the runtime type of the returned array is that of the specified array.
	 *
	 * @return An array containing the elements
	 */
    public Object[] toArray(Object[] a) {
        return ll.toArray(a);
    }

    /**
	 * Returns an array containing all of the elements in the List in proper sequence
	 *
	 * @return An array containing the elements
	 */
    public Object[] toArray() {
        return ll.toArray();
    }

    /**
	 * Set the contents of a position in the List
	 *
	 * @param index The position in the List to fill
	 * @param element The element to insert
	 * @return The inserted Object
	 */
    public Object set(int index, Object element) {
        if (element instanceof Track) {
            Object retval = ll.set(index, element);
            persist();
            return retval;
        } else {
            throw new ClassCastException("Cannot add a non Track object to a Playlist");
        }
    }

    /**
	 * Get the object at a specific index from the List
	 *
	 * @param index The index of the required object
	 * @return The object
	 */
    public Object get(int index) {
        return ll.get(index);
    }

    /**
	 * Get a sublist from the List
	 *
	 * @param fromIndex The position of the beginning of the sublist
	 * @param toIndex The position of the end of the sublist
	 */
    public List subList(int fromIndex, int toIndex) {
        return ll.subList(fromIndex, toIndex);
    }

    /**
	 * Get a pretty String representation of the list
	 */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        int counter = 0;
        Iterator i = ll.iterator();
        while (i.hasNext()) {
            sb.append(" [").append(counter++).append("] ").append(((Track) i.next()).getURL()).append("\n");
        }
        return StringDecorator.boxAroundString(sb.toString(), this.name);
    }

    public Object clone() {
        JukeXPlaylist retVal = new JukeXPlaylist();
        retVal.id = this.id;
        retVal.name = this.name;
        retVal.ll = (LinkedList) this.ll.clone();
        return retVal;
    }

    /**
	 * Return a String summarising the configuration of the task the source is 
	 * performing
	 *
	 * @return A String
	 */
    public String getSummary() {
        return "JukeXPlaylist: This is a user defined playlist which is used to hold selection of tracks";
    }

    /**
	 * Return a String describing what, in general terms, this TrackSource does
	 *
	 * @return A String
	 */
    public String getDescription() {
        return "This playlist currently contains " + this.ll.size() + " tracks";
    }
}
