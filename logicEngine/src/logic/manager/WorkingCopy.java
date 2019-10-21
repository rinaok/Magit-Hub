package logic.manager;
import logic.modules.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class WorkingCopy {
    private Set<String> wcFiles; //file paths
    private FileData rootFolder;

    public WorkingCopy(String rootFolder, String username){
        wcFiles = new HashSet<>();
        Folder root = new Folder(rootFolder);
        this.rootFolder = new FileData(root, username, rootFolder);
    }

    public Set<String> getWcFiles(){
        return wcFiles;
    }

    public String getRootFolderSha1() { return rootFolder.getFilePointer().createHashCode(); }

    public Folder getRootFolder(){
        return (Folder)rootFolder.getFilePointer();
    }

    public void initRootFolder(String path, String username) throws IOException{
        wcFiles.clear();
        String rootName = rootFolder.getName();
        rootFolder = new FileData(new Folder(rootName), username, rootName);
        updateRootFolder(path, username, (Folder)rootFolder.getFilePointer());
        calcRootFolderSha1();
    }

    public void calcRootFolderSha1(){
        rootFolder.setSha1(rootFolder.getFilePointer().createHashCode());
    }

    private void updateRootFolder(String path, String username, Folder mainFolder) throws IOException{
        File[] dirFiles = Utils.getAllFilesInDirectory(path);
        if(dirFiles != null) {
            for (File file : dirFiles) {
                if(!file.getCanonicalPath().contains(".magit")) {
                    if (file.isFile()) {
                        String content = Utils.readFile(file);
                        Blob blob = new Blob(content);
                        FileData fileData = new FileData(blob, username, file.getName());
                        mainFolder.addFileToFolder(blob, username, fileData.getName());
                    } else if (file.isDirectory()) {
                        Folder folder = new Folder(file.getName());
                        updateRootFolder(file.getCanonicalPath(), username, folder);
                        mainFolder.addFileToFolder(folder, username, folder.getName());
                    }
                    wcFiles.add(file.getCanonicalPath());
                }
            }
        }
    }
}
