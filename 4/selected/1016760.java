package be.yildiz.client.ressource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Check the resources integrity thanks to the list used by Ogre.
 * @author Van Den Borre Grégory
 */
public final class RessourceChecker {

    /**
     * Configuration file containing the path to the resources.
     */
    private static File resources = new File("resources.cfg");

    /**
     * Simple constructor, private to prevent instantiation.
     */
    private RessourceChecker() {
    }

    /**
     * Parse the configuration file and check if the resources exist.
     * @throws FileNotFoundException
     *             Exception if the configuration file is not found.
     */
    public static void check() throws FileNotFoundException {
        Scanner sc = new Scanner(new FileInputStream(resources));
        System.out.println(sc.nextLine());
    }
}
