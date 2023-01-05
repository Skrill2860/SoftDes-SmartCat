import smartcat.SmartCat;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        String directoryPathString = "";
        boolean isDirectoryPathStringSet = false;
        while (!isDirectoryPathStringSet) {
            // ask user to enter path to the root folder
            System.out.println("Enter path to the root folder:");
            Scanner scanner = new Scanner(System.in);
            directoryPathString = scanner.nextLine();
            // check if path was entered
            if (directoryPathString.isEmpty()) {
                System.out.println("Path is empty");
                continue;
            }
            // check if path is valid directory
            File rootFolder = new File(directoryPathString);
            if (!rootFolder.isDirectory()) {
                System.out.println("Path is not a directory");
                continue;
            }
            isDirectoryPathStringSet = true;
        }

        SmartCat smartCat = new SmartCat(directoryPathString);
        smartCat.concatenateFiles();
    }
}