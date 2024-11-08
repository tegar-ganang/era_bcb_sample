package ffmpeg.dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.designerator.common.data.VideoInfo;
import org.designerator.common.string.StringUtil;
import org.designerator.common.system.FileUtil;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import ffmpeg.FFmpeg;
import ffmpeg.VideoUtil;
import ffmpeg.dialog.tabs.BasicTab;
import ffmpeg.dialog.tabs.ExpertTab;
import ffmpeg.dialog.tabs.OptionsTab;
import ffmpeg.editor.TxtIO;

public class FFmpegRunner {

    public static final String ERROR = "An Error has occurred";

    public static final String FINNISHED = "Encoding finnished";

    public static final String FAILED = "Encoding failed";

    public static final String ENCODING_SECOND_PASS = "Encoding Second Pass";

    public static final String ENCODING = "Encoding in progress";

    public static final String TEST_IN_PROGRESS = "Test Encoding in progress";

    public static final String ENCODING_FIRST_PASS = "Encoding First Pass";

    public static final String opt = "-";

    DialogMain dialog;

    List<String> cmd;

    List<String> cmd2;

    boolean test;

    private String outfile;

    private String infile;

    private boolean error = false;

    private int pass = 1;

    private long firstPassDuration;

    private boolean addExpert = false;

    public FFmpegRunner(DialogMain dialogMain, boolean test) {
        this.dialog = dialogMain;
        cmd = new ArrayList<String>();
        this.test = test;
        outfile = dialog.getInOut().getOutfile();
        infile = dialog.getInOut().getinFile();
        init();
    }

    private boolean fillStandardCommands(boolean runTest) {
        BasicTab basic = dialog.getTabBasic();
        ICodec codec = basic.getvOut_codec();
        if (codec.isTwoPass()) {
            cmd2 = new ArrayList<String>(cmd);
            pass = 2;
        }
        int start = 0;
        if (runTest) {
            start = dialog.getTabTesting().addTestStartTime(cmd);
            if (pass == 2) {
                dialog.getTabTesting().addTestStartTime(cmd2);
            }
        }
        addCommand(opt + "i", infile);
        OptionsTab tabOptions = dialog.getTabOptions();
        File out = new File(outfile);
        if (out.exists()) {
            if (!tabOptions.getOverWriteButton_s().getSelection()) {
                MessageBox m = new MessageBox(dialog.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
                m.setMessage("OutFile " + out.getName() + " already exists! Press OK to Overwrite or Cancel");
                if (m.open() != SWT.OK) {
                    setError(true);
                    return false;
                } else {
                    addCommand("-y", null);
                }
            } else {
                addCommand("-y", null);
            }
        }
        if (pass == 2) {
            cmd.add("-pass");
            cmd.add("1");
            cmd2.add("-pass");
            cmd2.add("2");
        }
        if (runTest) {
            int duration = basic.getDuration();
            int testLength = dialog.getTabTesting().addTestRunTime(cmd);
            if (duration < testLength + start) {
                dialog.setMessage("Incorrect time of test. Test time is longer than input file.", IMessageProvider.ERROR, null);
                setError(true);
                return false;
            }
            if (pass == 2) {
                dialog.getTabTesting().addTestRunTime(cmd2);
            }
        }
        String acodec = basic.getaOut_codec();
        if (StringUtil.isEmpty(acodec)) {
            dialog.setMessage("Empty Audio Codec - please choose a codec or copy or skip", IMessageProvider.ERROR, null);
            setError(true);
            return false;
        }
        if (acodec.indexOf(IFFmpeg.SKIP) != -1) {
            addCommand("-an", null);
        } else {
            if (pass == 1) {
                addAudioOptions(cmd, basic, acodec);
            } else {
                cmd.add("-an");
                addAudioOptions(cmd2, basic, acodec);
            }
        }
        if (StringUtil.isEmpty(codec.getName())) {
            dialog.setMessage("Empty codec", IMessageProvider.ERROR, null);
            setError(true);
            return false;
        }
        if (codec.getName().indexOf(IFFmpeg.SKIP) != -1) {
            addCommand("-vn", null);
        } else {
            addCommand("-vcodec", codec.getName());
            if (!codec.getName().equals(IFFmpeg.COPY)) {
                String vOut_bitrate = basic.getvOut_bitrate();
                if (StringUtil.isEmpty(vOut_bitrate)) {
                    dialog.setMessage("Empty bitrate", IMessageProvider.ERROR, null);
                    setError(true);
                    return false;
                }
                addCommand("-b", vOut_bitrate);
                String width = basic.getvOut_width();
                String height = basic.getvOut_height();
                if (!StringUtil.isEmpty(width) || !StringUtil.isEmpty(height) || !width.equals(IFFmpeg.COPY) || !height.equals(IFFmpeg.COPY)) {
                    addCommand("-s", width + "x" + height);
                }
                String aspect = basic.getvOut_Aspect();
                if (isValidInput(aspect)) {
                    addCommand("-aspect", aspect);
                }
                String fps = basic.getvOut_fps();
                if (isValidInput(fps)) {
                    addCommand("-r", fps);
                }
            }
        }
        if (!StringUtil.isEmpty(codec.getPresetPath())) {
            String presetPath = null;
            String presetFirstPath = null;
            if (codec.isTwoPass()) {
                presetFirstPath = getPresetPath(codec.getFirstPasspresetPath());
                presetPath = getPresetPath(codec.getPresetPath());
                if (presetPath == null || presetFirstPath == null) {
                    dialog.setMessage("Preset not found", IMessageProvider.ERROR, null);
                    setError(true);
                    return false;
                }
                cmd.add("-fpre");
                cmd.add(presetFirstPath);
                cmd2.add("-fpre");
                cmd2.add(presetPath);
            } else {
                presetPath = getPresetPath(codec.getPresetPath());
                if (presetPath == null) {
                    dialog.setMessage("Preset not found", IMessageProvider.ERROR, null);
                    setError(true);
                    return false;
                }
                cmd.add("-fpre");
                cmd.add(presetPath);
            }
        } else if ((codec.getPreset() != null)) {
            if (codec.isTwoPass()) {
                codec.addfirstPasspresetToCmdList(cmd);
                codec.addPresetToCmdList(cmd2);
            } else {
                codec.addPresetToCmdList(cmd);
            }
        }
        if (addExpert) {
            dialog.getTabExpert().fillOutFileCmd(cmd);
        }
        if (codec.isTwoPass()) {
            tabOptions.addToCmdList(cmd);
            tabOptions.addToCmdList(cmd2);
            cmd.add(outfile);
            cmd2.add(outfile);
        } else {
            tabOptions.addToCmdList(cmd);
            cmd.add(outfile);
        }
        return true;
    }

    public void addAudioOptions(List<String> cmd, BasicTab basic, String acodec) {
        cmd.add("-acodec");
        if (acodec.equals(IFFmpeg.COPY)) {
            cmd.add(IFFmpeg.COPY);
        } else {
            cmd.add(acodec);
            String hz = basic.getaOut_hz();
            if (isValidInput(hz)) {
                cmd.add("-ar");
                cmd.add(hz);
            }
            String abitrate = basic.getaOut_bitrate();
            if (isValidInput(abitrate)) {
                cmd.add("-ab");
                cmd.add(abitrate);
            }
            String channels = basic.getaOut_channels();
            if (isValidInput(channels)) {
                cmd.add("-ac");
                cmd.add(channels);
            }
        }
    }

    private void addCommand(String command, String value) {
        cmd.add(command);
        if (value != null) {
            cmd.add(value);
        }
        if (pass == 2) {
            cmd2.add(command);
            if (value != null) {
                cmd2.add(value);
            }
        }
    }

    public void initTiming() {
        dialog.getDisplay().syncExec(new Runnable() {

            @Override
            public void run() {
                firstPassDuration = 0;
                dialog.getTabMessages().resetStartTime();
                if (test) {
                    int testLength = dialog.getTabTesting().getTestLengthtxt();
                    int starttime = dialog.getTabTesting().getStarttimetxt();
                    dialog.setDuration(testLength + starttime, true);
                } else {
                    int duration = dialog.getTabBasic().getDuration();
                    dialog.setDuration(duration, false);
                }
            }
        });
    }

    public boolean isValidInput(String value) {
        return !StringUtil.isEmpty(value) && !value.equals(IFFmpeg.COPY);
    }

    public String getPresetPath(String preset) {
        File p = new File(preset);
        if (p.exists()) {
            return preset;
        }
        String path = null;
        String dir = dialog.getPresetDirectory();
        if (dir != null) {
            File d = new File(dir);
            path = getPresetPath(preset, d);
        }
        if (path == null) {
            String defaultProgram = dialog.getTabFFmpeg().getDefaultProgram();
            if (defaultProgram != null) {
                File ff = new File(defaultProgram);
                if (ff.exists()) {
                    File parentFile = ff.getParentFile().getParentFile();
                    if (parentFile != null) {
                        File pres = new File(parentFile, "share/ffmpeg");
                        if (pres.exists()) {
                            path = getPresetPath(preset, pres);
                        }
                    }
                }
            }
        }
        return path;
    }

    private String getPresetPath(String preset, File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i] != null && files[i].getName().equals(preset)) {
                    return files[i].getAbsolutePath();
                }
            }
        }
        return null;
    }

    private void init() {
        dialog.getTabMessages().setText("");
        ExpertTab tabExpert = dialog.getTabExpert();
        if (tabExpert.activateExpertButton.getSelection()) {
            if (tabExpert.getMode() == ExpertTab.AppendMode) {
                dialog.getTabExpert().fillInFileCmd(cmd);
                if (isError()) {
                    return;
                }
                addExpert = true;
                fillStandardCommands(test);
            } else if (tabExpert.getMode() == ExpertTab.OverRideMode) {
                dialog.getTabExpert().fillInFileCmd(cmd);
                addInFile();
                addTestTime();
                if (isError()) {
                    return;
                }
                dialog.getTabExpert().fillOutFileCmd(cmd);
                addOutFile();
            } else if (tabExpert.getMode() == ExpertTab.OverRideAllMode) {
                dialog.getTabExpert().fillInFileCmd(cmd);
                dialog.getTabExpert().fillOutFileCmd(cmd);
            }
        } else {
            if (!fillStandardCommands(test)) {
                setError(true);
                return;
            }
        }
    }

    public void addOutFile() {
        cmd.add(outfile);
    }

    public void addInFile() {
        cmd.add("-i");
        cmd.add(infile);
    }

    public void addTestTime() {
        if (test) {
            int duration = dialog.getTabBasic().getDuration();
            int l = dialog.getTabTesting().addTestRunTime(cmd);
            if (duration < l) {
                dialog.setMessage("Incorrect time of test. Test time is longer than input file.", IMessageProvider.ERROR, null);
                setError(true);
            }
        }
    }

    public void run() {
        if (isError()) {
            return;
        }
        if (!validate()) {
            dialog.setMessage("In File or OutFile are invalid.", IMessageProvider.ERROR, null);
            return;
        }
        activateProgress();
        String defaultProgram = dialog.getTabFFmpeg().getDefaultProgram();
        if (FileUtil.isFile(defaultProgram)) {
            FFmpeg.setFfmpegPath(defaultProgram);
        }
        final Display display = dialog.getDisplay();
        Thread t = new Thread("FFmpeg") {

            public void run() {
                if (pass == 2) {
                    dialog.setTitle(ENCODING_FIRST_PASS);
                    dialog.setCurrentTitle(ENCODING_FIRST_PASS);
                    dialog.setMessage("Start encoding! First Pass of 2", IMessageProvider.INFORMATION, null);
                } else {
                    if (test) {
                        dialog.setTitle(TEST_IN_PROGRESS);
                        dialog.setCurrentTitle(TEST_IN_PROGRESS);
                    } else {
                        dialog.setTitle(ENCODING);
                        dialog.setCurrentTitle(ENCODING);
                    }
                    dialog.setMessage("Start encoding!", IMessageProvider.INFORMATION, null);
                }
                initTiming();
                final FFmpeg fmpegTest = FFmpeg.getInstance();
                VideoInfo encodeVideo = fmpegTest.runFFmpeg(cmd, false, dialog.getTabMessages());
                if (pass == 2) {
                    firstPassDuration = (long) dialog.getRunTimeSecs();
                    initTiming();
                    dialog.resetProgress();
                    dialog.setTitle(ENCODING_SECOND_PASS);
                    dialog.setCurrentTitle(ENCODING_SECOND_PASS);
                    dialog.setMessage("Start encoding! Second Pass of 2", IMessageProvider.INFORMATION, null);
                    encodeVideo = fmpegTest.runFFmpeg(cmd2, false, dialog.getTabMessages());
                }
                if (encodeVideo == null) {
                    dialog.setTitle(ERROR);
                    dialog.setMessage(FAILED, IMessageProvider.ERROR, null);
                } else if (test) {
                    if (encodeVideo.exitCode == 0) {
                        runOutFile(display);
                        String t = VideoUtil.secondsToString((int) (dialog.getRunTimeSecs() + firstPassDuration));
                        dialog.setTitle(new File(outfile).getName() + " encoded in " + t);
                        dialog.setMessage(FINNISHED, IMessageProvider.INFORMATION, null);
                    } else {
                        dialog.setTitle(new File(outfile).getName());
                        dialog.setMessage(FAILED, IMessageProvider.ERROR, null);
                    }
                } else {
                    String t = VideoUtil.secondsToString((int) (dialog.getRunTimeSecs() + firstPassDuration));
                    dialog.setTitle(new File(outfile).getName() + " encoded in " + t);
                    dialog.setMessage(FINNISHED, IMessageProvider.INFORMATION, null);
                }
                dialog.setProgressFinnished();
                dialog.reset(display);
            }
        };
        t.start();
    }

    public void activateProgress() {
        dialog.setProgressVisible(true);
        dialog.resetProgress();
        if (test) {
            int l = dialog.getTabTesting().getTestLengthtxt();
            int s = dialog.getTabTesting().getStarttimetxt();
            dialog.getProgress().setMaximum(s + l);
            dialog.getTabMessages().setUpdateTime(2);
        } else {
            dialog.getProgress().setMaximum(dialog.getTabBasic().getDuration());
            dialog.getTabMessages().setUpdateTime(3);
        }
    }

    public boolean validate() {
        return TxtIO.checkValid(outfile, infile);
    }

    private void setCodec(String codec) {
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public List<String> getCmd2() {
        if (cmd2 == null) {
            cmd2 = new ArrayList<String>();
        }
        return cmd2;
    }

    public void runOutFile(final Display display) {
        display.asyncExec(new Runnable() {

            @Override
            public void run() {
                dialog.getTabTesting().runFile(outfile, true);
            }
        });
    }
}
