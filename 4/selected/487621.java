package syntelos.tools;

import syntelos.sys.Init;
import alto.sys.Reference;
import alto.sys.Thread;

/**
 * 
 */
public class Configure extends syntelos.sys.Init {

    public static void usage(java.io.PrintStream out) {
        out.println();
        out.println("Overview");
        out.println();
        out.println("  This tool reads and writes text configuration files");
        out.println("  from 'Sys.Init' locations in the local store.");
        out.println();
        out.println("  The 'Sys.Init' facility starts threads.  It uses");
        out.println("  a file format based on headers, and a feature set ");
        out.println("  defined in 'syntelos.sys.Init'.");
        out.println();
        out.println("  A location is defined by arbitrarily selecting a ");
        out.println("  location string.  As these location strings are ");
        out.println("  thread names, they typically look like the names of ");
        out.println("  other threads, and the names of system statistics. ");
        out.println();
        out.println();
        out.println("Synopsis");
        out.println();
        out.println("  Configure  target ");
        out.println();
        out.println("Description");
        out.println();
        out.println("  Read 'Sys.Init' configuration from target to stdout.");
        out.println();
        out.println();
        out.println("Synopsis");
        out.println();
        out.println("  Configure  target  source");
        out.println();
        out.println("Description");
        out.println();
        out.println("  Write 'Sys.Init' configuration from source to target.");
        out.println("  Source is a local file name or URL.");
        out.println();
        out.println();
        out.println("SX Server HTTP");
        out.println();
        out.println("  The SX HTTP server configuration target is ");
        out.println("      SX/Server/HTTP");
        out.println();
        System.exit(1);
    }

    public static void main(String[] argv) {
        if (Thread.In()) {
            if (SInitServer()) {
                int argc = argv.length;
                if (2 == argc) {
                    Reference target = Init.Resources.ReferenceTo(argv[0]);
                    Reference source = (new Reference(argv[1])).toRemote();
                    try {
                        if (target.put(source)) {
                            System.out.println(target.toString());
                            System.exit(0);
                        } else {
                            System.err.println("Error bad address in target '" + target + "'.");
                            System.exit(1);
                        }
                    } catch (Exception any) {
                        any.printStackTrace();
                        System.exit(1);
                    }
                } else if (1 == argc) {
                    Reference target = Init.Resources.ReferenceTo(argv[0]);
                    Reference source = (new Reference("system:out")).toRemote();
                    try {
                        if (target.get(source)) {
                            System.exit(0);
                        } else {
                            System.err.println("Error bad address in target '" + target + "'.");
                            System.exit(1);
                        }
                    } catch (Exception any) {
                        any.printStackTrace();
                        System.exit(1);
                    }
                } else usage(System.err);
            } else {
                System.err.println(FailedSInitServer);
                System.exit(1);
            }
        } else {
            new Main(Configure.class, argv).start();
        }
    }
}
