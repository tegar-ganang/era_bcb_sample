package org.eclipse.core.runtime;

import java.io.File;
import org.eclipse.core.internal.runtime.Assert;

/** 
 * The standard implementation of the <code>IPath</code> interface.
 * Paths are always maintained in canonicalized form.  That is, parent
 * references (i.e., <code>../../</code>) and duplicate separators are 
 * resolved.  For example,
 * <pre>     new Path("/a/b").append("../foo/bar")</pre>
 * will yield the path
 * <pre>     /a/foo/bar</pre>
 * <p>
 * This class is not intended to be subclassed by clients but
 * may be instantiated.
 * </p>
 * @see IPath
 */
public class Path implements IPath, Cloneable {

    /** masks for separator values */
    private static final int HAS_LEADING = 1;

    private static final int IS_UNC = 2;

    private static final int HAS_TRAILING = 4;

    private static final int ALL_SEPARATORS = HAS_LEADING | IS_UNC | HAS_TRAILING;

    /** Constant empty string value. */
    private static final String EMPTY_STRING = "";

    private static final String[] EMPTY_STRING_ARRAY = new String[] {};

    /** Constant value indicating no segments */
    private static final String[] NO_SEGMENTS = new String[0];

    /** Constant value containing the empty path with no device. */
    public static final Path EMPTY = new Path(EMPTY_STRING);

    /** Mask for all bits that are involved in the hashcode */
    private static final int HASH_MASK = ~HAS_TRAILING;

    /** Constant root path string (<code>"/"</code>). */
    private static final String ROOT_STRING = "/";

    /** Constant value containing the root path with no device. */
    public static final Path ROOT = new Path(ROOT_STRING);

    /** Constant value indicating if the current platform is Windows */
    private static final boolean WINDOWS = java.io.File.separatorChar == '\\';

    /** The device id string. May be null if there is no device. */
    private String device = null;

    /** The path segments */
    private String[] segments;

    /** flags indicating separators (has leading, is UNC, has trailing) */
    private int separators;

    /** 
	 * Constructs a new path from the given string path.
	 * The string path must represent a valid file system path
	 * on the local file system. 
	 * The path is canonicalized and double slashes are removed
	 * except at the beginning. (to handle UNC paths). All forward
	 * slahes ('/') are treated as segment delimiters, and any
	 * segment and device delimiters for the local file system are
	 * also respected.
	 *
	 * @param pathString the portable string path
	 * @see IPath#toPortableString()
	 * @since 3.1
	 */
    public static IPath fromOSString(String pathString) {
        return new Path(pathString);
    }

    /** 
	 * Constructs a new path from the given path string.
	 * The path string must have been produced by a previous
	 * call to <code>IPath.toPortableString</code>.
	 *
	 * @param pathString the portable path string
	 * @see IPath#toPortableString()
	 * @since 3.1
	 */
    public static IPath fromPortableString(String pathString) {
        int firstMatch = pathString.indexOf(DEVICE_SEPARATOR) + 1;
        if (firstMatch <= 0) return new Path().initialize(null, pathString);
        String devicePart = null;
        int pathLength = pathString.length();
        if (firstMatch == pathLength || pathString.charAt(firstMatch) != DEVICE_SEPARATOR) {
            devicePart = pathString.substring(0, firstMatch);
            pathString = pathString.substring(firstMatch, pathLength);
        }
        if (pathString.indexOf(DEVICE_SEPARATOR) == -1) return new Path().initialize(devicePart, pathString);
        char[] chars = pathString.toCharArray();
        int readOffset = 0, writeOffset = 0, length = chars.length;
        while (readOffset < length) {
            if (chars[readOffset] == DEVICE_SEPARATOR) if (++readOffset >= length) break;
            chars[writeOffset++] = chars[readOffset++];
        }
        return new Path().initialize(devicePart, new String(chars, 0, writeOffset));
    }

    private Path() {
    }

    /** 
	 * Constructs a new path from the given string path.
	 * The string path must represent a valid file system path
	 * on the local file system. 
	 * The path is canonicalized and double slashes are removed
	 * except at the beginning. (to handle UNC paths). All forward
	 * slashes ('/') are treated as segment delimiters, and any
	 * segment and device delimiters for the local file system are
	 * also respected (such as colon (':') and backslash ('\') on some file systems).
	 *
	 * @param fullPath the string path
	 * @see #isValidPath(String)
	 */
    public Path(String fullPath) {
        String devicePart = null;
        if (WINDOWS) {
            fullPath = fullPath.indexOf('\\') == -1 ? fullPath : fullPath.replace('\\', SEPARATOR);
            int i = fullPath.indexOf(DEVICE_SEPARATOR);
            if (i != -1) {
                int start = fullPath.charAt(0) == SEPARATOR ? 1 : 0;
                devicePart = fullPath.substring(start, i + 1);
                fullPath = fullPath.substring(i + 1, fullPath.length());
            }
        }
        initialize(devicePart, fullPath);
    }

    /** 
	 * Constructs a new path from the given device id and string path.
	 * The given string path must be valid.
	 * The path is canonicalized and double slashes are removed except
	 * at the beginning (to handle UNC paths). All forward
	 * slashes ('/') are treated as segment delimiters, and any
	 * segment delimiters for the local file system are
	 * also respected (such as backslash ('\') on some file systems).
	 *
	 * @param device the device id
	 * @param path the string path
	 * @see #isValidPath(String)
	 * @see #setDevice(String)
	 */
    public Path(String device, String path) {
        if (WINDOWS) {
            path = path.indexOf('\\') == -1 ? path : path.replace('\\', SEPARATOR);
        }
        initialize(device, path);
    }

    private Path(String device, String[] segments, int _separators) {
        this.segments = segments;
        this.device = device;
        this.separators = (computeHashCode() << 3) | (_separators & ALL_SEPARATORS);
    }

    public IPath addFileExtension(String extension) {
        if (isRoot() || isEmpty() || hasTrailingSeparator()) return this;
        int len = segments.length;
        String[] newSegments = new String[len];
        System.arraycopy(segments, 0, newSegments, 0, len - 1);
        newSegments[len - 1] = segments[len - 1] + "." + extension;
        return new Path(device, newSegments, separators);
    }

    public IPath addTrailingSeparator() {
        if (hasTrailingSeparator() || isRoot()) {
            return this;
        }
        if (isEmpty()) {
            return new Path(device, segments, HAS_LEADING);
        }
        return new Path(device, segments, separators | HAS_TRAILING);
    }

    public IPath append(IPath tail) {
        if (tail == null || tail.segmentCount() == 0) return this;
        if (this.isEmpty()) return tail.setDevice(device).makeRelative();
        if (this.isRoot()) return tail.setDevice(device).makeAbsolute();
        int myLen = segments.length;
        int tailLen = tail.segmentCount();
        String[] newSegments = new String[myLen + tailLen];
        System.arraycopy(segments, 0, newSegments, 0, myLen);
        for (int i = 0; i < tailLen; i++) {
            newSegments[myLen + i] = tail.segment(i);
        }
        Path result = new Path(device, newSegments, (separators & (HAS_LEADING | IS_UNC)) | (tail.hasTrailingSeparator() ? HAS_TRAILING : 0));
        String tailFirstSegment = newSegments[myLen];
        if (tailFirstSegment.equals("..") || tailFirstSegment.equals(".")) {
            result.canonicalize();
        }
        return result;
    }

    public IPath append(String tail) {
        if (tail.indexOf(SEPARATOR) == -1 && tail.indexOf("\\") == -1 && tail.indexOf(DEVICE_SEPARATOR) == -1) {
            int tailLength = tail.length();
            if (tailLength < 3) {
                if (tailLength == 0 || ".".equals(tail)) {
                    return this;
                }
                if ("..".equals(tail)) return removeLastSegments(1);
            }
            int myLen = segments.length;
            String[] newSegments = new String[myLen + 1];
            System.arraycopy(segments, 0, newSegments, 0, myLen);
            newSegments[myLen] = tail;
            return new Path(device, newSegments, separators & ~HAS_TRAILING);
        }
        return append(new Path(tail));
    }

    /**
	 * Destructively converts this path to its canonical form.
	 * <p>
	 * In its canonical form, a path does not have any
	 * "." segments, and parent references ("..") are collapsed
	 * where possible.
	 * </p>
	 * @return true if the path was modified, and false otherwise.
	 */
    private boolean canonicalize() {
        for (int i = 0, max = segments.length; i < max; i++) {
            String segment = segments[i];
            if (segment.charAt(0) == '.' && (segment.equals("..") || segment.equals("."))) {
                collapseParentReferences();
                if (segments.length == 0) separators &= (HAS_LEADING | IS_UNC);
                separators = (separators & ALL_SEPARATORS) | (computeHashCode() << 3);
                return true;
            }
        }
        return false;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
	 * Destructively removes all occurrences of ".." segments from this path.
	 */
    private void collapseParentReferences() {
        int segmentCount = segments.length;
        String[] stack = new String[segmentCount];
        int stackPointer = 0;
        for (int i = 0; i < segmentCount; i++) {
            String segment = segments[i];
            if (segment.equals("..")) {
                if (stackPointer == 0) {
                    if (!isAbsolute()) stack[stackPointer++] = segment;
                } else {
                    if ("..".equals(stack[stackPointer - 1])) stack[stackPointer++] = ".."; else stackPointer--;
                }
            } else if (!segment.equals(".") || (i == 0 && !isAbsolute())) stack[stackPointer++] = segment;
        }
        if (stackPointer == segmentCount) return;
        String[] newSegments = new String[stackPointer];
        System.arraycopy(stack, 0, newSegments, 0, stackPointer);
        this.segments = newSegments;
    }

    /**
	 * Removes duplicate slashes from the given path, with the exception
	 * of leading double slash which represents a UNC path.
	 */
    private String collapseSlashes(String path) {
        int length = path.length();
        if (length < 3) return path;
        if (path.indexOf("//", 1) == -1) return path;
        char[] result = new char[path.length()];
        int count = 0;
        boolean hasPrevious = false;
        char[] characters = path.toCharArray();
        for (int index = 0; index < characters.length; index++) {
            char c = characters[index];
            if (c == SEPARATOR) {
                if (hasPrevious) {
                    if (device == null && index == 1) {
                        result[count] = c;
                        count++;
                    }
                } else {
                    hasPrevious = true;
                    result[count] = c;
                    count++;
                }
            } else {
                hasPrevious = false;
                result[count] = c;
                count++;
            }
        }
        return new String(result, 0, count);
    }

    private int computeHashCode() {
        int hash = device == null ? 17 : device.hashCode();
        int segmentCount = segments.length;
        for (int i = 0; i < segmentCount; i++) {
            hash = hash * 37 + segments[i].hashCode();
        }
        return hash;
    }

    private int computeLength() {
        int length = 0;
        if (device != null) length += device.length();
        if ((separators & HAS_LEADING) != 0) length++;
        if ((separators & IS_UNC) != 0) length++;
        int max = segments.length;
        if (max > 0) {
            for (int i = 0; i < max; i++) {
                length += segments[i].length();
            }
            length += max - 1;
        }
        if ((separators & HAS_TRAILING) != 0) length++;
        return length;
    }

    private int computeSegmentCount(String path) {
        int len = path.length();
        if (len == 0 || (len == 1 && path.charAt(0) == SEPARATOR)) {
            return 0;
        }
        int count = 1;
        int prev = -1;
        int i;
        while ((i = path.indexOf(SEPARATOR, prev + 1)) != -1) {
            if (i != prev + 1 && i != len) {
                ++count;
            }
            prev = i;
        }
        if (path.charAt(len - 1) == SEPARATOR) {
            --count;
        }
        return count;
    }

    /**
	 * Computes the segment array for the given canonicalized path.
	 */
    private String[] computeSegments(String path) {
        int segmentCount = computeSegmentCount(path);
        if (segmentCount == 0) return NO_SEGMENTS;
        String[] newSegments = new String[segmentCount];
        int len = path.length();
        int firstPosition = (path.charAt(0) == SEPARATOR) ? 1 : 0;
        if (firstPosition == 1 && len > 1 && (path.charAt(1) == SEPARATOR)) firstPosition = 2;
        int lastPosition = (path.charAt(len - 1) != SEPARATOR) ? len - 1 : len - 2;
        int next = firstPosition;
        for (int i = 0; i < segmentCount; i++) {
            int start = next;
            int end = path.indexOf(SEPARATOR, next);
            if (end == -1) {
                newSegments[i] = path.substring(start, lastPosition + 1);
            } else {
                newSegments[i] = path.substring(start, end);
            }
            next = end + 1;
        }
        return newSegments;
    }

    /**
	 * Returns the platform-neutral encoding of the given segment onto
	 * the given string buffer. This escapes literal colon characters with double colons.
	 */
    private void encodeSegment(String string, StringBuffer buf) {
        int len = string.length();
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            buf.append(c);
            if (c == DEVICE_SEPARATOR) buf.append(DEVICE_SEPARATOR);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Path)) return false;
        Path target = (Path) obj;
        if ((separators & HASH_MASK) != (target.separators & HASH_MASK)) return false;
        String[] targetSegments = target.segments;
        int i = segments.length;
        if (i != targetSegments.length) return false;
        while (--i >= 0) if (!segments[i].equals(targetSegments[i])) return false;
        return device == target.device || (device != null && device.equals(target.device));
    }

    public String getDevice() {
        return device;
    }

    public String getFileExtension() {
        if (hasTrailingSeparator()) {
            return null;
        }
        String lastSegment = lastSegment();
        if (lastSegment == null) {
            return null;
        }
        int index = lastSegment.lastIndexOf(".");
        if (index == -1) {
            return null;
        }
        return lastSegment.substring(index + 1);
    }

    public int hashCode() {
        return separators & HASH_MASK;
    }

    public boolean hasTrailingSeparator() {
        return (separators & HAS_TRAILING) != 0;
    }

    private IPath initialize(String deviceString, String path) {
        Assert.isNotNull(path);
        this.device = deviceString;
        path = collapseSlashes(path);
        int len = path.length();
        if (len < 2) {
            if (len == 1 && path.charAt(0) == SEPARATOR) {
                this.separators = HAS_LEADING;
            } else {
                this.separators = 0;
            }
        } else {
            boolean hasLeading = path.charAt(0) == SEPARATOR;
            boolean isUNC = hasLeading && path.charAt(1) == SEPARATOR;
            boolean hasTrailing = !(isUNC && len == 2) && path.charAt(len - 1) == SEPARATOR;
            separators = hasLeading ? HAS_LEADING : 0;
            if (isUNC) separators |= IS_UNC;
            if (hasTrailing) separators |= HAS_TRAILING;
        }
        segments = computeSegments(path);
        if (!canonicalize()) {
            separators = (separators & ALL_SEPARATORS) | (computeHashCode() << 3);
        }
        return this;
    }

    public boolean isAbsolute() {
        return (separators & HAS_LEADING) != 0;
    }

    public boolean isEmpty() {
        return segments.length == 0 && ((separators & ALL_SEPARATORS) != HAS_LEADING);
    }

    public boolean isPrefixOf(IPath anotherPath) {
        if (device == null) {
            if (anotherPath.getDevice() != null) {
                return false;
            }
        } else {
            if (!device.equalsIgnoreCase(anotherPath.getDevice())) {
                return false;
            }
        }
        if (isEmpty() || (isRoot() && anotherPath.isAbsolute())) {
            return true;
        }
        int len = segments.length;
        if (len > anotherPath.segmentCount()) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!segments[i].equals(anotherPath.segment(i))) return false;
        }
        return true;
    }

    public boolean isRoot() {
        return this == ROOT || (segments.length == 0 && ((separators & ALL_SEPARATORS) == HAS_LEADING));
    }

    public boolean isUNC() {
        if (device != null) return false;
        return (separators & IS_UNC) != 0;
    }

    public boolean isValidPath(String path) {
        Path test = new Path(path);
        for (int i = 0, max = test.segmentCount(); i < max; i++) if (!isValidSegment(test.segment(i))) return false;
        return true;
    }

    public boolean isValidSegment(String segment) {
        int size = segment.length();
        if (size == 0) return false;
        for (int i = 0; i < size; i++) {
            char c = segment.charAt(i);
            if (c == '/') return false;
            if (WINDOWS && (c == '\\' || c == ':')) return false;
        }
        return true;
    }

    public String lastSegment() {
        int len = segments.length;
        return len == 0 ? null : segments[len - 1];
    }

    public IPath makeAbsolute() {
        if (isAbsolute()) {
            return this;
        }
        Path result = new Path(device, segments, separators | HAS_LEADING);
        if (result.segmentCount() > 0) {
            String first = result.segment(0);
            if (first.equals("..") || first.equals(".")) {
                result.canonicalize();
            }
        }
        return result;
    }

    public IPath makeRelative() {
        if (!isAbsolute()) {
            return this;
        }
        return new Path(device, segments, separators & HAS_TRAILING);
    }

    public IPath makeUNC(boolean toUNC) {
        if (!(toUNC ^ isUNC())) return this;
        int newSeparators = this.separators;
        if (toUNC) {
            newSeparators |= HAS_LEADING | IS_UNC;
        } else {
            newSeparators &= HAS_LEADING | HAS_TRAILING;
        }
        return new Path(toUNC ? null : device, segments, newSeparators);
    }

    public int matchingFirstSegments(IPath anotherPath) {
        Assert.isNotNull(anotherPath);
        int anotherPathLen = anotherPath.segmentCount();
        int max = Math.min(segments.length, anotherPathLen);
        int count = 0;
        for (int i = 0; i < max; i++) {
            if (!segments[i].equals(anotherPath.segment(i))) {
                return count;
            }
            count++;
        }
        return count;
    }

    public IPath removeFileExtension() {
        String extension = getFileExtension();
        if (extension == null || extension.equals("")) {
            return this;
        }
        String lastSegment = lastSegment();
        int index = lastSegment.lastIndexOf(extension) - 1;
        return removeLastSegments(1).append(lastSegment.substring(0, index));
    }

    public IPath removeFirstSegments(int count) {
        if (count == 0) return this;
        if (count >= segments.length) {
            return new Path(device, NO_SEGMENTS, 0);
        }
        Assert.isLegal(count > 0);
        int newSize = segments.length - count;
        String[] newSegments = new String[newSize];
        System.arraycopy(this.segments, count, newSegments, 0, newSize);
        return new Path(device, newSegments, separators & HAS_TRAILING);
    }

    public IPath removeLastSegments(int count) {
        if (count == 0) return this;
        if (count >= segments.length) {
            return new Path(device, NO_SEGMENTS, separators & (HAS_LEADING | IS_UNC));
        }
        Assert.isLegal(count > 0);
        int newSize = segments.length - count;
        String[] newSegments = new String[newSize];
        System.arraycopy(this.segments, 0, newSegments, 0, newSize);
        return new Path(device, newSegments, separators);
    }

    public IPath removeTrailingSeparator() {
        if (!hasTrailingSeparator()) {
            return this;
        }
        return new Path(device, segments, separators & (HAS_LEADING | IS_UNC));
    }

    public String segment(int index) {
        if (index >= segments.length) return null;
        return segments[index];
    }

    public int segmentCount() {
        return segments.length;
    }

    public String[] segments() {
        String[] segmentCopy = new String[segments.length];
        System.arraycopy(segments, 0, segmentCopy, 0, segments.length);
        return segmentCopy;
    }

    public IPath setDevice(String value) {
        if (value != null) {
            Assert.isTrue(value.indexOf(IPath.DEVICE_SEPARATOR) == (value.length() - 1), "Last character should be the device separator");
        }
        if (value == device || (value != null && value.equals(device))) return this;
        return new Path(value, segments, separators);
    }

    public File toFile() {
        return new File(toOSString());
    }

    public String toOSString() {
        int resultSize = computeLength();
        if (resultSize <= 0) return EMPTY_STRING;
        char FILE_SEPARATOR = File.separatorChar;
        char[] result = new char[resultSize];
        int offset = 0;
        if (device != null) {
            int size = device.length();
            device.getChars(0, size, result, offset);
            offset += size;
        }
        if ((separators & HAS_LEADING) != 0) result[offset++] = FILE_SEPARATOR;
        if ((separators & IS_UNC) != 0) result[offset++] = FILE_SEPARATOR;
        int len = segments.length - 1;
        if (len >= 0) {
            for (int i = 0; i < len; i++) {
                int size = segments[i].length();
                segments[i].getChars(0, size, result, offset);
                offset += size;
                result[offset++] = FILE_SEPARATOR;
            }
            int size = segments[len].length();
            segments[len].getChars(0, size, result, offset);
            offset += size;
        }
        if ((separators & HAS_TRAILING) != 0) result[offset++] = FILE_SEPARATOR;
        return new String(result);
    }

    public String toPortableString() {
        int resultSize = computeLength();
        if (resultSize <= 0) return EMPTY_STRING;
        StringBuffer result = new StringBuffer(resultSize);
        if (device != null) result.append(device);
        if ((separators & HAS_LEADING) != 0) result.append(SEPARATOR);
        if ((separators & IS_UNC) != 0) result.append(SEPARATOR);
        int len = segments.length;
        for (int i = 0; i < len; i++) {
            if (segments[i].indexOf(DEVICE_SEPARATOR) >= 0) encodeSegment(segments[i], result); else result.append(segments[i]);
            if (i < len - 1 || (separators & HAS_TRAILING) != 0) result.append(SEPARATOR);
        }
        return result.toString();
    }

    public String toString() {
        int resultSize = computeLength();
        if (resultSize <= 0) return EMPTY_STRING;
        char[] result = new char[resultSize];
        int offset = 0;
        if (device != null) {
            int size = device.length();
            device.getChars(0, size, result, offset);
            offset += size;
        }
        if ((separators & HAS_LEADING) != 0) result[offset++] = SEPARATOR;
        if ((separators & IS_UNC) != 0) result[offset++] = SEPARATOR;
        int len = segments.length - 1;
        if (len >= 0) {
            for (int i = 0; i < len; i++) {
                int size = segments[i].length();
                segments[i].getChars(0, size, result, offset);
                offset += size;
                result[offset++] = SEPARATOR;
            }
            int size = segments[len].length();
            segments[len].getChars(0, size, result, offset);
            offset += size;
        }
        if ((separators & HAS_TRAILING) != 0) result[offset++] = SEPARATOR;
        return new String(result);
    }

    public IPath uptoSegment(int count) {
        if (count == 0) return new Path(device, EMPTY_STRING_ARRAY, separators & (HAS_LEADING | IS_UNC));
        if (count >= segments.length) return this;
        Assert.isTrue(count > 0, "Invalid parameter to Path.uptoSegment");
        String[] newSegments = new String[count];
        System.arraycopy(segments, 0, newSegments, 0, count);
        return new Path(device, newSegments, separators);
    }
}
