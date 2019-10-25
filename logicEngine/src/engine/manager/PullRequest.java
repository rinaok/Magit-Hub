package engine.manager;

public class PullRequest {
    String targetBranch;
    String baseBranch;
    String msg;

    public PullRequest(String targetBranch, String baseBranch, String msg){
        this.baseBranch = baseBranch;
        this.targetBranch = targetBranch;
        this.msg = msg;
    }
}

