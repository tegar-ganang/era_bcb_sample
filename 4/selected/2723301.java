package remotelrcontrol;

import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author victorn
 */
public class MoveController extends Thread {

    public static int robotsCount;

    public static Vector<BTConnector> btconn;

    public static VideoDetection videoDetector;

    private String btRecieved[];

    public static Vector<Vector<Point>> trajectoryPoints;

    private static Point current[], destination[];

    private static Dimension imgSize;

    private double compressionCoeff;

    public static double discretisation;

    public MoveController() {
    }

    public void initialize(int robots) {
        imgSize = new Dimension(Integer.parseInt((String) RCProperties.config.getProperty("imgSizeX")), Integer.parseInt((String) RCProperties.config.getProperty("imgSizeY")));
        robotsCount = robots;
        current = new Point[robotsCount];
        destination = new Point[robotsCount];
        btRecieved = new String[robotsCount];
        trajectoryPoints = new Vector<Vector<Point>>();
        btconn = new Vector<BTConnector>();
        for (int id = 0; id < robotsCount; id++) {
            btconn.add(new BTConnector(id));
            trajectoryPoints.add(new Vector<Point>());
            current[id] = new Point(0, 0);
            destination[id] = new Point(0, 0);
            btRecieved[id] = new String();
        }
    }

    @Override
    public void run() {
        if (RCProperties.config.getProperty("useVideoCamDetection".intern()).contains("false")) moveThroughTrajectory(); else {
            videoDetector.moveTracking();
            return;
        }
    }

    private void moveThroughTrajectory() {
        int maxLength = 0;
        int id;
        for (int i = 0; i < robotsCount; i++) {
            maxLength = maxLength < trajectoryPoints.elementAt(i).size() ? trajectoryPoints.elementAt(i).size() : maxLength;
        }
        for (int j = 0; j < maxLength; j++) {
            for (id = 0; id < robotsCount; id++) {
                if (trajectoryPoints.elementAt(id).size() > j) {
                    destination[id].setLocation(trajectoryPoints.elementAt(id).elementAt(j));
                } else {
                    btRecieved[id] = "1".intern();
                }
            }
            coordinatesConvert();
            for (id = 0; id < robotsCount; id++) {
                if (trajectoryPoints.elementAt(id).size() >= j) {
                    btconn.elementAt(id).createBTMessage(0, current[id], destination[id], 25);
                    current[id].setLocation(destination[id]);
                }
            }
            for (id = 0; id < robotsCount; id++) if (trajectoryPoints.elementAt(id).size() >= j) btconn.elementAt(id).sendBTMessage();
            while (!waitForAnswers()) {
            }
            RemoteLRControlView.progressBarUpdater(j, maxLength);
        }
    }

    public void coordinatesConvert() {
        int x, y;
        for (int id = 0; id < robotsCount; id++) {
            x = -current[id].y + (int) (imgSize.height / 2.0);
            y = current[id].x - (int) (imgSize.width / 2.0);
            x /= compressionCoeff;
            y /= compressionCoeff;
            current[id].setLocation(x, y);
            x = -destination[id].y + (int) (imgSize.height / 2.0);
            y = destination[id].x - (int) (imgSize.width / 2.0);
            x /= compressionCoeff;
            y /= compressionCoeff;
            destination[id].setLocation(x, y);
        }
    }

    public boolean waitForAnswers() {
        try {
            Scanner scan = null;
            for (int i = 0; i < robotsCount; i++) {
                if (btRecieved[i] == null) btRecieved[i] = btconn.elementAt(i).fromBt.readLine();
                if (btRecieved[i].isEmpty()) {
                    btRecieved[i] = btconn.elementAt(i).fromBt.readLine();
                }
                if (btRecieved[i].contains("BlueCove")) {
                    btRecieved[i] = btconn.elementAt(i).fromBt.readLine();
                }
                if (btRecieved[i].isEmpty()) return false; else {
                    scan = new Scanner(btRecieved[i]);
                    if (scan.hasNextInt() && scan.nextInt() == 1) continue; else return false;
                }
            }
            for (int i = 0; i < robotsCount; i++) {
                btRecieved[i] = new String();
            }
            return true;
        } catch (Exception ex) {
            RemoteLRControlApp.writeToErr("Something was wrong in readFromBT()." + ex.toString());
            Logger.getLogger(RemoteLRControlApp.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public void trajectoryDisplacing() {
        try {
            PrintWriter displaces;
            BufferedReader input;
            input = new BufferedReader(new FileReader(".generated0"));
            displaces = new PrintWriter(new OutputStreamWriter(new FileOutputStream(".generated1")));
            String line = input.readLine();
            Scanner scan = new Scanner(line);
            Point p0 = new Point(scan.nextInt(), scan.nextInt());
            Point p1 = new Point(0, 0);
            while ((line = input.readLine()) != null) {
                scan = new Scanner(line);
                p1.setLocation(scan.nextInt(), scan.nextInt());
                displace(p0, p1, 130);
                p0.setLocation(p1);
                displaces.println(destination[1].x + " " + destination[1].y);
            }
            displaces.close();
            input.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RemoteLRControlApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RemoteLRControlApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void displace(Point previous, Point destination, int dist) {
        if (previous.x == destination.x) {
            if (previous.y < destination.y) {
                MoveController.destination[1].x = destination.x + dist;
                MoveController.destination[1].y = destination.y;
            } else {
                MoveController.destination[1].x = destination.x - dist;
                MoveController.destination[1].y = destination.y;
            }
        } else if (previous.y == destination.y) {
            if (previous.x < destination.x) {
                MoveController.destination[1].x = destination.x;
                MoveController.destination[1].y = destination.y - dist;
            } else {
                MoveController.destination[1].x = destination.x;
                MoveController.destination[1].y = destination.y + dist;
            }
        } else {
            if (previous.x < destination.x) if (previous.y < destination.y) {
                MoveController.destination[1].x = (int) (destination.x + dist / Math.sqrt(1 + Math.pow((destination.x - previous.x) / (destination.y - previous.y), 2)));
                MoveController.destination[1].y = (int) (destination.y - dist / Math.sqrt(1 + Math.pow((destination.y - previous.y) / (destination.x - previous.x), 2)));
            } else {
                MoveController.destination[1].x = (int) (destination.x - dist / Math.sqrt(1 + Math.pow((destination.x - previous.x) / (destination.y - previous.y), 2)));
                MoveController.destination[1].y = (int) (destination.y - dist / Math.sqrt(1 + Math.pow((destination.y - previous.y) / (destination.x - previous.x), 2)));
            } else {
                if (previous.y < destination.y) {
                    MoveController.destination[1].x = (int) (destination.x + dist / Math.sqrt(1 + Math.pow((destination.x - previous.x) / (destination.y - previous.y), 2)));
                    MoveController.destination[1].y = (int) (destination.y + dist / Math.sqrt(1 + Math.pow((destination.y - previous.y) / (destination.x - previous.x), 2)));
                } else {
                    MoveController.destination[1].x = (int) (destination.x - dist / Math.sqrt(1 + Math.pow((destination.x - previous.x) / (destination.y - previous.y), 2)));
                    MoveController.destination[1].y = (int) (destination.y + dist / Math.sqrt(1 + Math.pow((destination.y - previous.y) / (destination.x - previous.x), 2)));
                }
            }
        }
    }

    public int getRobotsCount() {
        return robotsCount;
    }

    public double getDiscretisation() {
        return discretisation;
    }

    public double getCompressionCoeff() {
        return compressionCoeff;
    }

    public Point getCurrentPoint(int id) throws IOException {
        if (id < robotsCount) return current[id]; else throw new IOException("Illegal ID");
    }

    public Point getDestinationPoint(int id) throws IOException {
        if (id < robotsCount) return current[id]; else throw new IOException("Illegal ID");
    }

    public void setCurrentPoint(int id, int x, int y) {
        if (id < robotsCount) current[id].setLocation(x, y);
    }

    public void setCurrentPoint(int id, Point p) {
        if (id < robotsCount) current[id].setLocation(p);
    }

    public void setDestinationPoint(int id, Point p) {
        if (id < robotsCount) destination[id].setLocation(p);
    }

    public void setDestinationPoint(int id, int x, int y) {
        if (id < robotsCount) destination[id].setLocation(x, y);
    }

    public void setDiscretisation(int discr) {
        discretisation = discr;
    }

    public void setCompressionCoeff(double compression) {
        compressionCoeff = compression;
    }

    public static void modelCircle(int r) {
        try {
            PrintWriter modCircleOutput;
            int x0 = (imgSize.width) / 2;
            int y0 = (imgSize.height) / 2;
            modCircleOutput = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(".generated0"))));
            for (double t = Math.PI; t > -Math.PI; t -= 0.1) modCircleOutput.println((int) (r * Math.cos(t) + x0) + " " + (int) (-r * Math.sin(t) + y0));
            modCircleOutput.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RemoteLRControlApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void modelSquare(int length) {
        try {
            int x0 = (imgSize.width - length) / 2;
            int y0 = (imgSize.height + length) / 2;
            PrintWriter modelSquarePrinter;
            modelSquarePrinter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(".generated0"))));
            for (int i = y0; i > y0 - length; i -= discretisation) modelSquarePrinter.println(x0 + " " + i);
            for (int i = x0; i < x0 + length; i += discretisation) modelSquarePrinter.println(i + " " + (y0 - length));
            for (int i = y0 - length; i < y0; i += discretisation) modelSquarePrinter.println((x0 + length) + " " + i);
            for (int i = x0 + length; i >= x0; i -= discretisation) modelSquarePrinter.println(i + " " + y0);
            modelSquarePrinter.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RemoteLRControlApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
