package com.ibm.realtime.flexotask.distribution;

import com.ibm.realtime.flexotask.system.FlexotaskSystemSupport;
import com.ibm.realtime.flexotask.template.FlexotaskCommunicatorTemplate;
import com.ibm.realtime.flexotask.template.FlexotaskValidationException;

/**
 * Distributers must extend this class to achieve distribution across machines.
 * <p>A Flexotask graph that is distributed across multiple processes will have a distributer to
 *   handle communication and clock synchronization.  A registered FlexotaskDistributerFactory will
 *   create FlexotaskDistributer for the graph given an FlexotaskDistributionContext.  FlexotaskDistributer
 *   implementations must inherit from this one.  It implements degenerate behavior suitable
 *   for a non-distributed graph.
 */
public class FlexotaskDistributer {

    /**
   * Get the communication channel for replicating a particular Communicator
   * @param spec the FlexotaskCommunicatorTemplate of the communicator
   * @return a channel (this class returns one with degenerate behavior)
   * @throws FlexotaskValidationException if the distributer is unable to create a channel when
   *    the communicator should be replicated
   */
    public FlexotaskChannel getChannel(FlexotaskCommunicatorTemplate spec) throws FlexotaskValidationException {
        return new TrivialChannel();
    }

    /**
   * Get a FlexotaskTimerService for scheduler threads to use
   * @return a FlexotaskTimerService suitable for this distribution paradigm
   */
    public FlexotaskTimerService getTimerService() {
        return new LocalTimerService();
    }

    /**
   * Take action when the graph is fully instantiated
   */
    public void instantiated() {
    }

    /**
   * Take action when the graph is about to be destroyed
   */
    public void destroyed() {
    }

    /**
   * A Trivial channel class
   */
    public static class TrivialChannel implements FlexotaskChannel {

        public Object exchangeValue(Object oldValue) {
            return oldValue;
        }
    }

    /**
   * A FlexotaskTimerService that just uses local time
   */
    public static class LocalTimerService implements FlexotaskTimerService {

        public long nanoTime() {
            return FlexotaskSystemSupport.nanoTime();
        }

        public void nanosleep(long deadline) {
            FlexotaskSystemSupport.nanosleep(deadline);
        }
    }
}
