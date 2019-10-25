package logic.manager.Managers;
import logic.modules.Branch;

import java.text.SimpleDateFormat;
import java.util.*;

public class BranchManager implements ManagerInterface<Branch>{

    private Map<String, Branch> nameToBranchMap;
    private String activeBranch;
    private List<Branch> branchesByCreationDate;

    public BranchManager(){
        branchesByCreationDate = new ArrayList<>();
        nameToBranchMap = new HashMap<>();
    }

    public List<Branch> getBranches() {
        return branchesByCreationDate;
    }

    @Override
    public Branch getActive() {
        return nameToBranchMap.get(activeBranch);
    }

    @Override
    public void setActive(Branch newActive) {
        if(nameToBranchMap.containsKey(activeBranch))
            nameToBranchMap.get(activeBranch).setIsHead(false);
        activeBranch = newActive.getName();
        if(!nameToBranchMap.containsKey(newActive.getName())){
            nameToBranchMap.put(newActive.getName(), newActive);
            addToList(newActive);
            newActive.setIsHead(true);
        }
        else{
            nameToBranchMap.get(newActive.getName()).setIsHead(true);
        }
    }

    @Override
    public void addItem(Branch item) {
        if (nameToBranchMap.containsKey(item.getName())) {
            nameToBranchMap.remove(item.getName());
        }
        nameToBranchMap.put(item.getName(), item);
        addToList(item);
    }

    @Override
    public void clear() {
        nameToBranchMap.clear();
        branchesByCreationDate.clear();
    }

    public void deleteBranch(String toDelete){
        if(nameToBranchMap.containsKey(toDelete)){
            nameToBranchMap.remove(toDelete);
            deleteFromBranchesList(toDelete);
        }
    }

    private void deleteFromBranchesList(String branchName){
        for(Branch branch : branchesByCreationDate)
        {
            if(branch.getName().equals(branchName)) {
                branchesByCreationDate.remove(branch);
                break;
            }
        }
    }

    public boolean isBranchExists(String branch){
        return nameToBranchMap.containsKey(branch);
    }

    public Branch getBranch(String branch){
        if(nameToBranchMap.containsKey(branch))
            return nameToBranchMap.get(branch);
        return null;
    }

    public List<Map<String,String>> listOfBranches() {
        List<Map<String, String>> branchMap = new ArrayList<>();
        for (Map.Entry<String, Branch> entry : nameToBranchMap.entrySet()) {
            Map<String, String> branchDetails = new TreeMap<>();
            branchDetails.put("Branch Name", entry.getValue().getName());
            if (entry.getValue().getHead().getRootSha1() == null && entry.getValue().getHead().getMessage() == null)
                branchDetails.put("Commit SHA1", "null");
            else
                branchDetails.put("Commit SHA1", entry.getValue().getHead().createHashCode());
            branchDetails.put("Commit Message", entry.getValue().getHead().getMessage());
            branchMap.add(branchDetails);
        }
        return branchMap;
    }

    public void deleteAll(){
        nameToBranchMap.clear();
        branchesByCreationDate.clear();
    }

    private void addToList(Branch branchToAdd){
        for(Branch branch : branchesByCreationDate)
        {
            if(branch.getName().equals(branchToAdd.getName())) {
                branchesByCreationDate.remove(branch);
                break;
            }
        }
        branchesByCreationDate.add(branchToAdd);
        Collections.sort(branchesByCreationDate, new Comparator<Branch>() {
            @Override
            public int compare(Branch o1, Branch o2) {
                return o1.getCreationDate().compareTo(o2.getCreationDate());
            }
        });
    }
}
