package net.sf.chellow.physical;

import net.sf.chellow.data08.HhDatumRaw;
import net.sf.chellow.monad.DeployerException;
import net.sf.chellow.monad.DesignerException;
import net.sf.chellow.monad.Invocation;
import net.sf.chellow.monad.ProgrammerException;
import net.sf.chellow.monad.Urlable;
import net.sf.chellow.monad.UserException;
import net.sf.chellow.monad.types.MonadCharacter;
import net.sf.chellow.monad.types.MonadFloat;
import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.UriPathElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class HhDatum extends PersistentEntity implements Urlable {

    private Channel channel;

    private HhEndDate endDate;

    private float value;

    private Character status;

    public HhDatum() {
    }

    public HhDatum(Channel channel, HhDatumRaw datumRaw) throws UserException, ProgrammerException {
        setChannel(channel);
        setEndDate(datumRaw.getEndDate());
        setValue(datumRaw.getValue());
        setStatus(datumRaw.getStatus());
    }

    public Channel getChannel() {
        return channel;
    }

    void setChannel(Channel channel) {
        this.channel = channel;
    }

    public HhEndDate getEndDate() {
        return endDate;
    }

    void setEndDate(HhEndDate endDate) {
        this.endDate = endDate;
    }

    public float getValue() {
        return value;
    }

    void setValue(float value) {
        this.value = value;
    }

    public Character getStatus() {
        return status;
    }

    void setStatus(Character status) {
        this.status = status;
    }

    public void update(float value, Character status) throws UserException, ProgrammerException {
        this.value = value;
        this.status = new HhDatumStatus(status).getCharacter();
    }

    public Node toXML(Document doc) throws ProgrammerException, UserException {
        setTypeName("hh-datum");
        Element element = (Element) super.toXML(doc);
        element.appendChild(endDate.toXML(doc));
        element.setAttributeNode(MonadFloat.toXml(doc, "value", value));
        if (status != null) {
            element.setAttributeNode(MonadCharacter.toXml(doc, "status", status));
        }
        return element;
    }

    public String toString() {
        return "End date " + endDate + ", Value " + value + ", Status " + status;
    }

    public MonadUri getUri() throws ProgrammerException, UserException {
        return channel.getHhDataInstance().getUri().resolve(getUriId());
    }

    public Urlable getChild(UriPathElement urlId) throws ProgrammerException, UserException {
        throw UserException.newNotFound();
    }

    public void httpGet(Invocation inv) throws DesignerException, ProgrammerException, UserException, DeployerException {
    }

    public void httpPost(Invocation inv) throws ProgrammerException, UserException {
    }

    public void httpDelete(Invocation inv) throws ProgrammerException, UserException {
    }
}
