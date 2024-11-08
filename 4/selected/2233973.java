package com.cbsgmbh.xi.af.edifact.jca;

import java.util.LinkedList;
import java.util.Locale;
import javax.resource.ResourceException;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.service.administration.api.AdapterAlreadyRegisteredException;
import com.sap.aii.af.service.administration.api.AdapterCallback;
import com.sap.aii.af.service.administration.api.AdapterCapability;
import com.sap.aii.af.service.administration.api.AdapterRegistry;
import com.sap.aii.af.service.administration.api.AdapterRegistryFactory;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.administration.api.cpa.CPALookupManager;
import com.sap.aii.af.service.administration.api.cpa.ChannelLifecycleCallback;
import com.sap.aii.af.service.administration.api.i18n.LocalizationCallback;
import com.sap.aii.af.service.administration.api.i18n.LocalizationNotPossibleException;
import com.sap.aii.af.service.administration.api.monitoring.ChannelState;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatus;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatusCallback;
import com.sap.aii.af.service.administration.api.monitoring.ChannelStatusFactory;
import com.sap.aii.af.service.administration.api.monitoring.ChannelUnknownException;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.Direction;

public class ChannelConfiguration implements ChannelLifecycleCallback, ChannelStatusCallback, LocalizationCallback {

    private static final String VERSION_ID = "$Id: //OPI2_EDIFACT_Adapter_Http/com/cbsgmbh/opi2/xi/af/edifact/jca/ChannelConfiguration.java#1 $";

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(VERSION_ID, TracerCategories.APP_ADAPTER_HTTP);

    private String adapterType;

    private String adapterNameSpace;

    private AdapterRegistry adapterRegistry = null;

    private LocalizationCallback channelLocalizer = null;

    private CPALookupManager cpaLookupManager = null;

    private LinkedList outboundChannels = null;

    private LinkedList inboundChannels = null;

    private SPIManagedConnectionFactory spiManagedConnectionFactory = null;

    public ChannelConfiguration(String adapterType, String adapterNameSpace) throws ResourceException {
        final Tracer tracer = baseTracer.entering("ChannelConfiguration(String adapterType, String adapterNameSpace)");
        this.adapterType = adapterType;
        this.adapterNameSpace = adapterNameSpace;
        try {
            CPAFactory cpaFactory = CPAFactory.getInstance();
            this.cpaLookupManager = cpaFactory.getLookupManager();
        } catch (Exception ex) {
            tracer.catched(ex);
            tracer.error("Channel configuration not accessible, because of CPALookupManager error: " + ex.getMessage());
        }
        tracer.leaving();
    }

    public void registerAdapter(SPIManagedConnectionFactory spiManagedConnectionFactory) throws ResourceException {
        final Tracer tracer = baseTracer.entering("registerAdapter(SPIManagedConnectionFactory spiManagedConnectionFactory)");
        this.spiManagedConnectionFactory = spiManagedConnectionFactory;
        synchronized (this) {
            this.inboundChannels = new LinkedList();
            this.outboundChannels = new LinkedList();
            try {
                LinkedList allChannels = this.cpaLookupManager.getChannelsByAdapterType(this.adapterType, this.adapterNameSpace);
                tracer.debug("XI CPA service - number of channels: {0} for adapter type {1} with namespace {2}", new Object[] { new Integer(allChannels.size()), this.adapterType, this.adapterNameSpace });
                for (int i = 0; i < allChannels.size(); i++) {
                    Channel channel = (Channel) allChannels.get(i);
                    String status = channel.getValueAsString("adapterStatus");
                    if (channel.getDirection() == Direction.INBOUND) {
                        this.inboundChannels.add(channel);
                    } else if (channel.getDirection() == Direction.OUTBOUND) {
                        this.outboundChannels.add(channel);
                    } else continue;
                    tracer.debug("Channel with ID {0} for party {1} and service {2} added (direction is {3}, status: {4}).", new Object[] { channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString(), status });
                }
            } catch (Exception e) {
                tracer.catched(e);
                ResourceException re = new ResourceException("CPA lookup failed: " + e.getMessage());
                tracer.throwing(re);
                throw re;
            }
            try {
                this.channelLocalizer = ChannelLocalization.getLocalizationCallback();
                AdapterRegistryFactory adapterRegistryFactory = AdapterRegistryFactory.getInstance();
                this.adapterRegistry = adapterRegistryFactory.getAdapterRegistry();
                this.adapterRegistry.registerAdapter(this.adapterNameSpace, this.adapterType, new AdapterCapability[] { AdapterCapability.PUSH_PROCESS_STATUS }, new AdapterCallback[] { this });
                tracer.debug("Adapter registered for pushing process state. Adapter namespace: {0}, adapter type: {1}.", new Object[] { this.adapterNameSpace, this.adapterType });
            } catch (AdapterAlreadyRegisteredException aare) {
                tracer.catched(aare);
                stop();
                this.adapterRegistry.registerAdapter(this.adapterNameSpace, this.adapterType, new AdapterCapability[] { AdapterCapability.PUSH_PROCESS_STATUS }, new AdapterCallback[] { this });
                tracer.debug("Adapter registered for pushing process state. Adapter namespace: {0}, adapter type: {1}.", new Object[] { this.adapterNameSpace, this.adapterType });
            } catch (Exception ex) {
                tracer.catched(ex);
                ResourceException re = new ResourceException("XI AAM registration error: " + ex.getMessage());
                tracer.throwing(re);
                throw re;
            }
        }
        tracer.leaving();
    }

    public void channelAdded(Channel channel) {
        String status = null;
        final Tracer tracer = baseTracer.entering("channelAdded(Channel channel)");
        synchronized (this) {
            if (channel.getDirection() == Direction.OUTBOUND) {
                this.outboundChannels.add(channel);
            } else if (channel.getDirection() == Direction.INBOUND) {
                this.inboundChannels.add(channel);
            }
        }
        try {
            status = channel.getValueAsString("adapterStatus");
        } catch (Exception e) {
            tracer.catched(e);
            tracer.error("Adapter status value not set.");
        }
        tracer.debug("Channel with ID {0} for party {1} and service {2} added (direction is {3}, status: {4}).", new Object[] { channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString(), status });
        tracer.leaving();
    }

    public void channelRemoved(Channel channel) {
        final Tracer tracer = baseTracer.entering("channelRemoved(Channel channel)");
        LinkedList channels = null;
        tracer.debug("Channel with ID {0} for party {1} and service {2} will be removed now. (direction is {3}).", new Object[] { channel.getObjectId(), channel.getParty(), channel.getService(), channel.getDirection().toString() });
        String channelID = channel.getObjectId();
        if (channel.getDirection() == Direction.INBOUND) channels = this.inboundChannels; else if (channel.getDirection() == Direction.OUTBOUND) channels = this.outboundChannels; else return;
        synchronized (this) {
            for (int i = 0; i < channels.size(); i++) {
                Channel storedChannel = (Channel) channels.get(i);
                if (storedChannel.getObjectId().equalsIgnoreCase(channelID)) {
                    channels.remove(i);
                    if (channel.getDirection() == Direction.OUTBOUND) {
                        try {
                            this.spiManagedConnectionFactory.destroyManagedConnection(channelID);
                        } catch (Exception e) {
                            tracer.catched(e);
                            tracer.warn("ManagedConnection for channel " + channelID + " cannot be destroyed.");
                        }
                    }
                    return;
                }
            }
        }
        tracer.leaving();
    }

    public void channelUpdated(Channel channel) {
        final Tracer tracer = baseTracer.entering("channelUpdated(Channel channel)");
        channelRemoved(channel);
        channelAdded(channel);
        tracer.leaving();
    }

    public ChannelStatus getChannelStatus(Channel channel, Locale locale) throws ChannelUnknownException {
        final Tracer tracer = baseTracer.entering("getChannelStatus(Channel channel, Locale locale)");
        boolean channelMatch = false;
        Channel savedChannel = null;
        String channelID = "null";
        Exception cause = null;
        ChannelStatus channelStatus = null;
        try {
            channelID = channel.getObjectId();
            LinkedList channels = null;
            channels = channel.getDirection() == Direction.OUTBOUND ? this.outboundChannels : this.inboundChannels;
            synchronized (this) {
                for (int i = 0; i < channels.size(); i++) {
                    savedChannel = (Channel) channels.get(i);
                    if (savedChannel.getObjectId().equalsIgnoreCase(channelID)) {
                        channelMatch = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            tracer.catched(e);
            cause = e;
            tracer.error("Channel not found in lookup. Reason: " + e.getMessage());
        }
        if (!channelMatch) {
            ChannelUnknownException cue = new ChannelUnknownException("Channel with ID " + channelID + " not found.", cause);
            tracer.error("Channel {0} is not found.", channelID);
            tracer.throwing(cue);
            throw cue;
        }
        ChannelStatusFactory channelStatusFactory = ChannelStatusFactory.getInstance();
        if (channelStatusFactory == null) {
            ChannelUnknownException cue = new ChannelUnknownException("Internal error: Unable to get instance of ChannelStatusFactory.", cause);
            tracer.error("Unable to get instance of ChannelStatusFactory.");
            tracer.throwing(cue);
            throw cue;
        }
        try {
            String channelAdapterStatus = channel.getValueAsString("adapterStatus");
            if ((channelAdapterStatus == null) || !(channelAdapterStatus.equalsIgnoreCase("active"))) {
                channelStatus = channelStatusFactory.createChannelStatus(channel, ChannelState.INACTIVE, channelLocalizer.localizeString("CHANNEL_INACTIVE", locale));
            } else {
                channelStatus = channelStatusFactory.createChannelStatus(channel, ChannelState.OK, this.channelLocalizer.localizeString("CHANNEL_OK", locale));
            }
        } catch (Exception e) {
            tracer.catched(e);
            tracer.error("Channel status {0} cannot be determinded. Exception is: {1}", new Object[] { channel.getChannelName(), e.getMessage() });
            channelStatus = channelStatusFactory.createChannelStatus(channel, ChannelState.ERROR, "Channel status cannot be determined due to: " + e.getMessage());
        }
        tracer.leaving();
        return channelStatus;
    }

    public LinkedList getCopy(Direction direction) throws ResourceException {
        final String SIGNATURE = "getCopy(Direction direction)";
        LinkedList out = null;
        if ((this.inboundChannels == null) || (this.outboundChannels == null)) registerAdapter(this.spiManagedConnectionFactory);
        synchronized (this) {
            if (direction == Direction.INBOUND) out = (LinkedList) this.inboundChannels.clone(); else if (direction == Direction.OUTBOUND) out = (LinkedList) this.outboundChannels.clone(); else {
                ResourceException re = new ResourceException("Invalid direction");
                baseTracer.withoutEntering(SIGNATURE).throwing(re);
                throw re;
            }
        }
        return out;
    }

    public String localizeString(String arg0, Locale locale) throws LocalizationNotPossibleException {
        return channelLocalizer.localizeString(arg0, locale);
    }

    public void stop() throws ResourceException {
        final Tracer tracer = baseTracer.entering("stop()");
        try {
            this.adapterRegistry.unregisterAdapter(this.adapterNameSpace, this.adapterType);
        } catch (Exception e) {
            tracer.catched(e);
            ResourceException re = new ResourceException("XI AAM unregistration not possible due to: " + e);
            tracer.throwing(re);
            throw re;
        }
        tracer.leaving();
    }
}
