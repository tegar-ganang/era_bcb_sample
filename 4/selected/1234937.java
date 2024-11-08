package urban;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Command-line version of the compiler.
 */
public class Main {

    private static String usage = "Usage is urban --[sim|compile|cflow] file.urb\n  if only file.urb is specified then simplx will not be executed, --version print simplx version\n  --sim name of the kappa file to simulate\n  --time (infinite): time units of computation\n  --event (infinite): number of rule applications\n  --points number of data points per plots)\n  --rescale (1.0): rescaling factor (eg. '10.0' or '0.10')\n  --output-final-state output final state\n  --plot Creates a file containing the simulation data in space separated format\n  --gp Requires the creation of a gnuplot readable file at the end on the simulation\n  --compile name of the kappa file to compile\n  --cflow name of the kappa file to analyse\n  --no-compression do not compress stories\n  --weak-compression use only weak compression to classify stories\n  --iteration (1): number of stories to be searched for (with --storify option only)\n  --init (0.0): start taking measures (stories) at indicated time\n  --no-arrow-closure do not perform arrows transitive closure when displaying stories\n  --quotient-refinements replace each rule by the most general rule it is a refinement of when computing stories\n  --dot-output dot output for stories\n  --generate-map name of the kappa file for which the influence map should be computed\n  --no-inhibition-map do not construct inhibition map\n  --no-activation-map do not construct activation map\n  --no-maps do not construct inhibition/activation maps\n  --merge-maps also constructs inhibition maps\n  --output-dir (current dir) directory on which to put computed data\n  --seed seed the random generator using given integer (same integer will generate the same random number sequence)\n  --no-measure causes simplx to ignore observables\n  --memory-limit limit the usage of the memory (in Mb). Default is infinite (0)\n  --snapshot-at takes a snapshot of solution at specified time unit (may use option several times)\n  --mixture-file-scheme (~tmp_mixture_[t]) Naming scheme for serialization of mixtures\n  --save-mixture-at Save mixture at specified time (can be used multiple times)\n  --load-mixture Use given mixture file as initial conditions (%init instructions in the kappa file will be ignored)\n  --state-file-scheme (~tmp_state_[t]) Naming scheme for serialization of simulation state\n  --save-state-at Save simulation state at specified time (can be used multiple times)\n  --load-state Load given simulation state (only %mod instruction will be parsed from the kappa file)\n  --max-clashes (10000) max number of consecutive clashes before aborting\n  --storify [deprecated]\n  --set-snapshot-time [deprecated]\n  --data-tmp-file [deprecated]\n  --no-compress-stories [deprecated]\n  --no-use-strong-compression [deprecated] as --no-strong-compression (for backward compatibility)\n  -help  Display this list of options\n  --help  Display this list of options\n";

    /**
	 * @param args
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println(usage);
        } else {
            String filename = null;
            String kappa = null;
            int i;
            for (i = 0; i < args.length; i++) {
                if (args[i].endsWith(".urb")) {
                    filename = args[i];
                    kappa = args[i] = filename.replaceAll("\\.urb", ".ka");
                    break;
                }
            }
            if (filename != null) {
                UrbanToKappaTransformer t = new UrbanToKappaTransformer(new File(filename).getParentFile());
                Writer out = new FileWriter(kappa);
                String urban = readFile(filename);
                out.write(t.transform(urban));
                out.close();
            }
            if (filename != null && args.length > 1) {
                Process p = Runtime.getRuntime().exec(simplx(args));
                new Pipe(p.getErrorStream(), System.err).start();
                new Pipe(p.getInputStream(), System.out).start();
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private static String[] simplx(String[] args) {
        String[] result = new String[args.length + 1];
        result[0] = "simplx";
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }

    private static String readFile(String filename) throws IOException {
        File file = new File(filename);
        FileReader reader = new FileReader(file);
        char[] cbuf = new char[(int) file.length()];
        reader.read(cbuf);
        return new String(cbuf);
    }

    static class Pipe extends Thread {

        InputStream is;

        OutputStream os;

        Pipe(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
        }

        public void run() {
            try {
                int i;
                while ((i = is.read()) != -1) os.write(i);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
