package jemu.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import jemu.core.*;
import jemu.core.device.*;
import jemu.core.device.memory.*;
import jemu.util.diss.*;

/**
 *
 * @author  Richard
 */
public class Debugger extends JFrame implements MouseListener, ActionListener {

    public static final Color navy = new Color(0, 0, 127);

    protected Computer computer;

    protected long startCycles = 0;

    protected JFileChooser fileDlg;

    /** Creates new form Debugger */
    public Debugger() {
        initComponents();
        bRun.setBorder(new BevelBorder(BevelBorder.RAISED));
        bRun.setBackground(new Color(70, 255, 70));
        bRun.setForeground(new Color(0, 0, 0));
        bRun.addActionListener(this);
        bStop.addActionListener(this);
        bStop.setBackground(new Color(255, 70, 70));
        bStop.setForeground(new Color(0, 0, 0));
        bStop.setBorder(new BevelBorder(BevelBorder.RAISED));
        bStep.addActionListener(this);
        bStep.setBackground(new Color(255, 255, 70));
        bStep.setForeground(new Color(0, 0, 0));
        bStep.setBorder(new BevelBorder(BevelBorder.RAISED));
        bStepOver.addActionListener(this);
        bStepOver.setBackground(new Color(70, 255, 255));
        bStepOver.setForeground(new Color(0, 0, 0));
        bStepOver.setBorder(new BevelBorder(BevelBorder.RAISED));
        mSave.addActionListener(this);
        mGoto.addActionListener(this);
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(getFontMetrics(eMemory.getFont()).getHeight());
        jScrollPane1.setBorder(new BevelBorder(BevelBorder.LOWERED));
    }

    public void setComputer(Computer value) {
        if (computer != null) computer.removeActionListener(this);
        computer = value;
        eDisassembler.setComputer(computer);
        eMemory.setComputer(computer);
        if (computer != null) {
            computer.addActionListener(this);
            eRegisters.setDevice(computer.getProcessor());
            updateDisplay();
        } else eRegisters.setDevice(null);
    }

    protected void updateDisplay() {
        eDisassembler.setPC(computer.getProcessor().getProgramCounter());
        lCycleCount.setText(Long.toString(computer.getProcessor().getCycles() - startCycles));
        eRegisters.setValues();
        repaint();
    }

    protected long getGotoAddress() {
        String address = JOptionPane.showInputDialog("Address: ", "#");
        if (address == null) return -1;
        address = address.trim();
        if (address.length() == 0) return -1;
        switch(address.charAt(0)) {
            case '#':
            case '&':
            case '$':
                return Long.parseLong(address.substring(1), 16);
            default:
                return Long.parseLong(address);
        }
    }

    public void actionPerformed(ActionEvent e) {
        computer.clearRunToAddress();
        if (e.getSource() == bRun) computer.start(); else if (e.getSource() == bStop) computer.stop(); else if (e.getSource() == bStep) computer.step(); else if (e.getSource() == bStepOver) computer.stepOver(); else if (e.getSource() == computer) updateDisplay(); else if (e.getSource() == mGoto) {
            long address = getGotoAddress();
            if (address != -1) {
                if (popupMenu.getInvoker() == eDisassembler) eDisassembler.setAddress((int) address); else eMemory.setAddress((int) address);
            }
        } else if (e.getSource() == mSave) {
            if (popupMenu.getInvoker() == eDisassembler) saveDisassembly(); else saveMemory();
        }
        computer.setFrameSkip(0);
        computer.updateDisplay(false);
    }

    protected File showSaveDialog(String title) {
        if (fileDlg == null) fileDlg = new JFileChooser();
        fileDlg.setDialogTitle(title);
        return fileDlg.showSaveDialog(bRun) == JFileChooser.APPROVE_OPTION ? fileDlg.getSelectedFile() : null;
    }

    public void saveMemory() {
        saveMemory(eMemory.selStart, eMemory.selEnd);
    }

    public void saveMemory(int start, int end) {
        File file = showSaveDialog("Save Memory");
        if (file != null) {
            try {
                FileOutputStream io = new FileOutputStream(file);
                try {
                    Memory mem = computer.getMemory();
                    for (int addr = start; addr <= end; addr++) io.write(mem.readByte(addr));
                } finally {
                    io.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveDisassembly() {
        saveDisassembly(eDisassembler.selStart, eDisassembler.selEnd);
    }

    public void saveDisassembly(int start, int end) {
        File file = showSaveDialog("Save Disassembly");
        if (file != null) {
            int[] addr = new int[] { start };
            try {
                FileOutputStream io = new FileOutputStream(file);
                try {
                    Disassembler diss = computer.getDisassembler();
                    Memory mem = computer.getMemory();
                    while (addr[0] <= end) {
                        String s = Util.hex((short) addr[0]) + ": ";
                        io.write((s + diss.disassemble(mem, addr) + "\r\n").getBytes());
                    }
                } finally {
                    io.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initComponents() {
        popupMenu = new javax.swing.JPopupMenu();
        mGoto = new javax.swing.JMenuItem();
        mSave = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel2.setBackground(new Color(170, 170, 170));
        jPanel2.setBorder(new BevelBorder(BevelBorder.LOWERED));
        bRun = new javax.swing.JButton();
        bStop = new javax.swing.JButton();
        bStep = new jemu.ui.EButton();
        bStepOver = new jemu.ui.EButton();
        jPanel3 = new javax.swing.JPanel();
        jPanel3.setBackground(new Color(170, 170, 170));
        jPanel3.setBorder(new BevelBorder(BevelBorder.LOWERED));
        lCycles = new javax.swing.JLabel();
        lCycleCount = new javax.swing.JLabel();
        eRegisters = new jemu.ui.ERegisters();
        jSplitPane1 = new javax.swing.JSplitPane();
        eDisassembler = new jemu.ui.EDisassembler();
        eDisassembler.setBackground(new Color(190, 190, 220));
        jScrollPane1 = new javax.swing.JScrollPane();
        eMemory = new jemu.ui.EMemory();
        eMemory.setBackground(new Color(190, 220, 190));
        eRegisters.setBackground(new Color(170, 170, 170));
        mGoto.setText("Goto...");
        popupMenu.add(mGoto);
        mSave.setText("Save...");
        popupMenu.add(mSave);
        setTitle("JavaCPC Debugger");
        jPanel1.setLayout(new java.awt.BorderLayout());
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        bRun.setText("Run");
        jPanel2.add(bRun);
        bStop.setText("Stop");
        jPanel2.add(bStop);
        bStep.setText("Step");
        jPanel2.add(bStep);
        bStepOver.setText("Step Over");
        jPanel2.add(bStepOver);
        jPanel1.add(jPanel2, java.awt.BorderLayout.CENTER);
        lCycles.setForeground(new java.awt.Color(70, 255, 70));
        lCycles.setText("Cycles:");
        jPanel3.add(lCycles);
        lCycleCount.setText("0");
        lCycleCount.addMouseListener(this);
        lCycleCount.setForeground(new java.awt.Color(70, 70, 255));
        jPanel3.add(lCycleCount);
        jPanel1.add(jPanel3, java.awt.BorderLayout.EAST);
        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_END);
        eRegisters.setLayout(null);
        getContentPane().add(eRegisters, java.awt.BorderLayout.LINE_END);
        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setContinuousLayout(true);
        eDisassembler.setComponentPopupMenu(popupMenu);
        eDisassembler.addMouseListener(this);
        jSplitPane1.setTopComponent(eDisassembler);
        eMemory.setComponentPopupMenu(popupMenu);
        jScrollPane1.setViewportView(eMemory);
        jSplitPane1.setRightComponent(jScrollPane1);
        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);
        pack();
    }

    public void mouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getSource() == lCycleCount) {
            Debugger.this.lCycleCountMouseClicked(evt);
        } else if (evt.getSource() == eDisassembler) {
            Debugger.this.eDisassemblerMouseClicked(evt);
        }
    }

    public void mouseEntered(java.awt.event.MouseEvent evt) {
    }

    public void mouseExited(java.awt.event.MouseEvent evt) {
    }

    public void mousePressed(java.awt.event.MouseEvent evt) {
    }

    public void mouseReleased(java.awt.event.MouseEvent evt) {
    }

    private void eDisassemblerMouseClicked(java.awt.event.MouseEvent e) {
        if (e.getClickCount() == 2) {
            int addr = eDisassembler.getAddress(e.getY());
            if (addr != -1) {
                computer.setRunToAddress(addr);
                computer.start();
            }
        }
    }

    private void lCycleCountMouseClicked(java.awt.event.MouseEvent e) {
        if (e.getClickCount() == 2) {
            startCycles = computer.getProcessor().getCycles();
            lCycleCount.setText("0");
        }
    }

    protected javax.swing.JButton bRun;

    protected jemu.ui.EButton bStep;

    protected jemu.ui.EButton bStepOver;

    protected javax.swing.JButton bStop;

    protected jemu.ui.EDisassembler eDisassembler;

    protected jemu.ui.EMemory eMemory;

    protected jemu.ui.ERegisters eRegisters;

    protected javax.swing.JPanel jPanel1;

    protected javax.swing.JPanel jPanel2;

    protected javax.swing.JPanel jPanel3;

    protected javax.swing.JScrollPane jScrollPane1;

    protected javax.swing.JSplitPane jSplitPane1;

    protected javax.swing.JLabel lCycleCount;

    protected javax.swing.JLabel lCycles;

    protected javax.swing.JMenuItem mGoto;

    protected javax.swing.JMenuItem mSave;

    protected javax.swing.JPopupMenu popupMenu;
}
