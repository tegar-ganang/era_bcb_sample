package net.teqlo.components.standard.actionQueueV0_01;

import net.teqlo.bus.messages.mail.ActionQueueEntry;
import net.teqlo.components.AbstractActivity;
import net.teqlo.components.OutputSet;
import net.teqlo.db.ActivityLookup;
import net.teqlo.db.User;
import net.teqlo.db.XmlDatabase;
import net.teqlo.runtime.Context;

/**
 * Activity to receive an action from the Teqlo Action Queue
 */
public class ReceiveActivity extends AbstractActivity {

    public static final String RECORD_FIELD_KEY = "event";

    public static final String RECEIVED_OUTPUT_KEY = "Received";

    private ActionQueueEntry gate = null;

    public ReceiveActivity(User user, Context context, ActivityLookup al) {
        super(user, al);
        if (context.getGate() instanceof ActionQueueEntry) gate = (ActionQueueEntry) context.getGate();
    }

    protected void actionsOnRun() throws Exception {
        if (gate != null) {
            String key = RECEIVED_OUTPUT_KEY;
            String field = RECORD_FIELD_KEY;
            if (gate.getChannel() != null) {
                key += "." + gate.getChannel();
                field += "." + gate.getChannel();
            }
            OutputSet set = addOutputSet(key);
            set.put(field, gate.getData());
            XmlDatabase.getInstance().addStat(user.getUserFqn(), "receiveAction", gate.getTopic());
        }
    }
}
