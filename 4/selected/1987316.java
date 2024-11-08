package org.codecover.batch.commands;

import org.codecover.batch.*;
import org.codecover.model.*;
import org.codecover.model.exceptions.*;

/**
 * TouchCommand
 * 
 * @author Steffen Kieß
 * @version 1.0 ($Id: TouchCommand.java 1 2007-12-12 17:37:26Z t-scheller $)
 */
public class TouchCommand extends SimpleCommand {

    private static final TouchCommand instance = new TouchCommand();

    /**
     * Gets an instance of this class
     * 
     * @return the instance of the class
     */
    public static TouchCommand getInstance() {
        return instance;
    }

    private TouchCommand() {
        super(null, "touch-container", "reads and rewrites all given test session containers", new OptionSet(new Option[] {}, new Option[] {}), 0, -1, false);
    }

    @Override
    protected int run(CommandLine cl, BatchLogger logger, org.codecover.model.extensions.PluginManager pluginManager) {
        for (String containerLocation : cl.getNonOptionArgs()) {
            TestSessionContainer testSessionContainer = null;
            try {
                MASTBuilder builder = new MASTBuilder(logger);
                testSessionContainer = TestSessionContainer.load(pluginManager, logger, builder, containerLocation);
            } catch (FileLoadException e) {
                logger.fatal("An error occured during loading " + containerLocation, e);
            }
            try {
                if (!isPretend(cl)) {
                    testSessionContainer.save(containerLocation);
                }
            } catch (FileSaveException e) {
                logger.fatal("An error occured during saving " + containerLocation, e);
            }
        }
        return 0;
    }
}
