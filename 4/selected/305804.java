package net.sf.peervibes.utils.appl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class MergeFiles {

    private String baseFilename;

    private static final String recvDir = "recv";

    private static final String sendDir = "send";

    private static final String resultDir = "result";

    public MergeFiles(String basefilename) {
        this.baseFilename = basefilename;
    }

    private void exec() throws FileNotFoundException {
        Scanner mainInput = new Scanner(new File(this.baseFilename));
        PrintStream output = new PrintStream(MergeFiles.resultDir + "/" + this.baseFilename);
        String node = null;
        int currEpoch = -1;
        while (mainInput.hasNext()) {
            node = mainInput.nextLine();
            currEpoch = Integer.parseInt(mainInput.nextLine());
            this.writeHeader(node, currEpoch, output);
            this.addSentMessages(currEpoch, output);
            this.addRecvMessages(currEpoch, output);
            this.writeHeader(node, currEpoch, output);
            this.addMembership(mainInput, output);
            this.addMembership(mainInput, output);
        }
        mainInput.close();
        output.close();
    }

    private void addMembership(Scanner mainInput, PrintStream output) {
        output.println(mainInput.nextLine());
        String line = mainInput.nextLine();
        int lines = Integer.parseInt(new StringTokenizer(line).nextToken());
        output.println(line);
        for (int i = 0; i < lines; i++) output.println(mainInput.nextLine());
    }

    private void addRecvMessages(int currEpoch, PrintStream output) throws FileNotFoundException {
        output.println("Received:");
        String targetFile = new String(this.baseFilename);
        targetFile = targetFile.replace(".txt", ("_recv_" + currEpoch + ".txt"));
        Scanner sc = new Scanner(new File(MergeFiles.recvDir + "/" + targetFile));
        ArrayList<String> messages = new ArrayList<String>();
        while (sc.hasNext()) messages.add(sc.nextLine());
        sc.close();
        output.println(messages.size());
        while (messages.size() != 0) output.println(messages.remove(0));
    }

    private void addSentMessages(int currEpoch, PrintStream output) throws FileNotFoundException {
        output.println("Sent:");
        String targetFile = new String(this.baseFilename);
        targetFile = targetFile.replace(".txt", ("_send_" + currEpoch + ".txt"));
        Scanner sc = new Scanner(new File(MergeFiles.sendDir + "/" + targetFile));
        ArrayList<String> messages = new ArrayList<String>();
        while (sc.hasNext()) messages.add(sc.nextLine());
        sc.close();
        output.println(messages.size());
        while (messages.size() != 0) output.println(messages.remove(0));
    }

    private void writeHeader(String node, int currEpoch, PrintStream output) {
        output.println(node);
        output.println(currEpoch);
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        String[] list = new File(".").list();
        for (int i = 0; i < list.length; i++) {
            if (list[i].endsWith(".txt")) {
                System.err.println("Starting execution for: " + list[i]);
                MergeFiles instance = new MergeFiles(list[i]);
                try {
                    instance.exec();
                    System.err.println("Execution complete for: " + list[i]);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }
}
