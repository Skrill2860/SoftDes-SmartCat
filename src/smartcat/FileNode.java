package smartcat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileNode {
    private String pathname;
    public List<FileNode> requiredFileNodes = new ArrayList<>();

    public FileNode(String pathname) {
        this.pathname = pathname;
    }

    public String getFilePath() {
        return pathname;
    }

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
