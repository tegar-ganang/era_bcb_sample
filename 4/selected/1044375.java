package org.guberno.server.dispatcher;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.guberno.server.configuration.Configuration;
import org.guberno.server.core.RemoteProcedureCall;
import org.guberno.server.types.ValidationException;

/**
 * This is the dispatcher for changing configuration values.
 * @author daff
 */
public class ConfigurationDispatcher implements RpcDispatcher {

    private Configuration environment;

    private Logger log = Logger.getLogger(this.getClass());

    @Override
    public String handleRpc(RemoteProcedureCall rpc) throws ValidationException, DispatchException {
        return handleRpc(rpc, environment);
    }

    @Override
    public String handleRpc(RemoteProcedureCall rpc, Configuration environment) throws ValidationException, DispatchException {
        if (environment == null) throw new DispatchException("No configuration environment given! Can't handle request!");
        rpc.validate();
        log.info("Dispatching RemoteProcedureCall " + rpc.toString());
        this.environment = environment;
        if (rpc.getMethod().getName().equals("set")) {
            try {
                String key = rpc.getArguments().get(0).toString();
                if (System.getenv().containsKey(key)) throw new DispatchException("Configuration key " + key + " is not allowed since it exists already in the system enrivonmen variables. You are not allowed to overwrite system environment variables.");
                String value = rpc.getArguments().get(1).toString();
                environment.put(key, value);
                environment.saveToStorage();
                return key + "=" + value;
            } catch (IOException e) {
                throw new DispatchException("Could not save configuration!");
            }
        } else if (rpc.getMethod().getName().equals("get")) {
            String key = rpc.getArguments().get(0).toString();
            String result = environment.get(key);
            if (result == null) throw new DispatchException("Key " + key + " doesn't exist");
            return result;
        } else if (rpc.getMethod().getName().equals("list")) {
            String result = "";
            for (Iterator<Entry<String, String>> iter = environment.entrySet().iterator(); iter.hasNext(); ) {
                Entry<String, String> current = iter.next();
                result += current.getKey() + "=" + current.getValue();
                if (iter.hasNext()) result += "\n";
            }
            return result;
        } else throw new DispatchException("Method not supported!");
    }
}
