package name.angoca.zemucan.ui.impl.system;

import name.angoca.zemucan.AbstractZemucanException;
import name.angoca.zemucan.core.graph.model.AboutNode;
import name.angoca.zemucan.executer.api.ExecutionState;
import name.angoca.zemucan.executer.impl.ImplementationExecuter;
import name.angoca.zemucan.interfaze.InterfaceCore;
import name.angoca.zemucan.interfaze.model.ReturnOptions;
import name.angoca.zemucan.tools.configurator.Configurator;
import name.angoca.zemucan.tools.messages.system.Messages;
import name.angoca.zemucan.ui.api.AbstractInterfaceController;
import name.angoca.zemucan.ui.api.InputReader;
import name.angoca.zemucan.ui.api.InputReaderException;
import name.angoca.zemucan.ui.api.OutputWriter;
import name.angoca.zemucan.ui.api.SynchronizedLoad;

/**
 * Interface controller that manage the reader and the printer. This is just a
 * draft, because the functionality is restricted and the interaction with the
 * user is not the desired.
 * <p>
 * <b>Control Version</b>
 * <p>
 * <ul>
 * <li>0.0.1 Class creation.</li>
 * <li>0.1.0 Use of InterfaceCore.</li>
 * <li>0.1.1 Execution state.</li>
 * <li>0.1.2 enum.</li>
 * <li>0.1.3 Name of a state.</li>
 * <li>0.1.4 final.</li>
 * <li>1.0.0 Moved to version 1.</li>
 * <li>1.0.1 Show intro.</li>
 * <li>1.0.2 Show intro.</li>
 * <li>1.0.3 Strings externalized.</li>
 * <li>1.1.0 final.</li>
 * <li>1.1.1 Assert.</li>
 * <li>1.1.2 Executer not in interface.</li>
 * <li>1.2.0 Load synchronization.</li>
 * <li>1.2.1 Synchronization optional.</li>
 * <li>1.2.2 Show license.</li>
 * </ul>
 *
 * @author Andres Gomez Casanova <a
 *         href="mailto:a n g o c a at y a h o o dot c o m">(AngocA)</a>
 * @version 1.2.2 2010-06-01
 */
public final class ImplementationSystemInterfaceController extends AbstractInterfaceController {

    /**
     * Input reader.
     */
    private final InputReader input;

    /**
     * Screen printer.
     */
    private final OutputWriter output;

    /**
     * Constructor that creates a reader and a printer.
     *
     * @param prompt
     *            Prompt to show in each line of the console.
     * @throws InputReaderException
     *             When there is a problem establishing the input or the output.
     */
    public ImplementationSystemInterfaceController(final String prompt) throws InputReaderException {
        super();
        assert prompt != null;
        this.output = new SystemOutputWriter();
        this.input = new SystemInputReader(this.output, prompt);
    }

    @Override
    public void start(final Thread synchro, final SynchronizedLoad synchronizedLoad) throws AbstractZemucanException {
        ExecutionState execState = ExecutionState.INITIATED;
        if (Boolean.parseBoolean(Configurator.getInstance().getProperty(AbstractInterfaceController.SHOW_LICENSE))) {
            this.output.writeLine(AboutNode.readAboutInformation());
        }
        if (Boolean.parseBoolean(Configurator.getInstance().getProperty(AbstractInterfaceController.SHOW_INTRO_PROPERTY))) {
            this.output.writeLine(Messages.getString("ImplementationSystemInterfaceController.Instructions"));
        }
        if (synchro != null) {
            synchronized (synchro) {
                synchronizedLoad.alreadyLoaded();
                synchro.notifyAll();
            }
        }
        while (execState != ExecutionState.APPLICATION_EXIT) {
            final String phrase = this.input.readString();
            final int index = phrase.indexOf('?');
            if (index < 0) {
                execState = ImplementationExecuter.getInstance().execute(phrase, this.output);
            } else {
                final String newPhrase = phrase.substring(0, index);
                final ReturnOptions actual = InterfaceCore.analyzePhrase(newPhrase);
                this.output.writeLine(actual.toString());
            }
        }
    }
}
