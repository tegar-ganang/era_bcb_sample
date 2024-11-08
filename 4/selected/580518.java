package ch.intertec.storybook.toolkit;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import ch.intertec.storybook.main.MainFrame;
import ch.intertec.storybook.model.InternalPeer;
import ch.intertec.storybook.model.PersistenceManager;
import ch.intertec.storybook.toolkit.Constants.ComponentName;
import ch.intertec.storybook.toolkit.swing.SwingTools;

public class DbTools {

    private static Logger log = Logger.getLogger(DbTools.class);

    private static Statement stmt;

    public static boolean checkAndAlterModel() {
        String projectDbVersion = InternalPeer.getDbModelVersion();
        String currentDbVersion = Constants.Application.DB_MODEL_VERSION.toString();
        if (projectDbVersion.equals(currentDbVersion)) {
            return true;
        }
        try {
            stmt = PersistenceManager.getInstance().getConnection().createStatement();
        } catch (Exception e) {
            log.error(e);
            SwingTools.showException(e);
            return false;
        }
        if (projectDbVersion.equals("0") || projectDbVersion.equals("0.1") || projectDbVersion.equals("0.1") || projectDbVersion.equals("0.2") || projectDbVersion.equals("0.3") || projectDbVersion.equals("0.4") || projectDbVersion.equals("0.5") || projectDbVersion.equals("0.6") || projectDbVersion.equals("0.7") || projectDbVersion.equals("0.8") || projectDbVersion.equals("0.9") || projectDbVersion.equals("1.0")) {
            boolean ret = false;
            JOptionPane.showMessageDialog(MainFrame.getInstance(), "This file version is too old. Update is not supported anymore.", I18N.getMsg("msg.common.error"), JOptionPane.ERROR_MESSAGE);
            return ret;
        }
        UpdateDialog dlg = new UpdateDialog();
        SwingTools.showDialog(dlg, MainFrame.getInstance());
        File file = ProjectTools.getCurrentFile();
        File backupFile = new File(file.getAbsolutePath() + ".bak");
        try {
            FileUtils.copyFile(file, backupFile);
        } catch (IOException e1) {
            dlg.append();
            dlg.append("Cannot create backup file:");
            dlg.append(backupFile.getAbsolutePath());
            dlg.append();
        }
        boolean ret = false;
        if (projectDbVersion.equals("1.1")) {
            ret = alterFrom1_1to1_2(dlg);
            if (ret) {
                ret = alterFrom1_2to1_3(dlg);
            }
            if (ret) {
                ret = alterFrom1_3to1_4(dlg);
            }
            if (ret) {
                ret = alterFrom1_4to1_5(dlg);
            }
        } else if (projectDbVersion.equals("1.2")) {
            ret = alterFrom1_2to1_3(dlg);
            if (ret) {
                ret = alterFrom1_3to1_4(dlg);
            }
            if (ret) {
                ret = alterFrom1_4to1_5(dlg);
            }
        } else if (projectDbVersion.equals("1.3")) {
            ret = alterFrom1_3to1_4(dlg);
            if (ret) {
                ret = alterFrom1_4to1_5(dlg);
            }
        } else if (projectDbVersion.equals("1.4")) {
            ret = alterFrom1_4to1_5(dlg);
        }
        if (ret) {
            dlg.append();
            dlg.append("File");
            dlg.append(PersistenceManager.getInstance().getFile().toString());
            dlg.append("was updated successfully.");
            dlg.append();
            dlg.append("Press 'Close' to continue.");
            return ret;
        }
        JOptionPane.showMessageDialog(MainFrame.getInstance(), I18N.getMsg("msg.error.wrong.version", projectDbVersion), I18N.getMsg("msg.common.error"), JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private static boolean alterFrom1_1to1_2(UpdateDialog dlg) {
        try {
            dlg.append("Updating file version from 1.1 to 1.2 ...");
            String sql = "alter table person alter column firstname varchar(256)";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table person alter column lastname varchar(256)";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table person alter column abbreviation varchar(32)";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table person alter column occupation varchar(256)";
            executeSQLStatement(dlg, sql, stmt);
            InternalPeer.setDbModelVersion("1.2");
            return true;
        } catch (Exception e) {
            log.error(e);
            SwingTools.showException(e);
            return false;
        }
    }

    private static boolean alterFrom1_2to1_3(UpdateDialog dlg) {
        try {
            dlg.append("Updating file version from 1.2 to 1.3 ...");
            String sql = "create table ideas (" + "id identity primary key," + "status int," + "note varchar(8192)," + "category varchar(1024))";
            executeSQLStatement(dlg, sql, stmt);
            sql = "create table gender (" + "id identity primary key," + "name varchar(1024))";
            executeSQLStatement(dlg, sql, stmt);
            sql = "insert into gender(id, name) values(1, '" + I18N.getMsg("msg.dlg.person.gender.male") + "')";
            executeSQLStatement(dlg, sql, stmt);
            sql = "insert into gender(id, name) values(2, '" + I18N.getMsg("msg.dlg.person.gender.female") + "')";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table person add column gender_id integer";
            executeSQLStatement(dlg, sql, stmt);
            sql = "update person set gender_id = 2 where gender = false";
            executeSQLStatement(dlg, sql, stmt);
            sql = "update person set gender_id = 1 where gender = true";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table person drop column gender";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table scene add column relative_date_difference integer";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table scene add column relative_scene_id integer";
            executeSQLStatement(dlg, sql, stmt);
            sql = "update scene set relative_scene_id=-1, relative_date_difference=-1";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table chapter alter column notes varchar(32768)";
            executeSQLStatement(dlg, sql, stmt);
            InternalPeer.setDbModelVersion("1.3");
            return true;
        } catch (Exception e) {
            log.error(e);
            SwingTools.showException(e);
            return false;
        }
    }

    private static boolean alterFrom1_3to1_4(UpdateDialog dlg) {
        try {
            dlg.append("Updating file version from 1.3 to 1.4 ...");
            String sql = "drop table item if exists";
            executeSQLStatement(dlg, sql, stmt);
            sql = "drop table item_link if exists";
            executeSQLStatement(dlg, sql, stmt);
            sql = "create table tag (" + "id identity primary key," + "type int," + "category varchar(1024)," + "name varchar(1024)," + "description varchar(4096)," + "notes varchar(4096))";
            executeSQLStatement(dlg, sql, stmt);
            sql = "create table tag_link (" + "id identity primary key," + "tag_id int," + "start_scene_id int," + "end_scene_id int," + "character_id int," + "location_id int" + ")";
            executeSQLStatement(dlg, sql, stmt);
            InternalPeer.setDbModelVersion("1.4");
            return true;
        } catch (Exception e) {
            log.error(e);
            SwingTools.showException(e);
            return false;
        }
    }

    private static boolean alterFrom1_4to1_5(UpdateDialog dlg) {
        try {
            dlg.append("Updating file version from 1.4 to 1.5 ...");
            String sql = "";
            sql = "alter table gender add column childhood int";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table gender add column adolescence int";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table gender add column adulthood int";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table gender add column retirement int";
            executeSQLStatement(dlg, sql, stmt);
            for (int i = 1; i < 3; ++i) {
                sql = "update gender set gender.childhood='12' where GENDER.id=" + i;
                executeSQLStatement(dlg, sql, stmt);
                sql = "update gender set gender.adolescence='6' where GENDER.id=" + i;
                executeSQLStatement(dlg, sql, stmt);
                sql = "update gender set gender.adulthood='47' where GENDER.id=" + i;
                executeSQLStatement(dlg, sql, stmt);
                sql = "update gender set gender.retirement='14' where GENDER.id=" + i;
                executeSQLStatement(dlg, sql, stmt);
            }
            sql = "alter table chapter alter column description varchar(8192)";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table chapter alter column notes varchar(8192)";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table location alter column notes varchar(8192)";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table person alter column notes varchar(8192)";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table strand alter column notes varchar(8192)";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table tag alter column description varchar(8192)";
            executeSQLStatement(dlg, sql, stmt);
            sql = "alter table tag alter column notes varchar(8192)";
            executeSQLStatement(dlg, sql, stmt);
            InternalPeer.setDbModelVersion("1.5");
            return true;
        } catch (Exception e) {
            log.error(e);
            SwingTools.showException(e);
            return false;
        }
    }

    private static void executeSQLStatement(UpdateDialog dlg, String sql, Statement stmt) {
        try {
            log.debug(sql);
            stmt.execute(sql);
        } catch (SQLException e) {
            log.error(e);
            dlg.append(I18N.getMsgColon(("msg.common.warning") + e.getMessage()));
        }
    }

    public static java.sql.Date dateStrToSqlDate(String dateStr) throws ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date date = df.parse(dateStr);
        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        return sqlDate;
    }

    public static Date getNowAsSqlDate() {
        return new Date(new java.util.Date().getTime());
    }

    public static Date calendar2SQLDate(Calendar cal) {
        return new java.sql.Date(cal.getTimeInMillis());
    }
}

@SuppressWarnings("serial")
class UpdateDialog extends JDialog {

    private JTextArea taInfo;

    public UpdateDialog() {
        initGUI();
    }

    private void initGUI() {
        LayoutManager layout = new MigLayout("wrap,fill", "[]", "[grow,fill][]");
        setLayout(layout);
        setTitle("Updating file version ...");
        taInfo = new JTextArea();
        taInfo.setEditable(false);
        taInfo.setLineWrap(true);
        taInfo.setWrapStyleWord(true);
        taInfo.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        JScrollPane scroller = new JScrollPane(taInfo);
        scroller.setPreferredSize(new Dimension(400, 300));
        JButton btClose = new JButton();
        btClose.setAction(getOkAction());
        btClose.setText(I18N.getMsg("msg.common.close"));
        btClose.setName(ComponentName.OK_BUTTON.toString());
        SwingTools.addEnterAction(btClose, getOkAction());
        add(scroller, "grow");
        add(btClose);
    }

    public void append(String text) {
        taInfo.append(text + "\n");
    }

    public void append() {
        taInfo.append("\n");
    }

    private UpdateDialog getThis() {
        return this;
    }

    private AbstractAction getOkAction() {
        return new AbstractAction() {

            public void actionPerformed(ActionEvent evt) {
                getThis().dispose();
            }
        };
    }
}
