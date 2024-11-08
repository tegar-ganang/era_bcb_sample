package engine.boxes.effect;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import engine.api.MidiEffect;
import engine.boxes.effect.dispatch.ConditionEffect;
import engine.boxes.effect.dispatch.Conditions;
import engine.boxes.effect.dispatch.Expressions;
import engine.boxes.effect.dispatch.SimpleConditionEffect;

public class ChangeBank extends SimpleConditionEffect {

    public ChangeBank(int number) {
        super(new Conditions.ControlCondition(number));
    }

    public void onCondition(ShortMessage mes, long timeS) {
        int channel = mes.getChannel();
        int bank = mes.getData2();
        ShortMessage res = new ShortMessage();
        try {
            res.setMessage(ShortMessage.PROGRAM_CHANGE, channel, bank, -1);
        } catch (InvalidMidiDataException e) {
            throw new Error(e);
        }
        this.sendToAll(res, timeS);
    }
}
