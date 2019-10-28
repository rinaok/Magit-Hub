package engine.ui;

import engine.manager.PullRequest;
import logic.manager.*;
import logic.manager.Exceptions.FailedToCreateBranchException;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Exceptions.FailedToMergeException;
import logic.manager.Exceptions.XmlParseException;
import logic.manager.Merge.MergeResult;
import logic.manager.XmlHandler.CallableXml;
import logic.manager.XmlHandler.XmlAdapter;
import logic.modules.Branch;
import logic.modules.Commit;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UIManager {
    private XmlAdapter systemEngine;

    public UIManager(){
        systemEngine = new XmlAdapter();
    }

    public String createNewRepositoryByUsersDefinition(String path, String name)
            throws ParserConfigurationException, IOException, FailedToCreateRepositoryException {
        systemEngine.createNewRepository(name, path);
        return ("Successfully created a new repository under: " + path + "\\" + name);
    }

    public String getUsername(){
        return systemEngine.getUsername();
    }

    public String getHeadBranch(){
        return systemEngine.getBranchesManager().getActive().getName();
    }

    public Commit getHeadCommit(){
        return systemEngine.getBranchesManager().getActive().getHead();
    }

    public Branch getHeadBranchObj(){
        return systemEngine.getBranchesManager().getActive();
    }

    public String getStatus(){
        String repoPath = systemEngine.getRepository().getPath();
        String username = getUsername();
        try {
            List<String> deletedFiles = systemEngine.getDeletedFiles();
            List<String> editedFiles = systemEngine.getEditedFiles();
            List<String> newFiles = systemEngine.getNewFiles();
            if(deletedFiles.size() == 0 && editedFiles.size() == 0 && newFiles.size() == 0)
                return "All files are identical to last commit, no open changes";
            else {
                String deleted = String.join("\r\n", deletedFiles);
                String edited = String.join("\r\n", editedFiles);
                String newCreated = String.join("\r\n", newFiles);
                return "Modified Files:\r\n" + edited + "\r\n\r\n" +
                        "New Files:\r\n" + newCreated + "\r\n\r\n" +
                        "Removed Files:\r\n" + deleted + "\r\n\r\n";
            }
        }
        catch (IOException ex)
        {
            return "Something went wrong...\r\n" + ex.toString();
        }
    }

    public void loadXML(String stringXML) throws XmlParseException, FailedToCreateRepositoryException, IOException, ParserConfigurationException, ExecutionException, InterruptedException {
        systemEngine.setFilePath(stringXML);
        startNewThread();
    }

    private void startNewThread() throws InterruptedException, ExecutionException {
        CallableXml callableXml = new CallableXml();
        callableXml.setXmlEngine(systemEngine);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(callableXml);
        future.get();
    }

    public boolean isXmlRepoExists(String path, String userName) throws XmlParseException {
        return systemEngine.isRepositoryExists(path, userName);
    }

    public void deleteRepository() throws IOException {
        systemEngine.deleteRepo();
    }

    public String doCommit(String userMsg) throws ParserConfigurationException, IOException, FailedToCreateRepositoryException {
        return systemEngine.commit(userMsg, null);
    }

    public String changeActiveRepository(String path, String name) throws ParserConfigurationException, IOException, FailedToCreateRepositoryException {
        systemEngine.changeActiveRepository(path, name);
        return "Active repository was changed successfully!";
    }

    public String changeUserName(String username){
        systemEngine.changeUsername(username);
        return "Successfully changed username";
    }

    public boolean checkout(String newBranch, boolean toDelete) throws IOException, FailedToCreateBranchException, ParserConfigurationException, FailedToCreateRepositoryException {
        return systemEngine.checkout(newBranch, toDelete);
    }

    public boolean isRemoteBranch(String branchName){
        if(systemEngine.getBranchesManager().isBranchExists(branchName))
            return systemEngine.getBranchesManager().getBranch(branchName).getIsRemote();
        return false;
    }

    private boolean getResponseFromUser() {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        if (input.toLowerCase().equals("yes") || input.toLowerCase().equals("y")) {
            return true;
        } else if (input.toLowerCase().equals("no") || input.toLowerCase().equals("n")) {
            return false;
        } else {
            System.out.println("Please type yes or no");
            return getResponseFromUser();
        }
    }

    public List<Map<String, String>>  commitFilesDetails(String commitSha1) throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        return systemEngine.commitFilesDetails(commitSha1);
    }

    private String parseListOfMaps(List<Map<String,String>> fileDetails) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> item : fileDetails) {
            sb.append("\r\n");
            Iterator<Map.Entry<String, String>> iter = item.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                sb.append(entry.getKey());
                sb.append(": ");
                sb.append(entry.getValue());
                if (iter.hasNext()) {
                    sb.append("\r\n");
                }
            }
            sb.append("\r\n");
        }

        return sb.toString();
    }

    public List<Map<String, String>> branchesList() throws FailedToCreateBranchException {
        List<Map<String, String>> branches;
        branches = systemEngine.listOfBranches();
        String activeBranch = systemEngine.getActiveBranch();
        branches.forEach(x -> {
            if (x.get("Branch Name").equals(activeBranch)) {
                String name = x.get("Branch Name");
                name += "      <---- HEAD BRANCH";
                x.put("Branch Name", name);
            }
        });
        return branches;
    }

    public void addNewBranch(String name, String commitSha1) throws FailedToCreateBranchException, IOException, FailedToCreateRepositoryException {
        systemEngine.addBranch(name,commitSha1);
    }

    public void addNewBranchOnCommit(String branchName, String commit) throws IOException, FailedToCreateRepositoryException {
        systemEngine.addNewBranchOnCommit(branchName, commit);
    }

    public String deleteBranch(String name) throws Exception {
        try{
            systemEngine.deleteBranch(name);
            return "Branch was deleted successfully";
        }
        catch (IOException | FailedToCreateBranchException e){
            return e.getMessage();
        }
    }

    public Map<String, Commit> getCommitsMap(){ return systemEngine.getCommitMapSha1(); }

    public List<Branch> getBranches(){ return systemEngine.getBranchesManager().getBranches(); }

    public List<Map<String,String>> getCommitsHistory() throws ParserConfigurationException, IOException, FailedToCreateRepositoryException {
        return systemEngine.commitHistory();
    }

    public String exportRepositoryToXml(String filePath){
        return systemEngine.exportRepositoryToXml(filePath);
    }

    public String resetBranch(String commitSha1, boolean toDelete) throws ParserConfigurationException, FailedToCreateBranchException, FailedToCreateRepositoryException, IOException {
        return systemEngine.resetBranch(commitSha1, toDelete);
    }

    public MergeResult merge(String branchName) throws FailedToCreateBranchException {
        MergeResult res = null;
        try {
            if (systemEngine.getChanges()) {
                try {
                    res = systemEngine.merge(branchName);
                    System.out.println("MERGE RESULT: " + res.toString());
                    if (res != MergeResult.FAILURE) {
                        if (res == MergeResult.FAST_FORWARD_MERGE_CONTAINED) {
                            systemEngine.checkout(branchName, false);
                        } else if (res == MergeResult.FAST_FORWARD_MERGE_CONCEALED) {
                            System.out.println("Nothing to merge");
                        }
                    }
                } catch (FailedToMergeException e) {
                    e.printStackTrace();
                } catch (FailedToCreateRepositoryException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
            } else {
                return MergeResult.OPEN_CHANGES;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public List<String> getConflicts() throws IOException, FailedToCreateRepositoryException, FailedToMergeException, ParserConfigurationException {
        return systemEngine.getConflicts();
    }

    public Map<String, String> getConflictFilesContent(String file) throws IOException {
        return systemEngine.getConflictFilesContent(file);
    }

    public void solveConflict(String file, String content) throws IOException, FailedToCreateRepositoryException {
        systemEngine.solveConflict(file, content);
    }

    public void commitOpenChanges(String msg, String branchName) throws FailedToMergeException, ParserConfigurationException, FailedToCreateRepositoryException, IOException {
        String commit = systemEngine.commitOpenChanges(msg, branchName);
    }

    public Repository getRepositoryByPath(String path){
        return systemEngine.getRepositoryByPath(path);
    }

    public String getRepositoryName(){
        return systemEngine.getRepository().getName();
    }

    public String getRepositoryLocation(){
        return systemEngine.getRepository().getPath();
    }

    public void createCheckoutOnRTB(String branch) throws ParserConfigurationException, FailedToCreateBranchException, FailedToCreateRepositoryException, IOException {
        systemEngine.checkoutWithRTB(branch);
    }

    public void doClone(String remote, String local, String name) throws Exception {
        systemEngine.clone(remote, local, name);
    }

    public boolean doPull() throws Exception {
        return systemEngine.pull();
    }

    public boolean doPush() throws IOException, ParserConfigurationException {
        return systemEngine.push();
    }

    public void doFetch() throws Exception {
        systemEngine.fetch();
    }

    public void doPushToRR() throws Exception {
        systemEngine.pushToRR();
    }

    public boolean isRepositoryInitialized(){
        if(systemEngine.getRepository() != null)
            return true;
        return false;
    }

    public boolean isLocalRepository(){
        return systemEngine.isLocalRepository();
    }

    public String getFileContent(String filePath) throws IOException {
        return Utils.readFile(new File(filePath));
    }

    public CommitDelta getCommitDelta(Commit commit) throws ParserConfigurationException, IOException, FailedToCreateRepositoryException {
        return systemEngine.getDeltaToPreviousCommit(commit);
    }

    public void editFileInServer(String filePath, String content) throws IOException, FailedToCreateRepositoryException {
        Utils.writeFile(new File(filePath), content);
    }

    public String findFilePath(String sha1) throws IOException {
        String rootSha1 = systemEngine.getBranchesManager().getActive().getHead().getRootSha1();
        return systemEngine.findPath(sha1, rootSha1);
    }

    public void deleteFile(String path) throws IOException {
        Utils.deleteFile(path);
    }

    public List<WCFileNode> createFilesTree() throws IOException, ParserConfigurationException, FailedToCreateRepositoryException {
        return systemEngine.createFilesTree();
    }

    public void addNewFile(String path, String fileName, String content) throws IOException, FailedToCreateRepositoryException {
        Utils.writeFile(new File(path + "\\" + fileName + ".txt"), content);
    }

    public List<String> getDeletedFiles() throws IOException {
        return systemEngine.getDeletedFiles();
    }

    public List<String> getNewFiles() throws IOException {
        return systemEngine.getNewFiles();
    }

    public List<String> getModifieddFiles() throws IOException {
        return systemEngine.getEditedFiles();
    }

    public boolean isForked(){
        return systemEngine.getRepository().isLocalRepository();
    }

    public String getPullRequestUser() throws Exception {
        return systemEngine.getRemoteRepositoryOwner();
    }

    public void deltaCommitPR(PullRequest PR) throws ParserConfigurationException, IOException, FailedToCreateRepositoryException {
        systemEngine.deltaCommitPR(PR);
    }

    public void acceptPR(PullRequest PR){
        systemEngine.acceptPR(PR);
    }

    public boolean isRTB(String branch){
        if(systemEngine.getBranchesManager().isBranchExists(branch)){
            return systemEngine.getBranchesManager().getBranch(branch).getTracking();
        }
        return false;
    }

    public String getRemoteOwner() throws Exception {
        if(systemEngine.getRepositoriesManager().getActive().isLocalRepository())
            return systemEngine.getRemoteRepositoryOwner();
        return null;
    }
}
