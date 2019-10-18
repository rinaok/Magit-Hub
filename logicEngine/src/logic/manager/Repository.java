package logic.manager;

public class Repository {
    private String path;
    private String name;
    private WorkingCopy wc;
    private String username;
    private String repositoryReference;

    public Repository(String name, String path, String rootFolder, String username){
        wc = new WorkingCopy(rootFolder, username);
        this.name = name;
        this.path = path;
        this.username = username;
        repositoryReference = "";
    }

    public String getPath(){
        return path;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public WorkingCopy getWorkingCopy(){
        return wc;
    }

    public void setRepositoryReference(String path){
        repositoryReference = path;
    }

    public boolean isLocalRepository(){
        return !repositoryReference.equals("");
    }

    public String getUsername(){
        return username;
    }
}
