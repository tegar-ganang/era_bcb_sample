package spindles.api.domain;

import static com.sleepycat.persist.model.DeleteAction.CASCADE;
import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;
import java.util.Date;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.SecondaryKey;

@Entity
public class SessionPart extends Interval {

    /**
	 * 
	 */
    private static final transient long serialVersionUID = -6260933357565658899L;

    @SecondaryKey(relate = MANY_TO_ONE)
    private String channel;

    @SecondaryKey(relate = MANY_TO_ONE, relatedEntity = SleepSession.class, onRelatedEntityDelete = CASCADE)
    private long sleepSessionID;

    public SessionPart() {
        super();
    }

    public SessionPart(Date from, String channel, long sleepSessionID) {
        super();
        this.setStart(from);
        this.channel = channel;
        this.sleepSessionID = sleepSessionID;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public long getSleepSessionID() {
        return sleepSessionID;
    }

    public void setSleepSessionID(long sleepSessionID) {
        this.sleepSessionID = sleepSessionID;
    }
}
