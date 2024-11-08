package org.semantic.reasoning.apprx.aqa.asyn;

import java.util.ArrayList;
import org.kaon2.basics.util.Result;
import org.semantic.reasoning.screech.asyn.util.ReadWriteLock;

public class AQAResultPool {

    private ReadWriteLock rwLock;

    private String status;

    private ArrayList<Result> data = new ArrayList<Result>();

    public AQAResultPool() {
        rwLock = new ReadWriteLock();
        status = "No lock issued";
    }

    public void writeData(int threadID, Result res) {
        rwLock.getWriteLock();
        status = threadID + " has been issued a write lock";
        System.out.println(status);
        System.out.println("Thread " + threadID + " is writing data");
        data.add(res);
        rwLock.done();
    }

    public boolean readData(int index, AnyTimeManager thread) {
        rwLock.getReadLock();
        boolean read = false;
        if (data.size() > index) {
            thread.setData(data.get(index));
            read = true;
        }
        rwLock.done();
        return read;
    }

    public String getStatus() {
        return status;
    }
}
