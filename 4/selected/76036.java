package jam4j.lang;

import jam4j.OutputChannel;
import jam4j.build.Target;
import jam4j.util.Strings;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The mechanism for automatic discovery of header-style dependencies.
 * 
 * @author Luke Maurer
 */
public class HeaderScanner {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final Context cxt;

    /**
     * Create a header scanner operating in the given context.
     * 
     * @param cxt The context to operate in.
     */
    public HeaderScanner(Context cxt) {
        this.cxt = cxt;
    }

    /**
     * Scan the given target for headers, operating in the given context.
     * 
     * @param cxt The context to operate in.
     * @param target The target to scan.
     */
    public static void scanForHeaders(Context cxt, Target target) {
        new HeaderScanner(cxt).scanForHeaders(target);
    }

    /**
     * Scan the given target for headers.
     * 
     * @param target The target to scan.
     */
    public void scanForHeaders(Target target) {
        cxt.enterOnTarget(target);
        try {
            scan(target);
        } finally {
            cxt.leaveScope();
        }
    }

    private void scan(Target target) {
        final Pattern pattern;
        {
            final String[] patternValue = SpecialVariable.HDRSCAN.get(cxt);
            if (patternValue.length == 0) return;
            try {
                pattern = Pattern.compile(Strings.join("|", patternValue), Pattern.MULTILINE);
            } catch (PatternSyntaxException e) {
                return;
            }
        }
        final String headerRule;
        {
            final String[] headerRuleValue = SpecialVariable.HDRRULE.get(cxt);
            if (headerRuleValue.length == 0) return; else headerRule = headerRuleValue[0];
        }
        OutputChannel.HEADER.printf("header scan %s\n", target.name());
        final String[] headers = performHeaderScan(target.boundFile(), pattern);
        if (headers.length > 0) cxt.rule(headerRule).call(cxt, target, new String[] { target.name() }, headers);
    }

    private static String[] performHeaderScan(File file, Pattern pattern) {
        final CharSequence chars;
        final Closeable closeable;
        try {
            final FileChannel channel = new FileInputStream(file).getChannel();
            final MappedByteBuffer byteBuffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
            chars = UTF8.decode(byteBuffer);
            closeable = channel;
        } catch (IOException e) {
            return null;
        }
        try {
            final List<String> ans = new ArrayList<String>();
            final Matcher matcher = pattern.matcher(chars);
            while (matcher.find()) {
                if (matcher.groupCount() < 1) continue;
                final String header = matcher.group(1);
                ans.add(header);
                OutputChannel.HEADER.printf("header found: %s\n", header);
            }
            return ans.toArray(new String[ans.size()]);
        } finally {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }
}
