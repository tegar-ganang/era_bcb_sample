package engine.boxes.effect;

import javax.sound.midi.ShortMessage;
import engine.boxes.effect.dispatch.Condition;
import engine.boxes.effect.dispatch.Conditions;

public class ChannelFilter extends Filter {

    public ChannelFilter(final int channel) {
        super(new Conditions.ShortMsgCondition() {

            public boolean canPass(ShortMessage message) {
                return message.getChannel() == channel;
            }
        });
    }
}
