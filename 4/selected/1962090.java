package de.jochenbrissier.backyard.web;

import java.util.ArrayList;
import java.util.Collection;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import de.jochenbrissier.backyard.core.Backyard;
import de.jochenbrissier.backyard.core.Member;
import de.jochenbrissier.backyard.core.MemberImpl;

@Path("/member")
public class MemberService {

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Collection<MemberImpl> getMember(@QueryParam("channel") String channel) {
        Collection<MemberImpl> list = new ArrayList<MemberImpl>();
        for (Member m : Backyard.channelhandler.getChannel(channel).getMembers()) {
            list.add((MemberImpl) m);
        }
        return list;
    }
}
