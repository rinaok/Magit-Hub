package logic.manager.Managers;
import logic.manager.Repository;
import java.util.HashMap;
import java.util.Map;

public class RepositoryManager implements ManagerInterface<Repository> {

    //private Map<String, Repository> nameToRepositoryMap;
    private Map<String, Repository> pathToRepositoryMap;
    //private Map<String, String> pathToName;
    private String activeRepository;

    public RepositoryManager(){
        pathToRepositoryMap = new HashMap<>();
        //pathToName = new HashMap<>();
    }

    public Repository getRepository(String path){
        if(pathToRepositoryMap.containsKey(path.toLowerCase()))
            return pathToRepositoryMap.get(path.toLowerCase());
        return null;
    }

    @Override
    public Repository getActive() {
        if(activeRepository != null && pathToRepositoryMap.containsKey(activeRepository.toLowerCase())){
            return pathToRepositoryMap.get(activeRepository.toLowerCase());
        }
        return null;
    }

    @Override
    public void setActive(Repository newActive) {
        activeRepository = newActive.getPath();
        if(!pathToRepositoryMap.containsKey(newActive.getPath().toLowerCase())){
            pathToRepositoryMap.put(newActive.getPath().toLowerCase(), newActive);
        }
    }

    @Override
    public void addItem(Repository item) {
        //pathToName.put(item.getPath().toLowerCase(), item.getName());
        pathToRepositoryMap.put(item.getPath().toLowerCase(), item);
    }

    @Override
    public void clear() {
        pathToRepositoryMap.clear();
        //pathToName.clear();
    }

    public Repository getRepositoryByPath(String path){
        if(pathToRepositoryMap.containsKey(path.toLowerCase())){
            return pathToRepositoryMap.get(path.toLowerCase());
        }
        return null;
    }
}
