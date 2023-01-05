package smartcat;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SmartCat {
    private HashMap<String, FileNode> nodeMap = new HashMap<>();
    private HashSet<FileNode> notVisited = new HashSet<>();
    private HashSet<FileNode> nodesRequiredForOthers = new HashSet<>();
    private ArrayList<FileNode> sortedList = new ArrayList<>();
    private Path rootPath;

    public static void concatenateFiles(String directoryPath) {
        SmartCat smartCat = new SmartCat(directoryPath);
        smartCat.concatenateFiles();
    }

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
