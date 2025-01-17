package ssmith.lang;

import ssmith.io.*;
import java.util.*;
import java.awt.*;
import java.io.*;
import java.nio.channels.*;

public final class Functions {

    private static Random random = new Random();

    public static int biggest(int a, int b) {
        return Math.max(a, b);
    }

    public static int rnd(int a, int b) {
        return random.nextInt(b + 1 - a) + a;
    }

    public static float rndFloat(float a, float b) {
        return (random.nextFloat() * (b - a)) + a;
    }

    public static double rndDouble(double a, double b) {
        return (random.nextDouble() * (b - a)) + a;
    }

    public static int sign(int a) {
        if (a == 0) {
            return 0;
        } else if (a > 0) {
            return 1;
        } else {
            return -1;
        }
    }

    public static int sign(float a) {
        if (a == 0) {
            return 0;
        } else if (a > 0) {
            return 1;
        } else {
            return -1;
        }
    }

    public static int sign(double a) {
        if (a == 0) {
            return 0;
        } else if (a > 0) {
            return 1;
        } else {
            return -1;
        }
    }

    public static double distance(int x1, int y1, int x2, int y2) {
        double side1 = 0;
        if (x1 != x2) {
            side1 = Math.pow((double) (x2 - x1), 2);
        }
        double side2 = 0;
        if (y1 != y2) {
            side2 = Math.pow((double) (y2 - y1), 2);
        }
        if (side1 == 0 && side2 == 0) {
            return 0;
        } else {
            double result = Math.sqrt(side1 + side2);
            return result;
        }
    }

    public static double distance(float x1, float y1, float x2, float y2) {
        double side1 = 0;
        if (x1 != x2) {
            float x3 = (x2 - x1);
            side1 = x3 * x3;
        }
        double side2 = 0;
        if (y1 != y2) {
            float y3 = (y2 - y1);
            side2 = y3 * y3;
        }
        double result = Math.sqrt(side1 + side2);
        return result;
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        double side1 = 0;
        if (x1 != x2) {
            side1 = Math.pow((x2 - x1), 2);
        }
        double side2 = 0;
        if (y1 != y2) {
            side2 = Math.pow((y2 - y1), 2);
        }
        double result = Math.sqrt(side1 + side2);
        return result;
    }

    public static double distance(float x1, float y1, float z1, float x2, float y2, float z2) {
        double side1 = Math.pow((double) (x2 - x1), 2);
        double side2 = Math.pow((double) (y2 - y1), 2);
        double side3 = Math.pow((double) (z2 - z1), 2);
        double result = Math.sqrt(side1 + side2 + side3);
        return result;
    }

    public static int mod(int x) {
        if (x >= 0) {
            return x;
        } else {
            return x * -1;
        }
    }

    public static float mod(float x) {
        if (x >= 0) {
            return x;
        } else {
            return x * -1;
        }
    }

    public static double mod(double x) {
        if (x >= 0) {
            return x;
        } else {
            return x * -1;
        }
    }

    public static Point getPoint(int x1, int y1, int x2, int y2, int pcent) {
        int width = mod(x2 - x1);
        int height = mod(y2 - y1);
        width = (width * pcent) / 100;
        height = (height * pcent) / 100;
        return new Point(Math.min(x1, x2) + width, Math.min(y1, y2) + height);
    }

    public int remainder(int a, int d) {
        int r = (int) Math.IEEEremainder((double) a, (double) d);
        if (r < 0) {
            r = r + d;
        }
        return r;
    }

    /**
	 * This will return true if targ x, y is inside the defined area.
	 */
    public static boolean isInsideArea(int areaX, int areaY, int width, int height, int targX, int targY) {
        boolean result = false;
        if (targX >= areaX && targY >= areaY) {
            if (targX <= areaX + width && targY <= areaY + height) {
                result = true;
            }
        }
        return result;
    }

    public static void delay(int milliseconds) {
        if (milliseconds > 0) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
            }
        }
    }

    public static void delay(long milliseconds) {
        if (milliseconds > 0) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
            }
        }
    }

    public static String AppendSlash(String file) {
        if (file.endsWith("\\") || file.endsWith("/")) {
            return file;
        } else {
            return file + "/";
        }
    }

    public static String CheckApostraphes(String SQL) {
        return SQL.replaceAll("'", "''");
    }

    public static void CopyFile(String in, String out) throws Exception {
        FileChannel sourceChannel = new FileInputStream(new File(in)).getChannel();
        FileChannel destinationChannel = new FileOutputStream(new File(out)).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    public static int GetNoOfParams(String Data, String Sep) {
        int Count = 0;
        int StartPos = 0;
        StartPos = 1;
        while (Data.indexOf(Sep, StartPos) > 0) {
            StartPos = Data.indexOf(Sep, StartPos) + 1;
            Count++;
        }
        return Count + 1;
    }

    public static String GetLastParam(String Data, String Sep) {
        return GetParam(GetNoOfParams(Data, Sep), Data, Sep);
    }

    public static String GetParam(int No, String Data, String Sep) {
        return GetParam(No, Data, Sep, 0);
    }

    public static String GetParam(int param_no, String data, String sep, int start_pos) {
        int fin_pos;
        if (param_no > 1) {
            start_pos = 0;
            for (int x = 2; x <= param_no; x++) {
                start_pos = data.indexOf(sep, start_pos + 1);
                if (start_pos == 0) {
                }
            }
            start_pos = start_pos + sep.length();
        }
        fin_pos = data.indexOf(sep, start_pos);
        if (fin_pos < 0) {
            fin_pos = data.length();
        }
        return data.substring(start_pos, fin_pos);
    }

    public static void LogStackTrace(Exception ex, String URL) {
        try {
            TextFile tf = new TextFile();
            tf.openFile(URL, TextFile.APPEND);
            tf.writeLine(ex.getMessage());
            StringBuffer str = new StringBuffer();
            for (int c = 0; c < ex.getStackTrace().length; c++) {
                str.append(ex.getStackTrace()[c].getClassName());
                str.append(":" + ex.getStackTrace()[c].getLineNumber() + " - ");
                str.append(ex.getStackTrace()[c].getMethodName());
                tf.writeLine(str.toString());
                str.delete(0, str.length() - 1);
            }
            tf.close();
        } catch (Exception e) {
            System.out.print("Error logging error: " + e.getMessage());
        }
    }

    public static int GetDiffBetweenAngles(int angle1, int angle2) {
        angle1 -= angle2;
        while (angle1 < -180) angle1 += 360;
        while (angle1 >= 180) angle1 -= 360;
        return Functions.mod(angle1);
    }

    public static int RestateAngle(int a) {
        while (a >= 360) {
            a -= 360;
        }
        while (a < 0) {
            a += 360;
        }
        return a;
    }

    public static int GetAbsoluteAngleTo(int sx, int sy, int ex, int ey) {
        double x = ex - sx;
        double z = ey - sy;
        if (z == 0) {
            z = 1;
        }
        if (x >= 0 && z > 0) {
            return (int) Math.toDegrees(Math.atan(x / z));
        } else if (x < 0 && z > 0) {
            return (int) Math.toDegrees(Math.atan(x / z)) + 360;
        } else {
            return (int) Math.toDegrees(Math.atan(x / z)) + 180;
        }
    }

    public static int GetAbsoluteAngleTo(float sx, float sy, float ex, float ey) {
        double x = ex - sx;
        double z = ey - sy;
        if (z == 0) {
            z = 1;
        }
        if (x >= 0 && z > 0) {
            return (int) Math.toDegrees(Math.atan(x / z));
        } else if (x < 0 && z > 0) {
            return (int) Math.toDegrees(Math.atan(x / z)) + 360;
        } else {
            return (int) Math.toDegrees(Math.atan(x / z)) + 180;
        }
    }
}
