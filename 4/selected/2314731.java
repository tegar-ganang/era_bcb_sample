package com.frinika.client.frinika;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import javax.sound.midi.MidiDevice;
import com.frinika.client.Context;
import com.frinika.ejb.LaneEJB;
import com.frinika.ejb.PartEJB;
import com.frinika.ejb.PartResourceEJB;
import com.frinika.ejb.SongEJB;
import com.frinika.ejb.VoiceInfo;
import com.frinika.ejb.VoiceResourceEJB;
import com.frinika.project.MidiDeviceDescriptor;
import com.frinika.project.ProjectContainer;
import com.frinika.project.gui.ProjectFrame;
import com.frinika.sequencer.gui.mixer.SynthWrapper;
import com.frinika.sequencer.model.Lane;
import com.frinika.sequencer.model.MidiLane;
import com.frinika.sequencer.model.MidiPart;
import com.frinika.sequencer.model.MultiEvent;
import com.frinika.sequencer.model.Part;
import com.frinika.synth.SynthRack;

public class FrinikaSongWrapper {

    ProjectFrame projectFrame = null;

    SongEJB songEJB = null;

    Context context;

    /**
	 * Create a wrapper by loading from the database
	 * 
	 * @param songEJB
	 *            songEntity to be loaded
	 * @param context
	 */
    FrinikaSongWrapper(SongEJB songEJB, Context context) {
        this.context = context;
        this.songEJB = songEJB;
        loadFromDB();
    }

    /**
	 * create a wrapper from an frinika ProjectFrame
	 * 
	 * @param projectFrame
	 * @param context
	 */
    public FrinikaSongWrapper(ProjectFrame projectFrame, Context context) {
        this.context = context;
        this.projectFrame = projectFrame;
        songEJB = null;
    }

    @SuppressWarnings("unchecked")
    void loadFromDB() {
        assert (projectFrame == null);
        ProjectContainer project = null;
        try {
            project = new ProjectContainer();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        try {
            project = new ProjectContainer();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        if (songEJB.getVoiceResources() != null) {
            for (VoiceResourceEJB resource : songEJB.getVoiceResources()) {
                resource = context.loadVoiceResource(resource.getId());
                if (resource instanceof VoiceResourceEJB) {
                    MidiDeviceDescriptor dev = (MidiDeviceDescriptor) resource.getObject();
                    System.out.println("MidiDeviceDescriptor: serializable  midiDev " + dev.getSerializableMidiDevice() + "  " + dev.getMidiDevice());
                    project.getMidiDeviceDescriptors().add(dev);
                }
            }
        }
        project.installMidiDevices();
        for (LaneEJB laneEJB : songEJB.getLanes()) {
            MidiLane lane = project.createMidiLane();
            MidiDeviceVoiceInfo voiceInfo = (MidiDeviceVoiceInfo) laneEJB.getVoiceInfo();
            if (voiceInfo != null) {
                for (Long id : voiceInfo.getResourceList()) {
                    VoiceResourceEJB resourceEJB = context.loadVoiceResource(id);
                    MidiDeviceDescriptor dev = (MidiDeviceDescriptor) resourceEJB.getObject();
                    if (dev != null) {
                        lane.getTrack().setMidiDevice(dev.getMidiDevice());
                        lane.setMidiChannel(voiceInfo.getChannel());
                        lane.setProgram(voiceInfo.getMyPatch());
                    }
                }
            }
            for (PartEJB partEJB : laneEJB.getParts()) {
                MidiPart part = new MidiPart(lane);
                long t1 = partEJB.getTime();
                long dur = partEJB.getDuration();
                part.setStartTick(t1);
                part.setEndTick(t1 + dur);
                long dataID = partEJB.getResource().getId();
                PartResourceEJB dataEJB = context.loadPartResource(dataID);
                Object o = dataEJB.getObject();
                if (o instanceof ArrayList) {
                    ArrayList<MultiEvent> list = (ArrayList<MultiEvent>) o;
                    for (MultiEvent ev : list) {
                        MultiEvent clone;
                        try {
                            clone = (MultiEvent) ev.clone();
                            clone.deepMove(t1);
                            part.add(clone);
                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println(" ooops did not expect to see a: " + o);
                }
            }
        }
        project.getProjectLane().onLoad();
        project.rebuildGUI();
        try {
            projectFrame = new ProjectFrame(project);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Save a SongEJB representation from the current frinika project
	 * 
	 * @return child new song
	 */
    void saveSong() {
        ProjectContainer project = projectFrame.getProjectContainer();
        SongEJB childEJB = new SongEJB();
        if (songEJB != null) {
            System.out.println(" Parent is " + songEJB.getId());
            childEJB.setParent(songEJB);
        }
        if (project.getFile() != null) {
            childEJB.setTitle(project.getFile().getName());
        } else {
            childEJB.setTitle("Untitled");
        }
        ArrayList<LaneEJB> laneList = new ArrayList<LaneEJB>();
        List<MidiDeviceDescriptor> midiDeviceDescriptors = project.getMidiDeviceDescriptors();
        List<VoiceResourceEJB> voiceEJBS = new ArrayList<VoiceResourceEJB>();
        HashMap<MidiDevice, Long> map = new HashMap<MidiDevice, Long>();
        if (midiDeviceDescriptors != null) {
            List<VoiceResourceEJB> voiceResources = new ArrayList<VoiceResourceEJB>();
            System.out.println(" Saving " + midiDeviceDescriptors.size() + " descriptors ");
            for (MidiDeviceDescriptor dev : midiDeviceDescriptors) {
                MidiDevice midiDev = dev.getMidiDevice();
                if (dev.getMidiDevice() == null) {
                    continue;
                }
                if (midiDev instanceof SynthRack) {
                    ((SynthRack) midiDev).setSaveReferencedData(false);
                }
                if (midiDev instanceof SynthWrapper) {
                    ((SynthWrapper) midiDev).setSaveReferencedData(false);
                }
                VoiceResourceEJB midiEJB = new VoiceResourceEJB();
                voiceEJBS.add(midiEJB);
                midiEJB.setObject(dev);
                System.out.println("  device: " + dev);
                context.saveVoiceResource(midiEJB);
                map.put(dev.getMidiDevice(), midiEJB.getId());
                voiceResources.add(midiEJB);
            }
            childEJB.setVoiceResources(voiceResources);
        }
        for (Lane lane : project.getLanes()) {
            if (lane instanceof MidiLane) {
                MidiLane ml = (MidiLane) lane;
                LaneEJB laneEJB = createLaneEJB(ml);
                laneList.add(laneEJB);
                if (ml.getMidiDevice() != null) {
                    Long devId = map.get(ml.getMidiDevice());
                    VoiceInfo voiceInfo = new MidiDeviceVoiceInfo(devId, ml.getMidiChannel(), ml.getProgram().hashCode());
                    laneEJB.setVoiceInfo(voiceInfo);
                }
            }
        }
        childEJB.setLanes(laneList);
        context.saveSongEJB(childEJB);
        songEJB = childEJB;
    }

    /**
	 * 
	 * @param lane
	 * @param resourceList
	 * @return
	 */
    private LaneEJB createLaneEJB(MidiLane lane) {
        LaneEJB laneEJB = new LaneEJB();
        ArrayList<PartEJB> partList = new ArrayList<PartEJB>();
        for (Part part : lane.getParts()) {
            if (part instanceof MidiPart) {
                partList.add(createPartEJB((MidiPart) part));
            }
        }
        laneEJB.setParts(partList);
        return laneEJB;
    }

    private PartEJB createPartEJB(MidiPart part) {
        long t1 = part.getStartTick();
        long duration = part.getDurationInTicks();
        Collection<MultiEvent> list = part.getMultiEvents();
        ArrayList<MultiEvent> eventList = new ArrayList<MultiEvent>();
        for (MultiEvent e : list) {
            MultiEvent c = e.detachedCopy();
            c.deepMove(-t1);
            eventList.add(c);
        }
        PartResourceEJB data = new PartResourceEJB();
        data.setObject(eventList);
        context.savePartResource(data);
        PartEJB partEJB = new PartEJB();
        partEJB.setResource(data);
        partEJB.setTime(t1);
        partEJB.setDuration(duration);
        return partEJB;
    }

    public ProjectContainer getProjectContainer() {
        return projectFrame.getProjectContainer();
    }
}
