package admin.command;

import static db.DB.begin;
import static db.DB.commit;
import static db.DB.getAll;
import static db.DB.rollback;
import java.util.List;
import org.hibernate.Session;
import common.CmdLine;
import db.Channel;

public class ListChannel extends AbstractCommand {

    public ListChannel(Object app, CmdLine cmdLine) {
        super(app, cmdLine);
    }

    public String getName() {
        return "list-channel";
    }

    @SuppressWarnings("unchecked")
    public void run(Object params) {
        Session s = null;
        try {
            s = begin();
            List<Channel> chnls = getAll(s, Channel.class);
            for (Channel chnl : chnls) {
                System.out.println(chnl.getChannelName());
            }
            commit(s);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (s != null) {
                try {
                    rollback(s);
                } catch (Exception innerex) {
                }
            }
        }
    }
}
