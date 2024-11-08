package fr.inria.zvtm.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.jmx.JmxConfigurator;
import fr.inria.zvtm.clustering.ObjId;
import fr.inria.zvtm.clustering.ObjIdFactory;
import fr.inria.zvtm.engine.Camera;
import fr.inria.zvtm.engine.VirtualSpace;
import fr.inria.zvtm.engine.VirtualSpaceManager;
import fr.inria.zvtm.glyphs.Glyph;

/**
 * A class that applies incoming messages
 * to a VirtualSpace in order to keep it in sync
 * with a master reference.
 */
public class SlaveUpdater {

    private final VirtualSpace virtualSpace;

    private final JChannel channel = new JChannel(new java.io.File("chan_conf.xml"));

    private final Map<ObjId, Glyph> glyphMap = new HashMap<ObjId, Glyph>();

    private final Map<ObjId, Camera> cameraMap = new HashMap<ObjId, Camera>();

    Glyph getGlyphById(ObjId id) {
        return glyphMap.get(id);
    }

    void addGlyph(ObjId id, Glyph glyph) {
        virtualSpace.addGlyph(glyph);
        glyphMap.put(id, glyph);
    }

    void removeGlyph(ObjId id) {
        Glyph glyph = glyphMap.get(id);
        if (null == glyph) {
            System.out.println("Attempting to remove a non-existent Glyph.");
            return;
        }
        virtualSpace.removeGlyph(glyph);
        glyphMap.remove(id);
    }

    Camera getCameraById(ObjId id) {
        return cameraMap.get(id);
    }

    void addCamera(ObjId id) {
        Camera cam = VirtualSpaceManager.INSTANCE.addCamera(virtualSpace);
        cameraMap.put(id, cam);
    }

    void removeCamera(ObjId id) {
        Camera cam = cameraMap.get(id);
        if (null == cam) {
            System.out.println("Attempting to remove a non-existent Camera");
            return;
        }
        virtualSpace.removeCamera(cam.getIndex());
        cameraMap.remove(id);
    }

    CameraGroup getCameraGroup() {
        return virtualSpace.getCameraGroup();
    }

    void removeAllGlyphs() {
        virtualSpace.removeAllGlyphs();
        glyphMap.clear();
    }

    public SlaveUpdater(VirtualSpace vs, boolean jmx) throws Exception {
        this.virtualSpace = vs;
        virtualSpace.setSlave(true);
        channel.connect(vs.getName());
        if (jmx) {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            JmxConfigurator.registerChannel(channel, server, "JGroupsChannel=" + channel.getChannelName(), "wild", true);
        }
        channel.setReceiver(new ReceiverAdapter() {

            @Override
            public void viewAccepted(View newView) {
                System.out.println("** view: " + newView);
            }

            @Override
            public void receive(Message msg) {
                if (!(msg.getObject() instanceof Delta)) {
                    System.out.println("wrong message type (Delta expected)");
                    return;
                }
                Delta delta = (Delta) msg.getObject();
                delta.apply(SlaveUpdater.this);
                VirtualSpaceManager.INSTANCE.repaintNow();
            }
        });
    }

    public SlaveUpdater(VirtualSpace vs) throws Exception {
        this(vs, false);
    }
}
