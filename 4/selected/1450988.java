package net.sf.nodeInsecure.util;

import net.sf.nodeInsecure.common.CommandExecutor;
import net.sf.nodeInsecure.common.Interpretable;
import net.sf.nodeInsecure.computer.Directory;

/**
 * @author: janmejay
 * Date: 27 Dec, 2007
 * Job: mimics command 'ls' on a usual linux box
 */
public class Mkdir extends CommandExecutor {

    public static final String COMMAND = "mkdir";

    protected int execute(final Interpretable interpretable) {
        Directory target = interpretable.getContext().getPwd();
        if (hasInvalidPath(interpretable)) {
            interpretable.getTerminalAccessor().writeLine(COMMAND + ": No directory-name given");
            return -1;
        }
        String path = interpretable.getArguments()[0];
        PathResolvingUtil pathResolver = new PathResolvingUtil(interpretable);
        String newFileName;
        if (pathResolver.hasDirNameInPath(path)) {
            pathResolver.resolveParentDirAndItemNameUnderIt(target, path);
            target = pathResolver.getTarget();
            newFileName = pathResolver.getNewItemName();
            if (target == null) {
                interpretable.getTerminalAccessor().writeLine(COMMAND + ": Directory requested in path `" + path + "` does not exist");
                return -1;
            }
        } else {
            newFileName = path;
        }
        try {
            target.addNewDir(newFileName);
        } catch (Directory.NameClashException e) {
            interpretable.getTerminalAccessor().writeLine(COMMAND + ": " + newFileName + ": Directory already exists");
            return -1;
        }
        return 0;
    }

    private boolean hasInvalidPath(Interpretable interpretable) {
        return interpretable.getArguments().length == 0 || interpretable.getArguments()[0] == null || interpretable.getArguments()[0].trim().length() == 0;
    }

    protected boolean evaluate(final Interpretable interpretable) {
        return interpretable.commandIs(COMMAND);
    }
}
