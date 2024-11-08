package pyrasun.eio.services.object;

import pyrasun.eio.EIOWorker;
import pyrasun.eio.EIOEvent;
import pyrasun.eio.Endpoint;
import pyrasun.eio.protocols.object.Packet;
import java.io.IOException;

public class ObjectProcessor implements EIOWorker {

    ObjectProcessor() {
    }

    public EIOEvent getWorkerType() {
        return (EIOEvent.PROCESS);
    }

    public Object handleEvent(EIOEvent event, Object object, Endpoint genEndpoint) throws IOException {
        ObjectServerClient client = (ObjectServerClient) genEndpoint.attachment();
        if (client == null) {
            System.out.println("Listener is null, we're screwed");
            return (null);
        }
        if (object == null) {
            System.out.println("OSS Processor has a null input :-(");
            return (null);
        }
        Packet packet = (Packet) object;
        client.newMessage(packet.getChannel(), packet.getObject());
        return (null);
    }
}
