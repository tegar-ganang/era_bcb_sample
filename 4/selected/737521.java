package edu.whitman.halfway.jigs.cmdline;

import edu.whitman.halfway.jigs.*;
import org.apache.log4j.Logger;

/** Adds a given description field and entry to all images in the
 * given directory (possibly recursively). */
public class AddDescriptionField extends AbstractCmdLine {

    private static Logger log = Logger.getLogger(AddDescriptionField.class);

    static int[] opts = { LIST_ALBUM, LOG4J_FILE, HELP, VERBOSE };

    public AddDescriptionField() {
        super(opts);
    }

    public String getAdditionalParseArgs() {
        return "o";
    }

    public static void main(String[] args) {
        (new AddDescriptionField()).mainDriver(args);
    }

    public int getDesiredNumArgs() {
        return 2;
    }

    public void doMain() {
        boolean recursive = false;
        boolean overwrite = hasOption('o');
        if (argsLeft.length != 2) {
            exitError("Too many or too few command line parameters");
        }
        String fieldName = argsLeft[0];
        String fieldValue = argsLeft[1];
        if (verbose) {
            System.out.println("Setting " + fieldName + " to " + fieldValue + " for specified picturs.");
            System.out.println("recursive=" + recursive + ", overwrite=" + overwrite);
        }
        AlbumUtil.addDescriptionField(getAlbum(), fieldName, fieldValue, overwrite, recursive);
    }

    protected void specificUsage() {
        System.out.println(" usage:  AddDescriptionField [options] [-o] fieldname fieldvalue  [file list]");
        System.out.println("\t -o               -- overwrite fieldvalue if fieldname already has a value");
    }
}
