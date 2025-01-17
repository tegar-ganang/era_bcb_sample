package org.ccnx.ccn.apps.ccnfileproxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.ccnx.ccn.CCNFilterListener;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.config.ConfigurationException;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.CCNFileOutputStream;
import org.ccnx.ccn.profiles.CommandMarker;
import org.ccnx.ccn.profiles.SegmentationProfile;
import org.ccnx.ccn.profiles.VersioningProfile;
import org.ccnx.ccn.profiles.metadata.MetadataProfile;
import org.ccnx.ccn.profiles.nameenum.NameEnumerationResponse;
import org.ccnx.ccn.profiles.nameenum.NameEnumerationResponse.NameEnumerationResponseMessage;
import org.ccnx.ccn.profiles.nameenum.NameEnumerationResponse.NameEnumerationResponseMessage.NameEnumerationResponseMessageObject;
import org.ccnx.ccn.profiles.security.KeyProfile;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.Exclude;
import org.ccnx.ccn.protocol.ExcludeComponent;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;

/**
 * CCNFileProxy is a file system proxy that makes files on the local system
 * available over the CCNx network. It takes a directory from which to serve files,
 * which it treats as the root of its content tree, and an optional ccnx URI
 * to serve as the prefix for that file content as represented in CCNx.
 * 
 * For example, if you have a directory /foo in the file system, with the following
 * contents:
 * 	/foo/
 * 		bar.txt
 * 		baz/
 * 			box.txt
 * and you call CCNFileProxy /foo ccnx:/testprefix
 * 
 * then asking for ccnx:/testprefix/bar.txt would return the file bar.txt (segmented
 * appropriately), and asking for ccnx:/testprefix/baz/box.txt would return box.txt.
 * The version for each file is set using the last modified information available from
 * the file system for the real file (but the file is re-signed every time you ask
 * for it from this server, so will result in slightly different pieces of content
 * with different signatures). The default prefix is ccnx:/, which means asking
 * for ccnx:/bar.txt would get you bar.txt.
 * 
 * Future improvements: 
 * - cache the original signing information so even if the
 * data falls out of ccnd's cache, you get the same signature information back,
 * - implement a NE responder to list files. 
 * - signal handling
 * - logging level control from a command line argument
 * - move file writer to a separate thread
 */
public class CCNFileProxy implements CCNFilterListener {

    static String DEFAULT_URI = "ccnx:/";

    static int BUF_SIZE = 4096;

    protected boolean _finished = false;

    protected ContentName _prefix;

    protected String _filePrefix;

    protected File _rootDirectory;

    protected CCNHandle _handle;

    private ContentName _responseName = null;

    public static void usage() {
        System.err.println("usage: CCNFileProxy <file path to serve> [<ccn prefix URI> default: ccn:/]");
    }

    public CCNFileProxy(String filePrefix, String ccnxURI) throws MalformedContentNameStringException, ConfigurationException, IOException {
        _prefix = ContentName.fromURI(ccnxURI);
        _filePrefix = filePrefix;
        _rootDirectory = new File(filePrefix);
        if (!_rootDirectory.exists()) {
            Log.severe("Cannot serve files from directory {0}: directory does not exist!", filePrefix);
            throw new IOException("Cannot serve files from directory " + filePrefix + ": directory does not exist!");
        }
        _handle = CCNHandle.open();
        _responseName = KeyProfile.keyName(null, _handle.keyManager().getDefaultKeyID());
    }

    public void start() throws IOException {
        Log.info("Starting file proxy for " + _filePrefix + " on CCNx namespace " + _prefix + "...");
        System.out.println("Starting file proxy for " + _filePrefix + " on CCNx namespace " + _prefix + "...");
        _handle.registerFilter(_prefix, this);
    }

    public boolean handleInterest(Interest interest) {
        Log.info("CCNFileProxy main responder: got new interest: {0}", interest);
        if (!_prefix.isPrefixOf(interest.name())) {
            Log.info("Unexpected: got an interest not matching our prefix (which is {0})", _prefix);
            return false;
        }
        if (SegmentationProfile.isSegment(interest.name()) && !SegmentationProfile.isFirstSegment(interest.name())) {
            Log.info("Got an interest for something other than a first segment, ignoring {0}.", interest.name());
            return false;
        } else if (interest.name().contains(CommandMarker.COMMAND_MARKER_BASIC_ENUMERATION.getBytes())) {
            try {
                Log.info("Got a name enumeration request: {0}", interest);
                return nameEnumeratorResponse(interest);
            } catch (IOException e) {
                Log.warning("IOException generating name enumeration response to {0}: {1}: {2}", interest.name(), e.getClass().getName(), e.getMessage());
                return false;
            }
        } else if (MetadataProfile.isHeader(interest.name())) {
            Log.info("Got an interest for the first segment of the header, ignoring {0}.", interest.name());
            return false;
        }
        try {
            return writeFile(interest);
        } catch (IOException e) {
            Log.warning("IOException writing file {0}: {1}: {2}", interest.name(), e.getClass().getName(), e.getMessage());
            return false;
        }
    }

    protected File ccnNameToFilePath(ContentName name) {
        ContentName fileNamePostfix = name.postfix(_prefix);
        if (null == fileNamePostfix) {
            Log.info("Unexpected: got an interest not matching our prefix (which is {0})", _prefix);
            return null;
        }
        File fileToWrite = new File(_rootDirectory, fileNamePostfix.toString());
        Log.info("file postfix {0}, resulting path name {1}", fileNamePostfix, fileToWrite.getAbsolutePath());
        return fileToWrite;
    }

    /**
	 * Actually write the file; should probably run in a separate thread.
	 * @param fileNamePostfix
	 * @throws IOException 
	 */
    protected boolean writeFile(Interest outstandingInterest) throws IOException {
        File fileToWrite = ccnNameToFilePath(outstandingInterest.name());
        Log.info("CCNFileProxy: extracted request for file: " + fileToWrite.getAbsolutePath() + " exists? ", fileToWrite.exists());
        if (!fileToWrite.exists()) {
            Log.warning("File {0} does not exist. Ignoring request.", fileToWrite.getAbsoluteFile());
            return false;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileToWrite);
        } catch (FileNotFoundException fnf) {
            Log.warning("Unexpected: file we expected to exist doesn't exist: {0}!", fileToWrite.getAbsolutePath());
            return false;
        }
        CCNTime modificationTime = new CCNTime(fileToWrite.lastModified());
        ContentName versionedName = VersioningProfile.addVersion(new ContentName(_prefix, outstandingInterest.name().postfix(_prefix).components()), modificationTime);
        CCNFileOutputStream ccnout = new CCNFileOutputStream(versionedName, _handle);
        ccnout.addOutstandingInterest(outstandingInterest);
        byte[] buffer = new byte[BUF_SIZE];
        int read = fis.read(buffer);
        while (read >= 0) {
            ccnout.write(buffer, 0, read);
            read = fis.read(buffer);
        }
        fis.close();
        ccnout.close();
        return true;
    }

    /**
	 * Handle name enumeration requests
	 * 
	 * @param interest
	 * @throws IOException 
	 * @returns true if interest is consumed
	 */
    public boolean nameEnumeratorResponse(Interest interest) throws IOException {
        boolean result = false;
        ContentName neRequestPrefix = interest.name().cut(CommandMarker.COMMAND_MARKER_BASIC_ENUMERATION.getBytes());
        File directoryToEnumerate = ccnNameToFilePath(neRequestPrefix);
        if (!directoryToEnumerate.exists() || !directoryToEnumerate.isDirectory()) {
            return result;
        }
        NameEnumerationResponse ner = new NameEnumerationResponse();
        ner.setPrefix(new ContentName(neRequestPrefix, CommandMarker.COMMAND_MARKER_BASIC_ENUMERATION.getBytes()));
        Log.info("Directory to enumerate: {0}, last modified {1}", directoryToEnumerate.getAbsolutePath(), new CCNTime(directoryToEnumerate.lastModified()));
        ner.setTimestamp(new CCNTime(directoryToEnumerate.lastModified()));
        ContentName prefixWithId = new ContentName(ner.getPrefix(), _responseName.components());
        ContentName potentialCollectionName = VersioningProfile.addVersion(prefixWithId, ner.getTimestamp());
        potentialCollectionName = SegmentationProfile.segmentName(potentialCollectionName, SegmentationProfile.baseSegment());
        if (interest.matches(potentialCollectionName, null)) {
            String[] children = directoryToEnumerate.list();
            if ((null != children) && (children.length > 0)) {
                for (int i = 0; i < children.length; ++i) {
                    ner.add(children[i]);
                }
                NameEnumerationResponseMessage nem = ner.getNamesForResponse();
                NameEnumerationResponseMessageObject neResponse = new NameEnumerationResponseMessageObject(prefixWithId, nem, _handle);
                neResponse.save(ner.getTimestamp(), interest);
                result = true;
                Log.info("sending back name enumeration response {0}, timestamp (version) {1}.", ner.getPrefix(), ner.getTimestamp());
            } else {
                Log.info("no children available: we are not sending back a response to the name enumeration interest (interest = {0}); our response would have been {1}", interest, potentialCollectionName);
            }
        } else {
            Log.info("we are not sending back a response to the name enumeration interest (interest = {0}); our response would have been {1}", interest, potentialCollectionName);
            if (interest.exclude().size() > 1) {
                Exclude.Element el = interest.exclude().value(1);
                if ((null != el) && (el instanceof ExcludeComponent)) {
                    Log.info("previous version: {0}", VersioningProfile.getVersionComponentAsTimestamp(((ExcludeComponent) el).getBytes()));
                }
            }
        }
        return result;
    }

    /**
     * Turn off everything.
     * @throws IOException 
     */
    public void shutdown() throws IOException {
        if (null != _handle) {
            _handle.unregisterFilter(_prefix, this);
            Log.info("Shutting down file proxy for " + _filePrefix + " on CCNx namespace " + _prefix + "...");
            System.out.println("Shutting down file proxy for " + _filePrefix + " on CCNx namespace " + _prefix + "...");
        }
        _finished = true;
    }

    public boolean finished() {
        return _finished;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
            return;
        }
        String filePrefix = args[0];
        String ccnURI = (args.length > 1) ? args[1] : DEFAULT_URI;
        try {
            CCNFileProxy proxy = new CCNFileProxy(filePrefix, ccnURI);
            proxy.start();
            while (!proxy.finished()) {
                try {
                    Thread.sleep(100000);
                } catch (InterruptedException e) {
                }
            }
        } catch (Exception e) {
            Log.warning("Exception in ccnFileProxy: type: " + e.getClass().getName() + ", message:  " + e.getMessage());
            Log.warningStackTrace(e);
            System.err.println("Exception in ccnFileProxy: type: " + e.getClass().getName() + ", message:  " + e.getMessage());
            e.printStackTrace();
        }
    }
}
