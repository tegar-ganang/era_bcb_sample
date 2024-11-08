package com.ewansilver.raindrop;

import java.util.LinkedList;
import java.util.List;
import com.ewansilver.concurrency.Channel;
import com.ewansilver.concurrency.ChannelFactory;
import com.ewansilver.concurrency.ChannelImpl;

/**
 * Queues implement TaskQueue and are used to connect individual stages
 * together.
 * 
 * The Queue implementation guards access to the underlying TaskQueue by the use
 * of a QueueAdmissionsControllerManager.
 * 
 * @author Ewan Silver
 */
public class Queue implements WorkerChannel, TaskQueue {

    private Channel channel;

    /**
	 * The controllers.
	 */
    private List<QueueAdmissionsController> controllers;

    /**
	 * Constructor.
	 * 
	 * @param someControllers
	 *            the QueueAdmissionControllers.
	 * @param aChannel
	 *            the Channel.
	 */
    public Queue(List<QueueAdmissionsController> someControllers, Channel aChannel) {
        controllers = someControllers;
        channel = aChannel;
    }

    /**
	 * Constructor.
	 */
    public Queue() {
        this(new LinkedList<QueueAdmissionsController>(), new ChannelImpl());
    }

    /**
	 * Constructor.
	 * 
	 * @param anAdmissionsController
	 *            the QueueAdmissionsController guarding access to the Queue.
	 * @param aChannel
	 *            the underlying queueing mechanism.
	 */
    public Queue(QueueAdmissionsController anAdmissionsController, Channel aChannel) {
        this(new LinkedList<QueueAdmissionsController>(), aChannel);
        controllers.add(anAdmissionsController);
    }

    /**
	 * Constructor that uses an UnboundedChannel as its underlying queueing
	 * implementation.
	 * 
	 * @param anAdmissionsController
	 *            the QueueAdmissionsController guarding access to the Queue.
	 */
    public Queue(QueueAdmissionsController anAdmissionsController) {
        this(anAdmissionsController, ChannelFactory.instance().getChannel());
    }

    /**
	 * Sets the QueueAdmissionsController for this Queue.
	 * 
	 * @param anAdmissionsController
	 *            the QueueAdmissionsController for the queue.
	 */
    protected void setQueueAdmissionsController(QueueAdmissionsController anAdmissionsController) {
        controllers = new LinkedList<QueueAdmissionsController>();
        addQueueAdmissionsController(anAdmissionsController);
    }

    public void enqueue(Object aTask) throws TaskQueueException {
        for (QueueAdmissionsController admissionsController : controllers) {
            if (admissionsController.isActive() && !admissionsController.isAcceptable(aTask)) throw new TaskQueueException();
        }
        try {
            channel.put(aTask);
        } catch (InterruptedException e) {
            throw new TaskQueueException();
        }
    }

    /**
	 * Add the supplied QueueAdmissionsController to the list of
	 * QueueAdmissionsController contained within this Queue.
	 * 
	 * @param aController
	 *            the QueueAdmissionsController to be added.
	 */
    public void addQueueAdmissionsController(QueueAdmissionsController aController) {
        controllers.add(aController);
    }

    public Object getNextTask(long aTimeOut) throws InterruptedException {
        return channel.poll(aTimeOut);
    }

    /**
	 * Returns the number of Tasks that are currently waiting on the Queue.
	 */
    public int size() {
        return channel.size();
    }
}
