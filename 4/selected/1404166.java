package cmd;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Calendar;
import javax.swing.JOptionPane;
import controller.ProgDVBTimer;
import visualisation.JProgDVBTimer;

public class CreateTasksCmd implements ActionListener {

    private JProgDVBTimer parent;

    private File app;

    private String taskname;

    private String channel;

    private String winuserpassword;

    private Calendar start, stop;

    public CreateTasksCmd(Component comp) {
        if (comp instanceof JProgDVBTimer) {
            parent = (JProgDVBTimer) comp;
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (parent == null) return;
        getParameter();
        if (checkParameterOk()) {
            try {
                createTasks();
            } catch (FileNotFoundException ex) {
                parent.showErrorDialog("Programmdatei nicht gefunden", ex.getMessage());
                return;
            } catch (IllegalArgumentException ex) {
                parent.showErrorDialog("Fehlerhafte Eingabe", ex.getMessage());
                return;
            }
        } else {
            parent.showErrorDialog("Fehlerhafte Eingabe(n)", "Min. eine Eigabe ist fehlerhaft!");
            return;
        }
        JOptionPane.showMessageDialog(parent, "Aufnahme erfolgreich angelegt", "Erfolgreich", JOptionPane.INFORMATION_MESSAGE);
    }

    private void getParameter() {
        app = parent.getChoosenApp();
        taskname = parent.getTaskname();
        channel = parent.getChannel();
        winuserpassword = parent.getWinUserpassword();
        start = parent.getStartDateTime();
        stop = parent.getStopDateTime();
        start.add(Calendar.MONTH, 1);
        stop.add(Calendar.MONTH, 1);
    }

    private boolean checkParameterOk() {
        if (!app.exists() || !app.canExecute()) return false;
        if (taskname.isEmpty() || taskname.contains(":") || taskname.contains("/") || taskname.contains("\\")) return false;
        if (channel.isEmpty()) return false;
        if (winuserpassword.isEmpty()) return false;
        if (start.equals(stop)) return false;
        return true;
    }

    private void createTasks() throws FileNotFoundException, IllegalArgumentException {
        ProgDVBTimer timer = new ProgDVBTimer(taskname, channel, start, stop, winuserpassword);
        timer.setApp(app);
        if (timer.taskExist()) {
            int decision = parent.showConfirmDialog("Task �berschreiben?", "Der anzulegenden Task existiert bereits.\nSoll er �berschrieben werden?");
            if (decision == JOptionPane.NO_OPTION) {
                return;
            }
        }
        timer.generateTasks();
    }
}
