package fi.helsinki.cs.kaisei;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import fi.helsinki.cs.kaisei.Weekday.Day;

public class Cli {

    private static Scanner input = new Scanner(System.in);

    private static Schedule schedule = null;

    private static final String endCommand = "/q";

    private static ObjectOutputStream objectOutput;

    private static ObjectInputStream objectInput;

    public static void main(String[] args) {
        Character foo;
        System.out.println("KaiSei  - Interaktiivinen Ajankäyttöväline");
        System.out.println("-------------------------------------------");
        do {
            System.out.println();
            System.out.print("Current week: ");
            if (schedule != null) System.out.println(schedule.getPeriod()); else System.out.println("(none)");
            printCommands();
            printPrompt();
            String str = input.nextLine();
            while (str.length() == 0) {
                printPrompt();
                str = input.nextLine();
            }
            foo = sanitize(str);
            switch(foo) {
                case 'p':
                    if (schedule == null) {
                        break;
                    }
                    printReportDialogToScreenDialog();
                    break;
                case 'a':
                    if (schedule == null) {
                        break;
                    }
                    newEventDialog();
                    break;
                case 's':
                    if (schedule == null) {
                        break;
                    }
                    saveScheduleDialog();
                    break;
                case 'f':
                    if (schedule == null) {
                        break;
                    }
                    printReportToFileDialog();
                    break;
                case 'n':
                    newScheduleDialog();
                    break;
                case 'o':
                    openScheduleDialog();
                    break;
                case 'e':
                    exportScheduleDialog();
                    break;
                case 'q':
                    System.exit(0);
                    break;
                case 'r':
                    renameScheduleDialog();
                    break;
                case 'm':
                    multipleScheduleDialog();
                    break;
                default:
                    System.out.println("Don't know what that command is");
                    break;
            }
        } while (true);
    }

    private static boolean checkDate(String in) {
        Integer day = null;
        try {
            day = Integer.parseInt(in);
            if (day > 0 && day <= 7) {
                return true;
            }
            System.out.println("Sorry, but \"" + day + "\" is not a valid number for date");
        } catch (NumberFormatException e) {
            System.out.println("Sorry, cannot parse \"" + in + "\"");
        }
        return false;
    }

    private static boolean checkDate(String in, Schedule schedule) {
        if (!checkDate(in)) {
            return false;
        }
        return schedule.getSchedule().containsKey(Weekday.intToEnumMap.get(Integer.parseInt(in)));
    }

    private static HashMap<String, Object> getOptions(String key, Day value) {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(key, value);
        return ret;
    }

    private static HashMap<String, Object> getOptions(String key, ArrayList<Day> value) {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(key, value);
        return ret;
    }

    private static void newEventDialog() {
        String startTime = null, endTime = null, location = null, title = null, eventDayTemp;
        Event event = null;
        Day eventDay = null;
        do {
            System.out.println("");
            System.out.println("Which day is the event?  /q to exit");
            printDates();
            printPrompt();
            eventDayTemp = input.nextLine();
            if (eventDayTemp.equals(endCommand)) {
                return;
            }
            if (!checkDate(eventDayTemp)) {
                continue;
            }
            while (true) {
                System.out.println("What is the start time? Valid times are " + Event.getAllValidValues(Event.VALID_START_TIMES));
                printPrompt();
                startTime = input.nextLine();
                if (!Event.checkIfValidTime(startTime, Event.VALID_START_TIMES)) {
                    System.out.println("Invalid time");
                    continue;
                }
                break;
            }
            while (true) {
                System.out.println("What is the end time? Valid times are " + Event.getAllValidValues(Event.VALID_END_TIMES));
                printPrompt();
                endTime = input.nextLine();
                if (!Event.checkIfValidTime(endTime, Event.VALID_END_TIMES)) {
                    System.out.println("Invalid time");
                    continue;
                }
                break;
            }
            System.out.println("What this event should be named as?");
            System.out.println("(just press enter to skip this)");
            printPrompt();
            title = input.nextLine();
            System.out.println("Where this event is held?");
            System.out.println("(just press enter to skip this)");
            printPrompt();
            location = input.nextLine();
            try {
                eventDay = Weekday.intToEnumMap.get(Integer.parseInt(eventDayTemp));
                event = new Event(startTime, endTime, title, location);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("Sorry, but some mistakes were made:");
                System.out.println(e.getMessage());
            }
        } while (true);
        System.out.print("Adding event to schedule...");
        try {
            schedule.addEvent(eventDay, event);
        } catch (IllegalArgumentException e) {
            System.out.println("Something went wrong:");
            System.out.println(e.getMessage());
            System.out.println("Sorry, but once more");
            newEventDialog();
            return;
        }
        System.out.println("ok!");
    }

    private static void renameScheduleDialog() {
        System.out.println("Enter the new name for the week this schedule is for:");
        printPrompt();
        String period = input.nextLine();
        if (period.equals(endCommand)) return;
        schedule.setPeriod(period);
    }

    private static void multipleScheduleDialog() {
        Schedule orig = schedule;
        ArrayList<String> schedules = new ArrayList<String>();
        System.out.println("Enter the weeks to be printed (/q to quit, empty to print schedules):");
        printPrompt();
        while (input.hasNextLine()) {
            String str = input.nextLine();
            str = str.trim();
            if (str.equals("/q")) return;
            if (str.isEmpty()) break;
            str = (str.endsWith(".dat") ? str : str + ".dat");
            File tmp = new File(str);
            if (!tmp.exists()) {
                System.out.println("File " + str + " doesn't exist");
                continue;
            }
            schedules.add(str);
            printPrompt();
        }
        for (String s : schedules) {
            System.out.println("\n");
            open(s);
            ArrayList<Day> days = new ArrayList<Day>();
            for (Day day : schedule.getSchedule().keySet()) {
                days.add(day);
            }
            System.out.println(ReportFactory.makeReport(ReportFactory.ReportType.WEEK, schedule, getOptions("days", days)));
        }
        schedule = orig;
        return;
    }

    private static void newScheduleDialog() {
        System.out.println("What week is the schedule for?");
        printPrompt();
        String period = input.nextLine();
        if (period.equals(endCommand)) return;
        System.out.println("Give dates you want to include in the scedule");
        System.out.println("Example: 1 3 5");
        HashSet<Integer> dates = askDatesDialog();
        if (dates != null) {
            printSelection(dates);
            System.out.print("Creating schedule...");
            ArrayList<Day> daysArray = new ArrayList<Day>();
            for (Integer d : dates) {
                daysArray.add(Weekday.intToEnumMap.get(d));
            }
            schedule = new Schedule(daysArray, period);
            System.out.println("ok!");
        }
    }

    private static HashSet<Integer> askDatesDialog() {
        HashSet<Integer> result;
        String in = null;
        boolean keepAsking;
        do {
            keepAsking = false;
            result = new HashSet<Integer>();
            printDates();
            printPrompt();
            in = input.nextLine().trim();
            if (in.equals(endCommand)) {
                result = null;
                break;
            }
            String[] days = in.split(" ");
            for (int i = 0; i < days.length && keepAsking == false; i++) {
                if (checkDate(days[i])) {
                    result.add(Integer.parseInt(days[i]));
                } else keepAsking = true;
            }
        } while (keepAsking);
        return result;
    }

    private static boolean open(String filename) {
        objectInput = null;
        Schedule originalSchedule = schedule;
        schedule = new Schedule();
        FileInputStream fos = null;
        try {
            fos = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            System.out.println("File \"" + filename + "\" couldn't be opened");
            schedule = originalSchedule;
            return false;
        }
        try {
            objectInput = new ObjectInputStream(fos);
        } catch (IOException e) {
            System.out.println("Cannot read \"" + filename + "\" from FileInputStream");
            schedule = originalSchedule;
            return false;
        }
        try {
            schedule.setSchedule((Schedule) objectInput.readObject());
            return true;
        } catch (IOException e) {
            System.out.println("Cannot read \"" + filename + "\" from ObjectInputStream");
            schedule = originalSchedule;
        } catch (ClassNotFoundException e) {
            System.out.println("Cannot find class for the object when reading \"" + filename + "\"");
            schedule = originalSchedule;
        }
        return false;
    }

    private static void openScheduleDialog() {
        System.out.println("Give name of the file to be opened");
        printPrompt();
        String filename = input.nextLine().trim();
        while (true) {
            if (!filename.endsWith(".dat")) {
                filename += ".dat";
            }
            if (open(filename)) {
                break;
            } else {
                System.out.println("Please enter the name of the file again");
                System.out.println("You can exit with " + endCommand);
                filename = input.nextLine().trim();
                if (filename.equals(endCommand)) {
                    return;
                }
            }
        }
    }

    private static void printCommands() {
        System.out.println("Commands");
        System.out.println("--------");
        System.out.println("[N]ew schedule");
        System.out.println("[O]pen schedule from file");
        System.out.println("Print [M]ultiple schedules");
        if (schedule != null) {
            System.out.println("[A]dd event to schedule");
            System.out.println("[R]ename the schedule");
            System.out.println("[S]ave schedule to file");
            System.out.println("[P]rint a report on screen");
            System.out.println("Print a report to [F]ile");
            System.out.println("[E]xport for spreadsheet as CSV file");
        }
        System.out.println("[Q]uit");
    }

    private static void printDates() {
        System.out.print("Dates are: ");
        for (Day d : Day.values()) {
            System.out.print(Weekday.enumToIntMap.get(d));
            System.out.print(" - ");
            System.out.print(Weekday.longNameMap.get(d));
            System.out.print(" ");
        }
        System.out.println();
    }

    private static void printDates(Schedule schedule) {
        System.out.print("Dates are: ");
        for (Day d : schedule.getSchedule().keySet()) {
            System.out.print(Weekday.enumToIntMap.get(d));
            System.out.print(" - ");
            System.out.print(Weekday.longNameMap.get(d));
            System.out.print(" ");
        }
        System.out.println();
    }

    private static void printPrompt() {
        System.out.print("KaiSei>");
    }

    private static Report printReportDialog() {
        Character command = null;
        while (true) {
            System.out.print("Which type of report do you want to print? Options are: ");
            for (ReportFactory.ReportType type : ReportFactory.ReportType.values()) {
                System.out.print("[" + type.toString().charAt(0) + "]" + type.toString().substring(1).toLowerCase());
                System.out.print(" ");
            }
            System.out.println("[N]one");
            printPrompt();
            command = sanitize(input.nextLine());
            String in = null;
            switch(command) {
                case 'd':
                    System.out.println("Which day you want to see your schedule for?");
                    printDates();
                    printPrompt();
                    in = input.nextLine();
                    if (!checkDate(in)) {
                        System.out.println("Unvalid date");
                        break;
                    }
                    Day day = Weekday.intToEnumMap.get(Integer.parseInt(in));
                    return ReportFactory.makeReport(ReportFactory.ReportType.DAY, schedule, getOptions("day", day));
                case 'f':
                    return ReportFactory.makeReport(ReportFactory.ReportType.FULL, schedule, null);
                case 'w':
                    ArrayList<Day> days = new ArrayList<Day>();
                    System.out.println("Which days you want to include in this report? You can end with \"" + endCommand + "\"");
                    System.out.println("One at the time, please");
                    while (true) {
                        printDates(schedule);
                        printPrompt();
                        in = input.nextLine();
                        if (in.equals(endCommand)) {
                            break;
                        } else if (!checkDate(in, schedule)) {
                            System.out.println("Unvalid date");
                        } else if (days.contains(Weekday.intToEnumMap.get(Integer.parseInt(in)))) {
                            System.out.println("Day already chosen!");
                        } else {
                            days.add(Weekday.intToEnumMap.get(Integer.parseInt(in)));
                        }
                    }
                    return ReportFactory.makeReport(ReportFactory.ReportType.WEEK, schedule, getOptions("days", days));
                case 'n':
                    System.out.println("Returning back to main menu");
                    return null;
                default:
                    System.out.println("Cannot parse " + command);
                    break;
            }
        }
    }

    private static void printReportToFileDialog() {
        Report report = printReportDialog();
        if (report != null) {
            PrintWriter out = null;
            String filename = null;
            System.out.println("Give full file name and path (if applicable)");
            while (true) {
                printPrompt();
                try {
                    filename = input.nextLine().trim();
                    out = new PrintWriter(filename);
                    break;
                } catch (FileNotFoundException e) {
                    System.out.println("File " + filename + " was not found");
                }
            }
            System.out.print("Writing the file...");
            out.print(report);
            out.close();
            System.out.println("ok!");
        }
    }

    private static void printReportDialogToScreenDialog() {
        Report report = printReportDialog();
        if (report != null) {
            System.out.println(report);
        }
    }

    private static void printSelection(HashSet<Integer> dates) {
        if (dates.size() > 0) {
            System.out.print("You have selected: ");
            for (Integer d : dates) {
                System.out.print(Weekday.longNameMap.get(Weekday.intToEnumMap.get(d)));
                System.out.print(" ");
            }
            System.out.println();
        }
    }

    private static Character sanitize(String rawInput) {
        return new Character(rawInput.toLowerCase().charAt(0));
    }

    private static boolean save(String filename) {
        objectOutput = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            System.out.println("Cannot open \"" + filename + "\", something's wrong with it");
            return false;
        }
        try {
            objectOutput = new ObjectOutputStream(fos);
        } catch (IOException e) {
            System.out.println("Cannot write to \"" + filename + "\"");
            return false;
        }
        try {
            objectOutput.writeObject(schedule);
            objectOutput.close();
            return true;
        } catch (IOException e) {
            System.out.println("Writing to \"" + filename + "\" failed");
            return false;
        }
    }

    private static boolean saveString(String fileName, String data) {
        BufferedWriter out;
        try {
            out = new BufferedWriter(new PrintWriter(fileName));
        } catch (IOException e) {
            System.out.println("Cannot write to \"" + fileName + "\"");
            return false;
        }
        try {
            out.write(data);
            out.close();
            return true;
        } catch (IOException e) {
            System.out.println("Writing to \"" + fileName + "\" failed");
            return false;
        }
    }

    private static void saveScheduleDialog() {
        String fileName = schedule.getPeriod();
        if (fileName.isEmpty()) {
            System.out.println("Please (re)name the schedule first. (name cannot be empty)");
            return;
        }
        fileName += ".dat";
        File saveFile = new File(fileName);
        if (saveFile.exists()) {
            String answer;
            boolean ok = false;
            do {
                System.out.println("\"" + fileName + "\" already exists. Overwrite? (y or n)");
                answer = input.nextLine().trim();
                if (answer.equals("y")) ok = true; else {
                    System.out.println("Not saved.");
                    return;
                }
            } while (!ok);
        }
        save(fileName);
        System.out.println("Schedule saved as \"" + fileName + "\"");
    }

    private static void exportScheduleDialog() {
        System.out.println("Give a name for exported file. /q to cancel");
        System.out.println("Note that file will be saved with .csv-extension, eg. \"myfile\" will be \"myfile.csv\" ");
        printPrompt();
        String filename = input.nextLine().trim() + ".csv";
        if (filename.contains("/q")) return;
        while (true) {
            if (saveString(filename, Schedule.ScheduleToCSV(schedule))) {
                break;
            } else {
                System.out.println("Please enter the name of the file again");
                System.out.println("You can exit with " + endCommand);
                filename = input.nextLine().trim() + ".csv";
                if (filename.trim().toLowerCase().equals(endCommand)) {
                    return;
                }
            }
        }
        System.out.println("Schedule saved as \"" + filename + "\"");
    }
}
