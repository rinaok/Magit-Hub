package logic.manager.Merge;
import logic.manager.Engine;
import logic.modules.Commit;
import puk.team.course.magit.ancestor.finder.CommitRepresentative;

public class Representative implements CommitRepresentative {

    private static final String EMPTY_STRING = "";
    private String sha1;
    private String firstPrecedingSha1;
    private String secondPrecedingSha1;

    Representative(Commit commit){
        this.sha1 = commit.createHashCode();
        this.firstPrecedingSha1 = commit.getPreviousCommit().equals("null") ? EMPTY_STRING : commit.getPreviousCommit();
        this.secondPrecedingSha1 = Engine.getParentCommit(commit.getPreviousCommit());
        if(this.secondPrecedingSha1 == null || this.secondPrecedingSha1.equals("null")) {
            this.secondPrecedingSha1 = EMPTY_STRING;
        }
    }

    @Override
    public String getSha1() {
        return sha1;
    }

    @Override
    public String getFirstPrecedingSha1() {
        return firstPrecedingSha1;
    }

    @Override
    public String getSecondPrecedingSha1() {
        return secondPrecedingSha1;
    }
}
