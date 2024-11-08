package org.jtv.frontend;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedSet;
import org.jtv.common.RecordingData;
import org.jtv.common.TvController;
import org.jtv.common.TvControllerResult;

public class ConsoleTvController {

    private TvController controller;

    public ConsoleTvController(TvController controller) {
        super();
        this.controller = controller;
    }

    public void commandGETCHANNEL(Integer tunerNumber) {
        int channel = controller.getChannel(tunerNumber);
        System.out.println("channel " + channel);
    }

    public void commandGETRECORDINGSFROM(Long from) {
        SortedSet<RecordingData> datas = controller.getRecordingsFrom(from);
        for (RecordingData data : datas) {
            System.out.println("(" + data.getId() + ")\t" + data.getChannel() + "\t" + toTimeString(data.getStart()) + "\t" + toTimeString(data.getEnd()) + "\t" + data.getName());
        }
    }

    public void commandCANCELRECORDINGID(Integer id) {
        TvControllerResult result = controller.cancelRecordingId(id);
        showResult(result);
    }

    private void showResult(TvControllerResult result) {
        System.out.println(result.getOperation() + " " + result.getMessage());
    }

    public void commandCHANGECHANNEL(Integer tunerNumber, Integer channel) {
        TvControllerResult result = controller.changeChannel(tunerNumber, channel);
        showResult(result);
    }

    public void commandSCHEDULERECORDING(Integer channel, Long start, Long end, String name) {
        TvControllerResult result = controller.scheduleRecording(channel, start, end, name);
        showResult(result);
    }

    private String toTimeString(long millis) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(millis);
        return time.get(Calendar.YEAR) + ":" + leadingZero(time.get(Calendar.MONTH) + 1) + ":" + leadingZero(time.get(Calendar.DAY_OF_MONTH)) + ":" + leadingZero(time.get(Calendar.HOUR_OF_DAY)) + ":" + leadingZero(time.get(Calendar.MINUTE));
    }

    private String leadingZero(int n) {
        if (n < 10) {
            return "0" + n;
        } else {
            return String.valueOf(n);
        }
    }

    public void repLoop(InputStream in) {
        try {
            BufferedReader rin = new BufferedReader(new InputStreamReader(in));
            String line;
            prompt();
            while ((line = rin.readLine()) != null) {
                String[] params = line.split("\\s");
                if (params.length > 0 && !"".equals(params[0])) {
                    params[0] = params[0].toUpperCase();
                    if ("STOP".equals(params[0]) || "QUIT".equals(params[0]) || "EXIT".equals(params[0]) || "HALT".equals(params[0])) {
                        break;
                    }
                    String methodName = "command" + params[0];
                    try {
                        Object[] actualParams = new Object[params.length - 1];
                        Class[] actualParamsType = new Class[params.length - 1];
                        for (int i = 0; i < actualParams.length; i++) {
                            actualParams[i] = interpretParam(params[i + 1]);
                            actualParamsType[i] = actualParams[i].getClass();
                        }
                        Method method = getClass().getMethod(methodName, actualParamsType);
                        method.invoke(this, actualParams);
                    } catch (InvocationTargetException ite) {
                        ite.printStackTrace();
                    } catch (NoSuchMethodException nsme) {
                        System.out.println("Unknown command");
                        for (Method method : getClass().getMethods()) {
                            if (method.getName().equals(methodName)) {
                                System.out.println(params[0] + Arrays.toString(method.getParameterTypes()));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                prompt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(getClass().getSimpleName() + ": done");
    }

    private void prompt() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.print(new Date(controller.getTime()) + "> ");
    }

    private Object interpretParam(String param) {
        if ("$NOW".equalsIgnoreCase(param)) {
            return System.currentTimeMillis();
        } else if (param.indexOf(':') != -1) {
            String[] params = param.split(":");
            Calendar calendar = Calendar.getInstance();
            int index = 0;
            if (params.length > 4) {
                calendar.set(Calendar.YEAR, Integer.parseInt(params[index]));
                index++;
            }
            if (params.length > 3) {
                calendar.set(Calendar.MONTH, Integer.parseInt(params[index]) - 1);
                index++;
            }
            if (params.length > 2) {
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(params[index]));
                index++;
            }
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(params[index]));
            index++;
            calendar.set(Calendar.MINUTE, Integer.parseInt(params[index]));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTimeInMillis();
        } else if (Character.isDigit(param.charAt(0))) {
            return Integer.parseInt(param);
        } else {
            return String.valueOf(param);
        }
    }
}
