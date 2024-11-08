package uk.org.toot.audio.core;

import java.util.List;
import uk.org.toot.service.*;
import uk.org.toot.control.CompoundControlChain;
import uk.org.toot.control.CompoundControl;
import uk.org.toot.audio.spi.AudioControlServiceDescriptor;
import static uk.org.toot.misc.Localisation.getString;

/**
 * AudioControlsChain extends CompoundControlChain to provide
 * information regarding audio control services which may be plugged in.
 */
public class AudioControlsChain extends CompoundControlChain {

    private String sourceLabel;

    private String sourceLocation;

    /**
     * null means there are no format constraints
     * so all descriptors are available for insert
     * if set non-null, only compatible descriptors are returned
     * @supplierCardinality 0..1
     * @link aggregation 
     */
    private ChannelFormat constraintChannelFormat = null;

    public AudioControlsChain(int id, String name) {
        super(id, name);
    }

    public AudioControlsChain(int id, int index, String name, ChannelFormat constraintFormat) {
        super(id, index, name);
        constraintChannelFormat = constraintFormat;
    }

    public void setMetaInfo(AudioBuffer.MetaInfo metaInfo) {
        if (metaInfo != null) {
            sourceLabel = metaInfo.getSourceLabel();
            sourceLocation = metaInfo.getSourceLocation();
        } else {
            sourceLabel = " ";
            sourceLocation = " ";
        }
        setChanged();
        notifyObservers();
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public ChannelFormat getConstraintChannelFormat() {
        return constraintChannelFormat;
    }

    protected CompoundControl createControl(String name) {
        return AudioServices.createControls(name);
    }

    protected int getMaxInstance() {
        return 1024 - 1;
    }

    protected boolean isCompatibleDescriptor(ServiceDescriptor d) {
        if (d.getDescription().equals(getString("Reverb")) && getName().indexOf("FX") < 0) {
            return false;
        }
        if (constraintChannelFormat == null) return true;
        if (d instanceof AudioControlServiceDescriptor) {
            AudioControlServiceDescriptor acsd = (AudioControlServiceDescriptor) d;
            ChannelFormat descriptorFormat = acsd.getChannelFormat();
            if (descriptorFormat == null) return true;
            if (descriptorFormat.getCount() > constraintChannelFormat.getCount()) {
                return false;
            }
        }
        return true;
    }

    public List<ServiceDescriptor> descriptors() {
        final List<ServiceDescriptor> descriptors = new java.util.ArrayList<ServiceDescriptor>();
        AudioServices.accept(new ServiceVisitor() {

            public void visitDescriptor(ServiceDescriptor d) {
                if (isCompatibleDescriptor(d)) {
                    descriptors.add(d);
                }
            }
        }, AudioControls.class);
        return descriptors;
    }

    public String getPersistenceDomain() {
        return "audio";
    }
}
