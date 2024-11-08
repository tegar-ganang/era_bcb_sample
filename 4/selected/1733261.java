package org.semantic.reasoning.apprx.aqa.asyn;

import org.kaon2.basics.util.Result;

public abstract class WriterThread extends Thread {

    protected int threadID;

    protected String query;

    protected AQAResultPool pool;

    public WriterThread(int threadID, AQAResultPool pool) {
        this.threadID = threadID;
        this.pool = pool;
    }

    public void run() {
        if (query == null) {
            System.err.println("Error! The setQuery-Method should be invoked before the run-Method!");
        } else {
            Result result = load(query);
            System.out.println(result);
            pool.writeData(threadID, result);
        }
    }

    protected abstract Result load(String query);

    public void setQuery(String query) {
        this.query = query;
    }
}
