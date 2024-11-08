package org.algoristes.alkwarel.script;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;
import org.algoristes.alkwarel.utils.Log;

/**
 * Contains all the basic commands used by the script parser
 * 
 * @author Xavier Gouchet
 * 
 */
public class ScriptCommands {

    /**
	 * List of the name of all the builtin commands
	 */
    public static final Set<String> BuiltinCommands = new HashSet<String>(Arrays.asList(new String[] { "set", "print", "dialog", "if", "equals", "for", "def", "eval", "concat", "prompt", "debug", "return" }));

    /**
	 * Is the input command a builtin command name (set, puts, for etc...) ?
	 * 
	 * @param cmd
	 *            the command to test
	 * @return true if the command is a builtin function
	 */
    public static boolean isBuiltinCommand(String cmd) {
        return BuiltinCommands.contains(cmd);
    }

    /**
	 * Evaluate the commande as a builtin one
	 * 
	 * @param cmdName
	 *            the command name
	 * @param words
	 *            the list of words in the input order
	 * @param depth
	 *            the depth of the current parser (used to manage variables
	 *            scope)
	 * @return the result of the command
	 */
    public static String evalBuiltinCommand(String cmdName, String[] words, int depth) {
        if (cmdName.equals("debug")) return evalCmdDebug(words);
        if (cmdName.equals("set")) return evalCmdSet(words, depth);
        if (cmdName.equals("print")) return evalCmdPrint(words);
        if (cmdName.equals("dialog")) return evalCmdDialog(words);
        if (cmdName.equals("if")) return evalCmdIf(words, depth);
        if (cmdName.equals("equals")) return evalCmdEquals(words);
        if (cmdName.equals("for")) return evalCmdFor(words, depth);
        if (cmdName.equals("concat")) return evalCmdConcat(words);
        if (cmdName.equals("prompt")) return evalCmdPrompt(words);
        if (cmdName.equals("eval")) return evalCmdEval(words, depth);
        if (cmdName.equals("def")) return evalCmdDef(words, depth);
        if (cmdName.equals("return")) return evalCmdReturn(words);
        return "Undefined command";
    }

    /**
	 * Script Command : declare and sets a variable. Use like this : <br/>
	 * <br/>
	 * <code>
	 * set pi 3.14; # then you can use $pi anywhere else<br/>
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order
	 * @param depth
	 *            the depth of the current parser (used to managed variables
	 *            scope)
	 * @return the value of the variable
	 */
    public static String evalCmdSet(String[] words, int depth) {
        if ((words == null) || (words.length < 2)) throw new RuntimeException("<set> needs 2 arguments");
        ScriptParser.getVariablesStore().put(new ScriptVariable(words[0], words[1], depth));
        return words[1];
    }

    /**
	 * Script Command : prints something in the console. Use like this : <br/>
	 * <br/>
	 * <code>
	 * print "Hello World"; <br/>
	 * print "something " $i " else"; # prints all the words without line break<br/>
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order
	 * @return the printed string
	 */
    public static String evalCmdPrint(String[] words) {
        if ((words == null) || (words.length == 0)) throw new RuntimeException("<print> needs at least 1 argument");
        String result = "";
        for (String word : words) result += word;
        Log.info(result);
        return result;
    }

    /**
	 * Script Command : opens a YES/NO/CANCEL dialog. Use like this : <br/>
	 * <br/>
	 * <code>
	 * dialog "Do you like spam?"; <br/>
	 * dialog "Do you like spam?" "Title"; <br/>
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order
	 * @return "1" or "0" according to the user's choice
	 */
    public static String evalCmdDialog(String[] words) {
        if ((words == null) || (words.length == 0)) throw new RuntimeException("<dialog> needs at least 1 argument");
        int result = JOptionPane.showConfirmDialog(null, words[0], (words.length > 1) ? words[1] : "Dialog", JOptionPane.YES_NO_OPTION);
        switch(result) {
            case JOptionPane.OK_OPTION:
                return "1";
            case JOptionPane.NO_OPTION:
            default:
                return "0";
        }
    }

    /**
	 * Script Command : opens a prompt dialog. Use like this : <br/>
	 * <br/>
	 * <code>
	 * set name [prompt "what's your name";]; <br/>
	 * set name [prompt "what's your name" "Title";]; <br/>
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order
	 * @return the text entered by the user
	 */
    public static String evalCmdPrompt(String[] words) {
        if ((words == null) || (words.length == 0)) throw new RuntimeException("<prompt> needs at least 1 argument");
        String input = (String) JOptionPane.showInputDialog(null, words[0], (words.length > 1) ? words[1] : "Prompt", JOptionPane.PLAIN_MESSAGE);
        if (input == null) return "";
        return input;
    }

    /**
	 * Script Command : Checks if two values are equals (as strings). Use like
	 * this : <br/>
	 * <br/>
	 * <code>
	 * set toto "foo"; <br/>
	 * equals toto "bar"; # returns 0<br/>
	 * equals toto "foo"; # returns 1<br/>
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order
	 * @return the value of the variable
	 */
    public static String evalCmdEquals(String[] words) {
        if ((words == null) || (words.length != 2)) throw new RuntimeException("<equals> needs 2 arguments");
        String word0, word1;
        word0 = words[0];
        word1 = words[1];
        if (word0.equals(word1)) return "1";
        return "0";
    }

    /**
	 * Script Command : Concatenate severall words as strings. Use like this : <br/>
	 * <br/>
	 * <code>
	 * concat "Hello " "world" "!"; <br/>
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order
	 * @return the value of the variable
	 */
    public static String evalCmdConcat(String[] words) {
        if ((words == null) || (words.length < 2)) throw new RuntimeException("<concat> needs at least 2 arguments");
        String concat = "";
        for (String word : words) concat += word;
        return concat;
    }

    /**
	 * Script Commande : emulate a if ... else ... condition. Use like this : <br/>
	 * <br/>
	 * <code>
	 * if (...) {<br/>
	 *   condition;<br/>
	 * }<br/>
	 * </code> <br/>
	 * <code>
	 * if (...) {<br/>
	 *   condition1;<br/>
	 * } else {<br/>
	 *   condition2;<br/>
	 * }<br/>
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order
	 * @param depth
	 *            the depth of the current parser (used to managed variables
	 *            scope)
	 * @return the result of the condition evaluation (0 / 1)
	 */
    public static String evalCmdIf(String[] words, int depth) {
        if ((words == null) || (words.length < 2)) throw new RuntimeException("<if> needs at least 2 argument");
        boolean condition = (Double.parseDouble(words[0]) != 0);
        if (condition) {
            ScriptParser p = new ScriptParser(depth + 1);
            p.runScript(words[1]);
        } else {
            if ((words.length > 3) && (words[2].equals("else"))) {
                ScriptParser p = new ScriptParser(depth + 1);
                p.runScript(words[3]);
            }
        }
        return (condition ? "1" : "0");
    }

    /**
	 * Script Commande : emulate a for loop. Use like this : <br/>
	 * <br/>
	 * <code>
	 * for i 0 10 1{<br/>
	 * 	 # for ($i=0; $i<10; $i+=1){...}<br/>
	 *   print $i;<br/>
	 * }<br/>
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order
	 * @param depth
	 *            the depth of the current parser (used to managed variables
	 *            scope)
	 * @return always return "1"
	 */
    public static String evalCmdFor(String[] words, int depth) {
        if ((words == null) || (words.length < 5)) throw new RuntimeException("<for> needs 5 arguments");
        String var;
        int i, start, end, step;
        var = words[0];
        start = Integer.parseInt(words[1]);
        end = Integer.parseInt(words[2]);
        step = Integer.parseInt(words[3]);
        for (i = start; i < end; i += step) {
            ScriptParser.getVariablesStore().put(new ScriptVariable(var, "" + i, depth + 1));
            ScriptParser p = new ScriptParser(depth + 1);
            p.runScript(words[4]);
        }
        return "1";
    }

    /**
	 * Script Commande : emulate a for loop. Use like this : <br/>
	 * <br/>
	 * <code>
	 * eval "print \"Hello world\""<br/>
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order
	 * @param depth
	 *            the depth of the current parser (used to managed variables
	 *            scope)
	 * @return the result of the evaluation
	 */
    public static String evalCmdEval(String[] words, int depth) {
        if ((words == null) || (words.length != 1)) throw new RuntimeException("<eval> needs 1 arguments");
        ScriptParser p = new ScriptParser(depth + 1);
        return p.runScript(words[0]);
    }

    /**
	 * Script Commande : returns the parameter given. Use like this : <br/>
	 * <br/>
	 * <code>
	 * return 0;<br/>
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order
	 * @return the input or an empty string if there is no parameter
	 */
    public static String evalCmdReturn(String[] words) {
        if (words == null) return "";
        if (words.length > 1) throw new RuntimeException("<returns> needs at most 1 arguments");
        return ((words.length == 1) ? words[0] : "");
    }

    /**
	 * Script Commande : defines a function. Use like this : <br/>
	 * <br/>
	 * <code>
	 * def sum a b {<br/>
	 *   return (a+b);<br/>
	 * }<br/>
	 * 
	 * print [sum 1 4];
	 * </code>
	 * 
	 * @param words
	 *            the list of words in the input order * @param depth the depth
	 *            of the current parser (used to managed variables scope)
	 * @param depth
	 *            the depth of the current parser (used to managed variables
	 *            scope)
	 * @return "1"
	 */
    public static String evalCmdDef(String[] words, int depth) {
        if ((words == null) || (words.length <= 1)) throw new RuntimeException("<def> needs at least 2 arguments");
        if (isBuiltinCommand(words[0])) throw new RuntimeException("Cannot define function : " + words[0]);
        String[] params = new String[words.length - 2];
        for (int i = 0; i < (words.length - 2); i++) params[i] = words[i + 1];
        ScriptFunction func = new ScriptFunction(words[0], params, words[words.length - 1], depth);
        ScriptParser.getFunctionsStore().put(func);
        return "1";
    }

    /**
	 * Script Commande : prints debug informations.
	 * 
	 * @param words
	 * @return "1"
	 */
    public static String evalCmdDebug(String[] words) {
        ScriptParser.printTables();
        return "1";
    }
}
