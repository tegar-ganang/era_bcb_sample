package name.angoca.zemucan.ui.impl.jline;

import name.angoca.zemucan.AbstractZemucanException;
import name.angoca.zemucan.core.graph.model.AboutNode;
import name.angoca.zemucan.executer.api.ExecutingCommandException;
import name.angoca.zemucan.executer.api.ExecutionState;
import name.angoca.zemucan.executer.impl.ImplementationExecuter;
import name.angoca.zemucan.tools.configurator.Configurator;
import name.angoca.zemucan.tools.messages.jline.Messages;
import name.angoca.zemucan.ui.api.AbstractInterfaceController;
import name.angoca.zemucan.ui.api.InputReader;
import name.angoca.zemucan.ui.api.InputReaderException;
import name.angoca.zemucan.ui.api.OutputWriter;
import name.angoca.zemucan.ui.api.SynchronizedLoad;
import name.angoca.zemucan.ui.impl.system.SystemOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the interface controller that manage the read and the printer.
 * <p>
 * TODO v2.0 mostrar un mensaje cuando se puede ejecutar un comando (ending
 * node)
 * <p>
 * <b>Control Version</b>
 * <p>
 * <ul>
 * <li>0.0.1 Class creation.</li>
 * <li>0.0.2 Annotations.</li>
 * <li>0.0.3 Execution state.</li>
 * <li>0.0.4 Enum.</li>
 * <li>0.0.5 Read string just once.</li>
 * <li>0.0.6 Name of a state.</li>
 * <li>1.0.0 Moved to version 1.</li>
 * <li>1.0.1 Show intro.</li>
 * <li>1.0.2 Show intro.</li>
 * <li>1.0.3 Code changed and final.</li>
 * <li>1.0.4 Assert.</li>
 * <li>1.0.5 Executer not in interface.</li>
 * <li>1.1.0 Load synchronization.</li>
 * <li>1.2.0 Synchronized version with abstract part.</li>
 * <li>1.2.1 Synchronization optional.</li>
 * <li>1.2.2 Show license.</li>
 * <li>1.2.3 Show execution error.</li>
 * </ul>
 *
 * @author Andres Gomez Casanova <a
 *         href="mailto:a n g o c a at y a h o o dot c o m" >(AngocA)</a>
 * @version 1.2.3 2010-06-04
 */
public final class ImplementationJlineInterfaceController extends AbstractInterfaceController {

    /**
	 * Property max options to print.
	 */
    static final String AUTO_PRINT_THRESHOLD_KEY = "AutoPrintThreshold";

    /**
	 * Value max options to print.
	 */
    static final int AUTO_PRINT_THRESHOLD_VALUE = 50;

    /**
	 * Logger.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(ImplementationJlineInterfaceController.class);

    /**
	 * Input processor.
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
	 *            Prompt to show in each line of the console.@throws
	 *            InputReaderException When there is a problem establishing the
	 *            input or the output.
	 * @throws InputReaderException
	 *             When there is a problem establishing the input or the output.
	 */
    public ImplementationJlineInterfaceController(final String prompt) throws InputReaderException {
        super();
        assert prompt != null;
        this.output = new SystemOutputWriter();
        this.input = new JlineInputReader(prompt);
    }

    @Override
    public void start(final Thread synchro, final SynchronizedLoad synchronizedLoad) throws AbstractZemucanException {
        String phrase = null;
        if (Boolean.parseBoolean(Configurator.getInstance().getProperty(AbstractInterfaceController.SHOW_LICENSE))) {
            this.output.writeLine(AboutNode.readAboutInformation());
        }
        if (Boolean.parseBoolean(Configurator.getInstance().getProperty(AbstractInterfaceController.SHOW_INTRO_PROPERTY))) {
            this.output.writeLine(Messages.getString("ImplementationJlineInterfaceController" + ".Instructions"));
        }
        ExecutionState execState = ExecutionState.INITIATED;
        ImplementationJlineInterfaceController.LOGGER.debug("User interaction start");
        if (synchro != null) {
            synchronized (synchro) {
                synchronizedLoad.alreadyLoaded();
                synchro.notifyAll();
            }
        }
        while (execState != ExecutionState.APPLICATION_EXIT) {
            phrase = this.input.readString();
            try {
                execState = ImplementationExecuter.getInstance().execute(phrase, this.output);
            } catch (final ExecutingCommandException e) {
                ImplementationJlineInterfaceController.LOGGER.error(e.getMessage());
                ImplementationJlineInterfaceController.LOGGER.error(e.getCause().getMessage());
            }
        }
    }
}
