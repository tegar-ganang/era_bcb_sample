package org.dev2live.commands;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.dev2live.arguments.ArgumentUtil;
import org.dev2live.arguments.Arguments;
import org.dev2live.arguments.CommandArgument;

/**
 * @author bertram
 *
 */
public class FileCommand extends AbstractCommand {

    static final String key = "FileCommand";

    public Integer execute(Arguments args) {
        int result = -1;
        String source = (String) ArgumentUtil.getArgumentValue(args, "source");
        String target = (String) ArgumentUtil.getArgumentValue(args, "target");
        ActionType action = getActionType((String) ArgumentUtil.getArgumentValue(args, "type"));
        if (action == null || "".equals(target) || "".equals(source)) return new Integer(result);
        File filesource = new File(source);
        if (!filesource.exists()) return new Integer(result);
        File filetarget = new File(target);
        try {
            switch(action) {
                case Copy:
                    if (filesource.isDirectory()) FileUtils.copyDirectory(filesource, filetarget, true); else FileUtils.copyFile(filesource, filetarget, true);
                    break;
                case Move:
                    if (filesource.isDirectory()) FileUtils.moveDirectoryToDirectory(filesource, filetarget, true); else FileUtils.moveFile(filesource, filetarget);
                    break;
                case Delete:
                    if (filesource.isDirectory()) FileUtils.deleteDirectory(filesource); else FileUtils.deleteQuietly(filesource);
                    break;
                case Rename:
                    filesource.renameTo(filetarget);
                    break;
            }
            result = 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Integer(result);
    }

    /**
	 * @return a key for this class
	 */
    public static final String getKey() {
        return key;
    }

    public Arguments getUsableArguments() {
        Arguments arg = new Arguments();
        arg.add(new CommandArgument("source", "java.lang.String", true));
        arg.add(new CommandArgument("target", "java.lang.String", true));
        arg.add(new CommandArgument("type", "java.lang.String", true));
        return arg;
    }

    /**
	 * @author bertram
	 * private enum for switch
	 */
    private enum ActionType {

        Copy, Move, Delete, Rename
    }

    /**
	 * resolve the type string to enum
	 * @param type
	 * @return
	 */
    private ActionType getActionType(String type) {
        if ("copy".equals(type)) return ActionType.Copy;
        if ("move".equals(type)) return ActionType.Move;
        if ("delete".equals(type)) return ActionType.Delete;
        if ("rename".equals(type)) return ActionType.Rename;
        return null;
    }
}
