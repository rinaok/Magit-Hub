package logic.manager;
import java.util.ArrayList;
import java.util.List;

public class WCFileNode {
    private String text;
    private String filePath;
    private FileStatus status;
    private List<WCFileNode> nodes;

    public WCFileNode(String text, String filePath){
        this.text = text;
        this.filePath = filePath;
        status = FileStatus.NO_CHANGE;
        nodes = new ArrayList<>();
    }

    public void addNode(WCFileNode node){
        nodes.add(node);
    }

    public List<WCFileNode> getNodes(){
        return nodes;
    }

    public void setFileStatus(FileStatus fileStatus){
        this.status = fileStatus;
    }

    public FileStatus getStatus(){
        return status;
    }

    public String getFilePath(){
        return filePath;
    }
}

