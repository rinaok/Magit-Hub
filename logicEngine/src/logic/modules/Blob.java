package logic.modules;

import org.apache.commons.codec.digest.DigestUtils;

public class Blob implements GitFile {

    private String content;

    public Blob(String content){
        this.content = content.trim();
    }

    public String getContent(){
        return content;
    }

    public void setContent(String content){
        this.content = content;
    }

    @Override
    public String createHashCode() {
        return DigestUtils.sha1Hex(content);
    }

    @Override
    public String createGitFileText() {
        return content;
    }
}
