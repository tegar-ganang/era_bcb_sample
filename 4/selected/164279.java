package hambo.svc.log.enhydra;

import com.lutris.logging.Logger;
import hambo.svc.*;
import hambo.svc.log.*;

/**
 * Implements an Enhydra based log service.
 */
public class EnhydraLogService implements LogService {

    public EnhydraLogService(ClientIdentity cid) {
    }

    /**
   * Returns a log object for the given category.
   */
    public Log getLog(String category) {
        return new EnhydraLog(Logger.getCentralLogger().getChannel(category));
    }
}
