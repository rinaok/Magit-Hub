package logic.manager.Managers;
import logic.manager.Repository;
import java.util.HashMap;
import java.util.Map;

public class RepositoryManager implements ManagerInterface<Repository> {

    private Map<String, Repository> nameToRepositoryMap;
    private Map<String, String> pathToName;
    private String activeRepository;

    public RepositoryManager(){
        nameToRepositoryMap = new HashMap<>();
        pathToName = new HashMap<>();
    }

    public Repository getRepository(String name){
        if(nameToRepositoryMap.containsKey(name))
            return nameToRepositoryMap.get(name);
        return null;
    }

    @Override
    public Repository getActive() {
        if(activeRepository != null && nameToRepositoryMap.containsKey(activeRepository)){
            return nameToRepositoryMap.get(activeRepository);
        }
        return null;
    }

    @Override
    public void setActive(Repository newActive) {
        activeRepository = newActive.getName();
        if(!pathToName.containsKey(newActive.getPath())){
            nameToRepositoryMap.put(newActive.getName(), newActive);
            pathToName.put(newActive.getPath().toLowerCase(), newActive.getName());
        }
    }

    @Override
    public void addItem(Repository item) {
        pathToName.put(item.getPath().toLowerCase(), item.getName());
        nameToRepositoryMap.put(item.getName(), item);
    }

    @Override
    public void clear() {
        nameToRepositoryMap.clear();
        pathToName.clear();
    }

    public Repository getRepositoryByPath(String path){
        if(pathToName.containsKey(path.toLowerCase())){
            String name = pathToName.get(path.toLowerCase());
            return nameToRepositoryMap.containsKey(name) ? nameToRepositoryMap.get(name) : null;
        }
        return null;
    }
}
