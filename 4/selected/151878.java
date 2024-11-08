package strudle;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import java.io.*;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SoundTab extends CTabItem {

    String nomefile;

    boolean continuePlaying = false;

    private static final int accuracy = 100;

    AudioFileFormat aff = null;

    AudioInputStream ais = null;

    AudioFormat af = null;

    public Clip mySound = null;

    SourceDataLine line = null;

    WaveCanvas canvas;

    private Test test;

    Text startSel;

    Text endSel;

    Patient patient;

    SelectionSet selections;

    public SoundTab(final CTabFolder tabFolder, int style, Patient patient, String nomefile) {
        super(tabFolder, style);
        this.nomefile = nomefile;
        this.setText("New");
        final Composite myComposite = new Composite(tabFolder, SWT.NONE);
        this.setControl(myComposite);
        this.patient = patient;
        openAudioFile(new File(nomefile));
        System.out.println("framesize: " + af.getFrameSize());
        System.out.println("sample size in bits: " + af.getSampleSizeInBits());
        System.out.println("framerate: " + af.getFrameRate());
        System.out.println("length in frames: " + ais.getFrameLength());
        System.out.println("lunghezza: " + ais.getFrameLength() / af.getFrameRate() + " secondi");
        createContents(myComposite);
    }

    public long getAudioDataLength() {
        return ais.getFrameLength();
    }

    private boolean openAudioFile(File audioFile) {
        try {
            aff = AudioSystem.getAudioFileFormat(audioFile);
            ais = AudioSystem.getAudioInputStream(audioFile);
            af = ais.getFormat();
        } catch (Exception ex) {
            System.out.println(ex);
            return false;
        }
        return true;
    }

    @SuppressWarnings("unused")
    private int[] getAudioData(byte[] audioBytes) {
        int[] audioData = null;
        if (af.getSampleSizeInBits() == 16) {
            int nlengthInSamples = audioBytes.length / 2;
            audioData = new int[nlengthInSamples];
            if (af.isBigEndian()) {
                for (int i = 0; i < nlengthInSamples; i++) {
                    int MSB = (int) audioBytes[2 * i];
                    int LSB = (int) audioBytes[2 * i + 1];
                    audioData[i] = MSB << 8 | (255 & LSB);
                }
            } else {
                for (int i = 0; i < nlengthInSamples; i++) {
                    int LSB = (int) audioBytes[2 * i];
                    int MSB = (int) audioBytes[2 * i + 1];
                    audioData[i] = MSB << 8 | (255 & LSB);
                    System.out.println(audioData[i]);
                }
            }
        } else if (af.getSampleSizeInBits() == 8) {
            int nlengthInSamples = audioBytes.length;
            audioData = new int[nlengthInSamples];
            if (af.getEncoding().toString().startsWith("PCM_SIGN")) {
                for (int i = 0; i < audioBytes.length; i++) {
                    audioData[i] = audioBytes[i];
                }
            } else {
                for (int i = 0; i < audioBytes.length; i++) {
                    audioData[i] = audioBytes[i] - 128;
                }
            }
        }
        return audioData;
    }

    private void play() {
        int offset, count;
        continuePlaying = true;
        openAudioFile(new File(nomefile));
        if (canvas.getSelectionStart() == 0 && canvas.getSelectionEnd() == 0) {
            offset = 0;
            count = (int) ais.getFrameLength() * af.getFrameSize();
        } else {
            offset = canvas.getSelectionStart();
            count = (canvas.getSelectionEnd() - canvas.getSelectionStart());
            System.out.println("Suona da " + offset + " a " + (offset + count) + " in tutto " + count + " frames, " + count / af.getFrameRate() + " secondi");
        }
        int ok = 0;
        int read = 0;
        offset = offset * af.getFrameSize();
        count = count * af.getFrameSize();
        byte[] buffer = new byte[count];
        try {
            ais.skip(offset);
            while (continuePlaying && (ok < count) && (read = ais.read(buffer)) != -1) {
                ok += read;
                int w = line.write(buffer, 0, read);
                System.out.println("Letti " + read + " bytes, scritti " + w + " bytes");
            }
            ais.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        continuePlaying = false;
    }

    void stop() {
        if (continuePlaying) continuePlaying = false;
    }

    private void createContents(final Composite shell) {
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 20;
        layout.marginWidth = 20;
        shell.setLayout(layout);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(af);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        line.start();
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = 200;
        canvas = new WaveCanvas(shell, SWT.NONE, af);
        canvas.setLayoutData(gd);
        canvas.setSize((int) ((ais.getFrameLength() / af.getFrameSize()) / af.getFrameRate() * accuracy), 200);
        final ScrollBar hBar = canvas.getHorizontalBar();
        canvas.setHorizontalScrollBarVisibility(WaveCanvas.ALWAYS);
        canvas.setClipLength(this.getAudioDataLength(), accuracy);
        canvas.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        Composite compBarra = new Composite(shell, SWT.NONE);
        compBarra.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        compBarra.setLayout(new RowLayout());
        Display display = shell.getDisplay();
        final Text framenum = new Text(compBarra, SWT.BORDER);
        framenum.setEditable(false);
        framenum.setCapture(false);
        ToolBar barra = new ToolBar(compBarra, SWT.NONE);
        Image image1 = new Image(display, "..\\strudle\\images\\play.gif");
        ToolItem item1 = new ToolItem(barra, SWT.PUSH);
        item1.setImage(image1);
        item1.setToolTipText("Play");
        item1.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                play();
            }
        });
        Image image2 = new Image(display, "..\\Strudle\\images\\stop.gif");
        ToolItem item2 = new ToolItem(barra, SWT.PUSH);
        item2.setImage(image2);
        item2.setToolTipText("Stop");
        item2.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                stop();
            }
        });
        Image image3 = new Image(display, "..\\Strudle\\images\\startsel.gif");
        ToolItem item3 = new ToolItem(barra, SWT.PUSH);
        item3.setImage(image3);
        item3.setToolTipText("Inizio selezione");
        item3.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                startSel.setText(canvas.setSelectionStart(canvas.selectionCur) + "");
            }
        });
        Image image4 = new Image(display, "..\\Strudle\\images\\endsel.gif");
        ToolItem item4 = new ToolItem(barra, SWT.PUSH);
        item4.setImage(image4);
        item4.setToolTipText("Fine selezione");
        item4.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                endSel.setText(canvas.setSelectionEnd(canvas.selectionCur) + "");
            }
        });
        ToolItem item5 = new ToolItem(barra, SWT.PUSH);
        item5.setText("Associa ad una prova");
        canvas.addMouseMoveListener(new MouseMoveListener() {

            public void mouseMove(MouseEvent e) {
                if (e.widget.equals(canvas)) {
                    int x = hBar.getSelection() * (hBar.getMaximum() - hBar.getThumb()) / Math.max(1, hBar.getMaximum() - hBar.getThumb());
                    x += e.x;
                    canvas.setCurX(x);
                    canvas.redraw(x - 50, 0, 100, canvas.getSize().y, true);
                    if (x <= canvas.getCanvasLength()) framenum.setText(x * canvas.getFramesPerPixel() + "");
                    if (e.stateMask == 524288) {
                        System.out.println(e.toString());
                    }
                }
            }
        });
        canvas.addMouseListener(new MouseListener() {

            @SuppressWarnings("unused")
            int start;

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
                int x = hBar.getSelection() * (hBar.getMaximum() - hBar.getThumb()) / Math.max(1, hBar.getMaximum() - hBar.getThumb());
                x += e.x;
                start = x;
                canvas.selectionRec = null;
                canvas.setSelectionCur(x);
                canvas.redraw();
            }

            public void mouseUp(MouseEvent e) {
            }
        });
        final Group gruppo = new Group(shell, SWT.SHADOW_ETCHED_IN);
        gruppo.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout glgruppo = new GridLayout();
        glgruppo.numColumns = 3;
        gruppo.setLayout(glgruppo);
        Label selTest = new Label(gruppo, SWT.NONE);
        selTest.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        selTest.setText("Test da verificare: ");
        final Text testname = new Text(gruppo, SWT.BORDER);
        testname.setEditable(false);
        testname.setCapture(false);
        if (test != null) {
            testname.setText(test.getNome());
        }
        Button selTestButton = new Button(gruppo, SWT.PUSH);
        selTestButton.setText("Seleziona");
        selTestButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                Test tmp = new TestSelectionDialog(e.display.getActiveShell(), SWT.NONE).open();
                if (tmp != null) {
                    test = tmp;
                    testname.setText(test.getNome());
                    selections = new SelectionSet();
                }
            }
        });
        startSel = new Text(gruppo, SWT.BORDER);
        startSel.setEditable(false);
        startSel.setCapture(false);
        endSel = new Text(gruppo, SWT.BORDER);
        endSel.setEditable(false);
        endSel.setCapture(false);
        new Composite(gruppo, SWT.NONE);
        final List lista = new List(gruppo, SWT.BORDER);
        GridData gd1 = new GridData(GridData.FILL_BOTH);
        gd1.horizontalSpan = 3;
        lista.setLayoutData(gd1);
        item5.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (test != null) {
                    Trial tmp = new TrialSelectionDialog(e.display.getActiveShell(), SWT.NONE, test).open();
                    if (tmp != null) {
                        int c;
                        if ((c = selections.isIn(tmp)) != -1) {
                            selections.setSelection(c, new AudioSelection(canvas.getSelectionStart(), canvas.getSelectionEnd(), tmp));
                            lista.setItem(c, tmp.getElement() + "  > start: " + canvas.getSelectionStart() + " - fine: " + canvas.getSelectionEnd() + "    Durata " + ((canvas.getSelectionEnd() - canvas.getSelectionStart()) / af.getFrameRate()));
                        } else {
                            selections.addSelection(new AudioSelection(canvas.getSelectionStart(), canvas.getSelectionEnd(), tmp));
                            lista.add(tmp.getElement() + "  > start: " + canvas.getSelectionStart() + " - fine: " + canvas.getSelectionEnd() + "    Durata " + ((canvas.getSelectionEnd() - canvas.getSelectionStart()) / af.getFrameRate()));
                        }
                    }
                } else {
                    MessageDialog dialog = new MessageDialog(shell.getShell(), "Errore", null, "Errore! Selezionare un test", MessageDialog.QUESTION, new String[] { "OK" }, 0);
                    dialog.open();
                }
            }
        });
        Composite buttons = new Composite(shell, SWT.NONE);
        GridData gd2 = new GridData(GridData.FILL_HORIZONTAL);
        gd2.horizontalSpan = 3;
        gd2.heightHint = 50;
        buttons.setLayoutData(gd2);
        RowLayout rl = new RowLayout();
        rl.justify = true;
        rl.marginBottom = 7;
        rl.marginLeft = 7;
        rl.marginRight = 7;
        rl.marginTop = 7;
        rl.spacing = 7;
        buttons.setLayout(rl);
        final Button avanti = new Button(buttons, SWT.PUSH);
        avanti.setText("Avanti");
        RowData rowData = new RowData(100, 40);
        avanti.setLayoutData(rowData);
        avanti.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                avanti.setEnabled(false);
                File dir = new File(patient.getName() + "" + patient.getSurname());
                System.out.println(patient.getName() + "" + patient.getSurname() + " esiste: " + dir.exists());
                if (!dir.exists()) {
                    dir.mkdir();
                }
                int j = 0;
                dir = new File(dir.getPath() + File.separatorChar + nomefile.substring(nomefile.lastIndexOf('\\') + 1, nomefile.length()) + j);
                while (dir.exists()) {
                    String percorso = dir.getPath();
                    dir = new File(percorso.substring(0, percorso.length() - 1) + (++j));
                }
                dir.mkdir();
                AudioSelection[] recogtest = null;
                if (selections != null) {
                    recogtest = selections.getProve4Recog();
                    String[] recogString = new String[recogtest.length];
                    Shell shell1 = new Shell(SWT.ON_TOP);
                    ProgressBar bar = new ProgressBar(shell1, SWT.SMOOTH);
                    bar.setSize(500, 32);
                    shell1.pack();
                    Rectangle splashRect = shell1.getBounds();
                    Rectangle displayRect = shell.getDisplay().getBounds();
                    int x = (displayRect.width - splashRect.width) / 2;
                    int y = (displayRect.height - splashRect.height) / 2;
                    shell1.setLocation(x, y);
                    bar.setMaximum(recogtest.length);
                    shell1.open();
                    String testList = "";
                    for (int i = 0; i < recogtest.length; i++) {
                        System.out.println(recogtest[i]);
                        File files = new File(dir.getPath() + File.separatorChar + i + ".grm");
                        try {
                            System.out.println("HCopy -C config1.txt -s " + (recogtest[i].start / af.getFrameRate() * 10000000) + " -e " + (recogtest[i].fine / af.getFrameRate() * 10000000) + " \"" + nomefile + "\" \"" + dir.getPath() + File.separatorChar + i + ".mfc\"");
                            @SuppressWarnings("unused") Process p = Runtime.getRuntime().exec("HCopy -C config1.txt -s " + (recogtest[i].start / af.getFrameRate() * 10000000) + " -e " + (recogtest[i].fine / af.getFrameRate() * 10000000) + " \"" + nomefile + "\" \"" + dir.getPath() + File.separatorChar + i + ".mfc\"");
                            try {
                                Thread.sleep(500);
                            } catch (Throwable th) {
                            }
                            String gram = "(SENT-START {";
                            if (!test.getControllo().isTextControlled()) {
                                String[] con = test.getControllo().getControlElements();
                                for (int k = 0; k < con.length; k++) {
                                    gram += con[k].toUpperCase() + " | ";
                                }
                                gram = gram.substring(0, gram.length() - 2);
                            } else {
                                for (int k = 0; k < recogtest[i].getProvaLength(); k++) {
                                    gram += recogtest[i].getProva(k).getElement().toUpperCase() + " |";
                                }
                                gram = gram.substring(0, gram.length() - 2);
                            }
                            gram += "} SENT-END)";
                            System.out.println("Grammatica: " + gram);
                            Writer output = null;
                            output = new BufferedWriter(new FileWriter(files));
                            output.write(gram);
                            if (output != null) try {
                                output.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            p = Runtime.getRuntime().exec("HParse \"" + files.getPath() + "\" \"" + files.getParentFile() + File.separatorChar + i + ".net\"");
                            try {
                                Thread.sleep(500);
                            } catch (Throwable th) {
                            }
                            String s = null;
                            BufferedReader stdInput;
                            testList += dir.getPath() + File.separatorChar + i + ".mfc\n";
                            files = new File(dir.getPath() + File.separatorChar + i + ".scp");
                            output = null;
                            output = new BufferedWriter(new FileWriter(files));
                            output.write(dir.getPath() + File.separatorChar + i + ".mfc\n");
                            if (output != null) try {
                                output.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            System.out.println("HVite -o ST -H model\\hmm15\\macros.txt -H model\\hmm15\\hmmdefs.txt -S \"" + files.getPath() + "\" -l * -i \"" + files.getParentFile() + File.separatorChar + i + ".mlf\" -p 0.0 -s 5.0 -w \"" + files.getParentFile() + File.separatorChar + i + ".net\" dict.txt tiedlist.txt");
                            p = Runtime.getRuntime().exec("HVite -o ST -H model\\hmm15\\macros.txt -H model\\hmm15\\hmmdefs.txt -S \"" + files.getPath() + "\" -l * -i \"" + files.getParentFile() + File.separatorChar + i + ".mlf\" -p 0.0 -s 5.0 -w \"" + files.getParentFile() + File.separatorChar + i + ".net\" dict.txt tiedlist.txt");
                            try {
                                Thread.sleep(500);
                            } catch (Throwable th) {
                            }
                            files = new File("" + files.getParentFile() + File.separatorChar + i + ".mlf");
                            stdInput = new BufferedReader(new InputStreamReader(new FileInputStream(files)));
                            int k = 0;
                            recogString[i] = "";
                            while ((s = stdInput.readLine()) != null) {
                                if (!s.contains(".") && k++ >= 1) {
                                    System.out.println(s);
                                    recogString[i] += s + ";";
                                }
                            }
                            stdInput.close();
                            System.out.println(recogString[i] + " � la sequenza riconosciuta di lunghezza " + recogString[i].split(";").length);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        try {
                            Thread.sleep(100);
                        } catch (Throwable th) {
                        }
                        bar.setSelection(i);
                    }
                    shell1.dispose();
                    final Shell shell2 = new Shell(shell.getShell());
                    GridLayout gl = new GridLayout();
                    gl.numColumns = 1;
                    gl.verticalSpacing = 10;
                    shell2.setLayout(gl);
                    final Table tabella = new Table(shell2, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
                    GridData gd = new GridData(GridData.FILL_BOTH);
                    tabella.setLayoutData(gd);
                    String[] titoli = { "Giusto", "Intervallo di tempo", "test", "Controllo", "Riconosciuto" };
                    for (int i = 0; i < titoli.length; i++) {
                        TableColumn column = new TableColumn(tabella, SWT.NONE);
                        column.setText(titoli[i]);
                        column.setAlignment(SWT.CENTER);
                        column.setResizable(true);
                        column.setWidth(120);
                    }
                    for (int i = 0; i < recogtest.length; i++) {
                        String[] tmp = recogString[i].split(";");
                        for (int k = 0; k < java.lang.Math.max(recogtest[i].getProvaLength(), tmp.length); k++) {
                            TableItem item = new TableItem(tabella, SWT.NONE);
                            if (k == 0) item.setText(1, recogtest[i].start / af.getFrameRate() + " s - " + recogtest[i].fine / af.getFrameRate() + " s");
                            if (k < recogtest[i].getProvaLength()) {
                                item.setText(2, recogtest[i].getProva(k).getElement());
                                item.setText(3, recogtest[i].getProva(k).getControl());
                            }
                            if (k < tmp.length) {
                                item.setText(4, tmp[k]);
                            }
                            if (k < recogtest[i].getProvaLength() && k < tmp.length) {
                                item.setChecked(recogtest[i].getProva(k).getControl().equalsIgnoreCase(tmp[k]));
                            }
                        }
                    }
                    tabella.setSize(600, 500);
                    tabella.setLinesVisible(true);
                    tabella.setHeaderVisible(true);
                    Composite buttons = new Composite(shell2, SWT.NONE);
                    gd = new GridData(GridData.FILL_HORIZONTAL);
                    gd.heightHint = 50;
                    buttons.setLayoutData(gd);
                    RowLayout rl = new RowLayout();
                    rl.justify = true;
                    rl.marginBottom = 7;
                    rl.marginLeft = 7;
                    rl.marginRight = 7;
                    rl.marginTop = 7;
                    rl.spacing = 7;
                    buttons.setLayout(rl);
                    Button avanti = new Button(buttons, SWT.PUSH);
                    avanti.setText("Avanti");
                    RowData rowData = new RowData(100, 40);
                    avanti.setLayoutData(rowData);
                    avanti.setText("Avanti");
                    avanti.setSize(100, 40);
                    shell2.open();
                    final float testduration = (recogtest[recogtest.length - 1].getFine() - recogtest[0].getStart()) / af.getFrameRate();
                    avanti.addSelectionListener(new SelectionAdapter() {

                        public void widgetSelected(SelectionEvent e) {
                            int giusti = 0;
                            for (int i = 0; i < tabella.getItemCount(); i++) {
                                if (tabella.getItem(i).getChecked()) giusti++;
                            }
                            System.out.println("Le pronunce giuste sono: " + giusti);
                            shell2.dispose();
                            final Shell shell3 = new Shell(shell.getShell());
                            shell3.setText("Report");
                            GridLayout gl = new GridLayout();
                            gl.numColumns = 1;
                            gl.verticalSpacing = 10;
                            shell3.setLayout(gl);
                            Label errors = new Label(shell3, SWT.NONE);
                            errors.setText("Il numero di errori commessi dal paziente ammonta a: " + (selections.length() - giusti));
                            Label time = new Label(shell3, SWT.NONE);
                            time.setText("Test eseguito in " + testduration + " secondi");
                            Button close = new Button(shell3, SWT.PUSH);
                            close.setText("Chiudi");
                            shell3.pack();
                            shell3.open();
                            close.addSelectionListener(new SelectionAdapter() {

                                public void widgetSelected(SelectionEvent e) {
                                    shell3.dispose();
                                }
                            });
                        }
                    });
                } else {
                }
            }
        });
    }
}
