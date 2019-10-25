package logic.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WCFileNode {
    private final static String FILE_ICON = "glyphicon glyphicon-file";
    private final static String FOLDER_ICON = "glyphicon glyphicon-folder-open";
    private String text;
    private String filePath;
    private List<String> tags;
    private String nodeIcon;

    private List<WCFileNode> nodes;

    public WCFileNode(String text, String filePath){
        this.text = text;
        this.filePath = filePath;
        nodes = new ArrayList<>();
        tags = new ArrayList<>();
        if(new File(filePath).isFile())
            nodeIcon = FILE_ICON;
        else
            nodeIcon = FOLDER_ICON;
    }

    public void addNode(WCFileNode node){
        nodes.add(node);
    }

    public List<WCFileNode> getNodes(){
        return nodes;
    }

    public void setFileStatus(PRStatus fileStatus){
        tags.add(fileStatus.toString());
    }

    public String getFilePath(){
        return filePath;
    }
}

