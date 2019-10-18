package server.utils;
import logic.modules.Branch;
import logic.modules.Commit;

import java.util.List;

public class RepoMagitFile {
    private List<Branch> branches;
    private List<Commit> commits;

    public RepoMagitFile(List<Branch> branches, List<Commit> commits){
        this.branches = branches;
        this.commits = commits;
        setCommitSha1();
    }

    private void setCommitSha1(){
        for(Branch branch : branches){
            branch.setCommitSha1(branch.getHead().createHashCode());
        }
    }
}
