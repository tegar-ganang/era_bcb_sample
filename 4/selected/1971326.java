package org.xjchelper.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang.StringUtils;
import org.xjchelper.Constant;
import org.xjchelper.state.Param;
import org.xjchelper.state.XjcHelperState;
import javax.swing.*;
import java.awt.event.*;
import java.util.Map;

public class SaveParamDialog extends JDialog {

    private JPanel contentPane;

    private JButton buttonOK;

    private JButton buttonCancel;

    private JTextField nameField;

    private Param param;

    private Project project;

    public SaveParamDialog(Param param, Project project) {
        this.param = param;
        this.project = project;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        nameField.setText(param.getTargetPackage());
        buttonOK.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonCancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        contentPane.registerKeyboardAction(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        if (StringUtils.isEmpty(nameField.getText())) {
            return;
        }
        XjcHelperState xjcHelperState = XjcHelperState.getInstance(project);
        Map<String, Param> params = xjcHelperState.getState().getParams();
        if (params.containsKey(nameField.getText())) {
            String question = String.format("Name [%s] already exist, do you want to overwrite ?", nameField.getText());
            int ret = Messages.showOkCancelDialog(project, question, Constant.TITLE, Messages.getQuestionIcon());
            if (ret != 0) {
                return;
            }
        }
        param.setName(nameField.getText());
        params.put(param.getName(), param);
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
