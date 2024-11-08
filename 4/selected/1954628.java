package com.darkhonor.rage.tools.tascl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * RAGE Test Against Server is a command-line tool for testing student programs
 * against a RAGE AutoGrader Server.  This tool requires Java to run.
 * 
 * @author Alex Ackerman
 */
public class TAS {

    public static void usage() {
        LOGGER.error("Printing Usage");
        System.out.println("Usage: ");
        System.out.println("\tTAS [-c configFile] [-t processing|raptor] Question");
        System.out.println("\tTAS [-c configFile] -l\n");
        System.out.println("\t-c\t\tThe configuration file with server information. If");
        System.out.println("\t\t\tthis information is not included, it will default to");
        System.out.println("\t\t\tc:\\config.properties as the configuration file.\n");
        System.out.println("\t-t\t\tType of program to run.  Options are 'processing' and ");
        System.out.println("\t\t\t'raptor'.  The default is processing.\n");
        System.out.println("\t-l\t\tList the questions on the server\n");
        System.out.println("\tQuestion\tThe name of the question to test\n");
        System.exit(1);
    }

    /**
     * Returns a populated Properties object with the configuration options in
     * in the provided configuration file.  If the provided file name is
     * <code>null</code>, the default configuration file location will be used
     * based upon the Operating System:
     *
     * Windows:
     *     C:\Program Files\CS110H\runme.exe
     *
     * Linux/OSX:
     *     /usr/local/bin/runme
     * 
     * @param fileName  The location of the configuration file
     * @return          The populated Properties object or <code>null</code> if
     *                  there is an error loading the configuration.
     */
    private static Properties loadProperties(String fileName) {
        String configFile = "";
        Properties props = new Properties();
        try {
            LOGGER.info("Loading server and port information from config " + "file");
            if (fileName != null) configFile = configFile.concat(fileName); else if (System.getProperty("os.name").indexOf("Windows") != -1) configFile = configFile.concat(DEF_CONFIG_WIN); else configFile = configFile.concat(DEF_CONFIG_NIX);
            LOGGER.debug("Config File: " + configFile);
            props.load(new FileInputStream(configFile));
            LOGGER.debug("Server: " + props.getProperty("server"));
            LOGGER.debug("Port: " + props.getProperty("port"));
        } catch (IOException ex) {
            LOGGER.error("ERROR: " + ex.getLocalizedMessage());
            props = null;
        }
        return props;
    }

    private static void listQuestions(Properties props) {
        Socket conn = null;
        BufferedReader in = null;
        PrintWriter out = null;
        LOGGER.info("Getting list of questions from the server");
        try {
            boolean done = false;
            LOGGER.debug("Connecting to " + props.getProperty("server") + " on port " + props.getProperty("port"));
            conn = new Socket(props.getProperty("server"), Integer.valueOf(props.getProperty("port")));
            LOGGER.debug("Connected.  Setting up input/output streams");
            out = new PrintWriter(conn.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            LOGGER.debug("Sending DIRECTORY");
            out.print("DIRECTORY\r\n");
            out.flush();
            System.out.println("Questions on Server:");
            LOGGER.info("Questions on Server (" + props.getProperty("server") + "): ");
            while (!done) {
                String line;
                try {
                    line = in.readLine();
                } catch (NoSuchElementException exc) {
                    line = null;
                }
                if (line == null) done = true; else {
                    if (!line.equals("EOF")) {
                        System.out.println(line);
                        LOGGER.info("- " + line);
                    } else {
                        done = true;
                    }
                }
            }
            LOGGER.info("Finished getting list of Questions");
        } catch (IOException ioEx) {
            LOGGER.error("ERROR: Error connecting to server: " + ioEx.getLocalizedMessage());
            System.err.println("ERROR: Error connecting to server");
        } catch (NumberFormatException numEx) {
            LOGGER.error("ERROR: Invalid port number in config file: " + props.getProperty("port"));
            System.err.println("ERROR: Invalid port number in config file");
        } finally {
            closeConnection(conn, in, out);
        }
    }

    private static boolean runQuestion(Properties props, String question, String type) {
        Socket conn = null;
        BufferedReader in = null;
        PrintWriter out = null;
        boolean result = false;
        try {
            boolean done = false;
            LOGGER.debug("Connecting to " + props.getProperty("server") + " on port " + props.getProperty("port"));
            conn = new Socket(props.getProperty("server"), Integer.valueOf(props.getProperty("port")));
            LOGGER.debug("Connected.  Setting up input/output streams");
            out = new PrintWriter(conn.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            LOGGER.debug("Sending Question Name: " + question);
            out.print(question + "\r\n");
            out.flush();
            String line;
            try {
                line = in.readLine();
            } catch (NoSuchElementException exc) {
                line = null;
            }
            if (line == null) done = true; else if (line.equals("INVALID COMMAND OR ASSIGNMENT")) {
                System.err.println(line);
                LOGGER.error("ERROR: " + line);
                closeConnection(conn, in, out);
                System.exit(1);
            } else {
                System.out.println("Number of Test Cases:\t" + line);
                LOGGER.info("Number of Test Cases:\t" + line);
                int numTestCases = 0;
                try {
                    numTestCases = Integer.valueOf(line.trim());
                } catch (NumberFormatException numEx) {
                    LOGGER.error("ERROR: Invalid number: " + line);
                }
                for (int i = 1; i <= numTestCases; i++) {
                    List<String> testInputs = new ArrayList<String>();
                    while (!done) {
                        try {
                            line = in.readLine();
                        } catch (NoSuchElementException exc) {
                            line = null;
                        }
                        if (line == null) done = true; else {
                            if (!line.equals("EOF")) {
                                testInputs.add(line);
                                LOGGER.debug("Input: " + line);
                            } else {
                                LOGGER.debug("Line: " + line);
                                done = true;
                            }
                        }
                    }
                    List<String> cmd = new ArrayList<String>();
                    File tempIn = File.createTempFile("rage", ".tmp");
                    File tempOut = File.createTempFile("rage", ".tmp", new File(System.getProperty("user.dir")));
                    tempOut.setWritable(true);
                    if (type.equals("processing")) {
                        cmd.add(props.getProperty("runme"));
                        cmd.add(question);
                        for (int j = 0; j < testInputs.size(); j++) {
                            cmd.add(testInputs.get(j));
                        }
                        cmd.add(" >" + tempOut.getName());
                    } else if (type.equals("raptor")) {
                        LOGGER.debug("Writing test inputs to temp input file");
                        ByteBuffer buffer = ByteBuffer.allocate(1024 * 10);
                        FileOutputStream inFileStream = new FileOutputStream(tempIn);
                        FileChannel inChannel = inFileStream.getChannel();
                        inChannel.lock();
                        for (int j = 0; j < testInputs.size(); j++) {
                            buffer.put(testInputs.get(j).getBytes());
                            buffer.put(System.getProperty("line.separator").getBytes());
                            buffer.flip();
                            inChannel.write(buffer);
                            buffer.clear();
                        }
                        inFileStream.close();
                        inChannel.close();
                        LOGGER.debug("Building RAPTOR command");
                        cmd.add(props.getProperty("raptor"));
                        cmd.add("\"" + System.getProperty("user.dir") + System.getProperty("file.separator") + question + ".rap" + "\"");
                        cmd.add("/run");
                        cmd.add("\"" + tempIn.getCanonicalPath() + "\"");
                        cmd.add("\"" + tempOut.getCanonicalPath() + "\"");
                    } else {
                        LOGGER.error("ERROR:  Unsupported Option");
                    }
                    String callCommand = new String();
                    for (int j = 0; j < cmd.size(); j++) {
                        callCommand = callCommand.concat(cmd.get(j) + " ");
                    }
                    LOGGER.debug("Command: " + callCommand);
                    LOGGER.debug("Command Length: " + callCommand.length());
                    ProcessBuilder launcher = new ProcessBuilder();
                    Map<String, String> environment = launcher.environment();
                    launcher.redirectErrorStream(true);
                    launcher.directory(new File(System.getProperty("user.dir")));
                    launcher.command(cmd);
                    Process p = launcher.start();
                    Long startTimeInNanoSec = System.nanoTime();
                    Long delayInNanoSec;
                    try {
                        if (props.getProperty("infDetection") != null && props.getProperty("threshold") != null && props.getProperty("infDetection").equals("true")) {
                            try {
                                delayInNanoSec = Long.parseLong(props.getProperty("threshold")) * 1000000000;
                            } catch (NumberFormatException e) {
                                LOGGER.error("ERROR: Invalid Threshold " + "value.  Defaulting to 10");
                                delayInNanoSec = new Long(10 * 1000000000);
                            }
                            boolean timeFlag = true;
                            while (timeFlag) {
                                try {
                                    int val = p.exitValue();
                                    timeFlag = false;
                                    LOGGER.debug("Exit Value: " + val);
                                } catch (IllegalThreadStateException e) {
                                    Long elapsedTime = System.nanoTime() - startTimeInNanoSec;
                                    if (elapsedTime > delayInNanoSec) {
                                        LOGGER.warn("ERROR: Threshold time " + "exceeded.");
                                        p.destroy();
                                        timeFlag = false;
                                    }
                                    Thread.sleep(50);
                                }
                            }
                        } else {
                            p.waitFor();
                        }
                    } catch (InterruptedException ex) {
                        LOGGER.warn("Thread interrupted");
                    }
                    File newTemp = null;
                    BufferedReader inFile;
                    try {
                        LOGGER.debug("Output File: " + tempOut.getCanonicalPath());
                        LOGGER.debug("Output File Length: " + tempOut.length());
                        inFile = new BufferedReader(new FileReader(tempOut));
                    } catch (FileNotFoundException ex) {
                        LOGGER.warn("Warning: The file is in use by another " + "process");
                        newTemp = File.createTempFile("rage", ".tmp");
                        LOGGER.debug("New Temp: " + newTemp.getCanonicalPath());
                        inFile = new BufferedReader(new FileReader(newTemp));
                    }
                    String outputLine = null;
                    LOGGER.debug("Sending output back to server");
                    while ((outputLine = inFile.readLine()) != null) {
                        LOGGER.debug("Output: " + outputLine);
                        out.print(outputLine + "\r\n");
                    }
                    out.print("EOF\r\n");
                    LOGGER.debug("Sent: EOF");
                    out.flush();
                    inFile.close();
                    LOGGER.debug("Reading Response from Server");
                    line = in.readLine();
                    if (line.equals("CORRECT")) {
                        System.out.println("Test (" + i + "):\tCORRECT");
                        LOGGER.info("Test " + i + ":\tCORRECT");
                    } else {
                        System.out.println("Test (" + i + "):\tINCORRECT");
                        LOGGER.info("Test " + i + ":\tINCORRECT");
                        result = true;
                    }
                    LOGGER.debug("Deleting temp files");
                    tempIn.delete();
                    tempOut.delete();
                    if (newTemp != null) newTemp.delete();
                    done = false;
                }
            }
        } catch (IOException ioEx) {
            LOGGER.error("ERROR: Error connecting to server: " + ioEx.getLocalizedMessage());
            System.err.println("ERROR: Error connecting to server");
            System.exit(1);
        } catch (NumberFormatException numEx) {
            LOGGER.error("ERROR: Invalid port number in config file");
            System.err.println("ERROR: Invalid port number in config file");
            System.exit(1);
        } finally {
            LOGGER.debug("Closing IO resources");
            closeConnection(conn, in, out);
        }
        return result;
    }

    private static void outputResult(boolean result) {
        LOGGER.debug("Final determination of result");
        if (result) {
            System.out.println("Result:\tFAILED");
            LOGGER.debug("Result:\tFAILED");
        } else {
            System.out.println("Result:\tPASSED");
            LOGGER.debug("Result:\tPASSED");
        }
    }

    private static void checkProperties(Properties props) {
        if (props == null) {
            System.err.println("ERROR: TAS Configuration file cannot be found");
            usage();
        }
    }

    private static void closeConnection(Socket conn, BufferedReader in, PrintWriter out) {
        LOGGER.info("Closing connection to server");
        if (in != null) {
            try {
                in.close();
            } catch (IOException ex) {
            }
        }
        if (out != null) out.close();
        if (conn != null) {
            try {
                conn.close();
            } catch (IOException ex) {
            }
        }
    }

    public static void main(String args[]) {
        System.out.println("RAGE Test Against Server (v" + version + ")");
        LOGGER.info("RAGE Test Against Server (v" + version + ")");
        LOGGER.info("===============================");
        System.out.println("===============================");
        LOGGER.debug("# of args: " + args.length);
        for (int i = 0; i < args.length; i++) {
            LOGGER.debug("args[" + i + "]: " + args[i]);
        }
        Properties props = new Properties();
        if ((args.length != 5) && (args.length != 3) && (args.length != 1)) {
            usage();
        } else if (args.length == 1) {
            if (args[0].equals("-l")) {
                props = loadProperties(null);
                checkProperties(props);
                listQuestions(props);
            } else if (args[0].startsWith("-")) {
                LOGGER.error("ERROR: Question name cannot start with '-'.");
                LOGGER.error("\tCommand: TAS " + args[0]);
                usage();
            } else {
                props = loadProperties(null);
                checkProperties(props);
                boolean result = runQuestion(props, args[0], DEF_TYPE);
                outputResult(result);
            }
        } else if (args.length == 5) {
            if (!args[0].equals("-c") || !args[2].equals("-t")) {
                LOGGER.debug("args[0]: " + args[0] + "; args[2]: " + args[2]);
                usage();
            } else if (!args[3].equals("processing") && !args[3].equals("raptor")) {
                LOGGER.debug("args[3]: " + args[3]);
                usage();
            } else {
                props = loadProperties(args[1]);
                checkProperties(props);
                boolean result = runQuestion(props, args[4], args[3]);
                outputResult(result);
            }
        } else if (args.length == 3) {
            if (args[0].equals("-c") && args[2].equals("-l")) {
                props = loadProperties(args[1]);
                checkProperties(props);
                listQuestions(props);
            } else if (args[0].equals("-c")) {
                props = loadProperties(args[1]);
                checkProperties(props);
                boolean result = runQuestion(props, args[2], DEF_TYPE);
                outputResult(result);
            } else if (args[0].equals("-t")) {
                props = loadProperties(null);
                checkProperties(props);
                boolean result = runQuestion(props, args[2], args[1]);
                outputResult(result);
            } else {
                usage();
            }
        }
    }

    private static Logger LOGGER = Logger.getLogger(TAS.class);

    private static final String DEF_CONFIG_WIN = "C:\\config.properties";

    private static final String DEF_CONFIG_NIX = "/usr/local/etc/config.properties";

    private static final String DEF_TYPE = "processing";

    private static final double version = 1.1;
}
