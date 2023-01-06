package smartcat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a file node in the dependency graph.
 */
public class FileNode {
    /**
     * The name of the file.
     */
    private String pathname;
    /**
     * List of files that this file depends on. These files must be processed before this file.
     */
    public List<FileNode> requiredFileNodes = new ArrayList<>();

    public FileNode(String pathname) {
        this.pathname = pathname;
    }

    /**
     * Returns the pathname of the file.
     */
    public String getFilePath() {
        return pathname;
    }

    /**
     * Adds a file node to the list of required file nodes.
     */
    public void addRequiredFileNode(FileNode node) {
        requiredFileNodes.add(node);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileNode fileNode = (FileNode) o;
        return pathname.equals(fileNode.pathname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathname);
    }
}
