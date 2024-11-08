package easyplay.history;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.Insets;
import java.util.Random;
import java.util.ArrayList;
import java.io.*;
import sun.audio.*;
import javax.swing.text.*;
import com.docuverse.swt.flash.FlashPlayer;
import com.docuverse.swt.flash.FlashPlayerListener;
import tools.*;
import Config.*;

public class HistoryGame extends JDialog {

    GUI graphics;

    Box word;

    Box screen = new Box(BoxLayout.Y_AXIS);

    ImageIcon icon;

    JPanel picture = new JPanel();

    JPanel answerbutton;

    JPanel scorepanel;

    JLabel pic = new JLabel();

    JLabel wrd;

    JLabel scor;

    String[] questions;

    String[] answers;

    String[] wrong1;

    String[] wrong2;

    String[] wrong3;

    int quit;

    int difficulty;

    int questno;

    int score;

    int qsanswered;

    ArrayList<Double> hasanswered;

    int attempts;

    JFrame parent;

    static FlashPlayer mainPlayer;

    public HistoryGame(JFrame p, FlashPlayer player) {
        super(p);
        parent = p;
        parent.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
        mainPlayer = player;
        this.setUndecorated(true);
        graphics = new GUI(this);
    }

    public static void main(String args[]) {
        HistoryGame program = new HistoryGame(new JFrame(), mainPlayer);
        program.hasanswered = new ArrayList<Double>();
        program.hasanswered.add((double) -1);
        program.display(1, 0, 0, program.hasanswered);
    }

    public void bringToFront(String com) {
        if (com.equals("close")) {
            this.dispose();
        } else if (com.equals("lock")) {
            this.setAlwaysOnTop(true);
        } else if (com.equals("unlock")) {
            this.setAlwaysOnTop(false);
        } else if (com.equals("front")) {
            this.toFront();
        }
    }

    public void display(int diff, int scr, int qs, ArrayList<Double> la) {
        this.addFocusListener(new EasyFocusListener("history", mainPlayer));
        word = new Box(BoxLayout.X_AXIS);
        answerbutton = new JPanel();
        scorepanel = new JPanel();
        boolean skip = false;
        this.hasanswered = la;
        score = scr;
        qsanswered = qs;
        difficulty = diff;
        Container window = getContentPane();
        graphics.setFrameDetails(this);
        graphics.createMenuBar(this, new MenuListener(), new LevelListener(), diff);
        getQuestions();
        if (hasanswered.size() == questions.length) {
            if (graphics.startNew(score, qsanswered) == 0) {
                score = 0;
                qsanswered = 0;
                newgame(difficulty, true);
                skip = true;
            } else {
                dispose();
            }
        }
        if (!skip) {
            attempts = 0;
            questno = chooseQuestion();
            while (hasanswered.contains((double) questno)) {
                questno = chooseQuestion();
            }
            setAnswerButtons(questno);
            remove(screen);
            screen = new Box(BoxLayout.Y_AXIS);
            screen.add(word, BorderLayout.NORTH);
            screen.add(answerbutton, BorderLayout.CENTER);
            screen.add(scorepanel, BorderLayout.SOUTH);
            add(screen);
            setVisible(true);
            toFront();
            pack();
        }
    }

    public void getQuestions() {
        BufferedReader input;
        String line;
        ArrayList<String> lineList;
        lineList = new ArrayList<String>();
        int count = 1;
        try {
            input = new BufferedReader(new FileReader("easyplay//history//questions//easy.txt"));
            if (difficulty == 1) {
                input.close();
                input = new BufferedReader(new FileReader("easyplay//history//questions//standard.txt"));
            } else if (difficulty == 2) {
                input.close();
                input = new BufferedReader(new FileReader("easyplay//history//questions//hard.txt"));
            }
            line = input.readLine();
            count--;
            while (line != null) {
                count++;
                lineList.add(line);
                line = input.readLine();
            }
            input.close();
        } catch (IOException err) {
        }
        String[] lines = new String[lineList.size()];
        lineList.toArray(lines);
        int i = 0;
        int j = 0;
        int numberofqs = count / 5;
        questions = new String[numberofqs];
        answers = new String[numberofqs];
        wrong1 = new String[numberofqs];
        wrong2 = new String[numberofqs];
        wrong3 = new String[numberofqs];
        while (j < count) {
            questions[i] = lines[j];
            answers[i] = lines[j + 1];
            wrong1[i] = lines[j + 2];
            wrong2[i] = lines[j + 3];
            wrong3[i] = lines[j + 4];
            i++;
            j = j + 5;
        }
    }

    public int chooseQuestion() {
        Random generator = new Random();
        int rand = generator.nextInt(questions.length);
        return rand;
    }

    public void setAnswerButtons(int qnumber) {
        wrd = new JLabel(questions[qnumber]);
        wrd.setMaximumSize(new Dimension(500, 500));
        wrd.setFont(new Font("Serif", Font.BOLD, 18));
        wrd.setForeground(Color.green);
        word.add(wrd);
        answerbutton.setLayout(new GridLayout(2, 2, 20, 20));
        int i = 0;
        String[] tobuttons = new String[4];
        Random generator = new Random();
        int rand = generator.nextInt(4);
        tobuttons[rand] = answers[qnumber];
        if (rand == 0) {
            tobuttons[2] = wrong1[qnumber];
            tobuttons[3] = wrong2[qnumber];
            tobuttons[1] = wrong3[qnumber];
        } else if (rand == 1) {
            tobuttons[3] = wrong1[qnumber];
            tobuttons[0] = wrong2[qnumber];
            tobuttons[2] = wrong3[qnumber];
        } else if (rand == 2) {
            tobuttons[0] = wrong1[qnumber];
            tobuttons[1] = wrong2[qnumber];
            tobuttons[3] = wrong3[qnumber];
        } else if (rand == 3) {
            tobuttons[1] = wrong1[qnumber];
            tobuttons[2] = wrong2[qnumber];
            tobuttons[0] = wrong3[qnumber];
        }
        while (i < 4) {
            JButton button = new JButton("" + tobuttons[i]);
            button.setMaximumSize(new Dimension(100, 100));
            button.setMargin(new Insets(12, 10, 12, 10));
            button.addActionListener(new ButtonListener());
            answerbutton.add(button);
            i++;
        }
        scor = new JLabel("Score: " + score + " / " + qsanswered);
        scor.setFont(new Font("Serif", Font.BOLD, 14));
        scor.setForeground(Color.green);
        scorepanel.add(scor);
    }

    public void newgame(int diff, boolean wipe) {
        if (wipe) {
            this.hasanswered.clear();
        } else {
            this.hasanswered.add((double) questno);
        }
        HistoryGame newprogram = new HistoryGame(parent, mainPlayer);
        display(diff, score, qsanswered, this.hasanswered);
    }

    protected void exit() {
        int quit = graphics.dialogBox("");
        if (quit == 0) {
            mainPlayer.setVariable("rtaskbar", "history");
            dispose();
        }
    }

    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) exit();
    }

    protected class LevelListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("easy")) {
                score = 0;
                qsanswered = 0;
                newgame(0, true);
            } else if (e.getActionCommand().equals("norm")) {
                score = 0;
                qsanswered = 0;
                newgame(1, true);
            } else if (e.getActionCommand().equals("hard")) {
                score = 0;
                qsanswered = 0;
                newgame(2, true);
            }
        }
    }

    protected class MenuListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("new")) {
                score = 0;
                qsanswered = 0;
                newgame(difficulty, true);
            } else if (e.getActionCommand().equals("end")) {
                exit();
            } else if (e.getActionCommand().equals("info")) {
                graphics.displayHelpTheGame();
            } else if (e.getActionCommand().equals("howTo")) {
                graphics.displayHelpHowTo();
            } else if (e.getActionCommand().equals("about")) {
                graphics.displayHelpAbout();
            }
        }
    }

    protected class ButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            JButton b = (JButton) src;
            if (b.getText().equals(answers[questno])) {
                try {
                    InputStream in = new FileInputStream("easyplay//word//sounds//win.wav");
                    AudioStream out = new AudioStream(in);
                    AudioPlayer.player.start(out);
                } catch (IOException err) {
                }
                score++;
                qsanswered++;
                graphics.displaywin();
                newgame(difficulty, false);
            } else {
                try {
                    InputStream in = new FileInputStream("easyplay//word//sounds//lose.wav");
                    AudioStream out = new AudioStream(in);
                    AudioPlayer.player.start(out);
                } catch (IOException err) {
                }
                attempts++;
                if ((attempts + difficulty) > 2) {
                    graphics.displaylose(answers[questno]);
                    qsanswered++;
                    newgame(difficulty, false);
                } else {
                    b.setVisible(false);
                    graphics.displaytryagain(3 - (attempts + difficulty));
                }
            }
        }
    }
}
