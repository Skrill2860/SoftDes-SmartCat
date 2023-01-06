package smartcat;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Concatenates files in the specified directory and its subdirectories.
 * The concatenation order is determined by the dependencies between files.
 * The dependencies are specified in the files themselves.
 * To specify a dependency on other file, in any place of the file there must be a line of format [require 'path']
 * (no square brackets needed).
 * For establishing dependencies between multiple files, there should be a line for each file.
 * Example of dependency line: require 'folder1/file1.txt'
 */
public class SmartCat {
    /**
     * Map with all fileNodes. Key is the file path string. Using of absolute path is recommended.
     */
    private HashMap<String, FileNode> nodeMap = new HashMap<>();
    /**
     * Not visited fileNodes. Used to iterate over the graph. Solves problem of disconnected graph.
     */
    private HashSet<FileNode> notVisited = new HashSet<>();
    /**
     * Nodes that are required by at least one other node. Nodes not from this set should be processed first.
     */
    private HashSet<FileNode> nodesRequiredForOthers = new HashSet<>();
    /**
     * Final list with nodes sorted in order in which they should be processed.
     */
    private ArrayList<FileNode> sortedList = new ArrayList<>();
    /**
     * The root folder.
     */
    private Path rootPath;

    /**
     * Concatenates all files in the root folder and its subfolders.
     * @param directoryPath Path to the root folder.
     */
    public static void concatenateFiles(String directoryPath) {
        SmartCat smartCat = new SmartCat(directoryPath);
        smartCat.concatenateFiles();
    }

    /**
     * Constructor.
     * @param directoryPath Path to the root folder.
     */
    public SmartCat(String directoryPath) {
        // check if path was entered
        if (directoryPath.isEmpty()) {
            System.out.println("Path is empty");
            return;
        }
        // check if path is valid directory
        File rootFolder = new File(directoryPath);
        if (!rootFolder.isDirectory()) {
            System.out.println("Path is not a directory");
            return;
        }

        rootPath = Paths.get(directoryPath);
    }

    /**
     * Concatenates all files in the root folder and its subfolders.
     */
    public void concatenateFiles() {
        mapAllFilesInDirectory(rootPath.toAbsolutePath().toString());

        // initialize required filenodes
        for (FileNode node : nodeMap.values()) {
            initializeNodeRequiredFiles(node);
        }

        // check if there are any cycles
        if (hasCycleDependencies()) {
            System.out.println("There are cycle dependencies. Can't concatenate files.");
            return;
        }

        // create sorted list
        buildSortedFileList();

        // concatenate files in order of sorted list
        makeConcatenatedFile();

        System.out.println("Concatenation finished");
        // concatenate files in order of sorted list
        makeConcatenatedFile();

        System.out.println("Concatenation finished");
    }

    /**
     * Adds all files in the directory and its subdirectories to nodeMap.
     * @param directoryPath Path to the directory.
     */
    private void mapAllFilesInDirectory(String directoryPath) {
        if (directoryPath == null || directoryPath.isEmpty()) {
            System.out.println("No path was specified");
            return;
        }
        File rootFolder = new File(directoryPath);
        if (!rootFolder.isDirectory()) {
            System.out.println("Path is not a directory");
            return;
        }
        try {
            Files.walkFileTree(Paths.get(directoryPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!Files.isDirectory(file)) {
                        nodeMap.put(file.toAbsolutePath().toString(), new FileNode(file.toAbsolutePath().toString()));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.out.println("No files were found");
        }
    }

    /**
     * Initializes list of required fileNodes for the node by reading file contents.
     * Lines of format [require 'path'] are processed (no square brackets needed).
     * @param node Node to initialize.
     */
    private void initializeNodeRequiredFiles(FileNode node) {
        // open file
        File file = new File(node.getFilePath());
        // for each line in file
        // if line is in format of require 'path/to/file' then add filenode with pathname path/to/file to the list of required files
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("require")) {
                    String[] parts = line.split(" ");
                    if (parts.length == 2) {
                        String path = Paths.get(rootPath.toString(), parts[1].substring(1, parts[1].length() - 1)).
                                toAbsolutePath().toString();
                        if (nodeMap.containsKey(path)) {
                            node.addRequiredFileNode(nodeMap.get(path));
                            nodesRequiredForOthers.add(nodeMap.get(path));
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error while reading file");
        }
    }

    /**
     * Checks if there are any cycles in the graph.
     * @return True if there are cycles, false otherwise.
     */
    private boolean hasCycleDependencies() {
        notVisited = new HashSet<>(nodeMap.values());
        while (notVisited.size() > 0) {
            FileNode node = notVisited.iterator().next();
            HashSet<FileNode> visited = new HashSet<>();
            visited.add(node);
            notVisited.remove(node);
            for (FileNode requiredNode : node.requiredFileNodes) {
                if (hasCycleDependencies(requiredNode, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper for cycle dependencies check method.
     * Checks if there are any cycles in the subgraph that starts from the node.
     * @param node Node to start from.
     * @param visited Set of visited nodes.
     * @return True if there are cycles, false otherwise.
     */
    private boolean hasCycleDependencies(FileNode node, HashSet<FileNode> visited) {
        if (visited.contains(node)) {
            return true;
        }
        visited.add(node);
        notVisited.remove(node);
        for (FileNode requiredNode : node.requiredFileNodes) {
            if (hasCycleDependencies(requiredNode, visited)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds sorted list of nodes.
     * Nodes are sorted in order in which they should be processed.
     */
    private void buildSortedFileList() {
        notVisited = new HashSet<>(nodeMap.values());
        // get nodes that are not in nodesRequiredForOthers, we can start building list from them
        HashSet<FileNode> mostDependentNodes = new HashSet<>(notVisited);
        mostDependentNodes.removeAll(nodesRequiredForOthers);
        // start recursive building from most dependent nodes
        for (FileNode node : mostDependentNodes) {
            buildSortedFileList(node);
        }
    }

    /**
     * Helper for buildSortedFileList method.
     * Recursively builds sorted list of nodes.
     * @param node Node to start from.
     */
    private void buildSortedFileList(FileNode node) {
        // if visited, then subtree is already built
        if (!notVisited.contains(node)) {
            return;
        }
        notVisited.remove(node);
        for (FileNode requiredNode : node.requiredFileNodes) {
            buildSortedFileList(requiredNode);
        }
        sortedList.add(node);
    }

    /**
     * Concatenates files in order of sorted list.
     */
    private void makeConcatenatedFile() {
        String resultFilePath = Paths.get(rootPath.toString(), "concatenated.txt").toAbsolutePath().toString();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(resultFilePath))) {
            for (FileNode node : sortedList) {
                try (BufferedReader br = new BufferedReader(new FileReader(node.getFilePath()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        bw.write(line);
                        bw.newLine();
                    }
                } catch (IOException e) {
                    System.out.println("Error while reading file");
                }
            }
        } catch (IOException e) {
            System.out.println("Error while writing file");
        }
        System.out.println("Concatenated file is located at " + resultFilePath);
    }
}
