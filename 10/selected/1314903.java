package com.neoworks.jukex.tracksource;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.sql.*;
import java.io.*;
import org.apache.log4j.Category;
import com.neoworks.jukex.*;
import com.neoworks.jukex.sqlimpl.JukeXTrackStore;
import com.neoworks.connectionpool.PoolManager;

/**
 * A pipeline of TrackSources. This is the guts of the Track selection strategy for JukeX.
 * Individual TrackSources are chained together and result in a stream of Tracks that can
 * be requested by a player.
 *
 * @author Nigel Atkinson (<a href="mailto:nigel@neoworks.com">nigel@neoworks.com</a>)
 * @author Nick Vincent (<a href="mailto:nick@neoworks.com">nick@neoworks.com</a>)
 */
public class TrackSourcePipeline extends LinkedList implements TrackSource {

    private static final Category log = Category.getInstance(TrackSourcePipeline.class.getName());

    private static final boolean logDebugEnabled = log.isDebugEnabled();

    private static final boolean logInfoEnabled = log.isInfoEnabled();

    private long id = 0;

    private String name = null;

    private static Map classLUT = new HashMap();

    private static Map pipelines = new HashMap();

    /**
	 * Get a pipeline by name, creating a new one if it does not exist
	 *
	 * @param name The name of the pipeline to get
	 * @return A TrackSourcePipeline
	 */
    public static TrackSourcePipeline getPipeline(String name) {
        Connection conn = null;
        TrackSourcePipeline retval = null;
        synchronized (pipelines) {
            if (logDebugEnabled) log.debug("Getting pipeline " + name);
            retval = (TrackSourcePipeline) pipelines.get(name);
            if (retval == null) {
                if (logDebugEnabled) log.debug("Not in cache, fetching from db...");
                try {
                    conn = PoolManager.getInstance().getConnection(JukeXTrackStore.DB_NAME);
                    PreparedStatement ps = conn.prepareStatement("SELECT id FROM Pipeline WHERE name=?");
                    ps.setString(1, name);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        retval = new TrackSourcePipeline(rs.getLong(1), name);
                        retval.restorePipeline();
                    } else {
                        PreparedStatement createPipeline = conn.prepareStatement("INSERT INTO Pipeline (name) VALUES (?)");
                        createPipeline.setString(1, name);
                        createPipeline.executeUpdate();
                        PreparedStatement findID = conn.prepareStatement("SELECT LAST_INSERT_ID()");
                        ResultSet newID = findID.executeQuery();
                        if (newID.next()) {
                            retval = new TrackSourcePipeline(newID.getLong(1), name);
                        } else {
                            log.warn("Could not find last inserted id whilst creating a TrackSourcePipeline");
                        }
                    }
                    if (logDebugEnabled) log.debug("Caching pipeline " + name);
                    pipelines.put(name, retval);
                } catch (SQLException se) {
                    log.warn("Encountered an exception whilst fetching pipeline '" + name + "'", se);
                    try {
                        conn.close();
                    } catch (SQLException ignore) {
                    }
                }
            }
        }
        return retval;
    }

    /**
	 * Creates a new instance of TrackSourcePipeline
	 *
	 * @param id The database ID of the pipeline
	 * @param name The name of the pipeline
	 */
    private TrackSourcePipeline(long id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
	 * Get the database ID of this TrackSourcePipeline
	 *
	 * @return The database as a long
	 */
    public long getId() {
        return id;
    }

    /**
	 * Pipelines are always enabled (at the moment)
	 */
    public boolean isEnabled() {
        return true;
    }

    /**
	 * Get the name of this TrackSourcePipeline
	 *
	 * @return A String identifying the TrackSourcePipeline
	 */
    public String getName() {
        return name;
    }

    /**
	 * Get the next track from the source.
	 *
	 * @return A Track object
	 */
    public Track getNextTrack() {
        Track t = null;
        Attribute attrPlayed = TrackStoreFactory.getTrackStore().getAttribute("Played");
        if (logDebugEnabled) log.debug("TSP: getNextTrack()");
        if (this.size() > 0) {
            TrackSourcePipelineElement pe = (TrackSourcePipelineElement) super.getFirst();
            if (logDebugEnabled) log.debug("Getting next track from " + pe.getName());
            t = pe.getNextTrack();
            synchronized (t) {
                t.replaceAttributeValues(attrPlayed, attrPlayed.getAttributeValue((t.getAttributeValue("Played") != null) ? (t.getAttributeValue("Played").getInt() + 1) : 1));
            }
            return t;
        } else {
            if (logDebugEnabled) log.debug("No more tracks in the pipeline!");
            return null;
        }
    }

    public List peekTracks(int count) {
        if (logDebugEnabled) log.debug("Peeking at the next " + count + " tracks");
        List retVal = new ArrayList();
        if (this.size() > 0) {
            TrackSourcePipelineElement pe = (TrackSourcePipelineElement) super.getFirst();
            if (logDebugEnabled) log.debug("Peeking at a " + pe.getClass().getName());
            retVal.addAll(pe.peekTracks(count));
        }
        return retVal;
    }

    /**
	 * Restore the state of the Pipeline (ie it's elements) from storage.
	 */
    public synchronized boolean restorePipeline() {
        boolean retVal = false;
        Connection conn = null;
        if (logDebugEnabled) log.debug("Restoring pipeline " + this.name);
        try {
            conn = PoolManager.getInstance().getConnection(JukeXTrackStore.DB_NAME);
            PreparedStatement ps = conn.prepareStatement("SELECT datakey,datavalue,position,classname FROM PipelineBlackboard WHERE pipelineid=? ORDER BY position");
            ps.setLong(1, this.id);
            ResultSet rs = ps.executeQuery();
            List elements = new ArrayList();
            int position = -1;
            NamedMap element = null;
            String elementName = null;
            String keyName = null;
            if (logDebugEnabled) log.debug("Retrieving data from Pipeline Blackboard...");
            while (rs.next()) {
                element = null;
                position = rs.getInt(3);
                elementName = rs.getString(4);
                try {
                    element = (NamedMap) elements.get(position);
                } catch (IndexOutOfBoundsException e) {
                }
                if (element == null) {
                    element = new NamedMap(elementName);
                    elements.add(position, element);
                }
                keyName = rs.getString(1);
                if (!keyName.equals("nonce")) {
                    element.put(keyName, deSerialise(rs.getBytes(2)));
                }
            }
            if (logDebugEnabled) log.debug("Clearing any existing state...");
            super.clear();
            if (logDebugEnabled) log.debug("Rebuilding pipeline...");
            ListIterator i = elements.listIterator();
            TrackSourcePipelineElement newPe = null;
            int pos = 0;
            while (i.hasNext()) {
                element = (NamedMap) i.next();
                newPe = (TrackSourcePipelineElement) Class.forName(element.getName()).newInstance();
                newPe.setOwner(this);
                newPe.setState(element);
                this.add(pos, newPe);
                pos++;
            }
            return true;
        } catch (SQLException se) {
            log.warn(se);
        } catch (Exception e) {
            log.warn("Exception in TrackSourcePipeline.restorePipeline(): ", e);
        } finally {
            try {
                conn.close();
            } catch (SQLException ignore) {
            }
        }
        return retVal;
    }

    /**
	 * Handy little class that associates a name with a Map
	 */
    private class NamedMap extends HashMap {

        private String name = null;

        public NamedMap(String name) {
            super();
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
	 * Get a Class object by name, using cached values where possible
	 *
	 * @param name The name of the class
	 * @return A Class object corresponding to the name
	 * @exception ClassNotFoundException If the class counld not be found
	 */
    private Class getClassForName(String name) throws ClassNotFoundException {
        Class retVal = (Class) classLUT.get(name);
        if (retVal == null) {
            retVal = Class.forName(name);
            classLUT.put(name, retVal);
        }
        return retVal;
    }

    /**
	 * Store the state of the Pipeline to the database
	 *
	 * @return success
	 */
    public synchronized boolean storePipeline() {
        if (logDebugEnabled) log.debug("Storing pipeline " + this.getName() + " with " + this.size() + " elements");
        boolean retVal = false;
        Connection conn = null;
        try {
            conn = PoolManager.getInstance().getConnection(JukeXTrackStore.DB_NAME);
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("DELETE from PipelineBlackboard where pipelineid=" + this.getId());
            int rowCount = ps.executeUpdate();
            if (logDebugEnabled) log.debug("Removed old data (" + rowCount + " rows)");
            TrackSourcePipelineElement pe = null;
            Iterator i = super.iterator();
            while (i.hasNext()) {
                pe = (TrackSourcePipelineElement) i.next();
                retVal = pe.storeState(conn);
                if (!retVal) {
                    break;
                }
            }
            if (retVal) {
                conn.commit();
            } else {
                conn.rollback();
            }
            conn.setAutoCommit(true);
        } catch (SQLException se) {
            try {
                conn.rollback();
            } catch (SQLException ignore) {
            }
            log.error("Encountered an exception whilst storing the configuration for a pipeline element");
        } finally {
            try {
                conn.close();
            } catch (SQLException ignore) {
            }
        }
        return retVal;
    }

    /**
	 * Store the config for a TrackSourcePipelineElement (package private)
	 *
	 * @param conn The database connection to use, which must NOT be in AutoCommit mode.
	 * @param pe The TrackSourcePipelineElement to store config for
	 * @param config A Map of the configuration for the TrackSourcePipelineElement
	 * @return success
	 */
    synchronized boolean storePipelineElementState(Connection conn, TrackSourcePipelineElement pe, Map config) {
        boolean retVal = false;
        int pipelinePosition = this.indexOf(pe);
        try {
            Statement state = conn.createStatement();
            state.executeUpdate("DELETE FROM PipelineBlackboard WHERE pipelineid=" + this.id + " AND position=" + pipelinePosition);
            PreparedStatement ps = conn.prepareStatement("INSERT INTO PipelineBlackboard (pipelineid,position,classname,datakey,datavalue) VALUES (?,?,?,?,?)");
            Iterator i = config.keySet().iterator();
            if (i.hasNext()) {
                while (i.hasNext()) {
                    String currKey = (String) i.next();
                    ps.setLong(1, this.id);
                    ps.setInt(2, pipelinePosition);
                    ps.setString(3, pe.getClass().getName());
                    ps.setString(4, currKey);
                    ps.setBytes(5, getSerialised(config.get(currKey)));
                    ps.executeUpdate();
                }
            } else {
                ps.setLong(1, this.id);
                ps.setInt(2, pipelinePosition);
                ps.setString(3, pe.getClass().getName());
                ps.setString(4, "nonce");
                ps.setBytes(5, null);
                ps.executeUpdate();
            }
            retVal = true;
        } catch (SQLException se) {
            log.error("Encountered an exception whilst storing the configuration for a pipeline element");
            retVal = false;
        }
        return retVal;
    }

    /**
	 * Serialise the passed object
	 * @param obj The object to serialise
	 * @return The bytes representing the serialised object
	 */
    private byte[] getSerialised(Object obj) {
        byte[] retval = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            retval = baos.toByteArray();
            oos.close();
        } catch (IOException ioe) {
            log.warn("Could not serialise object " + obj, ioe);
        }
        return retval;
    }

    /**
	 * Deserialise an object serialised by getSerialised
	 *
	 * @param bytes A byte array containing a serialised Object graph
	 * @return The Object
	 */
    private Object deSerialise(byte[] bytes) {
        Object retVal = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            retVal = ois.readObject();
            ois.close();
        } catch (Exception e) {
            log.warn("Could not deserialise byte stream", e);
        }
        if (retVal == null) {
            log.warn("null object deserialised");
        }
        return retVal;
    }

    /**
	 * Add an element to the pipeline at a specified position
	 *
	 * @param index The position to add the element at
	 * @param element The TrackSourcePipelineElement to add
	 * @exception ClassCastException If element is not a TrackSourcePipelineElement
	 * @exception IndexOutOfBoundsException Self explanatory
	 */
    public synchronized void add(int index, Object element) {
        checkType(element);
        if (index > 0) {
            ((TrackSourcePipelineElement) super.get(index - 1)).setNextTrackSource((TrackSourcePipelineElement) element);
            if (logDebugEnabled) log.debug("Adding back link to element " + (index - 1));
        }
        if (super.size() > index) {
            ((TrackSourcePipelineElement) element).setNextTrackSource((TrackSourcePipelineElement) super.get(index));
            if (logDebugEnabled) log.debug("Adding forward link to element currently at " + index);
        }
        if (logDebugEnabled) log.debug("Adding element at position: " + index);
        super.add(index, element);
    }

    /**
	 * Add an element to the back of the pipeline
	 *
	 * @param element The TrackSourcePipelineElement to add
	 * @return Success
	 * @exception ClassCastException If element is not a TrackSourcePipelineElement
	 */
    public synchronized boolean add(Object element) {
        checkType(element);
        int size = super.size();
        if (logDebugEnabled) log.debug("Adding element at position: " + size);
        this.add(size, element);
        return true;
    }

    /**
	 * Add the contents of a Collection to the back of the pipeline
	 *
	 * @param c The Collection of TrackSourcePipelineElements
	 * @return Success
	 * @exception ClassCastException If any of the elemtns in c are not TrackSourcePipelineElements
	 */
    public synchronized boolean addAll(Collection c) {
        checkTypeCollection(c);
        Iterator i = c.iterator();
        while (i.hasNext()) {
            this.addLast(i.next());
        }
        return true;
    }

    /**
	 * Add the contents of a Collection to the pipeline at a specified position
	 *
	 * @param c The Collection of TrackSourcePipelineElements
	 * @param index The position to insert the elements
	 * @return Success
	 * @exception ClassCastException If any of the elemtns in c are not TrackSourcePipelineElements
	 * @exception IndexOutOfBoundsException Self explanatory
	 */
    public synchronized boolean addAll(int index, Collection c) {
        checkTypeCollection(c);
        if (index < 0 || index >= super.size()) {
            throw new IndexOutOfBoundsException();
        }
        int currindex = index;
        Iterator i = c.iterator();
        while (i.hasNext()) {
            this.add(currindex++, i.next());
        }
        return true;
    }

    /**
	 * Add an element to the head of the pipeline
	 *
	 * @param element The element to add
	 * @exception ClassCastException If element is not a TrackSourcePipelineElement
	 */
    public synchronized void addFirst(Object element) {
        checkType(element);
        if (super.size() > 0) ((TrackSourcePipelineElement) element).setNextTrackSource((TrackSourcePipelineElement) super.get(0));
        super.addFirst(element);
    }

    /**
	 * Add an element to the back of the pipeline
	 *
	 * @param element The element to add
	 * @exception ClassCastException If element is not a TrackSourcePipelineElement
	 */
    public synchronized void addLast(Object element) {
        checkType(element);
        if (super.size() > 0) ((TrackSourcePipelineElement) super.getLast()).setNextTrackSource((TrackSourcePipelineElement) element);
        super.addLast(element);
    }

    /** 
	 * Remove the element at the specified position
	 *
	 * @param index The index of the element to remove
	 * @return The removed TrackSourcePipelineElement
	 * @exception IndexOutOfBoundsException Self explanatory
	 */
    public synchronized Object remove(int index) {
        TrackSourcePipelineElement before = null;
        TrackSourcePipelineElement after = null;
        if (index < 0 || index >= super.size()) {
            throw new IndexOutOfBoundsException();
        }
        if (index > 0) {
            if (logDebugEnabled) log.debug("Getting previous element at position: " + (index - 1));
            before = (TrackSourcePipelineElement) super.get(index - 1);
        }
        if (index < super.size() - 1) {
            if (logDebugEnabled) log.debug("Getting next element at position: " + (index + 1));
            after = (TrackSourcePipelineElement) super.get(index + 1);
        }
        if (before != null) {
            if (after != null) {
                if (logDebugEnabled) log.debug("Relinking elements");
                before.setNextTrackSource(after);
            } else {
                before.setNextTrackSource(null);
            }
        }
        if (logDebugEnabled) log.debug("Removing element at position: " + index);
        return super.remove(index);
    }

    /**
	 * Remove the first occurence of an element from the pipeline
	 *
	 * @param element The object to remove
	 * @return Success
	 * @exception IndexOutOfBoundsException Self explanatory
	 */
    public synchronized boolean remove(Object element) {
        return (this.remove(super.indexOf(element)) != null);
    }

    /**
	 * Remove the first element in the pipeline
	 *
	 * @return the TrackSourcePipelineElement removed
	 */
    public synchronized Object removeFirst() {
        return this.remove(0);
    }

    /**
	 * Remove the last element from the pipeline
	 *
	 * @return the TrackSourcePipelineElement removed
	 */
    public synchronized Object removeLast() {
        return this.remove(super.size() - 1);
    }

    /**
	 * Replace the element at the specified position with the specified element
	 *
	 * @param index The position to set
	 * @param element The TrackSourcePipelineElement to be stored
	 * @return The PipeLineElement that has been replaced
	 * @exception IndexOutOfBoundsException index &lt; 0 | index &gt;= size()
	 * @exception ClassCastException If element is not a TrackSourcePipelineElement
	 */
    public synchronized Object set(int index, Object element) {
        checkType(element);
        if (index < 0 || index >= super.size()) {
            throw new IndexOutOfBoundsException();
        }
        if (logDebugEnabled) log.debug("Setting element at position " + index);
        this.add(index, element);
        return this.remove(index + 1);
    }

    /**
	 * Check for foreign elements, and if they aren't foreign then make them
	 * owned by this pipeline.
	 *
	 * @param o The Object to check
	 * @exception ClassCastException If the object is not a PipeLineElement
	 */
    private void checkType(Object o) {
        if (!(o instanceof TrackSourcePipelineElement)) {
            throw new ClassCastException("Cannot add non TrackSourcePipelineElement to the chain");
        } else {
            ((TrackSourcePipelineElement) o).setOwner(this);
        }
    }

    /**
	 * Check for correct types in the Collection and if it passes make them 
	 * owned by this pipeline.
	 * 
	 * @param c The Collection to check
	 */
    private void checkTypeCollection(Collection c) {
        Iterator i = c.iterator();
        Object obj = null;
        while (i.hasNext()) {
            obj = i.next();
            if (!(i instanceof TrackSourcePipelineElement)) {
                throw new ClassCastException("Cannot add non TrackSourcePipelineElement to the chain");
            } else {
                ((TrackSourcePipelineElement) obj).setOwner(this);
            }
        }
    }

    /**
	 * Get an Iterator on this Pipeline
	 *
	 * @return ListIterator for this Pipeline
	 */
    public Iterator iterator() {
        return new TrackSourcePipelineListIterator(super.listIterator());
    }

    /**
	 * Get an Iterator on this Pipeline
	 *
	 * @return ListIterator for this Pipeline
	 */
    public ListIterator listIterator() {
        return new TrackSourcePipelineListIterator(super.listIterator());
    }

    /**
	 * Get an Iterator on this Pipeline at a specific index
	 *
	 * @param index The index to begin at
	 * @return ListIterator for this Pipeline
	 */
    public ListIterator listIterator(int index) {
        return new TrackSourcePipelineListIterator(super.listIterator(index));
    }

    /**
	 * Get a String representation of this Pipeline
	 *
	 * @return a String
	 */
    public String toString() {
        TrackSourcePipelineElement ts = null;
        StringBuffer retVal = new StringBuffer();
        Iterator i = this.iterator();
        while (i.hasNext()) {
            ts = (TrackSourcePipelineElement) i.next();
            retVal.append(ts.toString());
            if (i.hasNext()) {
                retVal.append("\n");
            }
        }
        return retVal.toString();
    }

    /**
	 * Return a String describing what, in general terms, this TrackSource does
	 *
	 * @return A String
	 */
    public String getDescription() {
        return "TrackSourcePipeline: This is a pipeline which holds other elements";
    }

    /**
	 * Return a String summarising the configuration of the task the source is
	 * performing
	 *
	 * @return A String
	 */
    public String getSummary() {
        return new String("Currently holding a pipeline of " + this.size() + " tracksources");
    }

    /**
	 * TrackSourcePipeline iterator
	 */
    private class TrackSourcePipelineListIterator implements ListIterator {

        private ListIterator iterator = null;

        public TrackSourcePipelineListIterator(ListIterator iterator) {
            this.iterator = iterator;
        }

        public void add(Object obj) {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext() {
            return this.iterator.hasNext();
        }

        public boolean hasPrevious() {
            return this.iterator.hasPrevious();
        }

        public Object next() {
            return this.iterator.next();
        }

        public int nextIndex() {
            return this.iterator.nextIndex();
        }

        public Object previous() {
            return this.iterator.previous();
        }

        public int previousIndex() {
            return this.iterator.previousIndex();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(Object obj) {
            throw new UnsupportedOperationException();
        }
    }
}
