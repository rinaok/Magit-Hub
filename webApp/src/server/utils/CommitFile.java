package server.utils;

import java.io.File;
import java.util.Map;

public class CommitFile {
    private String name;
    private String type;
    private String sha1;
    private String lastModifier;
    private String modificationDate;

    public CommitFile(Map<String, String> file) {
        File filePath = new File(file.get("File Path"));
        name = filePath.getName();
        type = file.get("Type");
        sha1 = file.get("SHA1");
        lastModifier = file.get("Last Modifier");
        modificationDate = file.get("Modification Date");
    }
}
