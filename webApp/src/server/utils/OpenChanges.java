package server.utils;
import java.util.List;

public class OpenChanges {
    private List<String> newFiles;
    private List<String> deletedFiles;
    private List<String> modifiedFiles;

    public OpenChanges(List<String> newFiles, List<String> deletedFiles, List<String> modifiedFiles){
        this.newFiles = newFiles;
        this.modifiedFiles = modifiedFiles;
        this.deletedFiles = deletedFiles;
    }

}
