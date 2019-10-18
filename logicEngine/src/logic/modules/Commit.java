package logic.modules;
import logic.manager.Utils;
import org.apache.commons.codec.digest.DigestUtils;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Commit implements GitFile {
    private String rootSha1;
    private String previousCommit;
    private String secondPreviousCommit;
    private String message;
    private String creationDate;
    private String createdBy;
    private final String DELIMITER = ",";

    public Commit(){
        previousCommit = "";
        secondPreviousCommit = "";
    }

    public Commit(String rootSha1, Branch currentBranch, String message, String creationDate, String createdBy, boolean firstCommit){
        this.rootSha1 = rootSha1;
        if(!firstCommit) {
            this.previousCommit = currentBranch.getHead().createHashCode();
        }
        this.message = message;
        this.creationDate = creationDate;
        this.createdBy = createdBy;
        this.secondPreviousCommit = "";
    }

    public Commit(String rootSha1, String message, String creationDate, String createdBy, String previousCommit){
        this.rootSha1 = rootSha1;
        this.message = message;
        this.creationDate = creationDate;
        this.createdBy = createdBy;
        this.previousCommit = previousCommit;
        this.secondPreviousCommit = "";
    }

    public Commit(String rootSha1, String message, String creationDate, String createdBy, String previousCommit, String secondPreviousCommit){
        this.rootSha1 = rootSha1;
        this.message = message;
        this.creationDate = creationDate;
        this.createdBy = createdBy;
        this.previousCommit = previousCommit;
        this.secondPreviousCommit = secondPreviousCommit;
    }

    public String getMessage(){
        return message;
    }

    public String getRootSha1(){
        return rootSha1;
    }

    public String getSecondPreviousCommit(){
        return secondPreviousCommit;
    }

    public String getPreviousCommit(){
        return previousCommit;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getCreatedBy(){
        return createdBy;
    }

    @Override
    public String createHashCode() {
        return DigestUtils.sha1Hex(createTextToEncrypt());
    }

    @Override
    public String createGitFileText() {
        String sha1Str;
        if(secondPreviousCommit.equals("")) {
            sha1Str = rootSha1 + DELIMITER + previousCommit + DELIMITER + message + DELIMITER +
                    creationDate + DELIMITER + createdBy;
        }
        else {
            sha1Str = rootSha1 + DELIMITER + previousCommit + DELIMITER + secondPreviousCommit + DELIMITER +
                    message + DELIMITER + creationDate + DELIMITER + createdBy;
        }
        return sha1Str;
    }

    public String createTextToEncrypt() {
        String sha1Str;
        if(secondPreviousCommit == null || secondPreviousCommit.equals("")) {
            sha1Str = rootSha1 + DELIMITER + previousCommit + DELIMITER + message + DELIMITER + createdBy;
        }
        else {
            sha1Str = rootSha1 + DELIMITER + previousCommit + DELIMITER + secondPreviousCommit + DELIMITER +
                    message + DELIMITER + createdBy;
        }
        return sha1Str;
    }

    @Override
    public String getContent() {
        return createGitFileText();
    }

    public void setRootSha1(String rootSha1){
        this.rootSha1 = rootSha1;
    }

    public void parseCommitFile(File file) throws IOException, ParserConfigurationException {
        List<String[]> data = Utils.readObjectFileIntoArray(file, DELIMITER);
        if(data.get(0).length < 5)
            throw new ParserConfigurationException("Failed to parse, missing data in commit file");
        if(data.get(0).length == 5) {
            rootSha1 = data.get(0)[0];
            previousCommit = data.get(0)[1];
            message = data.get(0)[2];
            creationDate = data.get(0)[3];
            createdBy = data.get(0)[4];
        }
        else if(data.get(0).length == 6){
            rootSha1 = data.get(0)[0];
            previousCommit = data.get(0)[1];
            secondPreviousCommit = data.get(0)[2];
            message = data.get(0)[3];
            creationDate = data.get(0)[4];
            createdBy = data.get(0)[5];
        }
    }
}
