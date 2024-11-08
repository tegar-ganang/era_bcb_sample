package bagaturchess.ucitracker.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class Engine {

    private String startCommand;

    private String[] props;

    private String workDir;

    private Process process;

    private BufferedReader is;

    private BufferedWriter os;

    private BufferedReader err;

    private EngineDummperThread dummper;

    public Engine(String _startCommand, String[] _props, String _workDir) {
        startCommand = _startCommand;
        props = _props;
        workDir = _workDir;
    }

    public void setDummperMode(boolean enabled) {
        if (enabled) {
            dummper.enabled();
        } else {
            dummper.disable();
        }
    }

    public void start() throws IOException {
        process = Runtime.getRuntime().exec(startCommand, props, new File(workDir));
        is = new BufferedReader(new InputStreamReader(process.getInputStream()));
        os = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        dummper = new EngineDummperThread("OUT", is);
        dummper.start();
        (new EngineDummperThread("ERR", err)).start();
    }

    public void stop() throws IOException {
        process.destroy();
    }

    public boolean supportsUCI() throws IOException {
        os.write("uci");
        os.newLine();
        os.flush();
        String line;
        while ((line = is.readLine()) != null) {
            if (line.contains("uciok")) {
                return true;
            }
        }
        return false;
    }

    public boolean isReady() throws IOException {
        os.write("isready");
        os.newLine();
        os.flush();
        String line;
        while ((line = is.readLine()) != null) {
            if (line.contains("readyok")) {
                return true;
            }
        }
        return false;
    }

    public void setupPossition(String position) throws IOException {
        os.write("position " + position);
        os.newLine();
        os.flush();
    }

    public void go(int depth) throws IOException {
        os.write("go depth " + depth);
        os.newLine();
        os.flush();
    }

    public void newGame() throws IOException {
        os.write("ucinewgame");
        os.newLine();
        os.flush();
    }

    public String getInfoLine() throws IOException {
        List<String> lines = new ArrayList<String>();
        String line;
        while ((line = is.readLine()) != null) {
            if (line.contains("bestmove")) {
                for (int i = lines.size() - 1; i >= 0; i--) {
                    if (lines.get(i).startsWith("info") && lines.get(i).contains(" depth ") && (lines.get(i).contains(" pv ") || lines.get(i).contains(" multipv 1 "))) {
                        return lines.get(i);
                    }
                }
                System.out.println("No info before bestmove");
                return null;
            }
            lines.add(line);
        }
        throw new IllegalStateException();
    }
}