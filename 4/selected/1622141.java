package huf.misc.example;

import huf.misc.Getopt;

/**
 * This class demonstates usage of HUF's Getopt (<code>huf.misc.getopt</code>) package.
 *
 * This example takes a subset of wget options described in manpage's 'Logging and Input File Options' section.
 */
public class GetoptExample {

    private GetoptExample(String[] args) {
        Getopt getopt = new Getopt();
        getopt.addOption("h", "help");
        getopt.addOption("V");
        getopt.addOption("v", "verbose", new String[] { "low", "med", "high" }, "low");
        getopt.addOption("if", "input-file", true);
        getopt.addOption("of", "output-file", "output.txt");
        try {
            getopt.setArgs(args);
        } catch (IllegalArgumentException iae) {
            System.out.println("Invalid command-line parameter: " + iae.getMessage());
            System.exit(1);
        }
        checkArguments(getopt);
        int verboseLevel = getopt.getValueIndex("v");
        if (getopt.isSet("if")) {
            System.out.println("Input file:  " + getopt.getValue("if"));
        } else {
            System.out.println("Input file:  System.in");
        }
        System.out.println("Output file: " + getopt.getValue("of"));
        System.out.println("Verbose level: " + verboseLevel);
        int numArgs = args.length;
        for (int i = 0; i < numArgs; i++) {
            System.out.println("Argument #" + i + ":   " + args[i]);
        }
        System.out.println("\n");
    }

    private void checkArguments(Getopt getopt) {
        if (getopt.getNumCmdLineArguments() == 0 && getopt.getNumCmdLineOptions() == 0) {
            System.out.println("GetoptExample: missing argument\n" + "\n" + "Usage: java huf.misc.getopt.example.GetoptExample [OPTIONS] [arguments]\n" + "\n" + "Try 'java huf.misc.getopt.example.GetoptExample --help' for more options.");
            System.exit(0);
        }
        if (getopt.isSet("h")) {
            System.out.println("HUF Getopt Example 1.0, an example apppliaction\n" + "\n" + "This program serves no useful purpose and demonstrates usage of HUF's Getopt class.\n" + "\n" + "Usage: java huf.misc.getopt.example.GetoptExample [OPTIONS] [arguments]\n" + "\n" + "Options:\n" + "  -h,   --help               print help message (you're reading it)\n" + "  -V                         print version and copyright information\n" + "  -v,   --verbose            be more verbose; you can set it to three levels:\n" + "                             low, med, or high, eg. --verbose med\n" + "  -if,  --input-file FILE    read input from specified file instead of standard input\n" + "  -of,  --output-file FILE   write input to specified file instead of default \"output.txt\"\n" + "\n");
            System.exit(0);
        }
        if (getopt.isSet("V")) {
            System.out.println("HUF Getopt Example 1.0, an example apppliaction\n" + "\n" + "Copyright (c) 2003 Max Gilead <max.gilead@gmail.com>\n" + "\n" + "Distributed under terms of GNU General Public License.\n" + "This program is distributed in the hope that it will be useful,\n" + "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See COPYING\n" + "file or http://www.gnu.org/licenses/gpl.txt for more details.\n" + "\n");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        new GetoptExample(args);
    }
}
