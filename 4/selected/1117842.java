package lm;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;
import lm.LM_World.Agent;
import lm.LM_World.Sound;
import mms.EnvironmentAgent;
import mms.Event;
import mms.EventServer;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.sound.midi.MidiDevice.Info;

public class LM_SoundEventServer extends EventServer {

    LM_World world;

    Sound[][] newSounds;

    protected String agentName;

    protected String agentCompName;

    private int lastMidiNote = 84;

    ArrayList<Event> events = new ArrayList<Event>();

    Synthesizer synth;

    MidiChannel channel;

    Receiver rcv;

    public LM_SoundEventServer(EnvironmentAgent myAgent) {
        super(myAgent, "SOUND", null);
    }

    @Override
    public void init(Hashtable<String, String> parameters) {
        world = (LM_World) envAgent.getWorld();
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            MidiChannel[] channels = synth.getChannels();
            channel = channels[0];
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void setSound(int x, int y, int note, int amplitude, int direction) {
        if ((x >= 0 && x < LM_Constants.WorldSize) && (y >= 0 && y < LM_Constants.WorldSize)) {
            Sound sound = newSounds[x][y];
            if (amplitude > sound.amplitude) {
                sound.note = note;
                sound.amplitude = amplitude;
                sound.direction = direction;
                sound.propagated = true;
            }
        }
    }

    private void propagateSound(Sound sound, int i, int j, int note, int amplitude, int direction) {
        if ((i >= 0 && i < LM_Constants.WorldSize) && (j >= 0 && j < LM_Constants.WorldSize)) {
            Sound oldSound = world.squareLattice[i][j].sound;
            switch(sound.direction) {
                case LM_World.DIR_N:
                    if (!((oldSound.direction == LM_World.DIR_S || oldSound.direction == LM_World.DIR_SE || oldSound.direction == LM_World.DIR_SW) && (sound.amplitude < oldSound.amplitude || newSounds[i][j].direction != LM_World.DIR_NONE))) {
                        setSound(i, j, note, amplitude, direction);
                    }
                    break;
                case LM_World.DIR_NE:
                    if (!((oldSound.direction == LM_World.DIR_S || oldSound.direction == LM_World.DIR_SW || oldSound.direction == LM_World.DIR_W) && (sound.amplitude < oldSound.amplitude || newSounds[i][j].direction != LM_World.DIR_NONE))) {
                        setSound(i, j, note, amplitude, direction);
                    }
                    break;
                case LM_World.DIR_E:
                    if (!((oldSound.direction == LM_World.DIR_SW || oldSound.direction == LM_World.DIR_W || oldSound.direction == LM_World.DIR_NW) && (sound.amplitude < oldSound.amplitude || newSounds[i][j].direction != LM_World.DIR_NONE))) {
                        setSound(i, j, note, amplitude, direction);
                    }
                    break;
                case LM_World.DIR_SE:
                    if (!((oldSound.direction == LM_World.DIR_W || oldSound.direction == LM_World.DIR_NW || oldSound.direction == LM_World.DIR_N) && (sound.amplitude < oldSound.amplitude || newSounds[i][j].direction != LM_World.DIR_NONE))) {
                        setSound(i, j, note, amplitude, direction);
                    }
                    break;
                case LM_World.DIR_S:
                    if (!((oldSound.direction == LM_World.DIR_NW || oldSound.direction == LM_World.DIR_N || oldSound.direction == LM_World.DIR_NE) && (sound.amplitude < oldSound.amplitude || newSounds[i][j].direction != LM_World.DIR_NONE))) {
                        setSound(i, j, note, amplitude, direction);
                    }
                    break;
                case LM_World.DIR_SW:
                    if (!((oldSound.direction == LM_World.DIR_N || oldSound.direction == LM_World.DIR_NE || oldSound.direction == LM_World.DIR_E) && (sound.amplitude < oldSound.amplitude || newSounds[i][j].direction != LM_World.DIR_NONE))) {
                        setSound(i, j, note, amplitude, direction);
                    }
                    break;
                case LM_World.DIR_W:
                    if (!((oldSound.direction == LM_World.DIR_NE || oldSound.direction == LM_World.DIR_E || oldSound.direction == LM_World.DIR_SE) && (sound.amplitude < oldSound.amplitude || newSounds[i][j].direction != LM_World.DIR_NONE))) {
                        setSound(i, j, note, amplitude, direction);
                    }
                    break;
                case LM_World.DIR_NW:
                    if (!((oldSound.direction == LM_World.DIR_S || oldSound.direction == LM_World.DIR_SE || oldSound.direction == LM_World.DIR_E) && (sound.amplitude < oldSound.amplitude || newSounds[i][j].direction != LM_World.DIR_NONE))) {
                        setSound(i, j, note, amplitude, direction);
                    }
                    break;
            }
        }
    }

    @Override
    public void process() {
        newSounds = new Sound[LM_Constants.WorldSize][LM_Constants.WorldSize];
        for (int i = 0; i < newSounds.length; i++) {
            for (int j = 0; j < newSounds[i].length; j++) {
                newSounds[i][j] = world.new Sound();
            }
        }
        for (int i = 0; i < world.squareLattice.length; i++) {
            for (int j = 0; j < world.squareLattice[i].length; j++) {
                Sound sound = world.squareLattice[i][j].sound;
                switch(sound.direction) {
                    case LM_World.DIR_N:
                        propagateSound(sound, i - 1, j, sound.note, sound.amplitude - 1, LM_World.DIR_N);
                        break;
                    case LM_World.DIR_NE:
                        propagateSound(sound, i - 1, j, sound.note, sound.amplitude - 1, LM_World.DIR_N);
                        propagateSound(sound, i - 1, j + 1, sound.note, sound.amplitude - 1, LM_World.DIR_NE);
                        propagateSound(sound, i, j + 1, sound.note, sound.amplitude - 1, LM_World.DIR_E);
                        break;
                    case LM_World.DIR_E:
                        propagateSound(sound, i, j + 1, sound.note, sound.amplitude - 1, LM_World.DIR_E);
                        break;
                    case LM_World.DIR_SE:
                        propagateSound(sound, i, j + 1, sound.note, sound.amplitude - 1, LM_World.DIR_E);
                        propagateSound(sound, i + 1, j + 1, sound.note, sound.amplitude - 1, LM_World.DIR_SE);
                        propagateSound(sound, i + 1, j, sound.note, sound.amplitude - 1, LM_World.DIR_S);
                        break;
                    case LM_World.DIR_S:
                        propagateSound(sound, i + 1, j, sound.note, sound.amplitude - 1, LM_World.DIR_S);
                        break;
                    case LM_World.DIR_SW:
                        propagateSound(sound, i + 1, j, sound.note, sound.amplitude - 1, LM_World.DIR_S);
                        propagateSound(sound, i + 1, j - 1, sound.note, sound.amplitude - 1, LM_World.DIR_SW);
                        propagateSound(sound, i, j - 1, sound.note, sound.amplitude - 1, LM_World.DIR_W);
                        break;
                    case LM_World.DIR_W:
                        propagateSound(sound, i, j - 1, sound.note, sound.amplitude - 1, LM_World.DIR_W);
                        break;
                    case LM_World.DIR_NW:
                        propagateSound(sound, i, j - 1, sound.note, sound.amplitude - 1, LM_World.DIR_W);
                        propagateSound(sound, i - 1, j - 1, sound.note, sound.amplitude - 1, LM_World.DIR_NW);
                        propagateSound(sound, i - 1, j, sound.note, sound.amplitude - 1, LM_World.DIR_N);
                        break;
                }
            }
        }
        for (Event evt : events) {
            int note = Integer.parseInt(evt.content);
            Agent agent = world.agents.get(evt.oriAgentName);
            setSound(agent.pos_x - 1, agent.pos_y, note, LM_Constants.SoundRadius, LM_World.DIR_N);
            setSound(agent.pos_x - 1, agent.pos_y + 1, note, LM_Constants.SoundRadius, LM_World.DIR_NE);
            setSound(agent.pos_x, agent.pos_y + 1, note, LM_Constants.SoundRadius, LM_World.DIR_E);
            setSound(agent.pos_x + 1, agent.pos_y + 1, note, LM_Constants.SoundRadius, LM_World.DIR_SE);
            setSound(agent.pos_x + 1, agent.pos_y, note, LM_Constants.SoundRadius, LM_World.DIR_S);
            setSound(agent.pos_x + 1, agent.pos_y - 1, note, LM_Constants.SoundRadius, LM_World.DIR_SW);
            setSound(agent.pos_x, agent.pos_y - 1, note, LM_Constants.SoundRadius, LM_World.DIR_W);
            setSound(agent.pos_x - 1, agent.pos_y - 1, note, LM_Constants.SoundRadius, LM_World.DIR_NW);
        }
        events.clear();
        for (int i = 0; i < world.squareLattice.length; i++) {
            for (int j = 0; j < world.squareLattice[i].length; j++) {
                world.squareLattice[i][j].sound = newSounds[i][j];
            }
        }
        Set<String> set = sensors.keySet();
        for (String sensor : set) {
            String[] str = sensor.split(":");
            Agent agent = world.agents.get(str[0]);
            if (world.squareLattice[agent.pos_x][agent.pos_y].sound.direction != LM_World.DIR_NONE) {
                agentName = str[0];
                agentCompName = str[1];
                act();
            }
        }
    }

    @Override
    public void processSense(Event evt) {
        events.add(evt);
        float lp = Float.valueOf(envAgent.agentsPublicFacts.get(evt.oriAgentName + ":" + "ListeningPleasure"));
        float L = LM_Constants.MaxSoundGenomeLength;
        float L_2 = L * L;
        int velocity = Math.round(20f + ((lp + (1 / L_2)) * (100f - 80f) / (L - (1 / L_2))));
        if (velocity > 100) {
            velocity = 100;
        } else if (velocity < 20) {
            velocity = 20;
        }
        Agent agent = world.agents.get(evt.oriAgentName);
        int midiNote = 84;
        int agentNote = Integer.valueOf(evt.content);
        if (LM_Constants.AbsoluteNoteMapping) {
            midiNote = 84 + agentNote;
        } else {
            int localLastNote;
            if (LM_Constants.GlobalInterval) {
                localLastNote = lastMidiNote;
            } else {
                localLastNote = agent.lastSungMidiNote;
            }
            if (agent.direction >= 0 && agent.direction <= 3) {
                midiNote = localLastNote - agentNote;
            } else {
                midiNote = localLastNote + agentNote;
            }
            midiNote = LM_Constants.MinMidiNote + ((midiNote - LM_Constants.MinMidiNote) % (LM_Constants.MaxMidiNote + 1 - LM_Constants.MinMidiNote));
        }
        lastMidiNote = midiNote;
        agent.lastSungMidiNote = midiNote;
        channel.noteOn(midiNote, velocity);
    }

    @Override
    public Event processAction() {
        Event event = new Event();
        event.destAgentName = agentName;
        event.destAgentCompName = agentCompName;
        Agent agent = world.agents.get(agentName);
        event.content = world.squareLattice[agent.pos_x][agent.pos_y].sound.note + " " + world.squareLattice[agent.pos_x][agent.pos_y].sound.amplitude + " " + world.squareLattice[agent.pos_x][agent.pos_y].sound.direction;
        return event;
    }
}
