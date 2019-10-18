package logic.modules;
import logic.manager.Utils;
import org.apache.commons.codec.digest.DigestUtils;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Folder implements GitFile {

    private final String DELIMITER = ",";
    private String name;
    private Map<String, FileData> includedFiles; // file name to file data

    public Folder(String name) {
        includedFiles = new TreeMap<>();
        this.name = name;
    }

    public Map<String, FileData> getIncludedFiles() {
        return includedFiles;
    }

    public String getName(){
        return name;
    }

    public void addFileToFolder(GitFile newFile, String user, String fileName) {
        FileData newFileMetaData = new FileData(newFile, user, fileName);
        includedFiles.put(newFileMetaData.getName(), newFileMetaData);
    }

    public void removeFromFolder(String fileToRemove){
        includedFiles.remove(fileToRemove);
    }

    public String getContent(){
        return generateFolderFile(true);
    }

    public String generateFolderFile(boolean withTimestamp) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,FileData> fileInMap : includedFiles.entrySet())
        {
            FileData file = fileInMap.getValue();
            if(withTimestamp) {
                sb.append(file.getName() + DELIMITER + file.getFilePointer().createHashCode() + DELIMITER +
                        file.getType().toString() + DELIMITER + file.getModifiedBy() + DELIMITER +
                        file.getModificationDate() + "\r\n");
            }
            else{
                sb.append(file.getName() + DELIMITER + file.getFilePointer().createHashCode() + DELIMITER +
                        file.getType().toString() + DELIMITER + file.getModifiedBy() + "\r\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String createHashCode() {
        return DigestUtils.sha1Hex(generateFolderFile(false));
    }

    @Override
    public String createGitFileText() {
        return generateFolderFile(true);
    }

    public void parseFolderFile(File file) throws IOException, ParserConfigurationException {
        List<String[]> data = Utils.readObjectFileIntoArray(file, DELIMITER);
        if(data.get(0).length < 5)
            throw new ParserConfigurationException("Failed to parse, missing data in commit file");
        for(String[] includedFile : data){
            String fileName = includedFile[0];
            String sha1 = includedFile[1];
            String type = includedFile[2];
            String user = includedFile[3];
            String time = includedFile[4];
            FileData fd;
            if(type.equals(FileType.DIRECTORY.toString())) {
                fd = new FileData(new Folder(fileName), user, fileName);
            }
            else{
                String content = Utils.readFile(new File(file.getParent() + "\\" + sha1));
                fd = new FileData(new Blob(content), user, fileName);
            }
            fd.setSha1(sha1);
            includedFiles.put(fileName, fd);
        }
    }

}
