package net.sourceforge.seqware.pipeline.modules.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.configtools.ConfigTools;
import net.sourceforge.seqware.pipeline.module.Module;
import net.sourceforge.seqware.pipeline.module.ModuleInterface;
import org.openide.util.lookup.ServiceProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * 
 * Purpose:
 * 
 * This module simply lists files at the S3 URL you provide.  It's less useful
 * in workflows and more likely to be used by end-users to see what's in S3.
 * 
 * @author boconnor
 * @since 20110928
 * 
 */
@ServiceProvider(service = ModuleInterface.class)
public class S3ListFiles extends Module {

    protected OptionSet options = null;

    protected final int READ_ATTEMPTS = 1000;

    protected long size = 0;

    protected long position = 0;

    protected String fileName = "";

    protected File inputFile = null;

    protected String accessKey = null;

    protected String secretKey = null;

    private static final String[] Q = new String[] { "", "K", "M", "G", "T", "P", "E" };

    protected OptionParser getOptionParser() {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(Arrays.asList("s3-url", "u"), "Optional: a URL of the form s3://<bucket>/<path>/<file>").withRequiredArg().describedAs("S3 path");
        parser.acceptsAll(Arrays.asList("list-buckets", "l"), "Optional: list all the buckets you own.");
        parser.accepts("reset-owner-permissions", "Optional: this will give the bucket owner full read/write permissions, useful if many different people have been writing to the same bucket.");
        parser.acceptsAll(Arrays.asList("tab-output-file", "t"), "Optional: tab-formated output file.").withRequiredArg().describedAs("file path");
        parser.acceptsAll(Arrays.asList("search-local-dir", "s"), "Optional: attempt to match files in S3 with files in this local directory.").withRequiredArg().describedAs("directory path");
        return (parser);
    }

    public String get_syntax() {
        OptionParser parser = getOptionParser();
        StringWriter output = new StringWriter();
        try {
            parser.printHelpOn(output);
            return (output.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return (e.getMessage());
        }
    }

    /**
   * Not implemented
   */
    @Override
    public ReturnValue do_test() {
        return new ReturnValue(ReturnValue.SUCCESS);
    }

    /** 
   * Just makes sure the param was passed in.
   */
    @Override
    public ReturnValue do_verify_parameters() {
        ReturnValue ret = new ReturnValue();
        ret.setExitStatus(ReturnValue.SUCCESS);
        try {
            OptionParser parser = getOptionParser();
            options = parser.parse(this.getParameters().toArray(new String[0]));
        } catch (OptionException e) {
            ret.setStderr(e.getMessage() + System.getProperty("line.separator") + this.get_syntax());
            ret.setExitStatus(ReturnValue.INVALIDPARAMETERS);
            e.printStackTrace();
            return ret;
        }
        if (!options.has("s3-url") && !options.has("list-buckets")) {
            ret.setStderr("Must specify a --s3-url and/or --list-buckets option" + System.getProperty("line.separator") + this.get_syntax());
            ret.setExitStatus(ReturnValue.INVALIDPARAMETERS);
            return ret;
        }
        return (ret);
    }

    @Override
    public ReturnValue do_verify_input() {
        ReturnValue ret = new ReturnValue();
        ret.setExitStatus(ReturnValue.SUCCESS);
        List<String> inputs = (List<String>) options.valuesOf("s3-url");
        for (String input : inputs) {
            Pattern p = Pattern.compile("s3://(\\S+):(\\S+)@(\\S+)");
            Matcher m = p.matcher(input);
            boolean result = m.find();
            String URL = input;
            if (result) {
                accessKey = m.group(1);
                secretKey = m.group(2);
                URL = "s3://" + m.group(3);
            }
        }
        if (accessKey == null || secretKey == null) {
            try {
                HashMap<String, String> settings = (HashMap<String, String>) ConfigTools.getSettings();
                accessKey = settings.get("AWS_ACCESS_KEY");
                secretKey = settings.get("AWS_SECRET_KEY");
            } catch (Exception e) {
                e.printStackTrace();
                return (null);
            }
        }
        if (accessKey == null || "".equals(accessKey) || secretKey == null || "".equals(secretKey)) {
            ret.setExitStatus(ReturnValue.INVALIDPARAMETERS);
            ret.setStderr("You need to have a .seqware/settings file that contains AWS_ACCESS_KEY and AWS_SECRET_KEY");
            return (ret);
        }
        return (ret);
    }

    @Override
    public ReturnValue do_run() {
        ReturnValue ret = new ReturnValue();
        ret.setExitStatus(ReturnValue.SUCCESS);
        HashMap<String, HashMap<String, Long>> localFiles = new HashMap<String, HashMap<String, Long>>();
        HashMap<String, HashMap<String, Long>> remoteFiles = new HashMap<String, HashMap<String, Long>>();
        BufferedWriter tabWriter = null;
        if (options.has("tab-output-file")) {
            String tabOutFileStr = (String) options.valueOf("tab-output-file");
            File tabOutFile = new File(tabOutFileStr);
            try {
                tabWriter = new BufferedWriter(new FileWriter(tabOutFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (options.has("list-buckets") || options.has("l")) {
            AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
            boolean first = true;
            long allSize = 0l;
            for (Bucket bucket : s3.listBuckets()) {
                if (first) {
                    first = false;
                    System.out.println("\nMY BUCKETS:\n");
                }
                System.out.print(" - " + bucket.getName());
                ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucket.getName()));
                long totalSize = 0l;
                do {
                    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                        totalSize += objectSummary.getSize();
                        allSize += objectSummary.getSize();
                    }
                    objectListing = s3.listNextBatchOfObjects(objectListing);
                } while (objectListing.isTruncated());
                System.out.println(" size=" + getAsString(totalSize));
            }
            System.out.println("\nTOTAL SIZE: " + getAsString(allSize) + "\n");
        }
        if (options.has("search-local-dir")) {
            List<String> searchDirs = (List<String>) options.valuesOf("search-local-dir");
            for (String dir : searchDirs) {
                HashMap<String, Long> files = new HashMap<String, Long>();
                findFiles(dir, dir, files);
                localFiles.put(dir, files);
            }
        }
        long allSize = 0l;
        List<String> inputs = (List<String>) options.valuesOf("s3-url");
        for (String input : inputs) {
            if (input.startsWith("s3://")) {
                Pattern p = Pattern.compile("s3://(\\S+):(\\S+)@(\\S+)");
                Matcher m = p.matcher(input);
                boolean result = m.find();
                String URL = input;
                if (result) {
                    accessKey = m.group(1);
                    secretKey = m.group(2);
                    URL = "s3://" + m.group(3);
                } else {
                    try {
                        HashMap<String, String> settings = (HashMap<String, String>) ConfigTools.getSettings();
                        accessKey = settings.get("AWS_ACCESS_KEY");
                        secretKey = settings.get("AWS_SECRET_KEY");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return (null);
                    }
                }
                if (accessKey == null || secretKey == null) {
                    ret.setExitStatus(ReturnValue.INVALIDPARAMETERS);
                    ret.setStderr("You need to have a .seqware/settings file that contains AWS_ACCESS_KEY and AWS_SECRET_KEY");
                    return (ret);
                }
                AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
                p = Pattern.compile("s3://([^/]+)/*(\\S*)");
                m = p.matcher(URL);
                result = m.find();
                if (result) {
                    String bucket = m.group(1);
                    String key = m.group(2);
                    String bucketOwner = "";
                    if (options.has("reset-owner-permissions")) {
                        bucketOwner = s3.getBucketAcl(bucket).getOwner().getDisplayName();
                        System.out.println("Bucket Owner: " + bucketOwner);
                    }
                    ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucket).withPrefix(key));
                    long totalSize = 0l;
                    boolean first = true;
                    do {
                        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                            if (first) {
                                first = false;
                                if (key == null || "".equals(key)) {
                                    System.out.println("\nLISTING BUCKET: " + bucket + "\n");
                                } else {
                                    System.out.println("\nLISTING BUCKET: " + bucket + " AND KEY PREFIX: " + key + "\n");
                                }
                            }
                            totalSize += objectSummary.getSize();
                            allSize += objectSummary.getSize();
                            System.out.println(" * " + objectSummary.getKey() + " " + "size=" + getAsString(objectSummary.getSize()));
                            if (options.has("tab-output-file")) {
                                try {
                                    HashMap<String, Long> bucketMap = remoteFiles.get(bucket);
                                    if (bucketMap == null) {
                                        bucketMap = new HashMap<String, Long>();
                                        remoteFiles.put(bucket, bucketMap);
                                    }
                                    bucketMap.put(objectSummary.getKey(), objectSummary.getSize());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (options.has("search-local-dir")) {
                                for (String localDir : localFiles.keySet()) {
                                    HashMap<String, Long> currHash = localFiles.get(localDir);
                                    if (currHash.containsKey(objectSummary.getKey())) {
                                        System.out.println("    -> matches local file " + localDir + "/" + objectSummary.getKey());
                                        break;
                                    }
                                }
                            }
                            if (options.has("reset-owner-permissions")) {
                                try {
                                    System.out.println("   resetting bucket owner (" + bucketOwner + ") permissions for file owned by " + objectSummary.getOwner().getDisplayName());
                                    s3.setObjectAcl(bucket, objectSummary.getKey(), CannedAccessControlList.BucketOwnerFullControl);
                                } catch (Exception e) {
                                    System.out.println("     unable to reset permissions");
                                }
                            }
                        }
                        objectListing = s3.listNextBatchOfObjects(objectListing);
                    } while (objectListing.isTruncated());
                    System.out.println("\nBUCKET SIZE: " + getAsString(totalSize) + "\n");
                } else {
                    ret.setExitStatus(ReturnValue.FAILURE);
                    ret.setStderr("Problems connecting to S3");
                    return (ret);
                }
            } else {
                ret.setExitStatus(ReturnValue.FAILURE);
                ret.setStderr("You need to provide URLs that conform to the standard s3://<bucket>/<path>/<file>");
                return (ret);
            }
        }
        if (allSize > 0 && inputs.size() > 1) {
            System.out.println("TOTAL SIZE: " + getAsString(allSize) + "\n");
        }
        if (options.has("tab-output-file") && tabWriter != null) {
            try {
                tabWriter.write("# Remote_Files\n");
                tabWriter.write("# Remote\tS3_Bucket\tS3_Key\tS3_Size\tLocal\tLocal_Root\tLocal_Path\tLocal_Size\tSize_Equal\n");
                for (String remoteBucket : remoteFiles.keySet()) {
                    HashMap<String, Long> remote = remoteFiles.get(remoteBucket);
                    for (String file : remote.keySet()) {
                        tabWriter.write("Remote\t" + remoteBucket + "\t" + file + "\t" + remote.get(file));
                        for (String localDir : localFiles.keySet()) {
                            HashMap<String, Long> currHash = localFiles.get(localDir);
                            if (currHash.containsKey(file)) {
                                tabWriter.write("\tLocal\t" + localDir + "\t" + file + "\t" + currHash.get(file));
                                if (!remote.get(file).equals(currHash.get(file))) {
                                    tabWriter.write("\tNotEqualSize!");
                                } else {
                                    tabWriter.write("\tEqualSize!");
                                }
                                break;
                            }
                        }
                        tabWriter.write("\n");
                    }
                }
                if (options.has("search-local-dir")) {
                    tabWriter.write("# Local_Files\n");
                    tabWriter.write("# Local\tLocal_Root\tLocal_Path\tLocal_Size\tRemote\tS3_Bucket\tS3_Key\tS3_Size\tSize_Equal\n");
                    for (String localDir : localFiles.keySet()) {
                        HashMap<String, Long> currHash = localFiles.get(localDir);
                        for (String localFile : currHash.keySet()) {
                            tabWriter.write("Local\t" + localDir + "\t" + localFile + "\t" + currHash.get(localFile));
                            for (String remoteBucket : remoteFiles.keySet()) {
                                HashMap<String, Long> remote = remoteFiles.get(remoteBucket);
                                if (remote.containsKey(localFile)) {
                                    tabWriter.write("\tRemote\t" + remoteBucket + "\t" + localFile + "\t" + remote.get(localFile));
                                    if (!remote.get(localFile).equals(currHash.get(localFile))) {
                                        tabWriter.write("\tNotEqualSize!");
                                    } else {
                                        tabWriter.write("\tEqualSize!");
                                    }
                                    break;
                                }
                            }
                            tabWriter.write("\n");
                        }
                    }
                }
                tabWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return (ret);
    }

    private void findFiles(String rootDir, String file, HashMap<String, Long> fileMap) {
        File currFile = new File(file);
        if (currFile != null && currFile.canRead() && currFile.exists()) {
            if (currFile.isDirectory()) {
                File[] children = currFile.listFiles();
                for (File child : children) {
                    findFiles(rootDir, child.getPath(), fileMap);
                }
            } else {
                long size = currFile.length();
                String path = currFile.getPath();
                path = path.replace(rootDir, "");
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                fileMap.put(path, new Long(size));
            }
        }
    }

    private String getAsString(long bytes) {
        for (int i = 6; i > 0; i--) {
            double step = Math.pow(1024, i);
            if (bytes > step) return String.format("%3.1f%s", bytes / step, Q[i]);
        }
        return Long.toString(bytes);
    }

    @Override
    public ReturnValue do_verify_output() {
        ReturnValue ret = new ReturnValue();
        ret.setExitStatus(ReturnValue.SUCCESS);
        return (ret);
    }

    public ReturnValue init() {
        ReturnValue ret = new ReturnValue();
        ret.setReturnValue(ReturnValue.SUCCESS);
        Logger logger = Logger.getLogger("com.amazonaws");
        logger.setLevel(Level.SEVERE);
        return (ret);
    }

    public ReturnValue clean_up() {
        ReturnValue ret = new ReturnValue();
        ret.setReturnValue(ReturnValue.SUCCESS);
        return (ret);
    }
}
