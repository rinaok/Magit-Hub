package engine.manager;
import logic.manager.PRStatus;
import java.util.Map;

public class PullRequest {

    public class CommitsDelta{
        private Map<String, String> newFiles; // sha1 to file path
        private Map<String, String> deletedFiles; // sha1 to file path
        private Map<String, String> editedFiles; // sha1 to file path

        public void setCommitDelta(Map<String, String> newFiles, Map<String, String> deletedFiles, Map<String, String> editedFiles){
            this.newFiles = newFiles;
            this.deletedFiles = deletedFiles;
            this.editedFiles = editedFiles;
        }
    }

    private String targetBranch;
    private String baseBranch;
    private String msg;
    private PRStatus status;
    private String user;
    private String date;
    private CommitsDelta delta;

    public PullRequest(String targetBranch, String baseBranch, String msg, PRStatus status, String user, String date){
        this.baseBranch = baseBranch;
        this.targetBranch = targetBranch;
        this.msg = msg;
        this.status = status;
        this.user = user;
        this.date = date;
        this.delta = new CommitsDelta();
    }

    public String getTargetBranch(){
        return targetBranch;
    }

    public String getBaseBranch(){
        return baseBranch;
    }

    public void setCommitDelta(Map<String, String> newFiles, Map<String, String> deletedFiles, Map<String, String> editedFiles){
        this.delta.editedFiles = editedFiles;
        this.delta.deletedFiles = deletedFiles;
        this.delta.newFiles = newFiles;
    }
}

