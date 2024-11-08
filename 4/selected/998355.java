package repeatmap.controller;

import repeatmap.controller.Dictionary;
import repeatmap.util.BloomFilter;
import repeatmap.util.Compression;
import repeatmap.util.KmerReader;
import repeatmap.util.NioFileReader;
import repeatmap.types.Statics;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Vector;
import java.lang.Math;
import java.io.IOException;
import java.lang.Exception;
import java.io.*;
import java.net.*;

public class DictionaryClient {

    private static final char SEPARATOR = ',';

    ArrayList<Boolean> doReverses;

    ArrayList<Integer> kmerLengths;

    ArrayList<String> sequenceFilenames;

    Socket socket;

    String host;

    int socketPort;

    PrintWriter outToServer;

    BufferedReader inFromServer;

    public DictionaryClient(String[] args) throws Exception {
        parseConfigFile(args[0]);
        setupSockets();
        queryServer();
        closeSockets();
    }

    private void parseConfigFile(String configFilename) throws Exception {
        File configFile;
        configFile = new File(configFilename);
        if (!configFile.exists()) {
            String msg = "ERROR: File '" + configFilename + "' does not exists!";
            Statics.logger.info(msg);
            throw new IOException(msg);
        }
        Statics.logger.info("The config file " + configFilename + " is being read in.");
        kmerLengths = new ArrayList();
        doReverses = new ArrayList();
        sequenceFilenames = new ArrayList();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            String line = null;
            int k;
            String sequenceFilename;
            boolean doReverse = false;
            try {
                if ((line = reader.readLine()) != null) {
                    line = line.trim();
                    int hostStop = line.indexOf(SEPARATOR);
                    this.host = line.substring(0, hostStop).trim();
                    this.socketPort = Integer.parseInt(line.substring(hostStop + 1).trim());
                    Statics.logger.info("\thost: " + host + ", socket: " + this.socketPort);
                } else {
                    String msg = "ERROR: config file is empty!";
                    Statics.logger.info(msg);
                    throw new IOException(msg);
                }
            } catch (Exception e) {
                String msg = "ERROR: No appropriate host or port number specified!  Exception details: \n" + e.getMessage();
                Statics.logger.info(msg);
                throw new Exception(msg);
            }
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("")) {
                    continue;
                }
                int kStop = line.indexOf(SEPARATOR);
                int reverseStop = line.indexOf(SEPARATOR, kStop + 1);
                k = Integer.parseInt(line.substring(0, kStop).trim());
                String reverseStr = line.substring(kStop + 1, reverseStop).trim();
                sequenceFilename = line.substring(reverseStop + 1).trim();
                Statics.logger.info("Reverse is: " + reverseStr);
                doReverse = false;
                if (reverseStr.equals("1") || reverseStr.equals("+1") || reverseStr.equals("+") || reverseStr.toLowerCase().equals("plus")) doReverse = false; else if (reverseStr.equals("-1") || reverseStr.equals("-") || reverseStr.toLowerCase().equals("minus")) doReverse = true; else {
                    String msg = "ERROR: Must know strand directionality";
                    Statics.logger.info(msg);
                    throw new Exception(msg);
                }
                Integer kmerLength = new Integer(k);
                this.kmerLengths.add(kmerLength);
                this.sequenceFilenames.add(sequenceFilename);
                this.doReverses.add(doReverse);
            }
            reader.close();
        } catch (Exception e) {
            String msg = "ERROR: Having a hard time reading in the config file!  Exception details:\n" + e.getMessage();
            Statics.logger.info(msg);
            throw new Exception(msg);
        }
    }

    /**
     * Set up sockets for reading and writing to server.
     */
    private void setupSockets() throws Exception {
        try {
            socket = new Socket(host, socketPort);
            outToServer = new PrintWriter(socket.getOutputStream(), true);
            inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (UnknownHostException e) {
            String msg = "ERROR: Don't know about host(port):" + host + "(" + socketPort + ")" + ".\nException Details:\n" + e.getMessage();
            Statics.logger.info(msg);
            throw new Exception(msg);
        } catch (IOException e) {
            String msg = "ERROR: Couldn't get I/O for the connection to: " + host + "(" + socketPort + ")\n" + "ERROR: An educated guess is that the Dictionary Server is down.\n" + "Exception details\n:" + e.getMessage();
            Statics.logger.info(msg);
            throw new Exception(msg);
        }
    }

    /**
     * For each file and kmer specified in the config file, ask
     * the Dictionary server for the kmer-counts of that file.
     * Write the kmer-counts to an outfile.
     */
    private void queryServer() {
        StringBuffer sequence;
        BufferedReader fileReader;
        BufferedWriter fileWriter;
        File sequenceFile;
        String sequenceFilename;
        Integer kmerLength;
        Boolean doReverse;
        for (int i = 0; i < sequenceFilenames.size(); i++) {
            sequenceFilename = sequenceFilenames.get(i);
            kmerLength = kmerLengths.get(i);
            doReverse = doReverses.get(i);
            sequenceFile = new File(sequenceFilename);
            if (!sequenceFile.exists()) {
                Statics.logger.info("ERROR: File '" + sequenceFilename + "' does not exists!");
                continue;
            }
            sequence = new StringBuffer("");
            try {
                fileReader = new BufferedReader(new FileReader(sequenceFile));
                String line = null;
                while ((line = fileReader.readLine()) != null) {
                    if (line.length() == 0 || line.charAt(0) == '>' || line.trim().equals("")) continue;
                    sequence.append(line.trim());
                }
                if (doReverse) {
                    sequence.reverse();
                    char c;
                    for (int j = 0; j < sequence.length(); j++) {
                        c = Statics.getAcidComp(sequence.charAt(j));
                        sequence.setCharAt(j, c);
                    }
                }
                if (sequence.length() <= kmerLength.intValue()) {
                    Statics.logger.info("ERROR: Sequence is too short!");
                    Statics.logger.info("ERROR: Sequence: " + sequence);
                    Statics.logger.info("ERROR: Sequence length: " + sequence.length());
                    Statics.logger.info("ERROR: Kmer length: " + kmerLength);
                    continue;
                }
            } catch (Exception e) {
                Statics.logger.info("ERROR: " + e.getMessage());
                Statics.logger.info("ERROR: Unknown error while reading in sequence file: " + sequenceFilename);
            }
            StringBuffer response = new StringBuffer("");
            String line = new String("");
            try {
                String inputToServer = kmerLength.toString() + SEPARATOR + sequence;
                outToServer.println(inputToServer);
                while ((line = inFromServer.readLine()) != null) {
                    if (line.trim().equals("")) continue;
                    if (line.length() >= 3 && line.substring(0, 3).equals("BYE")) {
                        break;
                    }
                    response.append(line + "\n");
                }
            } catch (IOException e) {
                Statics.logger.info(e.getMessage());
                Statics.logger.info("ERROR: Sending/receiving repeat info from server is not working");
            }
            if (response.length() > 5 && response.substring(0, 5).equals("ERROR")) {
                Statics.logger.info("Error from server:\n" + response);
            }
            try {
                String outfileName = sequenceFilename + "." + kmerLength + ".graph";
                fileWriter = new BufferedWriter(new FileWriter(outfileName));
                fileWriter.write(response.toString());
                fileWriter.close();
                Statics.logger.info("Wrote graph for sequence file " + sequenceFilename + " to " + outfileName);
            } catch (IOException e) {
                Statics.logger.info(e.getMessage());
                Statics.logger.info("ERROR: Sending/receiving repeat info from server is not working");
            }
        }
    }

    private void closeSockets() {
        try {
            inFromServer.close();
            outToServer.close();
            socket.close();
        } catch (Exception e) {
            Statics.logger.info("Unknown error while closing sockets/writers/readers");
        }
    }

    public static void main(String[] args) throws Exception {
        DictionaryClient dc = new DictionaryClient(args);
    }
}
