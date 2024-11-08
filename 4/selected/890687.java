package org.maverickdbms.util;

import org.maverickdbms.basic.ConstantString;
import org.maverickdbms.basic.MaverickException;
import org.maverickdbms.basic.Factory;
import org.maverickdbms.basic.Program;
import org.maverickdbms.basic.Session;
import org.maverickdbms.basic.MaverickString;

public class VERSION implements Program {

    VERSION() {
    }

    public String getSpecificationVersion() {
        return getClass().getPackage().getSpecificationVersion();
    }

    public String getImplementationVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    public ConstantString run(Session session, MaverickString[] args) throws MaverickException {
        String spec = getSpecificationVersion();
        String build = getImplementationVersion();
        session.getChannel(Session.SCREEN_CHANNEL).PRINT(session.getFactory().getConstant("MaVerick version " + spec + " (build " + build + ")"), true, session.getStatus());
        return ConstantString.RETURN_SUCCESS;
    }

    public static void main(String[] args) {
        Session session = new Session();
        Program version = new VERSION();
        Factory factory = session.getFactory();
        MaverickString[] mvargs = new MaverickString[args.length];
        for (int i = 0; i < args.length; i++) {
            mvargs[i] = factory.getString();
            mvargs[i].set(args[i]);
        }
        session.EXECUTE(null, version, factory.getConstant("VERSION"), mvargs);
        System.exit(0);
    }
}
