package gosu;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import org.jouvieje.FmodEx.FmodEx;
import org.jouvieje.FmodEx.Init;
import org.jouvieje.FmodEx.System;
import org.jouvieje.FmodEx.Defines.*;
import static org.jouvieje.FmodEx.Defines.FMOD_INITFLAGS.*;
import org.jouvieje.FmodEx.Exceptions.InitException;
import org.jouvieje.FmodEx.Enumerations.FMOD_RESULT;
import static org.jouvieje.FmodEx.Enumerations.FMOD_RESULT.*;
import org.jouvieje.FmodEx.Misc.BufferUtils;

public abstract class Gosu {

    protected static System FMOD;

    private static boolean fmodInitialized = false;

    private static Object _lock = new Object();

    /**
   * Returns the current time in milliseconds.
   */
    public static long milliseconds() {
        return java.lang.System.currentTimeMillis();
    }

    /**
   * Returns the distance between two points.
   */
    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0));
    }

    /**
   * Returns the horizontal distance between the origin and the point to which
   * you would get if you moved <code>radius</code> pixels in the direction specified by <code>angle</code>.
   */
    public static double offsetX(double angle, double radius) {
        return Math.sin(angle / 180 * Math.PI) * radius;
    }

    /**
   * Returns the angle between two points in degrees, where 0.0 means upwards.
   * Returns 0 if both points are equal.
   */
    public static double angle(double fromX, double fromY, double toX, double toY) {
        double distX = toX - fromX;
        double distY = toY - fromY;
        if (distX == 0 && distY == 0) {
            return 0;
        }
        return radiansToGosu(Math.atan2(distY, distX));
    }

    /**
   * Returns the smallest angle that can be added to <code>angle1</code> to get to <code>angle2</code>
   * (can be negative if counter-clockwise movement is shorter).
   */
    public static double angleDiff(double from, double to) {
        return normalizeAngle(((to - from + 180) % 360) - 180);
    }

    /**
   * Normalizes an angle to fit within the range [0:360].
   */
    public static double normalizeAngle(double angle) {
        double result = angle % 360;
        return (result < 0) ? result + 360 : result;
    }

    /**
   * Translates from Gosu's angle system to radians.
   */
    public static double gosuToRadians(double angle) {
        return (angle - 90) * Math.PI / 180;
    }

    /**
   * Translates to Gosu's angle system from radians.
   */
    public static double radiansToGosu(double angle) {
        return angle * 180 / Math.PI + 90;
    }

    /**
   * Returns the vertical distance between the origin and the point to which 
   * you would get if you moved <code>radius</code> pixels in the direction specified by <code>angle</code>. 
   */
    public static double offsetY(double angle, double radius) {
        return -Math.cos(angle / 180 * Math.PI) * radius;
    }

    /**
   * Returns a font name that will work on any system.
   */
    public static String defaultFontName() {
        return "Helvetica";
    }

    public static double random(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    /**
   * Lazily initializes FMOD.
   */
    public static void checkFmodInit() {
        synchronized (_lock) {
            if (fmodInitialized) return;
            try {
                Init.loadLibraries(INIT_MODES.INIT_FMOD_EX);
            } catch (InitException e) {
                throw new RuntimeException(e.getMessage());
            }
            FMOD = new System();
            fmodCheck(FmodEx.System_Create(FMOD));
            fmodCheck(FMOD.init(32, FMOD_INIT_NORMAL, null));
            fmodInitialized = true;
        }
    }

    /**
   * Checks FMOD error code.
   */
    protected static void fmodCheck(FMOD_RESULT result) {
        if (result != FMOD_RESULT.FMOD_OK) {
            throw new RuntimeException(FmodEx.FMOD_ErrorString(result));
        }
    }

    /**
   * Checks the FMOD error code but ignores the invalid handle and channel stolen errors.
   */
    protected static void relaxedFmodCheck(FMOD_RESULT result) {
        if (result != FMOD_ERR_INVALID_HANDLE && result != FMOD_ERR_CHANNEL_STOLEN) {
            fmodCheck(result);
        }
    }

    /**
   * Loads a file into memory. Duh.
   */
    protected static ByteBuffer loadFileIntoMemory(String filename) {
        try {
            InputStream is = Gosu.class.getResourceAsStream("/" + filename);
            if (is == null && new File(filename).exists()) {
                is = new FileInputStream(new File(filename));
            }
            BufferedInputStream input = new BufferedInputStream(is);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            int read;
            while ((read = input.read(bytes, 0, bytes.length)) != -1) {
                output.write(bytes, 0, read);
            }
            input.close();
            ByteBuffer buffer = BufferUtils.newByteBuffer(output.size());
            buffer.put(output.toByteArray());
            buffer.rewind();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
