package org.eaiframework.support;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eaiframework.Filter;
import org.eaiframework.FilterController;
import org.eaiframework.FilterException;
import org.eaiframework.FilterLifecycleListener;
import org.eaiframework.FilterStatistics;
import org.eaiframework.LifecycleEnum;
import org.eaiframework.LifecycleException;
import org.eaiframework.MessageConsumer;

/**
 * 
 */
public abstract class AbstractFilterController implements FilterController {

    private static Log log = LogFactory.getLog(AbstractFilterController.class);

    /**
	 * The Filter this FilterController is controlling.
	 */
    protected Filter filter;

    /**
	 * The FilterStatistics of the filter.
	 */
    protected FilterStatistics filterStatistics;

    /**
	 * The state of the filter.
	 */
    protected LifecycleEnum state;

    /**
	 * The Collection of the MessageConsumer objects this FilterController
	 * is registered to.
	 */
    protected Collection<MessageConsumer> messageConsumers;

    /**
	 * The Collection of FilterLifecycleListener objects that are registered
	 * to listen lifecycle events of this controller.
	 */
    private Collection<FilterLifecycleListener> listeners;

    /**
	 * Constructor.
	 * @param filter
	 * @param filterStatistics
	 */
    public AbstractFilterController(Filter filter, FilterStatistics filterStatistics) {
        this.filter = filter;
        this.filterStatistics = filterStatistics;
        this.messageConsumers = new ArrayList<MessageConsumer>();
        this.listeners = new ArrayList<FilterLifecycleListener>();
        this.state = LifecycleEnum.IDLE;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public FilterStatistics getFilterStatistics() {
        return filterStatistics;
    }

    public void setFilterStatistics(FilterStatistics filterStatistics) {
        this.filterStatistics = filterStatistics;
    }

    public LifecycleEnum getState() {
        return state;
    }

    public void addMessageConsumer(MessageConsumer messageConsumer) throws FilterException {
        this.messageConsumers.add(messageConsumer);
        log.debug(getLogHead() + "message consumer added for channel " + messageConsumer.getChannel());
        if (this.state.equals(LifecycleEnum.STARTED)) {
            try {
                messageConsumer.start();
                log.debug(getLogHead() + "message consumer for channel " + messageConsumer.getChannel() + " started.");
            } catch (LifecycleException e) {
                throw new FilterException("could not start message consumer for channel " + messageConsumer.getChannel() + ": " + e.getMessage(), e);
            }
        }
    }

    public void removeMessageConsumer(MessageConsumer messageConsumer) throws FilterException {
        boolean found = this.messageConsumers.remove(messageConsumer);
        if (found) {
            messageConsumer.stop();
        }
        log.debug(getLogHead() + "message consumer removed for channel " + messageConsumer.getChannel());
    }

    public void removeMessageConsumer(String channelId) throws FilterException {
    }

    public Collection<MessageConsumer> getMessageConsumers() {
        return messageConsumers;
    }

    public void registerLifecycleListener(FilterLifecycleListener listener) {
        this.listeners.add(listener);
        log.debug("listener registered: " + listener.getClass().getName());
    }

    public void removeLifecycleListener(FilterLifecycleListener listener) {
        this.listeners.remove(listener);
        log.debug("listener removed: " + listener.getClass().getName());
    }

    public void setLifecycleListeners(Collection<FilterLifecycleListener> listeners) {
        this.listeners = listeners;
    }

    public Collection<FilterLifecycleListener> getLifecycleListeners() {
        return listeners;
    }

    protected void notifyCreate() {
        for (FilterLifecycleListener listener : listeners) {
            try {
                listener.onCreate(filter);
            } catch (Throwable e) {
                log.error(getLogHead() + "listener threw an exception when notifying onCreate: " + listener.getClass().getName());
            }
        }
    }

    protected void notifyStart() {
        for (FilterLifecycleListener listener : listeners) {
            try {
                listener.onStart(filter);
            } catch (Throwable e) {
                log.error(getLogHead() + "listener threw an exception when notifying onStart: " + listener.getClass().getName());
            }
        }
    }

    protected void notifyStop() {
        for (FilterLifecycleListener listener : listeners) {
            try {
                listener.onStop(filter);
            } catch (Throwable e) {
                log.error(getLogHead() + "listener threw an exception when notifying onStop: " + listener.getClass().getName());
            }
        }
    }

    protected void notifyDestroy() {
        for (FilterLifecycleListener listener : listeners) {
            try {
                listener.onDestroy(filter);
            } catch (Throwable e) {
                log.error(getLogHead() + "listener threw an exception when notifying onDestroy: " + listener.getClass().getName());
            }
        }
    }

    protected String getLogHead() {
        return "[filter=" + this.filter.getFilterContext().getFilterId() + ",type=" + this.filter.getFilterContext().getFilterDescriptor().getName() + "] ";
    }
}
