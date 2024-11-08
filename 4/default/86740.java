import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JPanel;

/** the battle field.
 * 
 * @author gldm
 *
 */
public class JBattlePanel extends JPanel {

    private static final long serialVersionUID = 3214587232930416936L;

    private static BufferedImage bkgnd;

    boolean LeftPlayer;

    boolean Shooting, GameStarted, TwoPlayers;

    int MyPanelWidth, MyPanelHeight;

    double x_now, y_now, vel_x, vel_y;

    double LeftLastAngle, LeftLastSpeed, RightLastAngle, RightLastSpeed;

    double GlobalScaleFactor;

    int velocita, angolo;

    double realspeed, radangle;

    private Planet Jupiter, Mercury, Venus;

    private static Ship ShipOne;

    private Ship ShipTwo;

    private Shoot RocketTrack;

    static final int fieldWidth = 4000;

    static final int fieldHeight = 3000;

    static final int JupiterMaxRadius = 340;

    static final int JupiterMinRadius = 110;

    static final int MercuryMaxRadius = 150;

    static final int MercuryMinRadius = 70;

    static final int VenusMaxRadius = 280;

    static final int VenusMinRadius = 90;

    static final int RocketMaxSpeed = 300;

    static final int RocketMinSpeed = 15;

    static final double SpeedFactor = 0.05;

    static final double GravityConstant = 0.00001;

    static final int MaxIterations = 30000;

    String ResetSound, ExplosionSound, WooshSound, PlanetCollisionSound;

    private String MyStatusLabel;

    /** the constructor. 
     * the battle fields starts with a void environment: no ship,
     * no shot already present, no planets.
     */
    public JBattlePanel() {
        super();
        GameStarted = false;
        Shooting = false;
        LeftPlayer = true;
        try {
            File dir = new File(".");
            System.out.println(dir.getCanonicalPath());
            Image localbkgnd = ImageIO.read(getClass().getResource("space.jpg"));
            bkgnd = (BufferedImage) localbkgnd;
        } catch (IOException e) {
        }
        ResetSound = "harp.au";
        ExplosionSound = "explbomb.au";
        WooshSound = "passing.au";
        PlanetCollisionSound = "drip.au";
        MyStatusLabel = "";
        this.addComponentListener(new java.awt.event.ComponentAdapter() {

            public void componentResized(java.awt.event.ComponentEvent evt) {
            }
        });
    }

    /**
	 * paints the battle field
	 */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (this.getHeight() < (3 * this.getWidth() / 4)) {
            MyPanelHeight = this.getHeight();
            MyPanelWidth = MyPanelHeight * 4 / 3;
        } else {
            MyPanelWidth = this.getWidth();
            MyPanelHeight = MyPanelWidth * 3 / 4;
        }
        GlobalScaleFactor = (double) (MyPanelWidth) / (double) (fieldWidth);
        if (GameStarted) {
            try {
                Graphics2D g2d = (Graphics2D) g;
                TexturePaint texture = new TexturePaint(bkgnd, new Rectangle(bkgnd.getWidth(), bkgnd.getHeight()));
                g2d.setPaint(texture);
                g2d.fillRect(0, 0, MyPanelWidth, MyPanelHeight);
                Jupiter.doPaint(g2d, GlobalScaleFactor);
                Mercury.doPaint(g2d, GlobalScaleFactor);
                Venus.doPaint(g2d, GlobalScaleFactor);
                ShipOne.doPaint(g, GlobalScaleFactor);
                ShipTwo.doPaint(g, GlobalScaleFactor);
                if (Shooting) RocketTrack.doPaint(g, GlobalScaleFactor);
                if (MyStatusLabel.equals("") == false) {
                    g2d.drawString(MyStatusLabel, 50, MyPanelHeight - 30);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * reset all
	 */
    public void ResetField() {
        MyStatusLabel = "";
        GameStarted = true;
        LeftPlayer = true;
        Jupiter = new JupiterPlanet((int) (fieldWidth * 0.33), (int) (Math.random() * fieldHeight * 0.8 + fieldHeight * 0.1), (int) (JupiterMinRadius + Math.random() * (JupiterMaxRadius - JupiterMinRadius)));
        Mercury = new MercuryPlanet((int) (fieldWidth * 0.51), (int) (Math.random() * fieldHeight * 0.6 + fieldHeight * 0.2), (int) (MercuryMinRadius + Math.random() * (MercuryMaxRadius - MercuryMinRadius)));
        Venus = new VenusPlanet((int) (fieldWidth * 0.7), (int) (Math.random() * fieldHeight * 0.9 + fieldHeight * 0.05), (int) (VenusMinRadius + Math.random() * (VenusMaxRadius - VenusMinRadius)));
        ShipOne = new LeftShip((int) (fieldWidth * 0.12), (int) (Math.random() * fieldHeight * 0.7 + fieldHeight * 0.15));
        ShipTwo = new RightShip((int) (fieldWidth * 0.88), (int) (Math.random() * fieldHeight * 0.7 + fieldHeight * 0.15));
        RocketTrack = new Shoot(0, 0);
        if (TwoPlayers) ShipOne.setSelected(true);
        Shooting = false;
        this.paintImmediately(0, 0, this.getWidth(), this.getHeight());
        try {
            suona(ResetSound);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** draws the shot.
	 * 
	 * @param LeftTurn true if it is left player's turn, false if right player's one
	 * @param SpeedValue intial value of the rocket's speed
	 * @param AngleValue initial value in degree of the rocket direction
	 * @return true if a ship is hit, false otherwise
	 */
    public boolean DoShoot(boolean LeftTurn, float SpeedValue, float AngleValue) {
        int i, isf, safethreshold;
        GlobalScaleFactor = (double) (MyPanelWidth) / (double) (fieldWidth);
        double timetopaint = 1 / GlobalScaleFactor;
        boolean Gotcha = false;
        Shooting = true;
        LeftPlayer = LeftTurn;
        ShipOne.setSelected(false);
        ShipTwo.setSelected(false);
        RocketTrack.clean();
        MyStatusLabel = "";
        this.paintImmediately(0, 0, this.getWidth(), this.getHeight());
        double x_new, y_new, x_old, y_old, aumentx, aumenty, speed_x, speed_y;
        double x_now, y_now;
        if (LeftTurn) {
            RocketTrack.SetStartingPoint(ShipOne.getCenterX(), ShipOne.getCenterY());
            x_now = ShipOne.getCenterX();
            y_now = ShipOne.getCenterY();
        } else {
            RocketTrack.SetStartingPoint(ShipTwo.getCenterX(), ShipTwo.getCenterY());
            x_now = ShipTwo.getCenterX();
            y_now = ShipTwo.getCenterY();
        }
        vel_x = realspeed * Math.cos(radangle);
        vel_y = realspeed * Math.sin(radangle);
        x_old = x_now + vel_x;
        y_old = y_now + vel_y;
        speed_x = vel_x;
        speed_y = vel_y;
        i = 0;
        isf = 0;
        safethreshold = (int) (800 / (double) realspeed);
        while ((i < MaxIterations) && !(Venus.checkCollision(x_old, y_old) || Jupiter.checkCollision(x_old, y_old) || Mercury.checkCollision(x_old, y_old)) && ((i < safethreshold) || !(ShipOne.checkCollision(x_old, y_old) || ShipTwo.checkCollision(x_old, y_old)))) {
            aumentx = speed_x + GravityConstant * (Jupiter.gravityEffectX(x_old, y_old) + Mercury.gravityEffectX(x_old, y_old) + Venus.gravityEffectX(x_old, y_old));
            aumenty = speed_y + GravityConstant * (Jupiter.gravityEffectY(x_old, y_old) + Mercury.gravityEffectY(x_old, y_old) + Venus.gravityEffectY(x_old, y_old));
            x_new = x_old + aumentx;
            y_new = y_old + aumenty;
            speed_x = x_new - x_old;
            speed_y = y_new - y_old;
            if (isf >= timetopaint) {
                RocketTrack.AddPosition(x_new, y_new);
                if (((int) (GlobalScaleFactor * x_new) < MyPanelWidth) && (GlobalScaleFactor * y_new < MyPanelHeight)) this.paintImmediately((int) (GlobalScaleFactor * x_new - 2), (int) (GlobalScaleFactor * y_new - 2), 4, 4);
                isf = 0;
            }
            x_old = x_new;
            y_old = y_new;
            i++;
            isf++;
        }
        if (TwoPlayers) {
            if (LeftTurn) ShipTwo.setSelected(true); else ShipOne.setSelected(true);
        }
        if (ShipOne.checkCollision(x_old, y_old)) {
            MyStatusLabel = "Blue ship destroyed.";
            ShipOne.kaboom();
            this.paintImmediately((int) (GlobalScaleFactor * x_old - 30), (int) (GlobalScaleFactor * y_old - 30), 60, 60);
            try {
                suona(this.ExplosionSound);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Gotcha = true;
        } else if (ShipTwo.checkCollision(x_old, y_old)) {
            ShipTwo.kaboom();
            MyStatusLabel = "Red ship destroyed.";
            this.paintImmediately((int) (GlobalScaleFactor * x_old - 30), (int) (GlobalScaleFactor * y_old - 30), 60, 60);
            try {
                suona(this.ExplosionSound);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Gotcha = true;
        } else if (i >= MaxIterations) {
            MyStatusLabel = "Rocket lost in space.";
        } else {
            try {
                suona(PlanetCollisionSound);
            } catch (IOException e) {
                e.printStackTrace();
            }
            MyStatusLabel = "The rocket hit a planet.";
        }
        this.repaint();
        return Gotcha;
    }

    public void suona(String filename) throws IOException {
        AudioInputStream ain = null;
        SourceDataLine line = null;
        try {
            ain = AudioSystem.getAudioInputStream(getClass().getResource(filename));
            AudioFormat format = ain.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                AudioFormat pcm = new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, false);
                ain = AudioSystem.getAudioInputStream(pcm, ain);
                format = ain.getFormat();
                info = new DataLine.Info(SourceDataLine.class, format);
            }
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            int framesize = format.getFrameSize();
            byte[] buffer = new byte[4 * 1024 * framesize];
            int numbytes = 0;
            boolean started = false;
            for (; ; ) {
                int bytesread = ain.read(buffer, numbytes, buffer.length - numbytes);
                if (bytesread == -1) break;
                numbytes += bytesread;
                if (!started) {
                    line.start();
                    started = true;
                }
                int bytestowrite = (numbytes / framesize) * framesize;
                line.write(buffer, 0, bytestowrite);
                int remaining = numbytes - bytestowrite;
                if (remaining > 0) System.arraycopy(buffer, bytestowrite, buffer, 0, remaining);
                numbytes = remaining;
            }
            line.drain();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } finally {
            if (line != null) line.close();
            if (ain != null) ain.close();
        }
    }

    /**
	 * 
	 * @return angle's vale
	 */
    public int getAngolo() {
        return angolo;
    }

    /**set angle's value
	 *  
	 * @param angolo the angle value
	 */
    public void setAngolo(int angolo) {
        this.angolo = angolo;
        radangle = -Math.PI / 180 * (double) (angolo);
    }

    public int getVelocita() {
        return velocita;
    }

    public void setVelocita(int velocita) {
        this.velocita = velocita;
        realspeed = (double) velocita * SpeedFactor;
    }

    /** toggle one / two players mode
 * 
 * @param twoPlayers true if if we want 2 players game, false otherwise
 */
    public void setTwoPlayers(boolean twoPlayers) {
        TwoPlayers = twoPlayers;
        if (TwoPlayers) {
            if (LeftPlayer) {
                if (ShipOne != null) ShipOne.setSelected(true);
                if (ShipTwo != null) ShipTwo.setSelected(false);
            } else {
                if (ShipOne != null) ShipOne.setSelected(false);
                if (ShipTwo != null) ShipTwo.setSelected(true);
            }
        } else {
            if (ShipOne != null) ShipOne.setSelected(false);
            if (ShipTwo != null) ShipTwo.setSelected(false);
        }
    }
}
