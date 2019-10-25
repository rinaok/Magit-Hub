package server.utils;

import engine.manager.PullRequest;
import logic.manager.WCFileNode;
import logic.modules.Branch;
import logic.modules.Commit;

import javax.swing.text.html.ListView;
import java.util.ArrayList;
import java.util.List;

public class RepoMagitFile {

    public class ExtendedCommit {
        private String sha1;
        private List<String> pointingBranches;
        private Commit commit;

        public ExtendedCommit(Commit commit){
            this.commit = commit;
            sha1 = commit.createHashCode();
            getPointingBranches();
        }

        private void getPointingBranches(){
            pointingBranches = new ArrayList<>();
            for(Branch branch : branches){
                if(branch.getHead().createHashCode().equals(sha1))
                    pointingBranches.add(branch.getName());
            }
        }
    }

    private List<Branch> branches;
    private List<ExtendedCommit> commits;
    private List<WCFileNode> wcFiles;
    private List<PullRequest> pullRequests;
    private boolean isForked;

    public RepoMagitFile(List<Branch> branches, List<Commit> commits, List<WCFileNode> wcFiles, boolean isForked,
                         List<PullRequest> pullRequests){
        this.branches = branches;
        this.commits = new ArrayList<>();
        for(Commit commit : commits){
            this.commits.add(new ExtendedCommit(commit));
        }
        setCommitSha1();
        this.wcFiles = wcFiles;
        this.isForked = isForked;
        this.pullRequests = pullRequests;
    }

    private void setCommitSha1(){
        for(Branch branch : branches){
            branch.setCommitSha1(branch.getHead().createHashCode());
        }
    }

}