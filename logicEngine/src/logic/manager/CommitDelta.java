package logic.manager;
import java.util.List;
import java.util.Map;

public class CommitDelta {
    private String commitSha1;
    private String secondCommitSha1;
    private Map<String, String> newFiles;
    private Map<String, String> editedFiled;
    private Map<String, String> deletedFiles;
    private Map<String, String> newFilesSecondCommit;
    private Map<String, String> editedFiledSecondCommit;
    private Map<String, String> deletedFilesSecondCommit;

    public CommitDelta(String commitSha1){
        this.commitSha1 = commitSha1;
        this.secondCommitSha1 = "";
    }

    public CommitDelta(String commitSha1, String secondCommitSha1){
        this.commitSha1 = commitSha1;
        this.secondCommitSha1 = secondCommitSha1;
    }

    public boolean hasSecondCommit(){
        if(this.secondCommitSha1.equals(""))
            return false;
        else return true;
    }

    public void setNewFiles(Map<String, String> newFiles){
        this.newFiles = newFiles;
    }

    public void setEditedFiled(Map<String, String> editedFiled){
        this.editedFiled = editedFiled;
    }

    public void setDeletedFiles(Map<String, String> deletedFiles){
        this.deletedFiles = deletedFiles;
    }

    public Map<String, String> getNewFiles(){
        return newFiles;
    }

    public Map<String, String> getEditedFiled(){
        return editedFiled;
    }

    public Map<String, String> getDeletedFiles(){
        return deletedFiles;
    }

    public String getCommitSha1(){
        return commitSha1;
    }

    public String getSecondCommitSha1(){
        return secondCommitSha1;
    }

    public void setNewFilesSecondCommit(Map<String, String> newFiles){
        this.newFilesSecondCommit = newFiles;
    }

    public void setEditedFiledSecondCommit(Map<String, String> editedFiled){
        this.editedFiledSecondCommit = editedFiled;
    }

    public void setDeletedFilesSecondCommit(Map<String, String> deletedFiles){
        this.deletedFilesSecondCommit = deletedFiles;
    }

    public Map<String, String> getNewFilesSecondCommit(){
        return newFilesSecondCommit;
    }

    public Map<String, String> getEditedFiledSecondCommit(){
        return editedFiledSecondCommit;
    }

    public Map<String, String> getDeletedFilesSecondCommit(){
        return deletedFilesSecondCommit;
    }

}
