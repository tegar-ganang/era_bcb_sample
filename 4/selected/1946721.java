package jdiff;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.util.Log;
import jdiff.component.*;
import jdiff.text.FileLine;
import jdiff.util.*;
import jdiff.util.patch.*;

/**
 * The DualDiffManager provides the API to handle DualDiffs with only knowing
 * the View and adjusting the diff settings globally for all DualDiffs.  Each
 * View may have at most 1 DualDiff.  This class provides some convenience
 * methods for finding a DualDiff and acting on it.  This class does not act
 * on Views or EditPane, rather, it delegates to the DualDiff for such work.
 */
public class DualDiffManager {

    public static final String JDIFF_LINES = "jdiff-lines";

    public static final String BEEP_ON_ERROR = "jdiff.beep-on-error";

    public static final String HORIZ_SCROLL = "jdiff.horiz-scroll";

    public static final String SELECT_WORD = "jdiff.select-word";

    private static HashMap<View, DualDiff> dualDiffs = new HashMap<View, DualDiff>();

    private static HashMap<View, String> splitConfigs = new HashMap<View, String>();

    private static HashMap<View, HashMap<String, List<Integer>>> caretPositions = new HashMap<View, HashMap<String, List<Integer>>>();

    /**
     * @param view A View to find the corresponding DualDiff.
     * @return The DualDiff for the given view, or null if there is no DualDiff
     * for this View.
     */
    public static DualDiff getDualDiffFor(View view) {
        return (DualDiff) dualDiffs.get(view);
    }

    /**
     * @return true if there is a DualDiff enabled for the given View.
     */
    public static boolean isEnabledFor(View view) {
        return (dualDiffs.get(view) != null);
    }

    /**
     * Creates and applies a DualDiff to the given View.
     */
    public static void addTo(View view) {
        DualDiff dualDiff = new DualDiff(view);
        dualDiffs.put(view, dualDiff);
    }

    /**
     * Removes a DualDiff from the given View.
     */
    public static void removeFrom(View view) {
        dualDiffs.remove(view);
        splitConfigs.remove(view);
        caretPositions.remove(view);
        EditBus.send(new DiffMessage(view, DiffMessage.OFF));
    }

    private static void validateConfig(View view, String splitConfig) {
        if (splitConfig == null) {
            return;
        }
        HashSet<String> filenames = new HashSet<String>();
        Pattern p = Pattern.compile("\"(.*?)\"");
        Matcher m = p.matcher(splitConfig);
        while (m.find()) {
            String match = m.group(1);
            if (match != null) {
                if ("global".equals(match) || "view".equals(match) || "editpane".equals(match)) {
                    continue;
                }
                filenames.add(match);
            }
        }
        if (filenames.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String filename : filenames) {
                if (jEdit.getBuffer(filename) == null) {
                    sb.append(filename).append('\n');
                }
            }
            if (sb.toString().trim().length() > 0) {
                JOptionPane.showMessageDialog(view, "JDiff encountered this problem while restoring perspective:\n\nFile closed during diff:\n" + sb.toString(), "JDiff Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Toggle the DualDiff for the given View on or off.
     */
    public static void toggleFor(final View view) {
        if (DualDiffManager.isEnabledFor(view)) {
            toggleOffFor(view);
        } else {
            toggleOnFor(view);
        }
    }

    private static void toggleOffFor(final View view) {
        String splitConfig = splitConfigs.get(view);
        HashMap<String, List<Integer>> carets = caretPositions.get(view);
        DualDiffManager.removeFrom(view);
        if (jEdit.getBooleanProperty("jdiff.restore-view", true)) {
            if (splitConfig != null) {
                validateConfig(view, splitConfig);
                view.setSplitConfig(null, splitConfig);
            } else {
                view.unsplit();
            }
        }
        if (jEdit.getBooleanProperty("jdiff.restore-caret", true) && carets != null) {
            for (EditPane ep : view.getEditPanes()) {
                List<Integer> values = carets.get(ep.getBuffer().getPath(false));
                if (values != null) {
                    TextArea textArea = ep.getTextArea();
                    int max_caret = textArea.getBufferLength() - 1;
                    int caret_position = Math.min(values.get(0), max_caret);
                    int max_line = textArea.getLineCount() - 1;
                    int first_physical_line = Math.min(values.get(1), max_line);
                    textArea.setCaretPosition(Math.max(0, caret_position));
                    textArea.setFirstPhysicalLine(Math.max(0, first_physical_line));
                }
            }
            carets = null;
        }
        view.invalidate();
        view.validate();
    }

    private static void toggleOnFor(final View view) {
        String splitConfig = view.getSplitConfig();
        if (splitConfig != null) {
            splitConfigs.put(view, splitConfig);
        }
        boolean horizontal = false;
        JSplitPane splitPane = view.getSplitPane();
        if (splitPane != null) {
            horizontal = splitPane.getOrientation() == JSplitPane.VERTICAL_SPLIT;
        }
        EditPane[] editPanes = view.getEditPanes();
        if (editPanes.length != 2 || horizontal) {
            view.unsplit();
            view.splitVertically();
        }
        editPanes = view.getEditPanes();
        HashMap<String, List<Integer>> cps = new HashMap<String, List<Integer>>();
        List<Integer> values = new ArrayList<Integer>();
        values.add(editPanes[0].getTextArea().getCaretPosition());
        values.add(editPanes[0].getTextArea().getFirstPhysicalLine());
        cps.put(editPanes[0].getBuffer().getPath(false), values);
        values = new ArrayList<Integer>();
        values.add(editPanes[1].getTextArea().getCaretPosition());
        values.add(editPanes[1].getTextArea().getFirstPhysicalLine());
        cps.put(editPanes[1].getBuffer().getPath(false), values);
        caretPositions.put(view, cps);
        DualDiffManager.addTo(view);
        EditBus.send(new DiffMessage(view, DiffMessage.ON));
    }

    /**
     * Refresh the DualDiff for the given View.
     */
    public static void refreshFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff != null) {
            dualDiff.refresh();
        } else {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                view.getToolkit().beep();
            }
        }
    }

    /**
     * @return The diff ignoreCase setting for the given View.
     */
    public static boolean getIgnoreCaseFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff == null) {
            return false;
        }
        return dualDiff.getIgnoreCase();
    }

    /**
     * Toggle the diff ignoreCase setting for the given view.
     */
    public static void toggleIgnoreCaseFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff != null) {
            dualDiff.toggleIgnoreCase();
            dualDiff.refresh();
        } else {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                view.getToolkit().beep();
            }
        }
    }

    /**
     * @return The diff trimWhitespace setting for the given View.
     */
    public static boolean getTrimWhitespaceFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff == null) {
            return false;
        }
        return dualDiff.getTrimWhitespace();
    }

    /**
     * Toggle the diff trimWhitespace setting for the given View.
     */
    public static void toggleTrimWhitespaceFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff != null) {
            dualDiff.toggleTrimWhitespace();
            dualDiff.refresh();
        } else {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                view.getToolkit().beep();
            }
        }
    }

    /**
     * @return The diff ignoreAmountOfWhitepace setting for the given View.
     */
    public static boolean getIgnoreAmountOfWhitespaceFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff == null) {
            return false;
        }
        return dualDiff.getIgnoreAmountOfWhitespace();
    }

    /**
     * Toggle the diff ignoreAmountOfWhitespace for the given View.
     */
    public static void toggleIgnoreAmountOfWhitespaceFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff != null) {
            dualDiff.toggleIgnoreAmountOfWhitespace();
            dualDiff.refresh();
        } else {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                view.getToolkit().beep();
            }
        }
    }

    /**
     * @return The diff ignoreLineSeparators setting for the given View.
     */
    public static boolean getIgnoreLineSeparatorsFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff == null) {
            return false;
        }
        return dualDiff.getIgnoreLineSeparators();
    }

    /**
     * Toggle the diff ignoreLineSeparators for the given View.
     */
    public static void toggleIgnoreLineSeparatorsFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff != null) {
            dualDiff.toggleIgnoreLineSeparators();
            dualDiff.refresh();
        } else {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                view.getToolkit().beep();
            }
        }
    }

    /**
     * @return The diff ignoreAllWhitespace setting for the given View.
     */
    public static boolean getIgnoreAllWhitespaceFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff == null) {
            return false;
        }
        return dualDiff.getIgnoreAllWhitespace();
    }

    /**
     * Toggle the diff ignoreAllWhitespace setting for the given View.
     */
    public static void toggleIgnoreAllWhitespaceFor(View view) {
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        if (dualDiff != null) {
            dualDiff.toggleIgnoreAllWhitespace();
            dualDiff.refresh();
        } else {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                view.getToolkit().beep();
            }
        }
    }

    /**
     * Move to the next diff in the given EditPane.
     */
    public static void nextDiff(EditPane editPane) {
        if (editPane == null) {
            return;
        }
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(editPane.getView());
        if (dualDiff == null) {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                editPane.getToolkit().beep();
            }
            return;
        }
        if (editPane.equals(dualDiff.getEditPane0())) {
            dualDiff.nextDiff0();
        } else if (editPane.equals(dualDiff.getEditPane1())) {
            dualDiff.nextDiff1();
        } else {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                editPane.getToolkit().beep();
            }
        }
    }

    /**
     * Move to the previous diff in the given EditPane.
     */
    public static void prevDiff(EditPane editPane) {
        if (editPane == null) {
            return;
        }
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(editPane.getView());
        if (dualDiff == null) {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                editPane.getToolkit().beep();
            }
            return;
        }
        if (editPane.equals(dualDiff.getEditPane0())) {
            dualDiff.prevDiff0();
        } else if (editPane.equals(dualDiff.getEditPane1())) {
            dualDiff.prevDiff1();
        } else {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                editPane.getToolkit().beep();
            }
        }
    }

    /**
     * Move to the first diff.    
     */
    public static void firstDiff(EditPane editPane) {
        if (editPane == null) {
            return;
        }
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(editPane.getView());
        if (dualDiff == null) {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                editPane.getToolkit().beep();
            }
            return;
        }
        if (editPane.equals(dualDiff.getEditPane0())) {
            dualDiff.firstDiff0();
        } else if (editPane.equals(dualDiff.getEditPane1())) {
            dualDiff.firstDiff1();
        } else {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                editPane.getToolkit().beep();
            }
        }
    }

    /**
     * Move to the last diff.    
     */
    public static void lastDiff(EditPane editPane) {
        if (editPane == null) {
            return;
        }
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(editPane.getView());
        if (dualDiff == null) {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                editPane.getToolkit().beep();
            }
            return;
        }
        if (editPane.equals(dualDiff.getEditPane0())) {
            dualDiff.lastDiff0();
        } else if (editPane.equals(dualDiff.getEditPane1())) {
            dualDiff.lastDiff1();
        } else {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                editPane.getToolkit().beep();
            }
        }
    }

    /**
     * Moves the current diff hunk from the left text area to the right text area.
     */
    public static void moveRight(EditPane editPane) {
        if (editPane == null) {
            return;
        }
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(editPane.getView());
        if (dualDiff == null) {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                editPane.getToolkit().beep();
            }
            return;
        }
        dualDiff.moveRight(editPane);
    }

    /**
     * Moves the current diff hunk from the right text area to the left text area.
     */
    public static void moveLeft(EditPane editPane) {
        if (editPane == null) {
            return;
        }
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(editPane.getView());
        if (dualDiff == null) {
            editPane.getToolkit().beep();
            return;
        }
        dualDiff.moveLeft(editPane);
    }

    /**
     * Move all non-conflicting diff hunks from the left text are to the right text area.
     */
    public static void moveMultipleRight(EditPane editPane) {
        if (editPane == null) {
            return;
        }
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(editPane.getView());
        if (dualDiff == null) {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                editPane.getToolkit().beep();
            }
            return;
        }
        dualDiff.moveMultipleRight(editPane);
    }

    /**
     * Move all non-conflicting diff hunks from the right text area to the left text area.
     */
    public static void moveMultipleLeft(EditPane editPane) {
        if (editPane == null) {
            return;
        }
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(editPane.getView());
        if (dualDiff == null) {
            editPane.getToolkit().beep();
            return;
        }
        dualDiff.moveMultipleLeft(editPane);
    }

    /**
     * TODO: This probably doesn't belong in DualDiffManager and should go into
     * a separate class with the applyPatch method.
     * This outputs a unified diff (as opposed to an edit diff or a context diff)
     * to a new buffer in the given View.
     */
    public static void diffNormalOutput(View view) {
        if (!DualDiffManager.isEnabledFor(view)) {
            if (jEdit.getBooleanProperty(BEEP_ON_ERROR)) {
                view.getToolkit().beep();
            }
            return;
        }
        DualDiff dualDiff = DualDiffManager.getDualDiffFor(view);
        Buffer buf0 = dualDiff.getEditPane0().getBuffer();
        Buffer buf1 = dualDiff.getEditPane1().getBuffer();
        FileLine[] fileLines0 = DualDiffUtil.getFileLines(dualDiff, buf0);
        FileLine[] fileLines1 = DualDiffUtil.getFileLines(dualDiff, buf1);
        Diff d = new JDiffDiff(fileLines0, fileLines1);
        Diff.Change script = d.diff_2();
        if (script == null) {
            GUIUtilities.message(view, "jdiff.identical-files", null);
            return;
        }
        StringWriter sw = new StringWriter();
        DiffOutput diffOutput = new DiffNormalOutput(fileLines0, fileLines1);
        diffOutput.setOut(new BufferedWriter(sw));
        diffOutput.setLineSeparator("\n");
        try {
            diffOutput.writeScript(script);
        } catch (IOException ioe) {
            Log.log(Log.DEBUG, DualDiff.class, ioe);
        }
        View outputView = jEdit.getFirstView();
        for (; outputView != null; outputView = outputView.getNext()) {
            if (!DualDiffManager.isEnabledFor(outputView)) {
                break;
            }
        }
        if (outputView == null) {
            outputView = jEdit.newView(view, view.getBuffer());
        }
        Buffer outputBuffer = jEdit.newFile(outputView);
        String s = sw.toString();
        outputBuffer.insert(0, s);
        if (s.endsWith("\n") && outputBuffer.getLength() > 0) {
            outputBuffer.remove(outputBuffer.getLength() - 1, 1);
        }
    }

    /**
     * TODO: This probably doesn't belong in DualDiffManager and should go into
     * a separate class with the diffNormalOutput method.
     * Shows a dialog for the user to select
     * a patch file, then applies that patch file to the current buffer.
     * @param view the view displaying the buffer
     */
    public static void applyPatch(View view) {
        try {
            PatchSelectionDialog dialog = new PatchSelectionDialog(view);
            DualDiffUtil.center(view, dialog);
            dialog.setVisible(true);
            String patch_file = dialog.getPatchFile();
            if (patch_file == null || patch_file.length() == 0) {
                return;
            }
            Reader reader = new BufferedReader(new FileReader(patch_file));
            StringWriter writer = new StringWriter();
            PatchUtils.copyToWriter(reader, writer);
            String patch = writer.toString();
            if (patch == null || patch.length() == 0) {
                JOptionPane.showMessageDialog(view, "Invalid patch file, file has no content.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Buffer buffer = view.getEditPane().getBuffer();
            String bufferText = buffer.getText(0, buffer.getLength());
            String results = Patch.patch(patch, bufferText);
            jEdit.newFile(view).insert(0, results);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(view, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
