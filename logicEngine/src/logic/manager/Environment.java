package logic.manager;

public enum Environment {
    MAGIT(".magit"),
    OBJECTS("objects"),
    BRANCHES("branches");

    private final String repositoryName;

    Environment(final String repositoryName){
        this.repositoryName = repositoryName;
    }

    public String toString(){
        return repositoryName;
    }
}
