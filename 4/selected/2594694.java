package org.atricore.idbus.kernel.main.mediation.provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrust;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustMemberDescriptor;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import java.util.*;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public abstract class AbstractFederatedProvider implements FederatedProvider {

    private static final Log logger = LogFactory.getLog(FederatedProvider.class);

    private String name;

    private String description;

    private String role;

    private FederationService defaultFederationService;

    private Set<FederationService> federationServices = new HashSet<FederationService>();

    private CircleOfTrust circleOfTrust;

    private String skin;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public FederationChannel getChannel() {
        if (defaultFederationService == null) return null;
        return defaultFederationService.getChannel();
    }

    public void setChannel(FederationChannel channel) {
        if (this.defaultFederationService == null) {
            this.defaultFederationService = new FederationService(channel);
        } else {
            this.defaultFederationService.setChannel(channel);
        }
    }

    public Set<FederationChannel> getChannels() {
        if (defaultFederationService == null) {
            return null;
        }
        return this.defaultFederationService.getOverrideChannels();
    }

    public FederationChannel getChannel(String configurationKey) {
        for (FederationService fc : federationServices) {
            if (fc.getName().equals(configurationKey)) return fc.getChannel();
        }
        return null;
    }

    public Set<FederationChannel> getChannels(String configurationKey) {
        for (FederationService fc : federationServices) {
            if (fc.getName().equals(configurationKey)) return fc.getOverrideChannels();
        }
        return null;
    }

    public FederationService getDefaultFederationService() {
        return defaultFederationService;
    }

    public void setDefaultFederationService(FederationService defaultFederationService) {
        this.defaultFederationService = defaultFederationService;
    }

    public Set<FederationService> getFederationServices() {
        return federationServices;
    }

    public void setFederationServices(Set<FederationService> federationServices) {
        this.federationServices = federationServices;
    }

    public CircleOfTrust getCircleOfTrust() {
        return circleOfTrust;
    }

    public void setCircleOfTrust(CircleOfTrust circleOfTrust) {
        this.circleOfTrust = circleOfTrust;
    }

    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    /**
     * This only works for the default channel configuration
     */
    public List<CircleOfTrustMemberDescriptor> getMembers() {
        List<CircleOfTrustMemberDescriptor> members = new ArrayList<CircleOfTrustMemberDescriptor>();
        if (defaultFederationService == null) return members;
        for (FederationChannel channel : defaultFederationService.getOverrideChannels()) {
            members.add(channel.getMember());
        }
        if (defaultFederationService.getChannel() != null) members.add(defaultFederationService.getChannel().getMember());
        return members;
    }

    public List<CircleOfTrustMemberDescriptor> getAllMembers() {
        List<CircleOfTrustMemberDescriptor> members = new ArrayList<CircleOfTrustMemberDescriptor>();
        if (defaultFederationService == null) return members;
        for (FederationChannel channel : defaultFederationService.getOverrideChannels()) {
            members.add(channel.getMember());
        }
        if (defaultFederationService.getChannel() != null) members.add(defaultFederationService.getChannel().getMember());
        for (FederationService svc : federationServices) {
            members.add(svc.getChannel().getMember());
            for (FederationChannel fc : svc.getOverrideChannels()) {
                members.add(fc.getMember());
            }
        }
        return members;
    }

    public List<CircleOfTrustMemberDescriptor> getMembers(String configurationKey) {
        List<CircleOfTrustMemberDescriptor> members = new ArrayList<CircleOfTrustMemberDescriptor>();
        FederationService federationSvc = null;
        for (FederationService fc : federationServices) {
            if (fc.getName().equals(configurationKey)) {
                federationSvc = fc;
                break;
            }
        }
        if (federationSvc == null) return members;
        for (FederationChannel channel : federationSvc.getOverrideChannels()) {
            members.add(channel.getMember());
        }
        if (federationSvc.getChannel() != null) members.add(federationSvc.getChannel().getMember());
        return members;
    }
}
