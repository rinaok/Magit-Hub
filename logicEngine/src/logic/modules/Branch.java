package logic.modules;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Utils;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Branch implements GitFile{
    private String name;
    private Commit head;
    private boolean isActive;
    private boolean isRemote;
    private boolean tracking;
    private String trackingAfter;
    private boolean isHead;
    private Date creationDate;

    public Branch(){
    }

    public Branch(Commit head, String name){
        this.name = name;
        this.head = head;
        isRemote = false;
        isHead = false;
        try {
            findCreationTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void findCreationTime() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyy-HH:mm:ss:sss");
        //if(head == null || head.getCreationDate() == null)
        creationDate = dateFormat.parse(Utils.getTime());
//        else
//            creationDate = dateFormat.parse(head.getCreationDate());
    }


    public Branch(Commit head, String name, boolean isRemote){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyy-HH:mm:ss:sss");
        this.name = name;
        this.head = head;
        this.isRemote = isRemote;
        this.isHead = false;
        try {
            findCreationTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getName(){
        return name;
    }

    public Commit getHead(){
        return head;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setHead(Commit head){
        this.head = head;
    }

    @Override
    public String createHashCode() {
        return name;
    }

    @Override
    public String createGitFileText() {
        return head.createHashCode();
    }

    @Override
    public String getContent() {
        return head.createHashCode();
    }

    public void createFile(String path) throws IOException, FailedToCreateRepositoryException {
        Utils.createTxtFile(path, name, head.createHashCode());
    }

    public String parseBranchFile(File file) throws IOException {
        String output = Utils.readFile(file);
        return output;
    }

    public void setTracking(String trackingAfter){
        this.trackingAfter = trackingAfter;
        tracking = true;
    }

    public void remoteTracking(){
        trackingAfter = "";
        tracking = false;
    }

    public boolean getTracking(){
        return tracking;
    }

    public boolean getIsRemote(){
        return isRemote;
    }

    public void setIsRemote() { isRemote = true; }

    public void setNotRemote() { isRemote = false; }

    public void setIsHead(boolean isHead){
        this.isHead = isHead;
    }

    public boolean getIsHead(){
        return isHead;
    }

    public Date getCreationDate(){
        return creationDate;
    }
}
