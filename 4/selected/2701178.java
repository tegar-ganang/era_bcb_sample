package de.jochenbrissier.backyard.web;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import de.jochenbrissier.backyard.core.Backyard;
import de.jochenbrissier.backyard.core.Channel;
import de.jochenbrissier.backyard.core.ChannelImpl;

@Path("/channel")
public class ChannelService {

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public List<ChannelImpl> getChannel() {
        List<ChannelImpl> cil = new ArrayList<ChannelImpl>();
        for (Channel ch : Backyard.channelhandler.getAllChannel()) {
            cil.add((ChannelImpl) ch);
        }
        return cil;
    }
}
