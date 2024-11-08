package application;

import java.io.File;
import java.util.Scanner;
import keymanager.KeyManager;
import controllers.MainMenuController;
import framework.CryptoAlgorithm;
import framework.Framework;
import framework.Mode;
import stream.InputStream;
import stream.Key;
import stream.StreamBlockReader;
import stream.StreamBlockWriter;

public class EventManager {

    private Key key;

    private Mode mode;

    private boolean running;

    private Controller controller;

    private CryptoAlgorithm algorithm;

    private StreamBlockReader streamblockreader;

    private StreamBlockWriter streamblockwriter;

    public EventManager() {
        this.running = true;
        this.controller = new MainMenuController(this, null);
    }

    /**
	 * Reads every single input line an redirects it to the currently active
	 * controller
	 */
    public void run() {
        if (new File(System.getProperty("user.dir") + "\\keys.lst").exists()) {
            InputStream input;
            try {
                input = Framework.getInstance().getInputStream(System.getProperty("user.dir") + "\\keys.lst");
                KeyManager.getInstance().load(input);
                input.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        Scanner scanner = new Scanner(System.in);
        while (this.running) {
            this.controller.getView().display();
            this.controller.dispatch(scanner.nextLine());
        }
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public void setStreamBlockReader(StreamBlockReader streamblockwriter) {
        this.streamblockreader = streamblockwriter;
    }

    public void setStreamBlockWriter(StreamBlockWriter streamblockwriter) {
        this.streamblockwriter = streamblockwriter;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public Key getKey() {
        return this.key;
    }

    public StreamBlockReader getStreamBlockReader() {
        return this.streamblockreader;
    }

    public StreamBlockWriter getStreamBlockWriter() {
        return this.streamblockwriter;
    }

    public Controller getController() {
        return this.controller;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setAlgorithm(CryptoAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public CryptoAlgorithm getCryptoAlgorithm() {
        return this.algorithm;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return this.mode;
    }
}
