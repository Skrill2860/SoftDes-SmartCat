import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class Main {

    /*
    * Имеется корневая папка. В этой папке могут находиться текстовые файлы, а также
        другие папки. В других папках также могут находиться текстовые файлы и папки
        (уровень вложенности может оказаться любым).
        В каждом файле может быть ни одной, одна или несколько директив формата:
        require ‘<путь к другому файлу от корневого каталога>’
        Директива означает, что текущий файл зависит от другого указанного файла.
        Необходимо выявить все зависимости между файлами, построить сортированный
        список, для которого выполняется условие: если файл А, зависит от файла В, то файл
        А находится ниже файла В в списке.
        Осуществить конкатенацию файлов в соответствии со списком. Если такой список
        построить невозможно (существует циклическая зависимость), программа должна
        вывести соответствующее сообщение.
    *
    *
    * Пример
        Дана структура файлов и каталогов:
    ● Каталог “Folder 1”
      ○ Файл “File 1-1”. Содержимое:
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse id enim euismod erat
        elementum cursus. In hac habitasse platea dictumst. Etiam vitae tortor ipsum. Morbi massa
        augue, lacinia sed nisl id, congue eleifend lorem.
        require ‘Folder 2/File 2-1’
        Praesent feugiat egestas sem, id luctus lectus dignissim ac. Donec elementum rhoncus
        quam, vitae viverra massa euismod a. Morbi dictum sapien sed porta tristique. Donec varius
        convallis quam in fringilla.
    ● Каталог “Folder 2”
      ○ Файл “File 2-1”. Содержимое:
        Phasellus eget tellus ac risus iaculis feugiat nec in eros. Aenean in luctus ante. In lacinia
        lectus tempus, rutrum ipsum quis, gravida nunc. Fusce tempor eleifend libero at pharetra.
        Nulla lacinia ante ac felis malesuada auctor. Vestibulum eget congue sapien, ac euismod
        elit. Fusce nisl ante, consequat et imperdiet vel, semper et neque.
      ○ Файл “File 2-2”. Содержимое:
        require ‘Folder 1/File 1-1’
        require ‘Folder 2/File 2-1’
        In pretium dictum lacinia. In rutrum, neque a dignissim maximus, dolor mi pretium ante, nec
        volutpat justo dolor non nulla. Vivamus nec suscipit nisl, ornare luctus erat. Aliquam eget est
        orci. Proin orci urna, elementum a nunc ac, fermentum varius eros. Mauris id massa elit.
        Для указанной структуры каталогов и файлов должен быть построен список:
        Folder 2/File 2-1
        Folder 1/File 1-1
        Folder 2/File 2-2
    * */

    private static HashMap<String, FileNode> nodeMap = new HashMap<>();
    private static HashSet<FileNode> notVisited = new HashSet<>();
    private static ArrayList<FileNode> sortedList = new ArrayList<>();

    public static void main(String[] args) {
        String directoryPath = "";
        // ask user to enter path to the root folder
        System.out.println("Enter path to the root folder:");
        Scanner scanner = new Scanner(System.in);
        directoryPath = scanner.nextLine();
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

        mapAllFilesInDirectory(directoryPath);

        // initialize required filenodes
        for (FileNode node : nodeMap.values()) {
            initializeNodeRequiredFiles(node);
        }

        // check if there are any cycles
        if (hasCycleDependencies()) {
            System.out.println("There are cycle dependencies. Can't concatenate files.");
            return;
        }

        // create list of files
        // add all files from root folder to the list
        List<File> files = new ArrayList<>();
        try {
            files = new ArrayList<>(Arrays.asList(Objects.requireNonNull(rootFolder.listFiles())));
        } catch (NullPointerException e) {
            System.out.println("No files in the directory");
            return;
        }
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            // if file is directory, add all files from this directory to the list
            if (file.isDirectory()) {
                files.addAll(Arrays.asList(Objects.requireNonNull(file.listFiles())));
            }
        }
        String resultFilePath = directoryPath + File.separator + "concatenated.txt";
        File file = new File(resultFilePath);
        List<String> list = new ArrayList<>();
        list.add(file.getName());
        list = getFiles(file, list);
        System.out.println(list);
    }

    private static void mapAllFilesInDirectory(String directoryPath) {
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

    private static void initializeNodeRequiredFiles(FileNode node) {
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
                        String path = Paths.get(parts[1].substring(1, parts[1].length() - 1)).toAbsolutePath().toString();
                        if (nodeMap.containsKey(path)) {
                            node.addRequiredFileNode(nodeMap.get(path));
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error while reading file");
        }
    }

    private static boolean hasCycleDependencies() {
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
        return true;
    }

    private static boolean hasCycleDependencies(FileNode node, HashSet<FileNode> visited) {
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

    private void buildFileList() {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                buildFileList(f, list);
            }
        } else {
            list.add(file.getName());
        }
    }
}