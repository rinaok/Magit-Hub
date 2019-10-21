package logic.manager;
import java.util.ArrayList;
import java.util.List;

public class WCFileNode {
    private String text;
    private String sha1;
    private List<WCFileNode> nodes;

    public WCFileNode(String text, String sha1){
        this.text = text;
        this.sha1 = sha1;
        nodes = new ArrayList<>();
    }

    public void addNode(WCFileNode node){
        nodes.add(node);
    }

    public List<WCFileNode> getNodes(){
        return nodes;
    }
}

