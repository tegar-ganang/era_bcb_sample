package com.rbnb.api;

abstract class StorageManager extends com.rbnb.api.Rmap implements GetLogInterface, Runnable {

    /**
     * have any sets been removed from the <code>StorageManager</code> since
     * the last time the <code>Registration</code> was updated?
     * <p>
     *
     * @author Ian Brown
     *
     * @see #addedSets
     * @since V2.0
     * @version 05/31/2001
     */
    private boolean removedSets = false;

    /**
     * the door to the <code>StorageManager</code>.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 05/10/2001
     */
    private Door door = null;

    /**
     * the current set.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 03/12/2001
     */
    private FrameManager set = null;

    /**
     * what is the index of the first set added since the last time the
     * <code>Registration</code> was updated?
     * <p>
     * This value is valid only if <code>removedSets</code> is not set.
     * <p>
     *
     * @author Ian Brown
     *
     * @see #removedSets
     * @since V2.0
     * @version 03/12/2001
     */
    private int addedSets = -1;

    /**
     * the maximum number of sets.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 03/12/2001
     */
    private int maxSets = -1;

    /**
     * the maximum number of elements per set.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 03/12/2001
     */
    private int maxElementsPerSet = -1;

    /**
     * the maximum amount of memory per set.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 03/12/2001
     */
    private int maxMemoryPerSet = -1;

    /**
     * last time the <code>Registration</code> was updated.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 03/12/2001
     */
    private long lastRegistration = Long.MIN_VALUE;

    /**
     * the next set index to create.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 12/14/2001
     */
    private long nextIndex = 1;

    /**
     * the <code>Registration</code> map.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 05/10/2001
     */
    private Registration registered = null;

    /**
     * compress FrameSets after adding this many frames
     * set later to sqrt (frameset size)
     * <p>
     *
     * @author Eric Friets
     *
     * @since V2.6
     * @version 05/03/2006
     */
    private int compressAfter = Integer.MAX_VALUE;

    /**
     * number of frames added to current FrameSet
     * <p>
     *
     * @author Eric Friets
     *
     * @since V2.6
     * @version 05/03/2006
     */
    private int framesAdded = 0;

    /**
     * milliseconds to sleep between data flushes
     * <p>
     *
     * @author Eric Friets
     *
     * @since V2.7
     * @version 10/02/2006
     */
    private long flushInterval = 0;

    /**
     * seconds to keep in cache/archive
     * <p>
     *
     * @author Eric Friets
     *
     * @since V2.7
     * @version 10/02/2006
     */
    private float trimInterval = 0;

    /**
     * close by time rather than frames
     * <p>
     *
     * @author Eric Friets
     *
     * @since V2.7
     * @version 10/03/2006
     */
    private boolean closeByTime = false;

    StorageManager(float flushIntervalI, float trimIntervalI) throws com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        super();
        setDoor(new Door(Door.READ_WRITE));
        if (flushIntervalI > 0 && trimIntervalI > 0) {
            closeByTime = true;
            flushInterval = (long) com.rbnb.compat.Utilities.round(flushIntervalI * 1000);
            trimInterval = trimIntervalI;
        } else closeByTime = false;
        if (closeByTime) {
            (new Thread(this)).start();
        }
    }

    public void run() {
        try {
            Thread.currentThread().sleep(flushInterval);
        } catch (Exception e) {
        }
        do {
            close();
            try {
                Thread.currentThread().sleep(flushInterval);
            } catch (Exception e) {
            }
        } while (getParent() != null);
    }

    synchronized void close() {
        if (getSet() != null) {
            try {
                getSet().close();
                setSet(null);
            } catch (Exception e) {
                System.err.println("Exception closing set:");
                e.printStackTrace();
            }
        }
    }

    final synchronized void addElement(Rmap elementI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        try {
            getDoor().setIdentification(getFullName() + "/" + getClass());
            getDoor().lock("StorageManager.addElement");
            if (getSet() == null) {
                if (this instanceof Cache) {
                    setSet(new FrameSet(getNextIndex()));
                    compressAfter = (int) Math.floor(Math.sqrt(getMeps()));
                    framesAdded = 0;
                } else {
                    setSet(new FileSet(getNextIndex()));
                }
                setNextIndex(getNextIndex() + 1);
            }
            getSet().addElement(elementI);
            framesAdded++;
            if (getAddedSets() == -1) {
                setAddedSets(getNchildren() - 1);
            }
            if (framesAdded == getMeps()) {
                if (this instanceof Cache) {
                    FrameSet newfs = null;
                    if (newfs != null) {
                        newfs.updateRegistration();
                        removeChildAt(getNchildren() - 1);
                        removedSets = true;
                        setSet(newfs);
                        getSet().setDataSize(-1);
                    }
                    if (!closeByTime && framesAdded == getMeps()) {
                        close();
                    }
                }
            }
            if (!closeByTime && (this instanceof Archive) && (getSet().getNchildren() == getMeps())) {
                close();
            }
        } finally {
            getDoor().unlock();
        }
    }

    String additionalToString() {
        return (" Sets: " + getMs() + " EPS: " + getMeps() + " MPS: " + getMmps());
    }

    TimeRelativeResponse afterTimeRelative(TimeRelativeRequest requestI, RequestOptions roI) throws com.rbnb.utility.SortException, com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        TimeRelativeResponse responseR = new TimeRelativeResponse();
        responseR.setStatus(-1);
        boolean locked = false;
        try {
            lockRead();
            locked = true;
            if (getRegistered() != null) {
                com.rbnb.utility.SortedVector channels = requestI.getByChannel();
                DataArray limits = getRegistered().extract(((TimeRelativeChannel) channels.firstElement()).getChannelName().substring(requestI.getNameOffset()));
                if ((limits.timeRanges != null) && (limits.timeRanges.size() != 0)) {
                    responseR.setTime(limits.getStartTime());
                    responseR.setStatus(0);
                    responseR.setInvert(false);
                }
            }
        } finally {
            if (locked) {
                unlockRead();
            }
        }
        return (responseR);
    }

    TimeRelativeResponse beforeTimeRelative(TimeRelativeRequest requestI, RequestOptions roI) throws com.rbnb.utility.SortException, com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        TimeRelativeResponse responseR = new TimeRelativeResponse();
        responseR.setStatus(-1);
        boolean locked = false;
        try {
            lockRead();
            locked = true;
            if (getRegistered() != null) {
                if ((roI != null) && roI.getExtendStart()) {
                    Rmap lastChild = getChildAt(getNchildren() - 1);
                    responseR = lastChild.beforeTimeRelative(requestI, roI);
                } else {
                    com.rbnb.utility.SortedVector channels = requestI.getByChannel();
                    DataArray limits = getRegistered().extract(((TimeRelativeChannel) channels.firstElement()).getChannelName().substring(requestI.getNameOffset()));
                    if ((limits.timeRanges != null) && (limits.timeRanges.size() != 0)) {
                        responseR.setTime(limits.getStartTime() + limits.getDuration());
                        responseR.setStatus(0);
                        responseR.setInvert(true);
                    }
                }
            }
        } finally {
            if (locked) {
                unlockRead();
            }
        }
        return (responseR);
    }

    final int getAddedSets() {
        return (addedSets);
    }

    final Door getDoor() {
        return (door);
    }

    public final Log getLog() {
        Log logR = null;
        if (getParent() instanceof GetLogInterface) {
            logR = ((GetLogInterface) getParent()).getLog();
        }
        return (logR);
    }

    final FrameManager getSet() {
        return (set);
    }

    final long getLastRegistration() {
        return (lastRegistration);
    }

    final int getMeps() {
        return (maxElementsPerSet);
    }

    final int getMmps() {
        return (maxMemoryPerSet);
    }

    final int getMs() {
        return (maxSets);
    }

    final long getNextIndex() {
        return (nextIndex);
    }

    final Registration getRegistered() {
        return (registered);
    }

    final boolean getRemovedSets() {
        return (removedSets);
    }

    final byte matches(Rmap requestI, DataRequest referenceI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException {
        byte matchesR = MATCH_NOINTERSECTION;
        boolean locked = false;
        try {
            getDoor().lockRead("StorageManager.matches");
            locked = true;
            matchesR = super.matches(requestI, referenceI);
        } finally {
            if (locked) {
                getDoor().unlockRead();
            }
        }
        return (matchesR);
    }

    final TimeRelativeResponse matchTimeRelative(TimeRelativeRequest requestI, RequestOptions roI) throws com.rbnb.utility.SortException, com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        TimeRelativeResponse responseR = new TimeRelativeResponse();
        boolean locked = false;
        try {
            getDoor().lockRead("StorageManager.matchTimeRelative");
            locked = true;
            com.rbnb.utility.SortedVector toMatch = requestI.getByChannel();
            java.util.Hashtable requests = new java.util.Hashtable();
            TimeRelativeRequest request;
            String channelName;
            DataArray limits;
            TimeRelativeChannel trc;
            int direction;
            int finalDirection = 2;
            int idx;
            FrameManager fm;
            for (idx = 0; (finalDirection != -2) && (idx < toMatch.size()); ++idx) {
                trc = (TimeRelativeChannel) toMatch.elementAt(idx);
                channelName = trc.getChannelName().substring(requestI.getNameOffset());
                limits = getRegistered().extract(channelName);
                if ((limits.timeRanges == null) || (limits.timeRanges.size() == 0)) {
                    continue;
                } else {
                    direction = requestI.compareToLimits(limits);
                    if (idx == 0) {
                        finalDirection = direction;
                    } else if (direction != finalDirection) {
                        finalDirection = -2;
                    }
                }
            }
            if (finalDirection == 2) {
                responseR.setStatus(-1);
            } else if (finalDirection != 0) {
                responseR.setStatus(finalDirection);
            } else {
                int lo = 0;
                int hi = getNchildren() - 1;
                int lastIdx = 0;
                int lastGoodStatus = Integer.MIN_VALUE;
                responseR.setStatus(-1);
                for (idx = (lo + hi) / 2; (responseR.getStatus() != -2) && (responseR.getStatus() != 0) && (lo <= hi); idx = (lo + hi) / 2) {
                    fm = (FrameManager) getChildAt(idx);
                    responseR = fm.matchTimeRelative(requestI, roI);
                    if ((responseR.getStatus() != -3) && (responseR.getStatus() != 3)) {
                        lastGoodStatus = responseR.getStatus();
                    }
                    lastIdx = idx;
                    switch(responseR.getStatus()) {
                        case -2:
                            break;
                        case -1:
                        case -3:
                            hi = idx - 1;
                            break;
                        case 0:
                            break;
                        case 1:
                        case 3:
                            lo = idx + 1;
                            break;
                    }
                }
                if (((responseR.getStatus() == -1) && (lastIdx > 0)) || ((responseR.getStatus() == 1) && (lastIdx < getNchildren() - 1))) {
                    if (responseR.getStatus() == 1) {
                        ++lastIdx;
                    }
                    switch(requestI.getRelationship()) {
                        case TimeRelativeRequest.BEFORE:
                        case TimeRelativeRequest.AT_OR_BEFORE:
                            fm = (FrameManager) getChildAt(lastIdx - 1);
                            responseR = fm.beforeTimeRelative(requestI, roI);
                            if ((responseR.getStatus() == -3) || (responseR.getStatus() == 3)) {
                                responseR.setStatus(-1);
                            }
                            break;
                        case TimeRelativeRequest.AT_OR_AFTER:
                        case TimeRelativeRequest.AFTER:
                            fm = (FrameManager) getChildAt(lastIdx);
                            responseR = fm.afterTimeRelative(requestI, roI);
                            if ((responseR.getStatus() == -3) || (responseR.getStatus() == 3)) {
                                responseR.setStatus(1);
                            }
                            break;
                    }
                }
                if ((responseR.getStatus() == -3) || (responseR.getStatus() == 3)) {
                    if (lastGoodStatus != Integer.MIN_VALUE) {
                        responseR.setStatus(lastGoodStatus);
                    }
                }
            }
        } finally {
            if (locked) {
                getDoor().unlockRead();
            }
        }
        return (responseR);
    }

    byte moveDownFrom(RmapExtractor extractorI, ExtractedChain unsatisfiedI, java.util.Vector unsatisfiedO) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException {
        byte reasonR = MATCH_UNKNOWN;
        boolean locked = false;
        try {
            getDoor().lockRead("StorageManager.moveDownFrom");
            locked = true;
            reasonR = super.moveDownFrom(extractorI, unsatisfiedI, unsatisfiedO);
        } finally {
            if (locked) {
                getDoor().unlockRead();
            }
        }
        return (reasonR);
    }

    final Rmap newInstance() {
        return (new Rmap());
    }

    public void nullify() {
        super.nullify();
        try {
            if (getDoor() != null) {
                getDoor().nullify();
                setDoor(null);
            }
            if (getSet() != null) {
                getSet().nullify();
                setSet(null);
            }
            if (getRegistered() != null) {
                getRegistered().nullify();
                setRegistered(null);
            }
        } catch (java.lang.Throwable e) {
        }
    }

    final void setAddedSets(int addedSetsI) {
        addedSets = addedSetsI;
    }

    private final void setDoor(Door doorI) {
        door = doorI;
    }

    final void setLastRegistration(long timeI) {
        lastRegistration = timeI;
    }

    final void setMeps(int maxElementsPerSetI) {
        maxElementsPerSet = maxElementsPerSetI;
    }

    final void setMmps(int maxMemoryPerSetI) {
        maxMemoryPerSet = maxMemoryPerSetI;
    }

    final void setMs(int maxSetsI) {
        maxSets = maxSetsI;
    }

    final void setNextIndex(long nextIndexI) {
        nextIndex = nextIndexI;
    }

    final void setRegistered(Registration registeredI) {
        registered = registeredI;
        registered.setParent(this);
    }

    final void setRemovedSets(boolean removedSetsI) {
        removedSets = removedSetsI;
    }

    void setSet(FrameManager setI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        set = setI;
        if (set != null) {
            addChild(set);
            if (getAddedSets() == -1) {
                setAddedSets(getNchildren() - 1);
            }
        }
        trim();
    }

    final void trim() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        int empties = (getSet() == null) ? 0 : (getSet().getNchildren() > 1) ? 0 : 1;
        if (getNchildren() < 2) return;
        if (closeByTime) {
            double duration = 0;
            FrameManager fm = null;
            Registration reg = null;
            TimeRange tr = null;
            try {
                for (int i = getNchildren() - 1; i >= 0; i--) {
                    fm = (FrameManager) getChildAt(i);
                    reg = fm.getSummary();
                    if (reg == null || (tr = reg.getTrange()) == null) {
                        return;
                    }
                    if (duration > trimInterval) {
                        for (int j = 0; j < i; j++) removeSet();
                        break;
                    }
                    if (tr.getDuration() > 0) duration += tr.getDuration(); else duration += flushInterval;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            while (((getMs() == 0) && ((getNchildren() - empties) > 1)) || ((getMs() > 0) && ((getNchildren() - empties) > getMs()))) {
                removeSet();
            }
        }
    }

    private void removeSet() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        FrameManager set = (FrameManager) getChildAt(0);
        boolean isMine = (set.getParent() == this);
        removeChildAt(0);
        removedSets = true;
        if (this instanceof Archive) {
            ((Archive) this).setOldest(((FileSet) getChildAt(0)).getIndex());
        }
        try {
            set.getDoor().lock("StorageManager.trim");
            if (set instanceof FileSet) {
                set.setParent(this);
            }
            set.clear();
            if (set instanceof FileSet) {
                set.setParent(null);
            }
        } finally {
            set.getDoor().unlock();
        }
        if (isMine && (set.getParent() == null)) {
            set.nullify();
        }
    }

    final int updateRegistration() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.IOException, java.lang.InterruptedException {
        int changedR = 0;
        try {
            getDoor().lock("StorageManager.updateRegistration");
            boolean rSets;
            int aSets;
            rSets = getRemovedSets();
            setRemovedSets(false);
            aSets = getAddedSets();
            setAddedSets(-1);
            boolean reset = rSets || (aSets == 0);
            int startAt = rSets ? 0 : aSets;
            if ((getRegistered() == null) || (startAt == 0)) {
                changedR = -1;
                setRegistered(new Registration());
            }
            if (startAt >= 0) {
                for (int idx = startAt, endIdx = getNchildren(); idx < endIdx; ++idx) {
                    FrameManager set = (FrameManager) getChildAt(idx);
                    if (set.updateRegistration() || (set.getLastRegistration() > getLastRegistration()) || (changedR == -1)) {
                        if (getRegistered().updateRegistration(set.getRegistered(), reset, false) && (changedR == 0)) {
                            changedR = 1;
                        }
                    }
                    reset = false;
                }
            }
            if (changedR != 0) {
                setLastRegistration(System.currentTimeMillis());
            }
        } finally {
            getDoor().unlock();
        }
        return (changedR);
    }

    /** Copies all the fields of the object to the given object
	 */
    protected void cloned(Object o) {
        super.cloned(o);
        StorageManager clonedR = (StorageManager) o;
        clonedR.removedSets = removedSets;
        clonedR.door = door;
        clonedR.set = set;
        clonedR.addedSets = addedSets;
        clonedR.maxSets = maxSets;
        clonedR.maxElementsPerSet = maxElementsPerSet;
        clonedR.maxMemoryPerSet = maxMemoryPerSet;
        clonedR.lastRegistration = lastRegistration;
        clonedR.nextIndex = nextIndex;
        clonedR.registered = registered;
        clonedR.compressAfter = compressAfter;
        clonedR.framesAdded = framesAdded;
        clonedR.flushInterval = flushInterval;
        clonedR.trimInterval = trimInterval;
        clonedR.closeByTime = closeByTime;
    }
}
