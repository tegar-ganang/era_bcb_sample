package lisppaste;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.WorkRequest;
import java.util.Vector;
import java.util.List;
import java.net.URL;

public class ChannelListRequest extends WorkRequest {

    public ChannelListRequest(View view) {
        this.view = view;
    }

    public void run() {
        setStatus(jEdit.getProperty("channel-list-request.status"));
        String url = jEdit.getProperty("lisp-paste-request.url");
        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(url));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            list = (List) client.execute("listchannels", new Vector());
        } catch (Exception e) {
            Log.log(Log.ERROR, this, e);
            VFSManager.error(view, url, "lisp-paste-request.error", new String[] { e.toString() });
        }
    }

    public List getChannelList() {
        return list;
    }

    private View view;

    private List list;
}
