package logic.manager;
import java.util.List;

public class CommitDelta {
    private String commitSha1;
    private String secondCommitSha1;
    private List<String> newFiles;
    private List<String> editedFiled;
    private List<String> deletedFiles;
    private List<String> newFilesSecondCommit;
    private List<String> editedFiledSecondCommit;
    private List<String> deletedFilesSecondCommit;

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

    public void setNewFiles(List<String> newFiles){
        this.newFiles = newFiles;
    }

    public void setEditedFiled(List<String> editedFiled){
        this.editedFiled = editedFiled;
    }

    public void setDeletedFiles(List<String> deletedFiles){
        this.deletedFiles = deletedFiles;
    }

    public List<String> getNewFiles(){
        return newFiles;
    }

    public List<String> getEditedFiled(){
        return editedFiled;
    }

    public List<String> getDeletedFiles(){
        return deletedFiles;
    }

    public String getCommitSha1(){
        return commitSha1;
    }

    public String getSecondCommitSha1(){
        return secondCommitSha1;
    }

    public void setNewFilesSecondCommit(List<String> newFiles){
        this.newFilesSecondCommit = newFiles;
    }

    public void setEditedFiledSecondCommit(List<String> editedFiled){
        this.editedFiledSecondCommit = editedFiled;
    }

    public void setDeletedFilesSecondCommit(List<String> deletedFiles){
        this.deletedFilesSecondCommit = deletedFiles;
    }

    public List<String> getNewFilesSecondCommit(){
        return newFilesSecondCommit;
    }

    public List<String> getEditedFiledSecondCommit(){
        return editedFiledSecondCommit;
    }

    public List<String> getDeletedFilesSecondCommit(){
        return deletedFilesSecondCommit;
    }

}
