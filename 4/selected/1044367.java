package org.jtools.rjdbc.server;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.SQLException;
import org.jpattern.io.Output;
import org.jtools.sql.values.Values;

class RJDBCSCallableStatement extends RJDBCSPreparedStatement<CallableStatement> {

    protected RJDBCSCallableStatement(Long id, CallableStatement delegate) throws SQLException {
        super(id, delegate);
        this.parameterMetaData.registerOutParameters(delegate);
    }

    @Override
    protected void writeCallableExecuteParameters(Output out) throws SQLException, IOException {
        new Values(this.parameterMetaData).read(getDelegate()).write(out);
    }
}
