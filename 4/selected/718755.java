package com.google.i18n.pseudolocalization.tool;

import com.google.i18n.pseudolocalization.PseudolocalizationPipeline;
import com.google.i18n.pseudolocalization.format.FormatRegistry;
import com.google.i18n.pseudolocalization.format.MessageCatalog;
import com.google.i18n.pseudolocalization.format.ReadableMessageCatalog;
import com.google.i18n.pseudolocalization.format.WritableMessageCatalog;
import com.google.i18n.pseudolocalization.message.Message;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Run a pseudolocalization pipeline on a set of input files.
 * <p>
 * See {@link Pseudolocalizer.PseudolocalizerArguments#printUsage()} for
 * command-line usage.
 */
public class Pseudolocalizer {

    static class PseudolocalizerArguments {

        /**
     * Print a usage message.
     */
        private static void printUsage() {
            System.err.println("Usage: Pseudolocalizer [--ext=fqcn[,fqcn...]] [--variant=varname|" + "--method=method[,method...] [--type=filetype] [<--interactive|files>]");
            System.err.println("filetype: a registered file type, typically the same as the extension");
            System.err.println();
            System.err.println("If given a list of files, output is written to file_variant.ext");
            System.err.println("If a method list is used instead of a variant, the suffix is \"pseudo\"");
            System.err.println("If no variant or methods are given, psaccent is used");
        }

        private final List<String> fileNames;

        private final String fileType;

        private final boolean isInteractive;

        private final List<String> methods;

        private final PseudolocalizationPipeline pipeline;

        private final String variant;

        /**
     * Process command-line arguments.
     * 
     * @throws RuntimeException with error message on fatal errors.
     */
        public PseudolocalizerArguments(String[] args) {
            Set<String> validMethods = PseudolocalizationPipeline.getRegisteredMethods();
            methods = new ArrayList<String>();
            boolean error = false;
            boolean tmpIsInteractive = false;
            String tmpVariant = null;
            String tmpFileType = null;
            int argIndex = 0;
            while (argIndex < args.length && args[argIndex].startsWith("--")) {
                String argName = args[argIndex].substring(2);
                if (argName.startsWith("method=")) {
                    for (String method : argName.substring(7).split(",")) {
                        if (!validMethods.contains(method)) {
                            System.err.println("Unknown method '" + method + "'");
                            error = true;
                            continue;
                        }
                        methods.add(method);
                    }
                } else if (argName.startsWith("variant=")) {
                    if (tmpVariant != null) {
                        throw new RuntimeException("More than one variant supplied");
                    }
                    tmpVariant = argName.substring(8);
                } else if (argName.startsWith("ext=")) {
                    for (String className : argName.substring(4).split(",")) {
                        try {
                            Class.forName(className);
                        } catch (ClassNotFoundException e) {
                            System.err.println("Unable to load extension class " + className);
                            error = true;
                        }
                    }
                } else if (argName.startsWith("type=")) {
                    tmpFileType = argName.substring(5);
                } else if (argName.equals("interactive")) {
                    tmpIsInteractive = true;
                } else {
                    System.err.println("Unrecognized option: " + argName);
                    error = true;
                }
                argIndex++;
            }
            if (tmpVariant != null) {
                if (!methods.isEmpty()) {
                    System.err.println("May not specify both --variant and --method, using variant");
                }
            } else if (methods.isEmpty()) {
                tmpVariant = "psaccent";
            }
            fileType = tmpFileType;
            variant = tmpVariant;
            isInteractive = tmpIsInteractive;
            if (error || (isInteractive && argIndex < args.length)) {
                printUsage();
                System.exit(1);
            }
            fileNames = new ArrayList<String>();
            while (argIndex < args.length) {
                fileNames.add(args[argIndex++]);
            }
            if (variant != null) {
                pipeline = PseudolocalizationPipeline.getVariantPipeline(variant);
            } else {
                pipeline = PseudolocalizationPipeline.buildPipeline(methods);
            }
            if (pipeline == null) {
                throw new RuntimeException("Unable to construct pipeline for methods " + methods);
            }
        }

        /**
     * @return list of filenames -- empty if should read from stdin
     */
        public List<String> getFileNames() {
            return fileNames;
        }

        /**
     * @return the methods
     */
        public List<String> getMethods() {
            return methods;
        }

        /**
     * @return the pipeline
     */
        public PseudolocalizationPipeline getPipeline() {
            return pipeline;
        }

        /**
     * @return the file type, or null if not specified
     */
        public String getType() {
            return fileType;
        }

        /**
     * @return the variant
     */
        public String getVariant() {
            return variant;
        }

        /**
     * @return the isInteractive
     */
        public boolean isInteractive() {
            return isInteractive;
        }
    }

    /**
   * @param args
   * @throws IOException 
   */
    public static void main(String[] args) throws IOException {
        Pseudolocalizer pseudolocalizer = new Pseudolocalizer();
        PseudolocalizerArguments arguments = new PseudolocalizerArguments(args);
        pseudolocalizer.run(arguments);
    }

    void run(PseudolocalizerArguments arguments) throws IOException {
        List<String> fileNames = arguments.getFileNames();
        PseudolocalizationPipeline pipeline = arguments.getPipeline();
        if (arguments.isInteractive()) {
            runStdin(pipeline);
            return;
        }
        if (fileNames.size() == 0) {
            MessageCatalog msgCat = FormatRegistry.getMessageCatalog(arguments.getType());
            writeMessages(msgCat, readAndProcessMessages(pipeline, msgCat, System.in), System.out);
            return;
        }
        String suffix = arguments.getVariant();
        if (suffix == null) {
            suffix = "_pseudo";
        } else {
            suffix = "_" + suffix;
        }
        for (String fileName : fileNames) {
            File file = new File(fileName);
            if (!file.exists()) {
                System.err.println("File " + fileName + " not found");
                continue;
            }
            int lastDot = fileName.lastIndexOf('.');
            String extension;
            String outFileName;
            if (lastDot >= 0) {
                extension = fileName.substring(lastDot + 1);
                outFileName = fileName.substring(0, lastDot) + suffix + "." + extension;
            } else {
                extension = "";
                outFileName = fileName + suffix;
            }
            System.out.println("Processing " + fileName + " into " + outFileName);
            String fileType = arguments.getType();
            if (fileType == null) {
                fileType = extension;
            }
            MessageCatalog msgCat = FormatRegistry.getMessageCatalog(fileType);
            InputStream inputStream = new FileInputStream(file);
            List<Message> processedMessages = readAndProcessMessages(pipeline, msgCat, inputStream);
            OutputStream outputStream = new FileOutputStream(new File(outFileName));
            writeMessages(msgCat, processedMessages, outputStream);
        }
    }

    /**
   * @param pipeline
   * @param msgCat
   * @param inputStream
   * @return processed messages
   * @throws IOException
   */
    private List<Message> readAndProcessMessages(PseudolocalizationPipeline pipeline, MessageCatalog msgCat, InputStream inputStream) throws IOException {
        List<Message> processedMessages = new ArrayList<Message>();
        ReadableMessageCatalog input = msgCat.readFrom(inputStream);
        try {
            for (Message msg : input.readMessages()) {
                pipeline.localize(msg);
                processedMessages.add(msg);
            }
        } finally {
            input.close();
        }
        return processedMessages;
    }

    /**
   * @param pipeline 
   * @throws IOException 
   */
    private void runStdin(PseudolocalizationPipeline pipeline) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        System.out.println("Enter text to pseudolocalize:");
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                break;
            }
            System.out.println("=> " + pipeline.localize(line));
        }
    }

    /**
   * @param msgCat
   * @param messages
   * @param outputStream
   * @throws IOException
   */
    private void writeMessages(MessageCatalog msgCat, List<Message> messages, OutputStream outputStream) throws IOException {
        WritableMessageCatalog output = msgCat.writeTo(outputStream);
        try {
            for (Message msg : messages) {
                output.writeMessage(msg);
            }
        } finally {
            output.close();
        }
    }
}
