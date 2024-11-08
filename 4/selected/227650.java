package keyboardhero;

import java.io.*;

class Tester {

    /** Specifies how many characters should be the width of the menu. */
    private static final int MENUWIDTH = 40;

    /**
	 * Specifies how many characters should be the width of the responses and statistics of the
	 * tests.
	 */
    private static final int TESTWIDTH = 78;

    /** The buffered reader used to read from the standard input. */
    private static final BufferedReader BUFFERED_RDR = new BufferedReader(new InputStreamReader(System.in));

    /**
	 * A static variable, which indicates how many test have failed overall in the whole
	 * application. It is used for statistics generation.
	 */
    private static int superFailed;

    /**
	 * A static variable, which indicates how many test have been successful overall in the whole
	 * application. It is used for statistics generation.
	 */
    private static int superPassed;

    /**
	 * Indicates whether the application should be terminated if the user exits from debugging menu
	 * of this tester object.
	 */
    private int exitMode = 0;

    /** The name of the owner class. */
    protected String name;

    /** The possible options to be displayed in the menu. */
    private String[] commands;

    /**
	 * Indicates whether the menu should be redisplayed again; or the user exited from it, thus it
	 * should be terminated.
	 */
    private boolean inMenu;

    /** The starting time of the current test. */
    private long startTime;

    /** The starting time of the unit test series in this tester object. */
    private long higherStartTime;

    /**
	 * This field is used when multiple unit test series is run, either inside this tester object or
	 * using other tester objects as well. The field indicates the starting time of this multiple
	 * unit test series.
	 * 
	 * @see #superTestStart()
	 */
    private long superStartTime;

    /** The name of the current test. */
    private String testName;

    /** Stores how many test have failed in this unit test series. */
    private int testFailed;

    /** Stores how many test have been successful in this unit test series. */
    private int testPassed;

    /** The main / always available menu options. */
    private static final String[] BASE_MENU = new String[] { "Quit", "Run Sandbox", "Execute an External Command", "Set Debug Level", "Run Unit Tests (+/-)" };

    /**
	 * Solo constructor, which sets the properties of the debugging menu.
	 * 
	 * @param name
	 *            the new value of the {@link #name} field.
	 * @param commands
	 *            the new value of the {@link #commands} field.
	 */
    Tester(String name, String[] commands) {
        this.name = name;
        this.commands = new String[BASE_MENU.length + commands.length];
        int i = 0;
        for (String s : BASE_MENU) {
            this.commands[i++] = s;
        }
        for (String s : commands) {
            this.commands[i++] = s;
        }
    }

    /**
	 * Starts the debugging menu and also sets whether the application should be terminated when the
	 * user exits from the menu. This method contains the loop which always reprints the menu, and
	 * handles the user choice by the {@link #menu(int)} method. It also prints the nature of a
	 * possibly thrown Exception.
	 * 
	 * @param inpExitMode
	 *            a boolean variable, which sets the value of the {@link #exitMode} field.
	 */
    void start(int inpExitMode) {
        exitMode = inpExitMode;
        start();
    }

    /**
	 * Starts the debugging menu. This method contains the loop which always reprints the menu, and
	 * handles the user choice by the {@link #menu(int)} method. It also prints the nature of a
	 * possibly thrown Exception.
	 * 
	 * @see #start(int)
	 * @see #end()
	 * @see #inMenu
	 */
    void start() {
        if (commands.length < 1) {
            System.err.println("There are no menu options: " + name + "!");
            return;
        }
        inMenu = true;
        final int titleLength = name.length();
        final int rptN = (MENUWIDTH - 12 - titleLength) / 2;
        String menuStr = "\n\n" + Util.charRepeat('=', rptN) + " " + name + " Debug Menu " + Util.charRepeat('=', rptN + (titleLength % 2)) + "\n" + Util.charRepeat('-', MENUWIDTH) + "\n";
        for (int i = 1; i < commands.length; i++) {
            menuStr += ((i < 10) ? "  " : " ") + i + ": " + commands[i] + "\n";
        }
        menuStr += "  0: " + commands[0] + "\n" + Util.charRepeat('=', MENUWIDTH) + "\nPlease enter your choice: ";
        while (inMenu) {
            System.out.print(menuStr);
            try {
                int choice = Integer.parseInt(BUFFERED_RDR.readLine());
                if (choice >= commands.length) {
                    invalidMenuOption();
                } else {
                    try {
                        Util.setDebugMode();
                        menu(choice);
                    } catch (Exception e) {
                        System.out.println(e.toString());
                        if (Util.getDebugLevel() > 128) e.printStackTrace();
                    }
                }
            } catch (NumberFormatException e) {
                invalidMenuOption();
            } catch (IOException e) {
                System.err.println("IOException while reading from the console in Tester.start: " + e.toString());
            }
        }
    }

    /**
	 * Exits from the debugging menu, and depending on the value of the {@link #exitMode} mode field
	 * also exits from the application.
	 * 
	 * @see #start()
	 * @see #start(int)
	 * @see #inMenu
	 */
    void end() {
        inMenu = false;
        switch(exitMode) {
            case 1:
                KeyboardHero.startDebugMenu();
                break;
            case 2:
                try {
                    KeyboardHero.exitGame();
                } catch (Exception e) {
                    System.out.println(e.toString());
                }
                System.exit(0);
                break;
        }
    }

    /** Asks the user for a new debug level and changes the current one to it. */
    void setDebugLevel() {
        System.out.println("Current debug level: " + Util.getDebugLevel());
        System.out.print("Please type in the new one: ");
        try {
            Util.setDebugLevel(Integer.parseInt(BUFFERED_RDR.readLine()));
        } catch (NumberFormatException e) {
            System.out.println("Invalid Debug Level! The debug level required to be an integer!");
        } catch (IOException e) {
            System.err.println("IOException while reading from the console in Tester.setDebugLevel: " + e.toString());
        }
    }

    void baseMenu(int choice) throws Exception {
        switch(choice) {
            case 0:
                end();
                break;
            case 1:
                try {
                    sandbox();
                } catch (Exception e) {
                    if (Util.getDebugLevel() > 10) e.printStackTrace();
                } catch (Throwable e) {
                    if (Util.getDebugLevel() > 5) {
                        System.err.println("========== ERROR START ==========");
                        e.printStackTrace();
                        System.err.println("==========  ERROR END  ==========");
                    }
                }
                break;
            case 2:
                exec();
                break;
            case 3:
                setDebugLevel();
                break;
            case 4:
                runUnitTests();
                break;
            case -4:
                final int max = readInt("How many times");
                superTestStart(name.toUpperCase());
                for (int i = 0; i < max; i++) {
                    runUnitTests();
                }
                superTestEnd();
                break;
            default:
                invalidMenuOption();
                break;
        }
    }

    /** Asks the user for a new debug level and changes the current one to it. */
    void exec() {
        System.out.print("Command to Execute: ");
        FileOutputStream foutp = null;
        try {
            foutp = new FileOutputStream("keyboardhero/Tmp.java");
            foutp.write(("package keyboardhero;\n" + "\n" + "import java.io.*;\n" + "\n" + "final class Tmp {\n" + "	public static void main(String[] args) {\n" + "		" + BUFFERED_RDR.readLine() + "\n" + "	}\n" + "}\n").getBytes());
            foutp.close();
            String ln;
            Process process = Runtime.getRuntime().exec("javac -classpath .;lib/jogl.jar;lib/gluegen-rt.jar keyboardhero/Tmp.java");
            BufferedReader buff = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((ln = buff.readLine()) != null) {
                System.out.println(ln);
            }
            boolean isOk = true;
            buff = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((ln = buff.readLine()) != null) {
                isOk = false;
                System.out.println(ln);
            }
            process.waitFor();
            if (isOk) {
                process = Runtime.getRuntime().exec("java keyboardhero.Tmp");
                buff = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((ln = buff.readLine()) != null) {
                    System.out.println(ln);
                }
                buff = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((ln = buff.readLine()) != null) {
                    System.out.println(ln);
                }
                process.waitFor();
            }
        } catch (Exception e) {
            if (Util.getDebugLevel() > 80) e.printStackTrace();
        } finally {
            if (foutp != null) {
                try {
                    foutp.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * Reads a line from the standard input and returns it.
	 * 
	 * @param prompt
	 *            the prompt printed to the user.
	 * @return the read string from the standard input.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    static String readString(String prompt) throws Exception {
        System.out.print(prompt + ": ");
        return BUFFERED_RDR.readLine();
    }

    /**
	 * Reads a line from the standard input and returns it as an array of strings.
	 * 
	 * @param prompt
	 *            the prompt printed to the user.
	 * @return the array of the read string. The separation is based on the white spaces.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    static String[] readStrings(String prompt) throws Exception {
        System.out.print(prompt + ": ");
        String line = BUFFERED_RDR.readLine();
        if (line == null) throw new NullPointerException();
        return line.split(" ");
    }

    /**
	 * Reads in an integer from the standard input and returns it.
	 * 
	 * @param prompt
	 *            the prompt printed to the user.
	 * @return the read integer from the standard input.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    static int readInt(String prompt) throws Exception {
        System.out.print(prompt + ": ");
        return Integer.parseInt(BUFFERED_RDR.readLine());
    }

    /**
	 * Reads multiple integers from the standard input and returns it as an array.
	 * 
	 * @param prompt
	 *            the prompt printed to the user.
	 * @return the array of the read integers. The separation is based on the white spaces.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    static int[] readInts(String prompt) throws Exception {
        System.out.print(prompt + ": ");
        String line = BUFFERED_RDR.readLine();
        if (line == null) throw new NullPointerException();
        if (line.equals("")) return new int[0];
        String[] parts = line.split(" ");
        int[] ints = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ints[i] = Integer.parseInt(parts[i]);
        }
        return ints;
    }

    /**
	 * Reads in an integer from the standard input and returns it.
	 * 
	 * @param prompt
	 *            the prompt printed to the user.
	 * @return the read integer from the standard input.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    static long readLong(String prompt) throws Exception {
        System.out.print(prompt + ": ");
        return Long.parseLong(BUFFERED_RDR.readLine());
    }

    /**
	 * Reads multiple long integers from the standard input and returns it as an array.
	 * 
	 * @param prompt
	 *            the prompt printed to the user.
	 * @return the array of the read long integers. The separation is based on the white spaces.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    static long[] readLongs(String prompt) throws Exception {
        System.out.print(prompt + ": ");
        String line = BUFFERED_RDR.readLine();
        if (line == null) throw new NullPointerException();
        if (line.equals("")) return new long[0];
        String[] parts = line.split(" ");
        long[] longs = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            longs[i] = Long.parseLong(parts[i]);
        }
        return longs;
    }

    /**
	 * Reads in a floating point number from the standard input and returns it.
	 * 
	 * @param prompt
	 *            the prompt printed to the user.
	 * @return the read long floating point number from the standard input.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    static float readFloat(String prompt) throws Exception {
        System.out.print(prompt + ": ");
        return Float.parseFloat(BUFFERED_RDR.readLine());
    }

    /**
	 * Reads multiple floating point numbers from the standard input and returns it as an array.
	 * 
	 * @param prompt
	 *            the prompt printed to the user.
	 * @return the array of the read long floating point numbers. The separation is based on the
	 *         white spaces.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    static float[] readFloats(String prompt) throws Exception {
        System.out.print(prompt + ": ");
        String line = BUFFERED_RDR.readLine();
        if (line == null) throw new NullPointerException();
        if (line.equals("")) return new float[0];
        String[] parts = line.split(" ");
        float[] floats = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            floats[i] = Float.parseFloat(parts[i]);
        }
        return floats;
    }

    /**
	 * Reads in a long floating point number from the standard input and returns it.
	 * 
	 * @param prompt
	 *            the prompt printed to the user.
	 * @return the read long floating point number from the standard input.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    static double readDouble(String prompt) throws Exception {
        System.out.print(prompt + ": ");
        return Double.parseDouble(BUFFERED_RDR.readLine());
    }

    /**
	 * Reads multiple long floating point numbers from the standard input and returns it as an
	 * array.
	 * 
	 * @param prompt
	 *            the prompt printed to the user.
	 * @return the array of the read long floating point numbers. The separation is based on the
	 *         white spaces.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    static double[] readDoubles(String prompt) throws Exception {
        System.out.print(prompt + ": ");
        String line = BUFFERED_RDR.readLine();
        if (line == null) throw new NullPointerException();
        if (line.equals("")) return new double[0];
        String[] parts = line.split(" ");
        double[] doubles = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            doubles[i] = Double.parseDouble(parts[i]);
        }
        return doubles;
    }

    /**
	 * This method is used when multiple unit test series is started, other tester objects will be
	 * used in this series.
	 * 
	 * @see #superStartTime
	 * @see #superTestEnd()
	 */
    void superTestStart() {
        superTestStart("ALL");
    }

    /**
	 * This method is used when multiple unit test series is started, only this tester object will
	 * be used.
	 * 
	 * @param name
	 *            the name of the class which contains this tester object.
	 * @see #superStartTime
	 * @see #superTestEnd()
	 */
    void superTestStart(String name) {
        superFailed = 0;
        superPassed = 0;
        superStartTime = System.currentTimeMillis();
        name = " " + name.toUpperCase() + " TESTS STARTED ";
        final int nameLength = name.length();
        final int rptN = (TESTWIDTH - nameLength) / 2;
        System.out.println("\n\n" + Util.charRepeat('*', rptN) + name + Util.charRepeat('*', rptN + (nameLength % 2)));
    }

    /**
	 * This method is used when multiple unit test series is ended, this method creates some statics
	 * about all the tests that have been made, since the starting of the multiple unit test series.
	 * 
	 * @see #superTestStart()
	 * @see #superTestStart(String)
	 */
    void superTestEnd() {
        final int num = superFailed + superPassed;
        final String str = " NUMBER OF TESTS: " + ((num < 10) ? "   " : ((num < 100) ? "  " : ((num < 1000) ? " " : ""))) + num + "   |   PASSED: " + ((superPassed < 10) ? "   " : ((superPassed < 100) ? "  " : ((superPassed < 1000) ? " " : ""))) + superPassed + "   |   " + ((superFailed > 0) ? "_FAILED_" : "FAILED") + ": " + ((superFailed < 10) ? "   " : ((superFailed < 100) ? "  " : ((superFailed < 1000) ? " " : ""))) + superFailed;
        final int length = str.length() + 16;
        String symbCharRepeated = Util.charRepeat('*', TESTWIDTH);
        System.out.println("\n" + symbCharRepeated + "\n" + str + ((length < TESTWIDTH) ? Util.charRepeat(' ', TESTWIDTH - length) : "") + " | TIME: " + ((System.currentTimeMillis() - superStartTime) / 1000D) + " s\n" + symbCharRepeated);
    }

    /**
	 * This method is used when a unit test series is started.
	 * 
	 * @param name
	 *            the name of the class which contains this tester object.
	 * @see #higherStartTime
	 * @see #higherTestEnd()
	 */
    void higherTestStart(String name) {
        testFailed = 0;
        testPassed = 0;
        higherStartTime = System.currentTimeMillis();
        name = " " + name + " Tests Started ";
        final int nameLength = name.length();
        final int rptN = (TESTWIDTH - nameLength) / 2;
        System.out.println("\n" + Util.charRepeat('=', rptN) + name + Util.charRepeat('=', rptN + (nameLength % 2)) + "\n" + Util.charRepeat('-', TESTWIDTH));
    }

    /**
	 * This method is used when a unit test series is ended, this method creates some statics about
	 * all the tests that have been made, since the starting of the unit test series.
	 * 
	 * @see #higherStartTime
	 * @see #higherTestStart(String)
	 */
    void higherTestEnd() {
        final int num = testFailed + testPassed;
        final String str = " Number of tests: " + ((num < 10) ? "   " : ((num < 100) ? "  " : ((num < 1000) ? " " : ""))) + num + "   |   Passed: " + ((testPassed < 10) ? "   " : ((testPassed < 100) ? "  " : ((testPassed < 1000) ? " " : ""))) + testPassed + "   |   " + ((testFailed > 0) ? "FAILED" : "Failed") + ": " + ((testFailed < 10) ? "   " : ((testFailed < 100) ? "  " : ((testFailed < 1000) ? " " : ""))) + testFailed;
        final int length = str.length() + 16;
        System.out.println(Util.charRepeat('-', TESTWIDTH) + "\n" + str + ((length < TESTWIDTH) ? Util.charRepeat(' ', TESTWIDTH - length) : "") + " | Time: " + ((System.currentTimeMillis() - higherStartTime) / 1000D) + " s\n" + Util.charRepeat('=', TESTWIDTH));
    }

    /**
	 * This method is used when a test is started.
	 * 
	 * @param name
	 *            the name of the test.
	 * @see #startTime
	 * @see #passed(String)
	 * @see #failed(String)
	 */
    void testStart(String name) {
        testName = name;
        startTime = System.currentTimeMillis();
    }

    /**
	 * This method is used when a test has failed and the reason is not known. It also ends the
	 * test, that was started by {@link #testStart(String)} method.
	 */
    void failed() {
        failed("");
    }

    /**
	 * This method is used when a test has failed and the reason is known. It also ends the test,
	 * that was started by {@link #testStart(String)} method.
	 * 
	 * @param reason
	 *            the description of the reason of the failure.
	 */
    void failed(String reason) {
        testFailed++;
        superFailed++;
        final int num = testPassed + testFailed;
        final String str = ((num < 10) ? "  " : " ") + num + ". test FAILED (" + testName + ")" + ((reason.length() != 0) ? ": " + reason : "");
        final int length = str.length() + 16;
        System.out.println(str + ((length < TESTWIDTH) ? Util.charRepeat(' ', TESTWIDTH - length) : "") + " | Time: " + ((System.currentTimeMillis() - startTime) / 1000D) + " s");
    }

    /**
	 * This method is used when a test has been successful and the reason is not known. It also ends
	 * the test, that was started by {@link #testStart(String)} method.
	 */
    void passed() {
        passed("");
    }

    /**
	 * This method is used when a test has been successful and the reason is known. It also ends the
	 * test, that was started by {@link #testStart(String)} method.
	 * 
	 * @param reason
	 *            the description of the reason of the success.
	 */
    void passed(String reason) {
        testPassed++;
        superPassed++;
        final int num = testPassed + testFailed;
        final String str = ((num < 10) ? "  " : " ") + num + ". test passed (" + testName + ")" + ((reason.length() != 0) ? ": " + reason : "");
        final int length = str.length() + 16;
        System.out.println(str + ((length < TESTWIDTH) ? Util.charRepeat(' ', TESTWIDTH - length) : "") + " | Time: " + ((System.currentTimeMillis() - startTime) / 1000D) + " s");
    }

    /**
	 * Ends the test, that was started by {@link #testStart(String)} method, and class the
	 * appropriate method depending on the value of given variable. Neither the reason of the
	 * success nor the failure is known.
	 * 
	 * @param ok
	 *            indicates whether the test have been successful.
	 * @see #passed()
	 * @see #failed()
	 */
    void testEnd(boolean ok) {
        testEnd(ok, "", "");
    }

    /**
	 * Ends the test, that was started by {@link #testStart(String)} method, and class the
	 * appropriate method depending on the value of given variable. Both the reason of the success
	 * and the failure is given.
	 * 
	 * @param ok
	 *            indicates whether the test have been successful.
	 * @param reasonPassed
	 *            the description of the reason of the success.
	 * @param reasonFailed
	 *            the description of the reason of the failure.
	 * @see #passed(String)
	 * @see #failed(String)
	 */
    void testEnd(boolean ok, String reasonPassed, String reasonFailed) {
        if (ok) {
            passed(reasonPassed);
        } else {
            failed(reasonFailed);
        }
    }

    /**
	 * Starts and ends a test. The success of the test depends on the equality of the given two
	 * objects.
	 * 
	 * @param name
	 *            the name of the test.
	 * @param o1
	 *            the first object to be compared. Usually this one indicates the required value.
	 * @param o2
	 *            the first object to be compared. Usually this one indicates the actual value.
	 * @see #testStart(String)
	 * @see #passed(String)
	 * @see #failed(String)
	 */
    void testEq(String name, Object o1, Object o2) {
        testStart(name);
        if (o1.equals(o2)) {
            passed("same values");
        } else {
            failed(o1.toString() + " != " + o2.toString());
        }
    }

    /**
	 * Starts and ends a test. The success of the test is indicated by the given variable. Neither
	 * the reason of the success nor the failure is known.
	 * 
	 * @param name
	 *            the name of the test.
	 * @param ok
	 *            indicates whether the test have been successful.
	 * @see #testStart(String)
	 * @see #testEnd(boolean)
	 */
    void test(String name, boolean ok) {
        test(name, ok, "", "");
    }

    /**
	 * Starts and ends a test. The success of the test is indicated by the given variable. Both the
	 * reason of the success and the failure is given.
	 * 
	 * @param name
	 *            the name of the test.
	 * @param ok
	 *            indicates whether the test have been successful.
	 * @param reasonPassed
	 *            the description of the reason of the success.
	 * @param reasonFailed
	 *            the description of the reason of the failure.
	 * @see #testStart(String)
	 * @see #testEnd(boolean, String, String)
	 */
    void test(String name, boolean ok, String reasonPassed, String reasonFailed) {
        testStart(name);
        testEnd(ok, reasonPassed, reasonFailed);
    }

    /**
	 * This method should contain the menu specific command based on the user's choice. Hence, it
	 * should be overridden at the instantiation.
	 * 
	 * @param choice
	 *            an integer specifying the user's menu choice.
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    void menu(@SuppressWarnings("unused") int choice) throws Exception {
        System.err.println("There is no menu implemented!");
    }

    /** Prints out an error message if an invalid menu option has been chosen. */
    void invalidMenuOption() {
        System.out.println("Invalid Menu Option!");
    }

    /**
	 * This method should contain the unit tests of the class, thus it should be overridden at the
	 * instantiation.
	 * 
	 * @throws Exception
	 *             if any error occurs. This exception will be handled in the {@link #start()}
	 *             method.
	 */
    void runUnitTests() throws Exception {
        System.err.println("No unit tests have been implemented!");
    }

    boolean isAutoSandbox() {
        return false;
    }

    void sandbox() throws Throwable {
        System.out.println("You can put your testing code here...");
    }

    public static void mainer(final String[] args, final Tester tester) {
        if (Util.IS_DEVELOPER_BUILD) {
            (new Thread() {

                public void run() {
                    KeyboardHero.startApp();
                    KeyboardHero.handleArgs(args, false);
                }
            }).start();
            (new Thread() {

                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    if (tester.isAutoSandbox()) {
                        try {
                            tester.sandbox();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } catch (Throwable e) {
                            System.err.println("========== ERROR START ==========");
                            e.printStackTrace();
                            System.err.println("==========  ERROR END  ==========");
                        }
                    }
                    tester.start(1);
                }
            }).start();
        } else {
            System.out.println(Util.NOT_DEVELOPER_STRING);
        }
    }

    /**
	 * Creates a string containing the most important information about the Util class. This method
	 * is focused on the runtime or platform specific fields. It is used only for debugging and
	 * testing purposes.
	 * 
	 * @return the created string.
	 */
    private static String getString() {
        return "Tester()";
    }

    /**
	 * This method serves security purposes. Provides an integrity string that will be checked by
	 * the {@link Connection#integrityCheck()} method; thus the application can only be altered if
	 * the source is known. Every class in the {@link keyboardhero} package has an integrity string.
	 * 
	 * @return the string of this class used for integrity checking.
	 */
    static String getIntegrityString() {
        return "8ł2+*43-+-5zSDFasd";
    }

    /**
	 * The tester object of this class. It provides a debugging menu and unit tests for this class.
	 * Its only purpose is debugging or testing.
	 */
    static final Tester TESTER = new Tester("Tester", new String[] { "getString()" }) {

        void menu(int choice) throws Exception {
            switch(choice) {
                case 5:
                    System.out.println(getString());
                    break;
                default:
                    baseMenu(choice);
                    break;
            }
        }

        void runUnitTests() throws Exception {
            higherTestStart("TargetsHelp");
            testEq("getIntegrityString()", "8ł2+*43-+-5zSDFasd", Tester.getIntegrityString());
            higherTestEnd();
        }
    };

    /**
	 * Starts the class's developing menu. If this build is a developer's one it starts the
	 * application in a normal way with the exception that it starts the debugging tool for this
	 * class as well; otherwise exits with an error message.
	 * 
	 * @param args
	 *            the arguments given to the program.
	 * @see KeyboardHero#startApp()
	 */
    public static void main(String[] args) {
        Tester.mainer(args, TESTER);
    }
}
