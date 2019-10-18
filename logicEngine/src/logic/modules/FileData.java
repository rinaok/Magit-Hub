package logic.modules;
import logic.manager.Utils;
import logic.manager.generated.*;

public class FileData {
    private String sha1;
    private String name;
    private FileType type;
    private String modifiedBy;
    private String modificationDate;
    private GitFile filePointer;

    public FileData(GitFile file, String user, String fileName){
        sha1 = file.createHashCode();
        name = fileName;
        filePointer = file;
        modificationDate = Utils.getTime();
        modifiedBy = user;
        if(file instanceof Blob)
            type = FileType.FILE;
        else if(file instanceof Folder)
            type = FileType.DIRECTORY;
    }

    public FileData(MagitBlob magitBlob, Blob blob){
        sha1 = blob.createHashCode();
        name = magitBlob.getName();
        this.type = FileType.FILE;
        this.modificationDate = magitBlob.getLastUpdateDate();
        this.modifiedBy = magitBlob.getLastUpdater();
        filePointer = blob;
    }

    public FileData(MagitSingleFolder magitFolder, Folder folder){
        sha1 = folder.createHashCode();
        name = magitFolder.getName();
        this.type = FileType.DIRECTORY;
        this.modificationDate = magitFolder.getLastUpdateDate();
        this.modifiedBy = magitFolder.getLastUpdater();
        filePointer = folder;
    }

    public FileType getType(){
        return type;
    }

    public GitFile getFilePointer(){
        return filePointer;
    }

    public String getSha1(){
        return sha1;
    }

    public void setModifiedBy(String modifiedBy){
        this.modifiedBy = modifiedBy;
    }

    public void setModificationDate(String modificationDate){
        this.modificationDate = modificationDate;
    }

    public void setSha1(String sha1) { this.sha1 = sha1;}

    public String getName(){
        return name;
    }

    public String getModifiedBy(){
        return modifiedBy;
    }

    public String getModificationDate(){
        return modificationDate;
    }
}
