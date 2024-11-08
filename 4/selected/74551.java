package PolishNatation.PolishNatation;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;

public class Interface implements ActionListener {

    private TranslationInPolishNotation translationInPolishNotation;

    private WriteAndReadToFile writeAndReadToFile;

    private JButton calculate;

    private JTextField input, output, result;

    private JLabel expression, rpn, res;

    private JCheckBox readAFile, writeToFile;

    private JTextArea log;

    Interface() {
        writeAndReadToFile = new WriteAndReadToFile();
        translationInPolishNotation = new TranslationInPolishNotation();
        JFrame frame = new JFrame("Translation in RPN");
        frame.setLayout(null);
        frame.setSize(373, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        readAFile = new JCheckBox("Read a file: ", false);
        writeToFile = new JCheckBox("Write to file: ", false);
        input = new JTextField(20);
        output = new JTextField(20);
        result = new JTextField(20);
        expression = new JLabel("Expression: ");
        rpn = new JLabel("RPN: ");
        res = new JLabel("Result: ");
        log = new JTextArea();
        calculate = new JButton("Calculate");
        input.addActionListener(this);
        calculate.addActionListener(this);
        readAFile.setSize(100, 20);
        readAFile.setLocation(96, 10);
        expression.setSize(100, 20);
        expression.setLocation(10, 40);
        input.setSize(250, 20);
        input.setLocation(100, 40);
        rpn.setSize(100, 20);
        rpn.setLocation(10, 68);
        output.setSize(250, 20);
        output.setLocation(100, 68);
        res.setSize(100, 20);
        res.setLocation(10, 95);
        result.setSize(250, 20);
        result.setLocation(100, 95);
        writeToFile.setSize(100, 20);
        writeToFile.setLocation(96, 120);
        calculate.setSize(150, 20);
        calculate.setLocation(100, 145);
        log.setSize(338, 300);
        log.setLocation(10, 180);
        frame.add(expression);
        frame.add(writeToFile);
        frame.add(input);
        frame.add(res);
        frame.add(output);
        frame.add(rpn);
        frame.add(result);
        frame.add(calculate);
        frame.add(log);
        frame.add(readAFile);
        frame.setVisible(true);
    }

    public void actionPerformed(ActionEvent action) {
        if (readAFile.isSelected()) {
            input.setEditable(false);
            String inputData = writeAndReadToFile.readFile("./inputData.txt", "");
            output.setText(translationInPolishNotation.inPolishNotation(inputData, false));
            input.setText(inputData);
            result.setText(Double.toString(translationInPolishNotation.solvePolishRecord(translationInPolishNotation.inPolishNotation(inputData, true))));
        }
        if (input.getText().length() > 0) {
            output.setText(translationInPolishNotation.inPolishNotation(input.getText(), false));
            result.setText(Double.toString(translationInPolishNotation.solvePolishRecord(translationInPolishNotation.inPolishNotation(input.getText(), true))));
        }
        if (writeToFile.isSelected()) {
            writeAndReadToFile.writeToFile("./result.txt", "Infix notation: " + input.getText() + " Polish notation: " + output.getText() + " Result: " + result.getText());
        }
    }

    public static void main(String[] args) {
        new Interface();
    }
}
