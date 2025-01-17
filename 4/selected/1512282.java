package jorgan.disposition;

import java.util.List;
import jorgan.disposition.Output.OutputMessage;
import jorgan.midi.mpl.Command;
import jorgan.midi.mpl.NoOp;
import jorgan.midi.mpl.ProcessingException;
import jorgan.midi.mpl.Set;
import jorgan.util.Null;

/**
 * A rank.
 */
public class Rank extends Engageable {

    private Command channel = new NoOp();

    private int delay = 0;

    public Rank() {
        addMessage(new Engaged().change(new Set(176), new Set(121), new NoOp()));
        addMessage(new Engaged().change(new Set(176), new Set(0), new Set(0)));
        addMessage(new Engaged().change(new Set(192), new Set(0), new NoOp()));
        addMessage(new NotePlayed().change(new Set(144), new Set(NotePlayed.PITCH), new Set(NotePlayed.VELOCITY)));
        addMessage(new NoteMuted().change(new Set(128), new Set(NoteMuted.PITCH), new NoOp()));
        addMessage(new Disengaged().change(new Set(176), new Set(123), new NoOp()));
    }

    /**
	 * Convenience method to set the midi program.
	 * 
	 * @param program
	 *            program to set
	 */
    public void setProgram(int program) {
        Engaged engaged = getProgramChange();
        if (engaged == null) {
            engaged = new Engaged();
            addMessage(engaged);
        }
        engaged.change(new Set(192), new Set(program), new NoOp());
    }

    /**
	 * Convenience method to set the midi bank.
	 * 
	 * @param bank
	 *            bank to set
	 */
    public void setBank(int bank) {
        Engaged engaged = getBankSelect();
        if (engaged == null) {
            engaged = new Engaged();
            addMessage(engaged);
        }
        engaged.change(new Set(176), new Set(0), new Set(bank));
    }

    public void setVelocity(int velocity) {
        NotePlayed notePlayed = getNotePlayed();
        if (notePlayed == null) {
            notePlayed = new NotePlayed();
            addMessage(notePlayed);
        }
        notePlayed.change(new Set(144), new Set("pitch"), new Set(velocity));
    }

    /**
	 * Get the {@link Engaged} message sending a midi program change.
	 * 
	 * @return program change message
	 * @throws ProcessingException
	 */
    private Engaged getProgramChange() {
        for (Engaged engaged : getMessages(Engaged.class)) {
            if (new Set(192).equals(engaged.get(Message.STATUS))) {
                return engaged;
            }
        }
        return null;
    }

    /**
	 * Get the {@link Engaged} message sending a midi bank select.
	 * 
	 * @return bank select message
	 * @throws ProcessingException
	 */
    private Engaged getBankSelect() {
        for (Engaged engaged : getMessages(Engaged.class)) {
            if (new Set(176).equals(engaged.get(Message.STATUS)) && new Set(0).equals(engaged.get(Message.DATA1))) {
                return engaged;
            }
        }
        return null;
    }

    /**
	 * Get the {@link NotePlayed} message.
	 * 
	 * @return bank select message
	 */
    private NotePlayed getNotePlayed() {
        for (NotePlayed notePlayed : getMessages(NotePlayed.class)) {
            return notePlayed;
        }
        return null;
    }

    @Override
    protected boolean canReference(Class<? extends Element> clazz) {
        return SoundFilter.class.isAssignableFrom(clazz) || Sound.class.isAssignableFrom(clazz);
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        if (this.delay != delay) {
            int oldDelay = this.delay;
            if (delay < 0) {
                throw new IllegalArgumentException("delay '" + delay + "'");
            }
            this.delay = delay;
            fireChange(new PropertyChange(oldDelay, this.delay));
        }
    }

    public Command getChannel() {
        return channel;
    }

    public void setChannel(Command channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }
        if (!Null.safeEquals(this.channel, channel)) {
            Command oldChannel = this.channel;
            this.channel = channel;
            fireChange(new PropertyChange(oldChannel, this.channel));
        }
    }

    public List<Class<? extends Message>> getMessageClasses() {
        List<Class<? extends Message>> classes = super.getMessageClasses();
        classes.add(Engaged.class);
        classes.add(Disengaged.class);
        classes.add(NotePlayed.class);
        classes.add(NoteMuted.class);
        return classes;
    }

    public static class Engaged extends OutputMessage {
    }

    public static class NotePlayed extends OutputMessage {

        public static final String VELOCITY = "velocity";

        public static final String PITCH = "pitch";
    }

    public static class NoteMuted extends OutputMessage {

        public static final String PITCH = "pitch";
    }

    public static class Disengaged extends OutputMessage {
    }
}
