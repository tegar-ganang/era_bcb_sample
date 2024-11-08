package de.fzi.herakles.strategy.impl.anytime;

import java.util.ArrayList;

public class ResultPool {

    private ReadWriteLock rwLock;

    private String status;

    private int readerIndex = 0;

    private ArrayList<AnytimeResult> data = new ArrayList<AnytimeResult>();

    public ResultPool() {
        rwLock = new ReadWriteLock();
        status = "No lock issued";
    }

    public void writeData(String threadID, Object res) {
        rwLock.getWriteLock();
        status = threadID + " has been issued a write lock";
        data.add(new AnytimeResult(threadID, res));
        rwLock.done();
    }

    public AnytimeResult readData() {
        rwLock.getReadLock();
        AnytimeResult result = null;
        if (data.size() > readerIndex) {
            result = data.get(readerIndex);
            readerIndex++;
        }
        rwLock.done();
        return result;
    }

    public String getStatus() {
        return status;
    }
}
