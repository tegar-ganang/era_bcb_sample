package com.rbnb.api;

abstract class FrameManager extends com.rbnb.api.RmapWithMetrics {

    /**
     * the <code>Door</code> to this <code>FrameManager</code>.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 02/21/2001
     */
    private Door door = null;

    /**
     * index used for identification.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 03/08/2001
     */
    private long idIndex;

    /**
     * last time the <code>Registration</code> was updated.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 12/14/2001
     */
    private long lastRegistration = Long.MIN_VALUE;

    /**
     * the registration map.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 02/09/2001
     */
    private Registration registered = null;

    /**
     * the summary registration map.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 02/09/2001
     */
    private Registration summary = null;

    /**
     * registration up-to-date?
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 02/20/2001
     */
    private boolean upToDate = false;

    FrameManager() throws com.rbnb.api.SerializeException, java.io.IOException, java.lang.InterruptedException {
        super();
        setDoor(new Door(Door.READ_WRITE));
        setDataSize(0);
    }

    FrameManager(long idIndexI) throws com.rbnb.api.SerializeException, java.io.IOException, java.lang.InterruptedException {
        this();
        setIndex(idIndexI);
    }

    final void addElement(Rmap elementI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        try {
            getDoor().setIdentification(getFullName() + "/" + getClass() + "_" + getIndex());
            getDoor().lock("FrameManager.addElement");
            setUpToDate(false);
            storeElement(elementI);
            setDataSize(getDataSize() + elementI.getDataSize());
        } finally {
            getDoor().unlock();
        }
    }

    String additionalToString() {
        return (" FMIndex: " + getIndex() + ", summary " + getSummary());
    }

    TimeRelativeResponse afterTimeRelative(TimeRelativeRequest requestI, RequestOptions roI) throws com.rbnb.utility.SortException, com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        TimeRelativeResponse responseR = new TimeRelativeResponse();
        responseR.setStatus(-1);
        boolean locked = false;
        try {
            lockRead("FrameManager.afterTimeRelative");
            locked = true;
            if (getRegistered() != null) {
                com.rbnb.utility.SortedVector channels = requestI.getByChannel();
                DataArray limits = getRegistered().extract(((TimeRelativeChannel) channels.firstElement()).getChannelName().substring(requestI.getNameOffset()));
                responseR.setTime(limits.getStartTime());
                responseR.setStatus(0);
                responseR.setInvert(false);
            } else if ((getParent() instanceof FileSet) && (getNchildren() != 0)) {
                responseR = super.afterTimeRelative(requestI, roI);
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
            lockRead("FrameSet.beforeTimeRelative");
            locked = true;
            if (((roI == null) || !roI.getExtendStart()) && (getRegistered() != null)) {
                com.rbnb.utility.SortedVector channels = requestI.getByChannel();
                DataArray limits = getRegistered().extract(((TimeRelativeChannel) channels.firstElement()).getChannelName().substring(requestI.getNameOffset()));
                responseR.setTime(limits.getStartTime() + limits.getDuration());
                responseR.setStatus(0);
                responseR.setInvert(true);
            } else if ((((roI != null) && roI.getExtendStart()) || (getParent() instanceof FileSet)) && (getNchildren() != 0)) {
                responseR = super.beforeTimeRelative(requestI, roI);
            }
        } finally {
            if (locked) {
                unlockRead();
            }
        }
        return (responseR);
    }

    abstract boolean buildRegistration() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.IOException, java.lang.InterruptedException;

    abstract void clear() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException;

    abstract void close() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException;

    public int compareTo(Object sidI, Object otherI) throws com.rbnb.utility.SortException {
        int comparedR = 0;
        if (otherI instanceof FrameManager) {
            comparedR = (int) (getIndex() - ((FrameManager) otherI).getIndex());
        } else {
            comparedR = super.compareTo(sidI, otherI);
        }
        return (comparedR);
    }

    final Door getDoor() {
        return (door);
    }

    final long getIndex() {
        return (idIndex);
    }

    final long getLastRegistration() {
        return (lastRegistration);
    }

    public final Registration getRegistered() {
        return (registered);
    }

    public final Registration getSummary() {
        return (summary);
    }

    final boolean getUpToDate() {
        return (upToDate);
    }

    final boolean isIdentifiable() {
        return (true);
    }

    final byte matches(Rmap requestI, DataRequest referenceI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException {
        byte matchesR = MATCH_NOINTERSECTION;
        if (requestI instanceof FrameManager) {
            FrameManager fmRequest = (FrameManager) requestI;
            long compare = getIndex() - fmRequest.getIndex();
            if (compare == 0) {
                matchesR = MATCH_EQUAL;
            } else if (compare < 0) {
                matchesR = MATCH_BEFORE;
            } else {
                matchesR = MATCH_AFTER;
            }
        } else {
            boolean locked = false;
            try {
                getDoor().lockRead("FrameManager.matches");
                locked = true;
                if (getSummary() != null) {
                    matchesR = getSummary().isWithinLimits(requestI);
                } else {
                    matchesR = MATCH_UNKNOWN;
                }
            } finally {
                if (locked) {
                    getDoor().unlockRead();
                }
            }
        }
        return (matchesR);
    }

    byte moveDownFrom(RmapExtractor extractorI, ExtractedChain unsatisfiedI, java.util.Vector unsatisfiedO) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException {
        byte reasonR = Rmap.MATCH_UNKNOWN;
        boolean locked = false;
        try {
            getDoor().lockRead("FrameManager.moveDownFrom");
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
        if (getDoor() != null) {
            getDoor().nullify();
            setDoor(null);
        }
        if (getRegistered() != null) {
            getRegistered().nullify();
            setRegistered(null);
        }
        if (getSummary() != null) {
            getSummary().nullify();
            setSummary(null);
        }
    }

    abstract boolean readFromArchive() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException;

    abstract void readOffsetsFromArchive() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException;

    abstract void readSkeletonFromArchive() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException;

    final void setDoor(Door doorI) {
        door = doorI;
    }

    final void setIndex(long idIndexI) {
        idIndex = idIndexI;
    }

    final void setLastRegistration(long timeI) {
        lastRegistration = timeI;
    }

    final void setRegistered(Registration registeredI) {
        registered = registeredI;
        if (registered != null) {
            registered.setParent(this);
        }
    }

    final void setSummary(Registration summaryI) {
        summary = summaryI;
        if (summaryI != null) {
            summaryI.setParent(this);
        }
    }

    final void setUpToDate(boolean upToDateI) {
        upToDate = upToDateI;
    }

    abstract void storeElement(Rmap elementI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException;

    final boolean updateRegistration() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.IOException, java.lang.InterruptedException {
        boolean updatedR = false;
        try {
            getDoor().lock("FrameManager.updateRegistration");
            if (!getUpToDate()) {
                if (getRegistered() == null) {
                    setRegistered(new Registration());
                    setSummary(new Registration());
                }
                if (buildRegistration()) {
                    setSummary((Registration) getRegistered().summarize());
                    updatedR = true;
                }
                setLastRegistration(System.currentTimeMillis());
                setUpToDate(true);
            }
        } finally {
            getDoor().unlock();
        }
        return (updatedR);
    }

    abstract void writeToArchive() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException;

    /** Copies all the fields of the object to the given object
	 */
    protected void cloned(Object o) {
        super.cloned(o);
        FrameManager clonedR = (FrameManager) o;
        clonedR.door = door;
        clonedR.idIndex = idIndex;
        clonedR.lastRegistration = lastRegistration;
        clonedR.registered = registered;
        clonedR.summary = summary;
        clonedR.upToDate = upToDate;
    }
}
