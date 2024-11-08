package allensoft.javacvs.client.ui.command;

import allensoft.javacvs.client.*;
import allensoft.javacvs.client.ui.command.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import allensoft.util.*;
import allensoft.diff.*;

/** Interprets commands of the format used by the cvs command line program and
 executes them using a CVSClient object. */
public class CommandInterpretter {

    /** Creates a new CommandInterpretter that inetrprets CVS commands and invokes the appropriate methods on the
	 CVSClient. */
    public CommandInterpretter(CVSClient client) {
        m_Client = client;
    }

    public synchronized void addCommandInterpretterListener(CommandInterpretterListener l) {
        if (m_Listeners == null) m_Listeners = new ArrayList(5);
        m_Listeners.add(l);
    }

    public synchronized void removeCommandListener(CommandInterpretterListener l) {
        if (m_Listeners == null) return;
        m_Listeners.remove(l);
    }

    /** Gets the current directory that commands are interpretted in. Files that are specified relatively
	    in commands will be assumed to be relative to this directory. Initially this will be initialized
	 to the user's current directory (from the System property user.dir). */
    public File getCurrentDirectory() {
        return m_CurrentDirectory;
    }

    /** Sets the current directory that commands are interpretted in. */
    public void setCurrentDirectory(File directory) {
        m_CurrentDirectory = directory;
    }

    /** Gets the repository location used for command such as checkout. If a repository location
	    has not yet been specified with the <code>setRepositoryLocation</code> method then this method
	    will attempt to use one defined by the system property <em>user.env.cvsroot</em>. It is expected
	    that this system property will be initialised by the JVM (using the -D option) to equal the
	 $CVSROOT environment variable. If this property has not been defined then a CVSException is thrown
	 with a message to indicate that the repository location has not yet been specified.
	 @throws CVSException if the repository location has not been set with <code>setRepositoryLocation</code>
	         and the system property "user.env.cvsroot" has not been set. */
    public RepositoryLocation getRepositoryLocation() throws CVSException {
        if (m_RepositoryLocation == null) {
            String sLocation = System.getProperty("user.env.cvsroot");
            if (sLocation == null) throw new CVSException("The repository location has not been specified");
            m_RepositoryLocation = new RepositoryLocation(sLocation);
        }
        return m_RepositoryLocation;
    }

    /** Sets the repository location used for commands such as checkout. */
    public void setRepositoryLocation(RepositoryLocation location) {
        m_RepositoryLocation = location;
    }

    public boolean getDisplayCommandHelp() {
        return m_bDisplayCommandHelp;
    }

    public void setDisplayCommandHelp(boolean b) {
        m_bDisplayCommandHelp = b;
    }

    /** Interprets a command and executes it using the CVSClient specified during construction.
	    For example, a command such as <em>commit -m "This is a commit" MyProject"</em> would cause
	 the directory <em>MyProject</em> to be commited to the repository using the supplied log message.
	 @return a CVSResponses object detailing the responses from all the servers. If no resonses were received
	    from any servers, for example if the user just requested help for a command with -H, then <code>null</code>
	    is returned. */
    public CVSRequestBatch interpretCommand(String sCommand) throws CVSException, IOException, ParseException {
        sCommand = sCommand.trim();
        ParsePosition p = new ParsePosition(0);
        try {
            m_Client.setGlobalOptions((GlobalOptions) m_GlobalOptionsParser.parseOptions(sCommand, p));
        } catch (ParseException e) {
            displayParseException(e);
            displayGlobalOptionsHelp();
            throw e;
        }
        int nStartOfCommand = p.getIndex();
        int nEndOfCommand = sCommand.indexOf(' ', nStartOfCommand);
        String sCommandName, sArguments = "";
        fireInterprettingCommand(sCommand);
        if (nEndOfCommand != -1) {
            sCommandName = sCommand.substring(nStartOfCommand, nEndOfCommand);
            sArguments = sCommand.substring(nEndOfCommand + 1);
        } else sCommandName = sCommand.substring(nStartOfCommand);
        if (sCommandName.length() == 0) {
            ParseException e = new ParseException("The command was not specified", 0);
            displayParseException(e);
            displayUsageHelp();
            throw e;
        }
        if (sCommandName.equals("checkout") || sCommandName.equals("co") || sCommandName.equals("get")) {
            if (getDisplayCommandHelp()) {
                displayCheckoutHelp();
                return null;
            } else return checkout(sArguments);
        } else if (sCommandName.equals("import") || sCommandName.equals("im") || sCommandName.equals("imp")) {
            if (getDisplayCommandHelp()) {
                displayImportDirectoryHelp();
                return null;
            } else return importDirectory(sArguments);
        } else if (sCommandName.equals("export")) {
            if (getDisplayCommandHelp()) {
                displayExportHelp();
                return null;
            } else return export(sArguments);
        } else if (sCommandName.equals("add") || sCommandName.equals("new")) {
            if (getDisplayCommandHelp()) {
                displayAddHelp();
                return null;
            } else return add(sArguments);
        } else if (sCommandName.equals("remove")) {
            if (getDisplayCommandHelp()) {
                displayRemoveHelp();
                return null;
            } else return remove(sArguments);
        } else if (sCommandName.equals("commit") || sCommandName.equals("com") || sCommandName.equals("ci")) {
            if (getDisplayCommandHelp()) {
                displayCommitHelp();
                return null;
            } else return commit(sArguments);
        } else if (sCommandName.equals("update") || sCommandName.equals("upp") || sCommandName.equals("up")) {
            if (getDisplayCommandHelp()) {
                displayUpdateHelp();
                return null;
            } else return update(sArguments);
        } else if (sCommandName.equals("diff")) {
            if (getDisplayCommandHelp()) {
                displayDiffHelp();
                return null;
            } else return diff(sArguments);
        } else if (sCommandName.equals("tag")) {
            if (getDisplayCommandHelp()) {
                displayTagHelp();
                return null;
            } else return tag(sArguments);
        } else {
            ParseException e = new ParseException("Unrecognized command: " + sCommandName, 0);
            displayParseException(e);
            displayUsageHelp();
            throw e;
        }
    }

    /** Interprets a command from an array of strings. This method is useful when the command
	    has been split up into an array (eg in the main method). It simply concatanates the
	    elements of the array to build a command string and then calls the <code>interpretCommand(String)</code>
	 method.*/
    public CVSRequestBatch interpretCommand(String[] command) throws CVSException, IOException, ParseException {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < command.length; i++) {
            boolean bAddQuote = false;
            if ((command[i].indexOf(' ') != -1 && command[i].charAt(0) != '"') || command[i].length() == 0) {
                buffer.append('"');
                bAddQuote = true;
            }
            buffer.append(command[i]);
            if (bAddQuote) buffer.append('"');
            if (i != command.length - 1) buffer.append(' ');
        }
        return interpretCommand(buffer.toString());
    }

    /** Performs a checkout command using the supplied arguments. */
    public CVSRequestBatch checkout(String sArguments) throws CVSException, IOException, ParseException {
        try {
            ParsePosition p = new ParsePosition(0);
            CheckoutOptions options = (CheckoutOptions) m_CheckoutOptionsParser.parseOptions(sArguments, p);
            String[] modules = StringUtilities.separateString(sArguments.substring(p.getIndex()), " ", true);
            if (modules.length == 0) throw new ParseException("No modules specified", p.getIndex());
            CVSRequestBatch batch = new CVSRequestBatch();
            batch.addRequest(new CheckoutRequest(getRepositoryLocation(), m_CurrentDirectory, modules, options));
            m_Client.performRequestBatch(batch);
            return batch;
        } catch (ParseException e) {
            displayParseException(e);
            displayCheckoutHelp();
            throw e;
        }
    }

    /** Performs a commit command using the supplied arguments. */
    public CVSRequestBatch commit(String sArguments) throws CVSException, IOException, ParseException {
        try {
            ParsePosition p = new ParsePosition(0);
            CommitOptions options = (CommitOptions) m_CommitOptionsParser.parseOptions(sArguments, p);
            File[] files = getOptionalFiles(sArguments, p);
            CVSRequestBatch batch = new CVSRequestBatch();
            batch.addCommitRequests(files, options);
            m_Client.performRequestBatch(batch);
            return batch;
        } catch (ParseException e) {
            displayParseException(e);
            displayCommitHelp();
            throw e;
        }
    }

    public CVSRequestBatch update(String sArguments) throws CVSException, IOException, ParseException {
        try {
            ParsePosition p = new ParsePosition(0);
            UpdateOptions options = (UpdateOptions) m_UpdateOptionsParser.parseOptions(sArguments, p);
            File[] files = getOptionalFiles(sArguments, p);
            CVSRequestBatch batch = new CVSRequestBatch();
            batch.addUpdateRequests(files, options);
            m_Client.performRequestBatch(batch);
            return batch;
        } catch (ParseException e) {
            displayParseException(e);
            displayUpdateHelp();
            throw e;
        }
    }

    /** Performs an add command using the supplied arguments. */
    public CVSRequestBatch add(String sArguments) throws CVSException, IOException, ParseException {
        try {
            ParsePosition p = new ParsePosition(0);
            AddOptions options = (AddOptions) m_AddOptionsParser.parseOptions(sArguments, p);
            File[] files = getFiles(sArguments, p);
            CVSRequestBatch batch = new CVSRequestBatch();
            batch.addAddRequests(files, options, null);
            m_Client.performRequestBatch(batch);
            return batch;
        } catch (ParseException e) {
            displayParseException(e);
            displayAddHelp();
            throw e;
        }
    }

    public CVSRequestBatch remove(String sArguments) throws CVSException, IOException, ParseException {
        try {
            ParsePosition p = new ParsePosition(0);
            RemoveOptions options = (RemoveOptions) m_RemoveOptionsParser.parseOptions(sArguments, p);
            File[] files = getFiles(sArguments, p);
            CVSRequestBatch batch = new CVSRequestBatch();
            batch.addRemoveRequests(files, options);
            m_Client.performRequestBatch(batch);
            return batch;
        } catch (ParseException e) {
            displayParseException(e);
            displayRemoveHelp();
            throw e;
        }
    }

    public CVSRequestBatch importDirectory(String sArguments) throws CVSException, IOException, ParseException {
        try {
            ParsePosition p = new ParsePosition(0);
            ImportOptions options = (ImportOptions) m_ImportOptionsParser.parseOptions(sArguments, p);
            if (p.getIndex() >= sArguments.length()) throw new ParseException("Directory to import not specified", p.getIndex());
            Tokenizer t = new Tokenizer(sArguments.substring(p.getIndex()));
            String sRepository, sVendorTag;
            if (t.hasMoreTokens()) sRepository = t.nextToken(); else throw new ParseException("Directory to import not specified", sArguments.length());
            if (t.hasMoreTokens()) sVendorTag = t.nextToken(); else throw new ParseException("Vendor tag not specified", sArguments.length());
            List releaseTagsList = new ArrayList(5);
            while (t.hasMoreTokens()) releaseTagsList.add(t.nextToken());
            if (releaseTagsList.size() == 0) throw new ParseException("No release tags spcified", sArguments.length());
            String[] releaseTags = (String[]) releaseTagsList.toArray(new String[releaseTagsList.size()]);
            CVSRequestBatch batch = new CVSRequestBatch();
            batch.addRequest(new ImportRequest(getCurrentDirectory(), getRepositoryLocation(), sRepository, sVendorTag, releaseTags, options));
            m_Client.performRequestBatch(batch);
            return batch;
        } catch (ParseException e) {
            displayParseException(e);
            displayImportDirectoryHelp();
            throw e;
        }
    }

    public CVSRequestBatch export(String sArguments) throws CVSException, IOException, ParseException {
        try {
            ParsePosition p = new ParsePosition(0);
            ExportOptions options = (ExportOptions) m_ExportOptionsParser.parseOptions(sArguments, p);
            CVSRequestBatch batch = new CVSRequestBatch();
            batch.addRequest(new ExportRequest(getRepositoryLocation(), getCurrentDirectory(), sArguments.substring(p.getIndex()), options));
            m_Client.performRequestBatch(batch);
            return batch;
        } catch (ParseException e) {
            displayParseException(e);
            displayImportDirectoryHelp();
            throw e;
        }
    }

    public CVSRequestBatch diff(String sArguments) throws CVSException, IOException, ParseException {
        try {
            ParsePosition p = new ParsePosition(0);
            ExtendedDiffOptions options = (ExtendedDiffOptions) m_DiffOptionsParser.parseOptions(sArguments, p);
            File[] files = getOptionalFiles(sArguments, p);
            CVSRequestBatch batch = new CVSRequestBatch();
            batch.addDiffRequests(files, options);
            m_Client.performRequestBatch(batch);
            CVSResponse[] diffResponses = batch.getResponsesOfType(DiffResponse.class);
            return batch;
        } catch (ParseException e) {
            displayParseException(e);
            displayDiffHelp();
            throw e;
        }
    }

    public CVSRequestBatch tag(String sArguments) throws CVSException, IOException, ParseException {
        try {
            ParsePosition p = new ParsePosition(0);
            TagOptions options = (TagOptions) m_TagOptionsParser.parseOptions(sArguments, p);
            int nTagStart = p.getIndex();
            if (nTagStart >= sArguments.length()) throw new ParseException("tag name not specified", p.getIndex());
            int nTagEnd = sArguments.indexOf(' ', nTagStart);
            File[] files;
            String sTag;
            if (nTagEnd != -1) {
                sTag = sArguments.substring(nTagStart, nTagEnd);
                p.setIndex(nTagEnd);
                files = getOptionalFiles(sArguments, p);
            } else {
                sTag = sArguments.substring(nTagStart);
                files = new File[] { m_CurrentDirectory };
            }
            CVSRequestBatch batch = new CVSRequestBatch();
            batch.addTagRequests(files, sTag, options);
            m_Client.performRequestBatch(batch);
            return batch;
        } catch (ParseException e) {
            displayParseException(e);
            displayTagHelp();
            throw e;
        }
    }

    protected void fireInterprettingCommand(String sCommand) {
        List listeners = null;
        synchronized (this) {
            if (m_Listeners == null || m_Listeners.size() == 0) return;
            listeners = (List) m_Listeners.clone();
        }
        InterprettingCommandEvent event = new InterprettingCommandEvent(this, sCommand);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            CommandInterpretterListener listener = (CommandInterpretterListener) i.next();
            listener.interprettingCommand(event);
        }
    }

    /** Displays help for global options.
	 @param e ParseException that occurred whilst trying to parse the global options or null if not applicable. */
    protected void displayGlobalOptionsHelp() {
        displayText("CVS global options (specified before the command name) are:\n" + "    -H           Displays usage information for command.\n" + "    -Q           Cause CVS to be really quiet.\n" + "    -q           Cause CVS to be somewhat quiet.\n" + "    -r           Make checked-out files read-only.\n" + "    -w           Make checked-out files read-write (default).\n" + "    -l           Turn history logging off.\n" + "    -n           Do not execute anything that will change the disk.\n" + "    -t           Show trace of program execution -- try with -n.\n" + "    -v           CVS version and copyright.\n" + "    -T tmpdir    Use 'tmpdir' for temporary files.\n" + "    -e editor    Use 'editor' for editing log information.\n" + "    -d CVS_root  Overrides $CVSROOT as the root of the CVS tree.\n" + "    -f           Do not use the ~/.cvsrc file.\n" + "    -z #         Use compression level '#' for net traffic.\n" + "    -a           Authenticate all net traffic.\n" + "    -s VAR=VAL   Set CVS user variable.\n" + "(Specify the --help option for a list of other help options)\n");
    }

    protected void displayUsageHelp() {
        displayText("Usage: cvs [cvs-options] command [command-options-and-arguments]\n" + "  where cvs-options are -q, -n, etc.\n" + "    (specify --help-options for a list of options)\n" + "  where command is add, admin, etc.\n" + "    (specify --help-commands for a list of commands\n" + "     or --help-synonyms for a list of command synonyms)\n" + "  where command-options-and-arguments depend on the specific command\n" + "    (specify -H followed by a command name for command-specific help)\n" + "  Specify --help to receive this message\n" + "\n" + "The Concurrent Versions System (CVS) is a tool for version control.\n" + "For CVS updates and additional information, see\n" + "    Cyclic Software at http://www.cyclic.com/ or\n" + "    Pascal Molli's CVS site at http://www.loria.fr/~molli/cvs-index.html\n");
    }

    protected void displayKeywordSubstitutionModesHelp() {
        displayText("Valid expansion modes include:\n" + "   -kkv Generate keywords using the default form.\n" + "   -kkvl        Like -kkv, except locker's name inserted.\n" + "   -kk  Generate only keyword names in keyword strings.\n" + "   -kv  Generate only keyword values in keyword strings.\n" + "   -ko  Generate the old keyword string (no changes from checked in file).\n" + "   -kb  Generate binary file unmodified (merges not allowed) (RCS 5.7).\n" + "(Specify the --help global option for a list of other help options)\n");
    }

    protected void displayUpdateHelp() {
    }

    protected void displayCommitHelp() {
    }

    protected void displayAddHelp() {
        displayText("Usage: cvs add [-k rcs-kflag] [-m message] files...\n" + "        -k      Use \"rcs-kflag\" to add the file with the specified kflag.\n" + "        -m      Use \"message\" for the creation log.\n" + "(Specify the --help global option for a list of other help options)\n");
    }

    protected void displayRemoveHelp() {
    }

    protected void displayImportDirectoryHelp() {
        displayText("Usage: cvs import [-d] [-k subst] [-I ign] [-m msg] [-b branch]\n" + "    [-W spec] repository vendor-tag release-tags...\n" + "        -d      Use the file's modification time as the time of import.\n" + "        -k sub  Set default RCS keyword substitution mode.\n" + "        -I ign  More files to ignore (! to reset).\n" + "        -b bra  Vendor branch id.\n" + "        -m msg  Log message.\n" + "        -W spec Wrappers specification line.\n" + "(Specify the --help global option for a list of other help options)\n");
    }

    protected void displayExportHelp() {
    }

    protected void displayCheckoutHelp() {
    }

    protected void displayDiffHelp() {
    }

    protected void displayTagHelp() {
    }

    protected void displayParseException(ParseException e) {
        if (e != null) {
            String sMessage = e.getMessage();
            if (sMessage != null) displayErrorText(sMessage + "\n");
        }
    }

    /** Displays error messages. By default this sends the error message to System.err. */
    protected void displayErrorText(String sText) {
        System.err.print(sText);
        System.err.flush();
    }

    /** Displays normal messages. By default this sends the message to System.out. */
    protected void displayText(String sText) {
        System.out.print(sText);
        System.out.flush();
    }

    /** Convenience method to get an array of files from a string. Files are assumed to
	 be realtive to the current directory if they are relative. */
    private File[] getFiles(String sArguments, ParsePosition p) throws ParseException {
        File[] files;
        if (p.getIndex() >= sArguments.length() || (files = FileUtilities.getFiles(sArguments.substring(p.getIndex()), m_CurrentDirectory)).length == 0) throw new ParseException("No files specified", p.getIndex());
        return files;
    }

    /** Convenience method to get an array of files from a string. Files are assumed to
	 be realtive to the current directory if they are relative. */
    private File[] getOptionalFiles(String sArguments, ParsePosition p) {
        File[] files = FileUtilities.getFiles(sArguments.substring(p.getIndex()), m_CurrentDirectory);
        if (files.length == 0) files = new File[] { m_CurrentDirectory };
        return files;
    }

    private CVSClient m_Client;

    private File m_CurrentDirectory = new File(System.getProperty("user.dir"));

    private OptionsParser m_AddOptionsParser = new AddOptionsParser();

    private OptionsParser m_RemoveOptionsParser = new RemoveOptionsParser();

    private OptionsParser m_GlobalOptionsParser = new GlobalOptionsParser(this);

    private OptionsParser m_CheckoutOptionsParser = new CheckoutOptionsParser();

    private OptionsParser m_CommitOptionsParser = new CommitOptionsParser();

    private OptionsParser m_UpdateOptionsParser = new UpdateOptionsParser();

    private OptionsParser m_ImportOptionsParser = new ImportOptionsParser();

    private OptionsParser m_ExportOptionsParser = new ExportOptionsParser();

    private OptionsParser m_DiffOptionsParser = new DiffOptionsParser();

    private OptionsParser m_TagOptionsParser = new TagOptionsParser();

    private RepositoryLocation m_RepositoryLocation;

    private ArrayList m_Listeners;

    private boolean m_bDisplayCommandHelp = false;
}
