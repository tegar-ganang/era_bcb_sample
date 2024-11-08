import java.io.*;
import java.util.*;

class Cartesian {

    int x, y;
}

/**
  *<p>This is the main class for the Gnubert distributed evolution project.</p>
  *<p>Gnubert will harness the unused processor cycles from computers across the
  *internet to solve genetic algorithm and genetic programming based problems.
  *Unlike other distributed computing programs Gnubert is not written to only
  *work on one problem, defined by it's author but rather it will allow it's
  *users to submit their own problems to the network in a peer to peer fassion.</p>
  *<p>This class contains the actual main method which performs the actual
  *problem solving and also starts the server and client threads which allow
  *communication.</p>
  */
public class Gnubert {

    public static double worstCase;

    static Cartesian coords = new Cartesian();

    static int xSize, ySize;

    static final int minimumStringSize = 15;

    public static int maze[][];

    static final int maximumStringStartingSize = 35;

    static final int maximumSringSize = 1000;

    public static boolean isSolutionInside = false;

    static GnubertClient clientThread;

    /**
    *<p>This is the main method of the Gnubert distributed evolution project.</p>
    *<p>This is probably the most poorly written part of the whole project but
    *most of it will probably go away before version 1.0 anyway.
    *most of the "magic numbers" will be replaced by values stored in a
    *configuration file.  The array of command strings will be replaced by an
    *array of tree structures. Also, instead of opening the maze file and getting
    *a maze a method will be called from a dynamically loaded class which will
    *be specific to the type of problem being solved and will read a file which
    *defines the problem being solved. <p>
    *When a problem is solved the program will send the solution back to the author
    *of the problem and start on a different problem.</p>
    */
    public static void main(String args[]) {
        int numberCommandStrings = 10000;
        GnubertProperties propertyList = new GnubertProperties();
        GnubertStatus currentStatus = new GnubertStatus();
        String commandStrings[] = new String[numberCommandStrings];
        double fitnessArray[] = new double[numberCommandStrings];
        int counter, index, bucket, tournamentSize, chance;
        int tournamentIndexes[] = null;
        boolean accepted = false;
        Random rand = new Random();
        double temp;
        int sizeImmigrantQueue = 10;
        LinkedList immigrantQueue = new LinkedList();
        initializeStrings(commandStrings, numberCommandStrings);
        int countX, countY;
        FileInputStream filein = null;
        BufferedReader reader = null;
        try {
            filein = new FileInputStream("maze");
        } catch (Exception e) {
            System.out.println("Something is wrong with your maze file!!!");
            System.out.println(e);
            System.exit(1);
        }
        reader = new BufferedReader(new InputStreamReader(filein));
        try {
            try {
                xSize = Integer.valueOf(reader.readLine()).intValue();
                ySize = Integer.valueOf(reader.readLine()).intValue();
                maze = new int[xSize][ySize];
                for (countY = 0; countY < ySize; countY++) for (countX = 0; countX < xSize; countX++) maze[countX][countY] = Integer.valueOf(reader.readLine()).intValue();
            } catch (NumberFormatException e) {
                System.out.println("Something is wrong with your maze file!!!");
                System.out.println(e);
                System.exit(1);
            }
        } catch (IOException e) {
            System.out.println("Your maze file is too short or the dimensions are wrong!");
            System.out.println(e);
            System.exit(1);
        }
        worstCase = Math.sqrt(Math.pow(xSize - 1, 2) + Math.pow(ySize - 1, 2));
        for (countX = 0; countX < xSize + 2; countX++) System.out.print("#");
        System.out.println();
        for (countY = 0; countY < ySize; countY++) {
            System.out.print("|");
            for (countX = 0; countX < xSize; countX++) if (maze[countX][countY] == 1) {
                System.out.print("#");
            } else System.out.print(" ");
            System.out.println("|");
        }
        for (countX = 0; countX < xSize + 2; countX++) System.out.print("#");
        System.out.println();
        HostCatcher hostCatcher = new HostCatcher(propertyList, currentStatus);
        hostCatcher.start();
        System.out.println("HostCatcher started!");
        clientThread = new GnubertClient(hostCatcher, propertyList, currentStatus);
        clientThread.start();
        System.out.println("Client Started!");
        GnubertServer serverThread = new GnubertServer(commandStrings, numberCommandStrings, propertyList, hostCatcher, currentStatus);
        serverThread.start();
        System.out.println("Server Started!");
        for (counter = 0; counter < numberCommandStrings; counter++) {
            fitnessArray[counter] = fitness(commandStrings[counter]);
            if (fitnessArray[counter] == 0) isSolutionInside = true;
        }
        if (isSolutionInside) {
            System.out.println("Solution Inside");
            for (counter = 0; counter < numberCommandStrings; counter++) System.out.println(fitnessArray[counter]);
        } else System.out.println("Fitness level is equal to the percentage of the distance from the starting point (upper Left) to the ending point (Lower Right) that the string reaches.");
        int generationCounter = 0;
        while (!isSolutionInside) {
            tournamentSize = (int) (rand.nextDouble() * (numberCommandStrings - 2)) + 2;
            tournamentIndexes = new int[tournamentSize];
            for (counter = 0; counter < tournamentSize; counter++) {
                while (!accepted) {
                    accepted = true;
                    tournamentIndexes[counter] = (int) (rand.nextDouble() * numberCommandStrings);
                    for (index = 0; index < counter; index++) {
                        if (tournamentIndexes[counter] == tournamentIndexes[index]) accepted = false;
                        break;
                    }
                }
                accepted = false;
            }
            accepted = false;
            index = tournamentSize;
            while (!accepted) {
                accepted = true;
                index--;
                for (counter = 0; counter < index; counter++) {
                    if (fitnessArray[tournamentIndexes[counter]] > fitnessArray[tournamentIndexes[counter + 1]]) {
                        bucket = tournamentIndexes[counter];
                        tournamentIndexes[counter] = tournamentIndexes[counter + 1];
                        tournamentIndexes[counter + 1] = bucket;
                        accepted = false;
                    }
                }
            }
            for (counter = tournamentSize / 2; counter < tournamentSize; counter++) {
                chance = (int) (rand.nextDouble() * 100);
                if (chance < 10) {
                    index = (int) (rand.nextDouble() * tournamentSize / 2);
                    commandStrings[tournamentIndexes[counter]] = duplicateSubstring(commandStrings[tournamentIndexes[index]]);
                    fitnessArray[tournamentIndexes[counter]] = fitness(commandStrings[tournamentIndexes[counter]]);
                    if (fitnessArray[tournamentIndexes[counter]] == 0) isSolutionInside = true;
                } else if (chance < 20) {
                    index = (int) (rand.nextDouble() * tournamentSize / 2);
                    commandStrings[tournamentIndexes[counter]] = deleteSubstring(commandStrings[tournamentIndexes[index]]);
                    fitnessArray[tournamentIndexes[counter]] = fitness(commandStrings[tournamentIndexes[counter]]);
                    if (fitnessArray[tournamentIndexes[counter]] == 0) isSolutionInside = true;
                } else if (chance < 70) {
                    index = (int) (rand.nextDouble() * tournamentSize / 2);
                    commandStrings[tournamentIndexes[counter]] = mutate(commandStrings[tournamentIndexes[index]]);
                    fitnessArray[tournamentIndexes[counter]] = fitness(commandStrings[tournamentIndexes[counter]]);
                    if (fitnessArray[tournamentIndexes[counter]] == 0) isSolutionInside = true;
                } else if (chance < 90) {
                    index = (int) (rand.nextDouble() * tournamentSize / 2);
                    commandStrings[tournamentIndexes[counter]] = recombination(commandStrings[tournamentIndexes[counter]], commandStrings[tournamentIndexes[index]]);
                    fitnessArray[tournamentIndexes[counter]] = fitness(commandStrings[tournamentIndexes[counter]]);
                    if (fitnessArray[tournamentIndexes[counter]] == 0) isSolutionInside = true;
                } else if (chance < 95) {
                    String immigrant = null;
                    immigrant = clientThread.getImmigrant();
                    if (immigrant != null) {
                        commandStrings[tournamentIndexes[counter]] = immigrant;
                        fitnessArray[tournamentIndexes[counter]] = fitness(commandStrings[tournamentIndexes[counter]]);
                        if (fitnessArray[tournamentIndexes[counter]] == 0) isSolutionInside = true;
                    }
                } else if (chance < 100) {
                    index = (int) (rand.nextDouble() * tournamentSize / 2);
                    commandStrings[tournamentIndexes[counter]] = commandStrings[tournamentIndexes[index]];
                    fitnessArray[tournamentIndexes[counter]] = fitness(commandStrings[tournamentIndexes[counter]]);
                }
            }
            if (isSolutionInside) {
                System.out.println("\nOne or more solutions found in generation number " + generationCounter + "!");
                for (counter = 0; counter < numberCommandStrings; counter++) {
                    if (fitnessArray[counter] == 0) {
                        System.out.println("Solution:");
                        System.out.println(commandStrings[counter]);
                    }
                }
            } else {
                temp = 0;
                for (counter = 0; counter < numberCommandStrings; counter++) temp += fitnessArray[counter];
                System.out.print("\rAverage fitness level from generation " + generationCounter + " was " + (1 - temp / numberCommandStrings) * 100 + "% ");
                generationCounter++;
            }
        }
    }

    /**Function to initialize strings to random sizes and contents.
    *Will need to be modified to initialize strings which are in a language
    *defined in a dynamically loaded class describing the type of problem
    *being solved.
    */
    public static void initializeStrings(String commandStrings[], int numberCommandStrings) {
        int size, stringCounter, charCounter;
        Random rand = new Random();
        int num;
        for (stringCounter = 0; stringCounter < numberCommandStrings; stringCounter++) {
            commandStrings[stringCounter] = "";
            size = (int) (rand.nextDouble() * (maximumStringStartingSize - minimumStringSize)) + minimumStringSize;
            for (charCounter = 0; charCounter < size; charCounter++) commandStrings[stringCounter] += (int) (rand.nextDouble() * 4) / 1;
        }
    }

    /**Creates a new string by combining portions of two old ones.
    *May need to be modified when problems other than mazes are being solved.
    */
    public static String recombination(String parent1, String parent2) {
        int parent1Cut = 0;
        int parent2Cut = 0;
        Random rand = new Random();
        boolean accept = false;
        while (!accept) {
            parent1Cut = (int) (rand.nextDouble() * (parent1.length() - 1));
            parent2Cut = (int) (rand.nextDouble() * (parent2.length() - 1));
            if ((parent1Cut + parent2.length() - parent2Cut) <= maximumSringSize) accept = true;
        }
        return (parent1.substring(0, parent1Cut) + parent2.substring(parent2Cut, (parent2.length())));
    }

    /**Mutates one command within a string.
    *Will need to be updated to fit the languages of dynamically chosen types
    *of problems.
    */
    public static String mutate(String x_string) {
        int index;
        Random rand = new Random();
        index = (int) (rand.nextDouble() * x_string.length());
        if (index == x_string.length()) return (x_string.substring(0, index - 1) + (int) (rand.nextDouble() * 4)); else if (index == 0) return ((int) (rand.nextDouble() * 4) + (x_string.substring(1))); else return (x_string.substring(0, index - 1) + (int) (rand.nextDouble() * 4) + x_string.substring(index + 1));
    }

    /**Creates a new string using a substring from another.
    *Will need to be altered once not using strings.
    */
    public static String duplicateSubstring(String x_string) {
        int begining;
        int size = 0;
        Random rand = new Random();
        if (x_string.length() >= maximumSringSize) return x_string;
        begining = (int) (rand.nextDouble() * x_string.length());
        while (x_string.length() > maximumSringSize) size = (int) (rand.nextDouble() * (x_string.length() - begining));
        return x_string + x_string.substring(begining, begining + size);
    }

    /**Creates a new string by taking an old string and removing part of it.
    *This may have to be modified for the new data structure.
    */
    public static String deleteSubstring(String x_string) {
        int size;
        int start;
        Random rand = new Random();
        size = (int) (rand.nextDouble() * (x_string.length() - 2) + 1);
        start = (int) (rand.nextDouble() * (x_string.length() - size));
        return (x_string.substring(0, start) + x_string.substring(start + size));
    }

    /**This function is used by the fitness function to represent one move within
    *a string.
    *It will be moved along with the fitness function to a dynamically loaded class
    *which will be used only for solving mazes.
    */
    public static void move(char move) {
        switch(move) {
            case '2':
                if ((coords.y < (ySize - 1)) && (maze[coords.x][coords.y + 1] == 0)) coords.y++;
                break;
            case '1':
                if ((coords.x < (xSize - 1)) && (maze[coords.x + 1][coords.y] == 0)) coords.x++;
                break;
            case '0':
                if ((coords.y > 0) && (maze[coords.x][coords.y - 1] == 0)) coords.y--;
                break;
            case '3':
                if ((coords.x > 0) && (maze[coords.x - 1][coords.y] == 0)) coords.x--;
                break;
        }
    }

    /**<p>This is the fitness function for solving mazes.</p>
    *<p>It will eventually be moved into a dynamically loaded class.  Other
    *classes may be loaded in it's place to solve problems other than mazes.</p>
    *<p>It is assumed that there is a minimum number of moves in which a maze
    *may be solved, therefore in order to save time strings smaller than or
    *equal to minimumStringSize are only evaluated in their last position.
    *Strings larger than minimumStringSize are evaluated at each move after
    *move #minimumStringSize. This way a string which passes through the
    *solution can be identified and truncated.</p>
    */
    public static double fitness(String command_String) {
        double fitness = 1;
        int counter = 0;
        coords.x = 0;
        coords.y = 0;
        if (command_String.length() < minimumStringSize) {
            for (counter = 0; counter < command_String.length(); counter++) move(command_String.charAt(counter));
            fitness = (Math.sqrt(Math.pow(xSize - 1 - coords.x, 2) + Math.pow(ySize - 1 - coords.y, 2)) / worstCase);
        } else {
            for (counter = 0; counter < minimumStringSize; counter++) move(command_String.charAt(counter));
            fitness = (Math.sqrt(Math.pow(xSize - 1 - coords.x, 2) + Math.pow(ySize - 1 - coords.y, 2)) / worstCase);
            for (; counter < command_String.length(); counter++) {
                move(command_String.charAt(counter));
                fitness = Math.min(fitness, (Math.sqrt(Math.pow(xSize - 1 - coords.x, 2) + Math.pow(ySize - 1 - coords.y, 2)) / worstCase));
            }
        }
        return fitness;
    }
}
