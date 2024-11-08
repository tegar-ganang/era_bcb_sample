package barde;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import barde.dufu.InputHandler;
import barde.log.Message;
import barde.log.MessageDispatcher;
import barde.log.view.LogView;
import barde.log.view.TreeLogView;

/**
 * A player for the command-line.
 * {@link http://en.wikipedia.org/wiki/Refrain}
 * @author cbonar
 */
public class Refrain extends DuFu implements Observer {

    /**
	 * Prints messages received as an observer.
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
    public void update(Observable o, Object arg) {
        if (arg instanceof Message) {
            Message message = (Message) arg;
            StringBuffer buffer = new StringBuffer();
            buffer.append(message.getDate().toString());
            buffer.append(" ");
            buffer.append("[" + message.getChannel() + "]");
            buffer.append(" ");
            buffer.append("\"" + message.getAvatar() + "\"");
            buffer.append(" ");
            buffer.append(message.getContent());
            System.out.println(buffer.toString());
        } else System.out.println(arg.toString());
    }

    public static void main(String[] args) {
        Refrain refrain = new Refrain();
        LogView view = new TreeLogView();
        if (refrain.parseArgs(args) == null) System.exit(ERROR_MISSING_ARG);
        InputHandler inputHandler = new InputHandler();
        for (Iterator ins = refrain.input.iterator(); ins.hasNext(); ) {
            try {
                view.addAll(inputHandler.getLogView((String) ins.next()));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error during input parsing", e);
            }
        }
        MessageDispatcher dispatcher = new MessageDispatcher(view);
        dispatcher.addObserver(refrain);
        dispatcher.run();
        System.exit(ERROR_NONE);
    }
}
