package spindles.api.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import org.apache.commons.lang.Validate;
import spindles.api.domain.Epoch;
import spindles.api.domain.SamplingRate;
import spindles.api.util.ApplicationException;
import spindles.api.util.ErrorMessages;
import spindles.api.util.FileUtil;
import spindles.api.util.Processor;
import spindles.api.util.UserException;
import spindles.api.util.Util;

/**
 * @author yorgos
 * 
 */
public class EEGParser {

    private SamplingRate samplingRate = SamplingRate.SR256;

    private String firstName;

    private String lastName;

    private Date sessionStartDate;

    private Date partStartDate;

    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private SimpleDateFormat greekDateFormat = new SimpleDateFormat("ddMMMMMyyyy", new Locale("el"));

    SimpleDateFormat enDateFormat = new SimpleDateFormat("MMMMdd,yyyy", new Locale("en"));

    private DateFormat[] parsers = new DateFormat[] { greekDateFormat, enDateFormat };

    private File eegFile;

    private String channel;

    private int channelColumn;

    private BufferedReader input;

    private boolean finished = false;

    public EEGParser(File eegFile, String channel) throws UserException {
        Validate.notNull(eegFile);
        this.eegFile = eegFile;
        this.channel = channel;
        init();
    }

    protected void init() throws UserException {
        parseEEGProperties();
        parseChannelColumn();
        parsePartStartTime();
        skipFirstLines();
    }

    private void parseEEGProperties() throws UserException {
        FileUtil.readFile(eegFile, new Processor() {

            public void process(BufferedReader in) throws Exception {
                String line = null;
                if ((line = in.readLine()) == null) {
                    throw new UserException(ErrorMessages.FILE_UNEXPECTED_FORMAT);
                }
                line = line.trim();
                String tokens[] = line.split("\\s");
                if (tokens.length < 8) {
                    throw new UserException(ErrorMessages.FILE_UNEXPECTED_FORMAT);
                }
                lastName = tokens[0];
                firstName = tokens[1];
                sessionStartDate = Util.parseDate(tokens[4] + tokens[5] + tokens[6], parsers);
                String result = line.substring(line.length() - 3);
                try {
                    samplingRate = SamplingRate.toEnum(Integer.parseInt(result));
                } catch (RuntimeException e) {
                    throw new UserException(ErrorMessages.FILE_UNEXPECTED_FORMAT);
                }
            }
        });
    }

    private void parseChannelColumn() throws UserException {
        FileUtil.readFile(eegFile, new Processor() {

            public void process(BufferedReader r) throws Exception {
                boolean channelFound = false;
                String line = null;
                for (int i = 1; (line = r.readLine()) != null && i < 7; i++) ;
                if (line == null) {
                    throw new UserException(ErrorMessages.CHANNEL_NOT_FOUND);
                }
                int counter = 1;
                Scanner s = new Scanner(line);
                for (; s.hasNext(); counter++) {
                    String ch = s.next();
                    if (ch.equals(channel)) {
                        channelFound = true;
                        break;
                    }
                }
                channelColumn = counter;
                if (!channelFound) {
                    throw new UserException(ErrorMessages.CHANNEL_NOT_FOUND);
                }
            }
        });
    }

    public SamplingRate getSamplingRate() {
        return samplingRate;
    }

    private void parsePartStartTime() {
        FileUtil.readFile(eegFile, new Processor() {

            public void process(BufferedReader r) throws Exception {
                int counter = 0;
                String line = null;
                while ((line = r.readLine()) != null) {
                    counter++;
                    if (hasTimeToken(line)) {
                        break;
                    }
                }
                if (line == null) {
                    throw new UserException(ErrorMessages.FILE_UNEXPECTED_FORMAT);
                }
                Scanner s = new Scanner(line);
                String time = s.next();
                Scanner timeTk = new Scanner(time).useDelimiter(":");
                Calendar cal = Calendar.getInstance();
                cal.setTime(sessionStartDate);
                cal.add(Calendar.HOUR_OF_DAY, timeTk.nextInt());
                cal.add(Calendar.MINUTE, timeTk.nextInt());
                cal.add(Calendar.SECOND, timeTk.nextInt());
                partStartDate = cal.getTime();
            }
        });
    }

    private void skipFirstLines() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(eegFile));
            int counter = 0;
            String line = null;
            while ((line = in.readLine()) != null) {
                counter++;
                if (hasTimeToken(line)) {
                    break;
                }
            }
            in.close();
            in = new BufferedReader(new FileReader(eegFile));
            for (int i = 0; i < counter - 1; i++) {
                in.readLine();
            }
            input = in;
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }

    public List<Double> getNextEpoch() throws UserException {
        if (finished) {
            return null;
        }
        try {
            List<Double> result = new ArrayList<Double>(Epoch.getSamplesCount(samplingRate));
            String line = null;
            int nentries = Epoch.getSamplesCount(samplingRate);
            for (int i = 0; i < nentries; i++) {
                if ((line = input.readLine()) != null) {
                    result.add(Double.parseDouble(getChannelValue(line)));
                } else {
                    if (i >= samplingRate.value()) {
                        Util.add(result, 0.0, nentries - i);
                    }
                    input.close();
                    finished = true;
                    break;
                }
            }
            if (result.size() < nentries) {
                return null;
            }
            return result;
        } catch (NumberFormatException e) {
            throw new UserException(ErrorMessages.FILE_UNEXPECTED_FORMAT);
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }

    public boolean hasTimeToken(String line) {
        Scanner s = new Scanner(line);
        if (s.hasNext()) {
            try {
                timeFormat.parse(s.next());
                return true;
            } catch (ParseException e) {
                return false;
            }
        }
        return false;
    }

    protected String getChannelValue(String line) {
        int column = channelColumn;
        if (hasTimeToken(line)) {
            column++;
        }
        int i = 1;
        for (Scanner s = new Scanner(line); s.hasNext(); i++) {
            if (i == column) {
                return s.next();
            } else {
                s.next();
            }
        }
        throw new RuntimeException(channel + " value could not be found.");
    }

    public String getChannel() {
        return channel;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Date getPartStartDate() {
        return partStartDate;
    }

    public Date getSessionStartDate() {
        return sessionStartDate;
    }

    public int getChannelColumn() {
        return channelColumn;
    }
}
