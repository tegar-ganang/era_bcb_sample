package com.qasystems.tools.docgenerator;

import com.qasystems.debug.DebugWriter;
import com.qasystems.io.control.DefaultIOController;
import com.qasystems.io.control.IOControllerException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

/**
 * This class implements the IO controller for the input files for the
 * docgenerator tool. Same file is available in mkruledata
 */
public class GeneralIOController extends DefaultIOController {

    /**
   * Creates a new GeneralIOController object.
   */
    public GeneralIOController() {
        super();
        setOpenPolicy(DefaultIOController.OPEN_POLICY_NEVER);
    }

    protected void onCanNotReadFile(String filename) throws IOControllerException {
        new DebugWriter().writeMessage("Can not read file '" + filename + "'", DebugWriter.VERBOSE_ANYTHING);
        throwCancelException();
    }

    protected void onFileNotExists(String filename) throws IOControllerException {
        new DebugWriter().writeMessage("File '" + filename + "' does not exist", DebugWriter.VERBOSE_ANYTHING);
        throwCancelException();
    }

    protected void onFileNotFoundException(FileNotFoundException ex) throws IOControllerException {
        new DebugWriter().writeException(ex, this);
        throwFailureException(ex);
    }

    protected void onIOException(IOException ex) throws IOControllerException {
        new DebugWriter().writeException(ex, this);
        throwFailureException(ex);
    }

    protected void onParseException(ParseException ex) throws IOControllerException {
        new DebugWriter().writeException(ex, this);
        throwFailureException(ex);
    }
}
