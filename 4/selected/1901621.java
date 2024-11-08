package edu.ucsd.osdt.sink;

import edu.ucsd.osdt.util.RBNBBase;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.ChannelMap;
import java.util.logging.Logger;

public class BaseSink extends com.rbnb.sapi.Sink {

    private String serverAddress;

    private static Logger logger;

    public BaseSink() {
        super();
    }

    public BaseSink(String varServerAddress) {
        super();
        serverAddress = varServerAddress;
    }

    public ChannelMap getChannelMap() throws SAPIException {
        ChannelMap retval = new ChannelMap();
        RequestRegistration(retval);
        return retval;
    }
}
