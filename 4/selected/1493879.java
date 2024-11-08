package org.simpleframework.util.select;

import java.nio.channels.SelectableChannel;
import java.util.concurrent.Executor;

/**
 * The <code>PartitionDistributor</code> object is a distributor that
 * partitions the selection process in to several threads. Each of
 * the threads has a single selector, and operations are distributed
 * amongst the threads using the hash code of the socket. Partitions
 * ensure that several selector threads can share a higher load and
 * respond to a more I/O events.
 * 
 * @author Niall Gallagher
 */
class PartitionDistributor implements Distributor {

    /**
    * This contains the distributors that represent a partition. 
    */
    private final Distributor[] list;

    /**
    * Constructor for the <code>PartitionDistributor</code> object. 
    * This will create a distributor that partitions the operations
    * amongst a pool of selectors using the channels hash code.
    * 
    * @param executor this is the executor used to run operations
    * @param count this is the number of partitions to be used
    */
    public PartitionDistributor(Executor executor, int count) throws Exception {
        this(executor, count, 300000);
    }

    /**
    * Constructor for the <code>PartitionDistributor</code> object. 
    * This will create a distributor that partitions the operations
    * amongst a pool of selectors using the channels hash code.
    * 
    * @param executor this is the executor used to run operations
    * @param count this is the number of partitions to be used
    * @param cancel should the channel be canceled after an event
    */
    public PartitionDistributor(Executor executor, int count, long expiry) throws Exception {
        this.list = new Distributor[count];
        this.start(executor, expiry);
    }

    /**
    * This is used to create the partitions that represent a thread
    * used for selection. Operations will index to a particular one
    * using the hash code of the operations channel. If there is only
    * one partition all operations will index to the partition.
    * 
    * @param executor the executor used to run the operations
    * @param cancel should the channel be canceled after an event
    */
    private void start(Executor executor, long expiry) throws Exception {
        for (int i = 0; i < list.length; i++) {
            list[i] = new EventDistributor(executor, true, expiry);
        }
    }

    /**
    * This is used to process the <code>Operation</code> object. This
    * will wake up the selector if it is currently blocked selecting
    * and register the operations associated channel. Once the 
    * selector is awake it will acquire the operation from the queue
    * and register the associated <code>SelectableChannel</code> for
    * selection. The operation will then be executed when the channel
    * is ready for the interested I/O events.
    * 
    * @param task this is the task that is scheduled for distribution   
    * @param require this is the bit-mask value for interested events
    */
    public void process(Operation task, int require) throws Exception {
        int length = list.length;
        if (length == 1) {
            list[0].process(task, require);
        } else {
            process(task, require, length);
        }
    }

    /**
    * This is used to process the <code>Operation</code> object. This
    * will wake up the selector if it is currently blocked selecting
    * and register the operations associated channel. Once the 
    * selector is awake it will acquire the operation from the queue
    * and register the associated <code>SelectableChannel</code> for
    * selection. The operation will then be executed when the channel
    * is ready for the interested I/O events.
    * 
    * @param task this is the task that is scheduled for distribution   
    * @param require this is the bit-mask value for interested events
    * @param length this is the number of distributors to hash with
    */
    private void process(Operation task, int require, int length) throws Exception {
        SelectableChannel channel = task.getChannel();
        int hash = channel.hashCode();
        list[hash % length].process(task, require);
    }

    /**
    * This is used to close the distributor such that it cancels all
    * of the registered channels and closes down the selector. This
    * is used when the distributor is no longer required, after the
    * close further attempts to process operations will fail.
    */
    public void close() throws Exception {
        for (Distributor entry : list) {
            entry.close();
        }
    }
}
