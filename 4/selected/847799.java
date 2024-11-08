package ftraq.fs;

import ftraq.FTraqError;
import ftraq.fs.exceptions.*;
import ftraq.fs.local.LocalFileSystem;
import ftraq.gui.UiInputOutputConsole;

/**
 * @author <a href="mailto:jssauder@tfh-berlin.de">Steffen Sauder</a>
 * @version 1.0
 */
public class UiFSConsole extends UiInputOutputConsole implements LgFileTransferThreadObserver {

    private LgFileSystemBrowser localFsBrowser;

    private LgFileSystemBrowser remoteFsBrowser;

    private LgFileSystemBrowser currentBrowser;

    private static ftraq.util.Logger logger = ftraq.util.Logger.getInstance(UiFSConsole.class);

    private Throwable lastException = null;

    private static java.util.Comparator fileNameComparator = new java.util.Comparator() {

        public int compare(Object o1, Object o2) {
            LgDirectoryEntry e1 = (LgDirectoryEntry) o1;
            LgDirectoryEntry e2 = (LgDirectoryEntry) o2;
            return e1.getAbsoluteName().compareTo(e2.getAbsoluteName());
        }
    };

    private static java.text.DateFormat directoryListingDateFormat;

    private static java.text.NumberFormat directoryListingNumberFormat;

    static {
        UiFSConsole.directoryListingDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        UiFSConsole.directoryListingNumberFormat = java.text.NumberFormat.getNumberInstance();
    }

    public UiFSConsole(boolean showGUI) throws FSConsoleInitFailure {
        super(showGUI);
        LgFileSystem localFileSystem = ftraq.fs.local.LocalFileSystem.getInstance();
        try {
            logger.info("creating local file system...");
            logger.info("creating file system browser...");
            this.localFsBrowser = new LgFileSystemBrowserImpl(localFileSystem);
            this.currentBrowser = this.localFsBrowser;
            this.localFsBrowser.makeFileSystemAvailable();
            logger.info("file system and browser succesfully initialized, printing welcome msg...");
            this.writeToConsole("you are browsing " + this.currentBrowser.getCurrentDirectory().getFileSystem());
            this.writeToConsole("your initial directory is " + this.currentBrowser.getCurrentDirectory().getAbsoluteName());
            if (this.hasGUI) {
                this.consoleFrame.setTitle(this.getCurrentDirectoryString());
            }
        } catch (Exception e) {
            logger.error("failed to initialize the file system console", e);
            throw new FSConsoleInitFailure(e, localFileSystem);
        }
    }

    private void writeToConsole(String s) {
        super.println(s);
    }

    private void createRemoteBrowser(final String i_remoteUrl) throws CreateRemoteBrowserCancelledExc, FileSystemNotAvailableFailure {
        if (this.hasGUI) {
            this.consoleFrame.setTitle("connecting to " + i_remoteUrl + "...");
        }
        String userName = "anonymous";
        String passWord = "abc@def.de";
        String hostName = i_remoteUrl;
        int portNr = 21;
        int indexOfAtChar = i_remoteUrl.indexOf('@');
        if (indexOfAtChar != -1) {
            userName = i_remoteUrl.substring(0, indexOfAtChar);
            hostName = i_remoteUrl.substring(indexOfAtChar + 1);
            passWord = super.showInputDialog("enter the password for username " + userName + " at " + hostName);
            if (passWord == null) {
                throw new CreateRemoteBrowserCancelledExc("connection to {0} was cancelled, no password entered.", hostName);
            }
        }
        this.lastException = null;
        logger.info("creating remote browser to " + i_remoteUrl);
        writeToConsole("connecting to " + i_remoteUrl + "...");
        LgFileSystem remoteFileSystem = ftraq.fs.ftp.FtpFileSystem.getInstance(hostName, portNr, userName, passWord);
        remoteFileSystem.makeAvailable();
        this.remoteFsBrowser = new LgFileSystemBrowserImpl(remoteFileSystem);
        this.currentBrowser = this.remoteFsBrowser;
        this.currentBrowser.makeFileSystemAvailable();
        this.writeToConsole("connected: " + this.currentBrowser.getFileSystem().getWelcomeString());
        if (this.hasGUI) {
            this.consoleFrame.setTitle(this.getCurrentDirectoryString());
        }
    }

    private void putFile(String i_fileName) throws StartTransferFailure {
        try {
            LgDirectoryEntry requestedEntry = this.localFsBrowser.getDirectoryEntryFromRelativeFileName(i_fileName);
            LgFile requestedFile = requestedEntry.toFile();
            if (requestedFile.exists() == false) {
                throw new NoSuchDirectoryEntryExc(requestedFile);
            }
            LgDirectory targetDirectory = this.remoteFsBrowser.getCurrentDirectory();
            String targetFileName = targetDirectory.getAbsoluteName();
            if (targetFileName.charAt(targetFileName.length() - 1) != targetDirectory.getFileSystem().getPathSeparator()) {
                targetFileName = targetFileName + targetDirectory.getFileSystem().getPathSeparator();
            }
            targetFileName = targetFileName + requestedFile.getName();
            LgFile targetFile = this.remoteFsBrowser.getFileSystem().getFile(targetFileName);
            LgFileTransferThread transferThread = new LgFileTransferThreadImpl(this, requestedFile, targetFile, false);
            transferThread.start();
        } catch (Exception e) {
            throw new StartTransferFailure(e, this.localFsBrowser.getCurrentDirectory(), this.remoteFsBrowser.getCurrentDirectory(), i_fileName);
        }
    }

    public void transferSucceeded(LgFileTransferThread i_threadThatSucceded) {
        this.writeToConsole("> succesfully transferred " + i_threadThatSucceded.getTransferredBytes() + " bytes to " + i_threadThatSucceded.getTargetFile().getURL());
    }

    public void transferFailed(LgFileTransferThread i_threadThatFailed, Exception i_exception) {
        this.writeToConsole("> failed to transfer " + i_threadThatFailed.getSourceFile() + ": ");
        if (i_exception instanceof ftraq.FTraqFailure) {
            this.writeToConsole(((ftraq.FTraqFailure) i_exception).getMessages());
        } else {
            this.writeToConsole("> an unexpeted exception occured: ");
            this.writeToConsole(i_exception.getClass() + ": " + i_exception.getMessage());
        }
    }

    public void transferCancelled(LgFileTransferThread i_threadThatWasCancelled) {
        this.writeToConsole("> the transfer of " + i_threadThatWasCancelled.getSourceFile() + " was cancelled.");
    }

    public void transferStatusUpdate(LgFileTransferThread i_threadThatChanged) {
        this.writeToConsole("> " + i_threadThatChanged.getSourceFile() + ": " + i_threadThatChanged.getStatusString() + ", " + i_threadThatChanged.getTransferredBytes() + " bytes transferred");
    }

    private void getFile(String i_fileName) throws StartTransferFailure {
        try {
            LgDirectoryEntry requestedEntry = this.remoteFsBrowser.getDirectoryEntryFromRelativeFileName(i_fileName);
            LgFile requestedFile = requestedEntry.toFile();
            if (requestedFile.exists() == false) {
                throw new NoSuchDirectoryEntryExc(requestedFile);
            }
            LgDirectory targetDirectory = this.localFsBrowser.getCurrentDirectory();
            String targetFileName = targetDirectory.getAbsoluteName();
            if (targetFileName.charAt(targetFileName.length() - 1) != targetDirectory.getFileSystem().getPathSeparator()) {
                targetFileName = targetFileName + targetDirectory.getFileSystem().getPathSeparator();
            }
            targetFileName = targetFileName + requestedFile.getName();
            LgFile targetFile = this.localFsBrowser.getFileSystem().getFile(targetFileName);
            LgFileTransferThread transferThread = new LgFileTransferThreadImpl(this, requestedFile, targetFile, false);
            transferThread.start();
        } catch (Exception e) {
            throw new StartTransferFailure(e, this.remoteFsBrowser.getCurrentDirectory(), this.localFsBrowser.getCurrentDirectory(), i_fileName);
        }
    }

    private void runGarbageCollector() {
        this.showMemoryUsage();
        this.writeToConsole("running garbage collector...");
        System.gc();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
        }
        this.showMemoryUsage();
    }

    private void showMemoryUsage() {
        long freeMem = Runtime.getRuntime().freeMemory() / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024;
        long usedMem = totalMem - freeMem;
        this.writeToConsole("memory usage: used " + usedMem + "Kb, allocated " + totalMem + "Kb, free: " + freeMem + "Kb");
    }

    public void commandEntered(String commandLine) {
        logger.info("the command '" + commandLine + "' was received.");
        if (this.hasGUI) {
            this.writeToConsole("->" + commandLine);
        }
        try {
            CommandLine cmd = new CommandLine(commandLine);
            if (cmd.getCmdName().equals("quit")) {
                System.exit(0);
            }
            if (cmd.getCmdName().equals("pwd")) {
                this.showCurrentDirectory();
                return;
            }
            if (cmd.getCmdName().equals("clear")) {
                if (this.hasGUI) {
                    super.clearConsole();
                }
                return;
            }
            if (cmd.getCmdName().equals("cd")) {
                if (cmd.getParameterCount() == 1) {
                    this.changeCurrentDirectory(cmd.getParameter(0));
                } else {
                    logger.warn("command '" + commandLine + "' doesn't have the right number of parameters, showing syntax message");
                    this.writeToConsole("syntax: cd directory");
                }
                return;
            }
            if (cmd.getCmdName().equals("ls")) {
                this.listCurrentDirectory(false);
                return;
            }
            if (cmd.getCmdName().equals("ll")) {
                this.listCurrentDirectory(true);
                return;
            }
            if (cmd.getCmdName().equals("ld")) {
                this.listCurrentDirectoriesSubdirectories(true);
                return;
            }
            if (cmd.getCmdName().equals("gc")) {
                this.runGarbageCollector();
                return;
            }
            if (cmd.getCmdName().equals("mkdir")) {
                if (cmd.getParameterCount() == 1) {
                    this.createSubDirectory(cmd.getParameter(0));
                } else {
                    logger.warn("command '" + commandLine + "' doesn't have the right number of parameters, showing syntax message");
                    this.writeToConsole("syntax: mkdir directory");
                }
                return;
            }
            if (cmd.getCmdName().equals("rm")) {
                if (cmd.getParameterCount() == 1) {
                    this.deleteDirectoryEntry(cmd.getParameter(0));
                } else {
                    logger.warn("command '" + commandLine + "' doesn't have the right number of parameters, showing syntax message");
                    this.writeToConsole("syntax: rm (file|directory)");
                }
                return;
            }
            if (cmd.getCmdName().equals("get")) {
                if (this.remoteFsBrowser == null) {
                    this.writeToConsole("connect to remote file system first");
                    return;
                }
                if (cmd.getParameterCount() == 1) {
                    this.getFile(cmd.getParameter(0));
                } else {
                    this.writeToConsole("syntax: get filename");
                }
                return;
            }
            if (cmd.getCmdName().equals("put")) {
                if (this.remoteFsBrowser == null) {
                    this.writeToConsole("connect to remote file system first");
                    return;
                }
                if (cmd.getParameterCount() == 1) {
                    this.putFile(cmd.getParameter(0));
                } else {
                    this.writeToConsole("syntax: put filename");
                }
                return;
            }
            if (cmd.getCmdName().equals("local")) {
                this.currentBrowser = this.localFsBrowser;
                if (this.hasGUI) {
                    this.consoleFrame.setTitle(this.getCurrentDirectoryString());
                }
                return;
            }
            if (cmd.getCmdName().equals("remote")) {
                if (cmd.getParameterCount() == 0) {
                    if (this.remoteFsBrowser != null) {
                        this.currentBrowser = this.remoteFsBrowser;
                        if (this.hasGUI) {
                            this.consoleFrame.setTitle(this.getCurrentDirectoryString());
                        }
                    } else {
                        this.writeToConsole("connect with 'remote serveraddress' first.");
                    }
                    return;
                }
                if (cmd.getParameterCount() == 1) {
                    this.createRemoteBrowser(cmd.getParameter(0));
                    return;
                }
                this.writeToConsole("syntax: remote [serveraddress]");
                return;
            }
            if (cmd.getCmdName().equals("dump")) {
                LgDirectoryEntry_List allFiles = this.currentBrowser.getCurrentDirectory().getFileSystem().getAllKnownDirectoryEntries();
                this.printDirectoryEntryList(allFiles, false, true);
                return;
            }
            if (cmd.getCmdName().equals("cp")) {
                if (cmd.getParameterCount() == 2) {
                    this.copyFile(cmd.getParameter(0), cmd.getParameter(1));
                } else {
                    logger.warn("command '" + commandLine + "' doesn't have the right number of parameters, showing syntax message");
                    this.writeToConsole("syntax: cp file (file|directory)");
                }
                return;
            }
            if (cmd.getCmdName().equals("trace")) {
                if (this.lastException != null) {
                    this.writeToConsole(multex.Msg.getStackTrace(this.lastException));
                } else {
                    this.writeToConsole("no exception occured so far.");
                }
                return;
            }
            if (cmd.getCmdName().equals("mv")) {
                if (cmd.getParameterCount() == 2) {
                    this.moveFile(cmd.getParameter(0), cmd.getParameter(1));
                } else {
                    logger.warn("command '" + commandLine + "' doesn't have the right number of parameters, showing syntax message");
                    this.writeToConsole("syntax: mv (file|directory) (file|directory)");
                }
                return;
            }
            if (cmd.getCmdName().equals("cat")) {
                if (cmd.getParameterCount() == 1) {
                    this.showContentOfFile(cmd.getParameter(0));
                } else {
                    logger.warn("command '" + commandLine + "' doesn't have the right number of parameters, showing syntax message");
                    this.writeToConsole("syntax: cat file");
                }
                return;
            }
            if (cmd.getCmdName().equals("help")) {
                this.showAvailableCommands();
                return;
            }
            this.writeToConsole("I wish I could do that... (enter help for available commands)");
        } catch (CommandLineSyntaxFailure e) {
            this.writeToConsole(e.getMessage());
            this.showAvailableCommands();
            this.lastException = e;
        } catch (ftraq.fs.exceptions.FileSystemFailure ex) {
            if (this.hasGUI) {
            }
            this.writeToConsole(ex.getMessages());
            this.lastException = ex;
        } catch (Exception e) {
            logger.error("unexpected exception occured while processing '" + commandLine + "'", e);
            e.printStackTrace();
            this.writeToConsole("processing '" + commandLine + "' failed in an unexpected way: ");
            if (e instanceof ftraq.FTraqFailure) {
                this.writeToConsole(((ftraq.FTraqFailure) e).getMessages());
            } else {
                this.writeToConsole(e.getClass() + ": " + e.getMessage());
            }
            this.lastException = e;
        }
    }

    private void showAvailableCommands() {
        logger.info("displaying the list of available commands.");
        StringBuffer buf = new StringBuffer();
        buf.append("available commands: \n");
        buf.append("  local                        - switch to the local file system\n");
        buf.append("  remote [ftpServerAddress]    - switch to the ftp file system\n");
        buf.append("  cd dir                       - change the current working directory\n");
        buf.append("  pwd                          - show the current working directory\n");
        buf.append("  ls                           - list content of current working directory\n");
        buf.append("  ll                           - list directory content with details\n");
        buf.append("  ld                           - list subdirectories of current working directory\n");
        buf.append("  cat file                     - show the content of a file \n");
        buf.append("  get file                     - transfer file from remote to local file system\n");
        buf.append("  put file                     - transfer file from local to remote file system\n");
        buf.append("  mkdir dir                    - create a new subdirectory (in current directory)\n");
        buf.append("  rm (file|dir)                - remove a file or directory\n");
        buf.append("  mv (file|dir) (file|dir)     - move or rename a file or directory\n");
        buf.append("  trace                        - shows details about the last exception\n");
        buf.append("  quit                         - quit this program\n");
        this.writeToConsole(buf.toString());
    }

    void showContentOfFile(String sourceName) throws ShowFileContentFailure {
        logger.info("trying to display content of file " + sourceName + "...");
        LgDirectoryEntry entry;
        try {
            entry = this.currentBrowser.getDirectoryEntryFromRelativeFileName(sourceName);
            ;
        } catch (Exception ex) {
            throw new ShowFileContentFailure(ex, sourceName);
        }
        try {
            LgFile file = entry.toFile();
            String content = this.currentBrowser.getContentOfFile(file);
            this.writeToConsole(content);
            logger.info("succesfully displayed content of file " + entry.getURL());
        } catch (Exception ex) {
            throw new ShowFileContentFailure(ex, entry);
        }
    }

    void moveFile(String i_sourceName, String i_targetName) throws MoveDirectoryEntryFailure {
        try {
            LgDirectoryEntry sourceEntry = this.currentBrowser.getDirectoryEntryFromRelativeFileName(i_sourceName);
            LgDirectoryEntry targetEntry;
            if (sourceEntry instanceof LgFile) {
                targetEntry = this.currentBrowser.getDirectoryEntryFromRelativeFileName(i_targetName);
                if (targetEntry.exists()) {
                    if (targetEntry instanceof LgDirectory || targetEntry instanceof LgLink) {
                        LgDirectory targetDirectory = targetEntry.toDirectory();
                        sourceEntry.moveTo(targetDirectory);
                    } else {
                        throw new DirectoryEntryAlreadyExistsExc(targetEntry);
                    }
                } else {
                    if (sourceEntry.getParentDirectory() != targetEntry.getParentDirectory()) {
                        throw new OperationNotSupportedExc("can't rename and move a file at the same time!", sourceEntry);
                    }
                    sourceEntry.setName(targetEntry.getName());
                }
            }
            if (sourceEntry instanceof LgDirectory) {
                targetEntry = this.currentBrowser.getDirectoryEntryFromRelativeFileName(i_targetName);
                if (targetEntry.exists()) {
                    if (targetEntry instanceof LgDirectory || targetEntry instanceof LgLink) {
                        LgDirectory targetDirectory = targetEntry.toDirectory();
                        sourceEntry.moveTo(targetDirectory);
                    } else {
                        throw new DirectoryEntryAlreadyExistsExc(targetEntry);
                    }
                } else {
                    if (sourceEntry.getParentDirectory() != targetEntry.getParentDirectory()) {
                        throw new OperationNotSupportedExc("can't rename and move a directory at the same time!", sourceEntry);
                    }
                    sourceEntry.setName(targetEntry.getName());
                }
            }
        } catch (MoveDirectoryEntryFailure f) {
            throw f;
        } catch (Exception e) {
            throw new MoveDirectoryEntryFailure(e, this.currentBrowser.getCurrentDirectory(), i_sourceName, i_targetName);
        }
    }

    void copyFile(String sourceName, String targetName) throws CopyDirectoryEntryFailure {
        logger.info("trying to copy '" + sourceName + "' to '" + targetName + "' from within directory " + this.currentBrowser.getCurrentDirectory().getURL());
        try {
            LgDirectoryEntry sourceEntry = this.currentBrowser.getDirectoryEntryFromRelativeFileName(sourceName);
            LgDirectoryEntry targetEntry = this.currentBrowser.getDirectoryEntryFromRelativeFileName(targetName);
            LgFile sourceFile;
            try {
                sourceFile = sourceEntry.toFile();
            } catch (IsNoFileExc ex) {
                throw new OperationNotSupportedExc("not supported yet: can't copy the directory strucuture below {0} ", sourceEntry);
            }
            if (sourceFile.exists() == false) {
                throw new NoSuchDirectoryEntryExc(sourceFile);
            }
            LgFile targetFile;
            logger.debug("finding out if the target is a file or a directory...");
            try {
                targetFile = targetEntry.toFile();
                logger.debug("...it's a file");
            } catch (IsNoFileExc ex) {
                logger.debug("...it must be a directory, have to construct its absolute name first");
                String newAbsoluteFileName = targetEntry.getAbsoluteName();
                if (targetEntry.getFileSystem().isRootDirectoryName(newAbsoluteFileName) == false) {
                    newAbsoluteFileName = newAbsoluteFileName + targetEntry.getFileSystem().getPathSeparator();
                }
                logger.debug("...parent directory name is " + newAbsoluteFileName + "...");
                newAbsoluteFileName = newAbsoluteFileName + sourceFile.getName();
                logger.debug("the target file's name is " + newAbsoluteFileName);
                targetFile = this.currentBrowser.getDirectoryEntryFromRelativeFileName(newAbsoluteFileName).toFile();
            }
            LgFileTransferThread fileTransferThread = new LgFileTransferThreadImpl(this, sourceFile, targetFile, false);
            logger.info("starting file transfer thread for copying " + sourceFile.getURL() + " to " + targetFile.getURL());
            fileTransferThread.start();
        } catch (Exception ex) {
            throw new CopyDirectoryEntryFailure(ex, this.currentBrowser.getCurrentDirectory(), sourceName, targetName);
        }
    }

    void deleteDirectoryEntry(String fileName) throws DeleteDirectoryEntryFailure {
        this.currentBrowser.deleteDirectoryEntry(fileName);
    }

    void createSubDirectory(String directoryName) throws CreateDirectoryFailure {
        this.currentBrowser.createSubDirectory(directoryName);
    }

    void listCurrentDirectory(boolean detailed) throws ListDirectoryFailure {
        logger.info("listing contents of current directory...");
        try {
            this.printDirectoryEntryList(this.currentBrowser.getCurrentDirectoryListing(), detailed, false);
            logger.info("finished listing the current directory");
        } catch (ListDirectoryFailure ex1) {
            throw ex1;
        } catch (Exception ex) {
            throw new ListDirectoryFailure(ex, this.currentBrowser.getCurrentDirectory());
        }
    }

    void listCurrentDirectoriesSubdirectories(boolean detailed) throws ListDirectoryFailure {
        logger.info("listing subdirectories of current directory...");
        try {
            LgDirectoryEntry_List list = this.currentBrowser.getCurrentDirectoryListing();
            LgDirectoryEntry_List directoriesList = Lg_ListImpl.createLgDirectoryEntry_List();
            synchronized (list) {
                java.util.Iterator it = list.iterator();
                while (it.hasNext()) {
                    LgDirectoryEntry entry = (LgDirectoryEntry) it.next();
                    if (entry instanceof LgDirectory) {
                        directoriesList.add(entry);
                    }
                }
            }
            this.printDirectoryEntryList(directoriesList, detailed, false);
            logger.info("finished listing the subdirectories.");
        } catch (ListDirectoryFailure ex1) {
            throw ex1;
        } catch (Exception ex) {
            throw new ListDirectoryFailure(ex, this.currentBrowser.getCurrentDirectory());
        }
    }

    void changeCurrentDirectory(String newDirectoryName) throws ChangeDirectoryFailure {
        this.currentBrowser.setCurrentDirectory(newDirectoryName);
        if (this.hasGUI) {
            this.consoleFrame.setTitle(this.getCurrentDirectoryString());
        }
    }

    public String getWindowTitleString() {
        return this.getCurrentDirectoryString();
    }

    private String getCurrentDirectoryString() {
        try {
            String currentDir = this.currentBrowser.getCurrentDirectory().getAbsoluteName();
            return currentDir + " on " + this.currentBrowser.getCurrentDirectory().getFileSystem().toString();
        } catch (RuntimeException t) {
            return "starting up...";
        }
    }

    void showCurrentDirectory() {
        logger.info("display the current directory...");
        this.writeToConsole(this.getCurrentDirectoryString());
    }

    private String getParameterFromCommand(String cmdLine, int paramNumber) {
        java.util.StringTokenizer st = new java.util.StringTokenizer(cmdLine, " ", false);
        int paramCount = 0;
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            if (paramCount == paramNumber) return nextToken;
            paramCount++;
        }
        return new String();
    }

    public static void main(String[] args) {
        boolean enableGui = false;
        try {
            if (args.length > 0) {
                if (args[0].equals("--gui")) {
                    enableGui = true;
                }
            }
            UiFSConsole fsConsole = new UiFSConsole(enableGui);
            if (enableGui == false) {
                try {
                    while (true) {
                        String commandLine = fsConsole.readCommandFromConsole();
                        fsConsole.commandEntered(commandLine);
                    }
                } catch (java.io.IOException ex) {
                    logger.warn("IOException on input console.");
                }
            } else {
                ftraq.fs.ftp.FtpFileSystem.showDebugFrame();
            }
        } catch (Throwable t) {
            logger.error("can't start file system console application", t);
        }
    }

    private void printDirectoryEntryList(LgDirectoryEntry_List directoryEntryList, boolean detailed, boolean absoluteFileNames) {
        LgDirectoryEntry_List sortedList = Lg_ListImpl.createLgDirectoryEntry_List(directoryEntryList);
        java.util.Collections.sort(sortedList, this.fileNameComparator);
        java.util.Iterator it = sortedList.iterator();
        StringBuffer buf = new StringBuffer();
        while (it.hasNext()) {
            LgDirectoryEntry entry = (LgDirectoryEntry) it.next();
            if (detailed) {
                buf.append(this.getDetailedDirectoryLine(entry));
                buf.append('\n');
            } else {
                if (absoluteFileNames) {
                    buf.append(entry.getAbsoluteName());
                    buf.append('\n');
                } else {
                    buf.append(entry.getName());
                    buf.append('\n');
                }
            }
        }
        buf.append(sortedList.size() + " directory entries.");
        this.writeToConsole(buf.toString());
    }

    private String getDetailedDirectoryLine(LgDirectoryEntry entry) {
        StringBuffer buf = new StringBuffer();
        if (entry instanceof LgDirectory) {
            buf.append("D ");
        }
        if (entry instanceof LgLink) {
            buf.append("L ");
        }
        if (entry instanceof LgFile) {
            buf.append("  ");
        }
        buf.append(this.getFixedLengthString(entry.getName(), 50, true));
        buf.append(" ");
        try {
            String permissionString = entry.getPermissions();
            if (permissionString == null) permissionString = "n/a";
            buf.append(this.getFixedLengthString(permissionString, 10, true));
        } catch (Exception ex) {
            buf.append(this.getFixedLengthString("n/a", 10, true));
        }
        try {
            String sizeString = this.directoryListingNumberFormat.format(entry.getSize());
            buf.append(this.getFixedLengthString(sizeString, 11, false));
        } catch (FileSystemExc ex1) {
            buf.append(this.getFixedLengthString("  ", 11, false));
        }
        try {
            String dateString = this.directoryListingDateFormat.format(entry.getDate());
            buf.append(this.getFixedLengthString(dateString, 17, false));
        } catch (FileSystemExc ex1) {
            buf.append(this.getFixedLengthString("  ", 17, false));
        }
        return buf.toString();
    }

    private String getFixedLengthString(String string, int length, boolean alignLeft) {
        String shortString = string;
        if (string.length() > length) {
            shortString = string.substring(0, length - 3) + "...";
        }
        StringBuffer buf = new StringBuffer(length);
        if (alignLeft) {
            buf.append(shortString);
        }
        for (int i = string.length(); i < length; i++) {
            buf.append(' ');
        }
        if (!alignLeft) {
            buf.append(shortString);
        }
        return buf.toString();
    }

    private class CommandLineSyntaxFailure extends ftraq.FTraqFailure {

        private Throwable t;

        private String cmdLineString;

        CommandLineSyntaxFailure(Throwable t, String cmdLineString) {
            super(t, "syntax error in {0}", cmdLineString);
        }
    }

    private class CommandLine {

        private String cmdLineString;

        private String cmdName;

        private String[] parameters;

        CommandLine(String cmdLineString) throws CommandLineSyntaxFailure {
            this.cmdLineString = cmdLineString;
            this.parseCommandLine();
        }

        private void parseCommandLine() throws CommandLineSyntaxFailure {
            try {
                logger.debug("parsing cmd line: " + this.cmdLineString);
                java.util.StringTokenizer st = new java.util.StringTokenizer(this.cmdLineString, " ", false);
                int paramCount = 0;
                java.util.List parameterList = new java.util.LinkedList();
                if (this.cmdLineString.indexOf('"') == -1) {
                    logger.debug("it doesn't contain quotes, parameters are separated by whitespaces");
                    while (st.hasMoreTokens()) {
                        String nextToken = st.nextToken();
                        if (paramCount == 0) {
                            this.cmdName = nextToken.toLowerCase();
                            logger.debug("the command name is " + this.cmdName);
                        } else {
                            logger.debug("parameter " + parameterList.size() + " is " + nextToken);
                            parameterList.add(nextToken);
                        }
                        paramCount++;
                    }
                } else {
                    logger.debug("it contains quotes, parameters must be quoted");
                    int beginParameterIndex = this.cmdLineString.indexOf('"');
                    this.cmdName = this.cmdLineString.substring(0, beginParameterIndex).trim().toLowerCase();
                    logger.debug("the command name is " + this.cmdName);
                    logger.debug("next begin index is " + beginParameterIndex);
                    int endParameterIndex = this.cmdLineString.indexOf('"', beginParameterIndex + 1);
                    logger.debug("next end index is " + endParameterIndex);
                    while (beginParameterIndex != -1 && endParameterIndex != -1) {
                        String parameter = this.cmdLineString.substring(beginParameterIndex + 1, endParameterIndex);
                        logger.debug("parameter " + parameterList.size() + " is " + parameter);
                        parameterList.add(parameter);
                        beginParameterIndex = this.cmdLineString.indexOf('"', endParameterIndex + 1);
                        logger.debug("next begin index is " + beginParameterIndex);
                        if (beginParameterIndex != -1) {
                            endParameterIndex = this.cmdLineString.indexOf('"', beginParameterIndex + 1);
                            logger.debug("next end index is " + endParameterIndex);
                        }
                    }
                }
                this.parameters = (String[]) parameterList.toArray(new String[parameterList.size()]);
            } catch (RuntimeException t) {
                t.printStackTrace();
                throw new CommandLineSyntaxFailure(t, this.cmdLineString);
            }
        }

        String getCmdName() {
            return this.cmdName;
        }

        int getParameterCount() {
            return this.parameters.length;
        }

        String getParameter(int i) {
            return this.parameters[i];
        }
    }
}
