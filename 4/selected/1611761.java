package szene.display.event;

import java.util.Vector;
import szene.display.AbstractDisplayThread;
import szene.display.Waitscreen;
import szene.main.AbstractSzene1Client;
import szene.main.Szene1Client;
import weblife.datamodel.Event;
import weblife.object.ObjectEvent;
import weblife.section.SectionEvent;
import weblife.server.RequestInterface;
import weblife.xml.ErrorcodeException;
import de.enough.polish.ui.Command;
import de.enough.polish.ui.Displayable;
import de.enough.polish.ui.Form;

/**
 * Eventliste f�r spezifischen Tag
 * @author Knoll
 *
 */
class Eventlist extends AbstractDisplayThread {

    private Eventcalendar instance;

    private RequestInterface rq;

    private Szene1Client client;

    private String date;

    private Form eventlist;

    private SectionEvent eventsection;

    private Command Cmd_Details = new Command("Details", Command.OK, 0);

    private Vector eventvektor;

    private EventcalendarSettings settings;

    Eventlist(Szene1Client client, Eventcalendar instance, RequestInterface rq, EventcalendarSettings settings) {
        super(client);
        this.instance = instance;
        this.rq = rq;
        this.client = client;
        this.date = this.instance.GetDate();
        this.settings = settings;
        eventlist = new Form("Events (" + date + ")");
        eventlist.addCommand(AbstractSzene1Client.backCmd);
        eventlist.setCommandListener(this);
        new Waitscreen(this.client, this.eventlist);
        this.start();
    }

    public void run() {
        try {
            this.eventsection = new SectionEvent(rq);
            ObjectEvent eventobject;
            if (settings.getPlace().length() < 1) eventobject = eventsection.Methode_Geteventlist(date, settings.getPage(), settings.getLimit(), settings.getRelevance(), settings.getChannelid(), settings.getOrder(), settings.getOrderdirection(), settings.getLoadflyers()); else eventobject = eventsection.Methode_Geteventlist(date, settings.getPage(), settings.getLimit(), settings.getRelevance(), settings.getChannelid(), settings.getOrder(), settings.getOrderdirection(), settings.getLoadflyers(), settings.getPlace(), settings.getRadius());
            ObjectEvent eventobject2 = eventsection.Methode_Geteventcategories("de_AT");
            eventvektor = eventobject.getVector_event();
            for (int i = 0; i < eventvektor.size(); i++) {
                Event event = (Event) eventvektor.elementAt(i);
                String category = event.getCategoryid();
                category = eventobject2.getCategoryName(category);
                EventItem eventitem = new EventItem(event.getName(), event.getLocation(), event.getCity(), category);
                eventitem.addCommand(this.Cmd_Details);
                this.eventlist.append(eventitem);
            }
            if (!isCancel()) this.client.setCurrent(eventlist);
        } catch (ErrorcodeException e) {
            if (!isCancel()) {
                this.client.setCurrent(eventlist);
                AlertError(e.getErrormassage());
            }
        }
    }

    public void commandAction(Command cmd, Displayable dsp) {
        if (cmd == AbstractSzene1Client.backCmd) {
            Cancel();
            this.instance.Back();
        } else if (cmd == this.Cmd_Details) {
            Event event = (Event) eventvektor.elementAt(this.eventlist.getCurrentIndex());
            new Eventpage(client, rq, event.getId(), this.eventlist);
        }
    }
}
