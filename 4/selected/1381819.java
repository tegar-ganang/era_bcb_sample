package jwssbml;

import java.io.IOException;
import jargs.gnu.CmdLineParser;

public class Convert {

    private static String progname = "Convert";

    private static void loadSBML() {
        System.loadLibrary("sbmlj");
    }

    private static void printUsage(String prog) {
        System.err.println("Usage: " + prog + "[-h,--help] [-r,--roundtrip] [-o <output_file>] [-b <basedir>] <input_file>\n" + " h : Print this help message\n" + " r : Convert the file, and convert the result back again\n" + " o : Convert the file and name the result output_file" + " b : Write the converted file to a directory based on basedir");
    }

    private static String getOutputFromInput(String basedir, String inputFile) {
        int lastDot = inputFile.lastIndexOf('.');
        String basename = inputFile.substring(0, lastDot);
        String outdir = "";
        String ext = inputFile.substring(lastDot + 1);
        if (ext.equals("dat")) {
            if (basedir != null) outdir = basedir + "/ModelDescriptions/models/" + basename + "/";
            return outdir + basename + ".xml";
        } else if (ext.equals("xml")) {
            if (basedir != null) outdir = basedir + "/JWSinputfiles/JWS/";
            return outdir + basename + ".dat";
        } else {
            System.err.println("Input file must end in .sbml or .dat");
            System.exit(2);
            return basename;
        }
    }

    private static void doTransformation(String infile, String outfile) {
        System.out.println("Will read " + infile + " and write " + outfile);
        String ext = infile.substring(infile.lastIndexOf('.') + 1);
        if (ext.equals("xml")) {
            System.out.println("Converting SBML");
            SBMLtoJWS s2j = new SBMLtoJWS();
            if (!s2j.extractWriteModel(infile, outfile)) {
                System.out.println("Failed to extract SBML and write JWS format.");
            }
        } else if (ext.equals("dat")) {
            try {
                JWStoSBML j2s = new JWStoSBML();
                if (!j2s.extractWriteModel(infile, outfile)) {
                    System.out.println("Failed to extract JWS and write SBML format.");
                }
            } catch (Exception e) {
                System.out.println("Caught exception converting JWS to SBML: " + e.getMessage());
            }
        } else {
            System.err.println("Error: wrong extension!");
            System.exit(2);
        }
    }

    public static void main(String[] args) {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option help = parser.addBooleanOption('h', "help");
        CmdLineParser.Option round = parser.addBooleanOption('r', "roundtrip");
        CmdLineParser.Option out = parser.addStringOption('o', "outfile");
        CmdLineParser.Option based = parser.addStringOption('b', "basedir");
        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage(progname);
            System.exit(2);
        }
        if ((Boolean) parser.getOptionValue(help, Boolean.FALSE)) {
            printUsage(progname);
        }
        Boolean roundTrip = (Boolean) parser.getOptionValue(round, Boolean.FALSE);
        System.out.println("Doing round trip: " + roundTrip + "\n");
        String outname = (String) parser.getOptionValue(out, null);
        String basedir = (String) parser.getOptionValue(based, null);
        String[] otherArgs = parser.getRemainingArgs();
        if (otherArgs.length < 1) {
            printUsage(progname);
            System.exit(2);
        }
        String inputFile = otherArgs[0];
        String outputFile = getOutputFromInput(basedir, inputFile);
        if (outname != null) {
            outputFile = outname;
        }
        loadSBML();
        doTransformation(inputFile, outputFile);
        if (roundTrip) {
            inputFile = outputFile;
            outputFile = getOutputFromInput(basedir, inputFile);
            try {
                System.out.println("Doing transformation...");
                doTransformation(inputFile, outputFile);
            } catch (Exception e) {
                System.out.println("Caught Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("Done");
    }
}
