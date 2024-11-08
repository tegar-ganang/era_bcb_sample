package org.swemas.data.sql;

import java.lang.reflect.InvocationTargetException;
import org.swemas.core.Module;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.kernel.IKernel;

/**
 * @author Alexey Chernov
 * 
 */
public class SqlUsingModule extends Module {

    /**
	 * @param kernel
	 * @throws InvocationTargetException
	 */
    public SqlUsingModule(IKernel kernel) throws InvocationTargetException {
        super(kernel);
        try {
            _isql = (ISqlChannel) kernel().getChannel(ISqlChannel.class);
        } catch (ModuleNotFoundException e) {
            throw new InvocationTargetException(e);
        }
    }

    protected ISqlChannel isql() {
        return _isql;
    }

    ;

    protected DbConnection dbconn() {
        return _conn;
    }

    ;

    private DbConnection _conn;

    private ISqlChannel _isql;
}
