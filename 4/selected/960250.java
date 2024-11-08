package org.eaiframework.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eaiframework.Channel;
import org.eaiframework.ChannelManager;
import org.eaiframework.Filter;
import org.eaiframework.FilterContext;
import org.eaiframework.FilterController;
import org.eaiframework.FilterDefinition;
import org.eaiframework.FilterDescriptor;
import org.eaiframework.FilterDescriptorRegistry;
import org.eaiframework.FilterFactory;
import org.eaiframework.FilterManager;
import org.eaiframework.FilterStatistics;
import org.eaiframework.LifecycleException;
import org.eaiframework.MessageConsumer;
import org.eaiframework.MessageConsumerFactory;
import org.eaiframework.MessageSenderFactory;
import org.eaiframework.PropertyValue;
import org.eaiframework.Receiver;

/**
 * 
 */
public class FilterManagerImpl implements FilterManager {

    private static Log log = LogFactory.getLog(FilterManagerImpl.class);

    private Map<String, Filter> filters = new HashMap<String, Filter>();

    private FilterDescriptorRegistry filterDescriptorRegistry;

    private FilterFactory filterFactory;

    private MessageConsumerFactory messageConsumerFactory;

    private ChannelManager channelManager;

    private MessageSenderFactory messageSenderFactory;

    public FilterManagerImpl() {
    }

    public Filter createFilter(FilterDefinition filterDefinition) throws LifecycleException {
        String filterId = filterDefinition.getId();
        String filterDescriptorName = filterDefinition.getDescriptor();
        if (filters.containsKey(filterId)) {
            throw new LifecycleException("Filter id '" + filterId + "' already exists");
        }
        FilterDescriptor filterDescriptor = filterDescriptorRegistry.getFilterDescriptor(filterDescriptorName);
        if (filterDescriptor == null) {
            throw new LifecycleException("the filter descriptor with name " + filterDefinition.getDescriptor() + " was not found.");
        }
        Filter filter = filterFactory.createFilter(filterDescriptor);
        FilterStatistics filterStatistics = new FilterStatisticsImpl();
        FilterController filterController = new FilterControllerImpl(filter, filterStatistics);
        FilterContext filterContext = new FilterContext(filterId);
        filterContext.setFilterDescriptor(filterDescriptor);
        filterContext.setFilterController(filterController);
        filterContext.setFilterStatistics(filterStatistics);
        filterContext.setMessageSender(messageSenderFactory.createMessageSender());
        filter.setFilterContext(filterContext);
        this.setReceivers(filterController, filterDefinition.getReceivers());
        try {
            this.setFilterProperties(filter, filterDefinition.getProperties());
        } catch (Exception e) {
            throw new LifecycleException(e);
        }
        filterController.init();
        filters.put(filterId, filter);
        return filter;
    }

    private void setReceivers(FilterController filterController, Collection<Receiver> receivers) throws LifecycleException {
        for (Receiver receiver : receivers) {
            Channel channel = channelManager.getChannel(receiver.getChannelId());
            if (channel == null) {
                throw new LifecycleException("Channel '" + receiver.getChannelId() + "' couldn�t be found.");
            }
            MessageConsumer messageConsumer = messageConsumerFactory.createMessageConsumer(channel, receiver.isSynchronous(), receiver.getInterval());
            messageConsumer.addMessageListener(filterController);
            filterController.addMessageConsumer(messageConsumer);
        }
    }

    private void setFilterProperties(Filter filter, Collection<PropertyValue> properties) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        for (PropertyValue property : properties) {
            PropertyUtils.setSimpleProperty(filter, property.getName(), property.getValue());
        }
    }

    public void startAllFilters() {
        Collection<Filter> filtersCollection = filters.values();
        for (Filter filter : filtersCollection) {
            try {
                filter.getFilterContext().getFilterController().start();
            } catch (Exception e) {
                log.warn(getLogHead(filter) + "Exception while starting filter: " + e.getMessage(), e);
            }
        }
    }

    public void stopAllFilters() {
        Collection<Filter> filtersCollection = filters.values();
        for (Filter filter : filtersCollection) {
            try {
                filter.getFilterContext().getFilterController().stop();
            } catch (Exception e) {
                log.warn(getLogHead(filter) + "Exception while stopping filter: " + e.getMessage(), e);
            }
        }
    }

    public void destroyFilter(String filterId) throws LifecycleException {
        Filter filter = filters.get(filterId);
        if (filter == null) {
            throw new LifecycleException("Filter '" + filterId + "' not found");
        }
        this.destroyFilterHelper(filter);
        filters.remove(filterId);
    }

    public void destroyAllFilters() {
        Collection<Filter> filtersCollection = filters.values();
        for (Filter filter : filtersCollection) {
            try {
                this.destroyFilterHelper(filter);
            } catch (Exception e) {
                log.warn(getLogHead(filter) + "Exception destroying filter: " + e.getMessage(), e);
            }
        }
        filters.clear();
    }

    private void destroyFilterHelper(Filter filter) throws LifecycleException {
        FilterController filterController = filter.getFilterContext().getFilterController();
        filterController.stop();
        filterController.destroy();
    }

    public Filter getFilter(String id) {
        return filters.get(id);
    }

    public Collection<Filter> getFilters() {
        return filters.values();
    }

    /**
	 * @return the filterDescriptorRegistry
	 */
    public FilterDescriptorRegistry getFilterDescriptorRegistry() {
        return filterDescriptorRegistry;
    }

    /**
	 * @param filterDescriptorRegistry the filterDescriptorRegistry to set
	 */
    public void setFilterDescriptorRegistry(FilterDescriptorRegistry filterDescriptorRegistry) {
        this.filterDescriptorRegistry = filterDescriptorRegistry;
    }

    /**
	 * @return the filterFactory
	 */
    public FilterFactory getFilterFactory() {
        return filterFactory;
    }

    /**
	 * @param filterFactory the filterFactory to set
	 */
    public void setFilterFactory(FilterFactory filterFactory) {
        this.filterFactory = filterFactory;
    }

    /**
	 * @return the messageConsumerFactory
	 */
    public MessageConsumerFactory getMessageConsumerFactory() {
        return messageConsumerFactory;
    }

    /**
	 * @param messageConsumerFactory the messageConsumerFactory to set
	 */
    public void setMessageConsumerFactory(MessageConsumerFactory messageConsumerFactory) {
        this.messageConsumerFactory = messageConsumerFactory;
    }

    /**
	 * @return the channelManager
	 */
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    /**
	 * @param channelManager the channelManager to set
	 */
    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    /**
	 * @return the messageSenderFactory
	 */
    public MessageSenderFactory getMessageSenderFactory() {
        return messageSenderFactory;
    }

    /**
	 * @param messageSenderFactory the messageSenderFactory to set
	 */
    public void setMessageSenderFactory(MessageSenderFactory messageSenderFactory) {
        this.messageSenderFactory = messageSenderFactory;
    }

    private String getLogHead(Filter filter) {
        return "[filter=" + filter.getFilterContext().getFilterId() + ",descriptor=" + filter.getFilterContext().getFilterDescriptor().getName() + "]";
    }
}
