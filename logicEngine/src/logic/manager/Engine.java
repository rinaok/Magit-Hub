package logic.manager;
import engine.manager.PullRequest;
import logic.manager.Exceptions.FailedToCreateBranchException;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Exceptions.FailedToMergeException;
import logic.manager.Managers.BranchManager;
import logic.manager.Managers.RepositoryManager;
import logic.manager.Merge.MergeHandler;
import logic.manager.Merge.MergeResult;
import logic.modules.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.io.FilenameUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Engine {
    public final static String REMOTE_LOCATION = "RemoteRepository";
    private static final int modificationDateIndex = 4;
    private static final int modifiedByIndex = 3;
    private static final int fileTypeIndex = 2;
    private static final int sha1Index = 1;
    private static final int fileNameIndex = 0;
    private static final String DEFAULT_USERNAME = "Administrator";
    private static final String DEFAULT_BRANCH = "master";
    private static String wcRootPath;
    protected static String username;
    protected static String magitRepo;
    protected static final String HEAD = "head";
    protected RepositoryManager repositoriesManager;
    protected BranchManager branchesManager;
    protected boolean firstCommit;
    protected CollaborationHandler collaborationHandler;
    private Map<String, String> currentCommitSha1Files; // file path to sha1
    private static Map<String, Commit> commitMapSha1 = new HashMap<>(); // commit sha1 to commit object
    private MergeHandler mergeEngine;

    public Engine() {
        SystemInit();
    }

    private void SystemInit(){
        username = DEFAULT_USERNAME;
        branchesManager = new BranchManager();
        branchesManager.setActive(new Branch(null, DEFAULT_BRANCH));
        firstCommit = false;
        currentCommitSha1Files = new TreeMap<>();
        repositoriesManager = new RepositoryManager();
    }

    public static String getMagitRepo() { return magitRepo; }

    public Repository getRepository() {
        return repositoriesManager.getActive();
    }

    public boolean isLocalRepository(){
        return repositoriesManager.getActive().isLocalRepository();
    }

    public String getUsername() {
        return username;
    }

    public void changeUsername(String username) {
        this.username = username;
    }

    public Repository getRepositoryByPath(String path){
        return repositoriesManager.getRepositoryByPath(path);
    }

    public BranchManager getBranchesManager() {
        return branchesManager;
    }

    public Map<String, String> getCurrentCommitSha1Files(){
        return getCurrentCommitSha1Files();
    }

    public Map<String, Commit> getCommitMapSha1(){
        return commitMapSha1;
    }

    public RepositoryManager getRepositoriesManager(){
        return repositoriesManager;
    }

    public void createNewRepository(String name, String path) throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        initNewRepository(name, path, true, false);
    }

    public void initNewRepository(String name, String path, boolean createDir, boolean isRemote) throws FailedToCreateRepositoryException, IOException, ParserConfigurationException {
        currentCommitSha1Files.clear();
        commitMapSha1.clear();

        magitRepo = path + "\\" + Environment.MAGIT.toString();
        if(!isRemote)
            branchesManager.deleteAll();
        File head = new File(magitRepo + "\\" + Environment.BRANCHES + "\\" + HEAD + ".txt");
        wcRootPath = path;
        String headCommit = null;
        Commit headObj;
        if(head.exists()){
            String activeBranchName = Utils.readFile(head);
            headCommit = new Branch().parseBranchFile(new File(magitRepo + "\\" + Environment.BRANCHES + "\\" +
                                activeBranchName + ".txt"));
            headObj = new Commit();
            branchesManager.setActive(new Branch(headObj, activeBranchName));
            if(!headCommit.equals("")) {
                headObj.parseCommitFile(new File(magitRepo + "\\" + Environment.OBJECTS + "\\" + headCommit));
            }
            else{
                firstCommit = true;
            }
        }
        else {
            firstCommit = true;
            Utils.createTxtFile(magitRepo + "\\" + Environment.BRANCHES, HEAD, DEFAULT_BRANCH);
            Utils.createTxtFile(magitRepo + "\\" + Environment.BRANCHES, DEFAULT_BRANCH, "");
            Branch newBranch = new Branch(new Commit(), DEFAULT_BRANCH);
            branchesManager.setActive(newBranch);
        }
        if (createDir) {
            Utils.createNewDirectory(magitRepo, Environment.OBJECTS.toString());
        }
        if(repositoriesManager.getRepositoryByPath(path) != null) {
            repositoriesManager.setActive(repositoriesManager.getRepositoryByPath(path));
        }else
            repositoriesManager.setActive(new Repository(name, path, branchesManager.getActive().getName(), username));
        repositoriesManager.getActive().getWorkingCopy().initRootFolder(path, username);
        createBranchObjects(magitRepo + "\\" + Environment.BRANCHES, null);
        updateCurrentCommitFiles();
        initSha1ToCommitMap();
        if(!isRemote)
            isRemote();
    }

    private void isRemote() throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        File remoteLocation = new File(magitRepo + "\\" + REMOTE_LOCATION + ".txt");
        if(remoteLocation.exists()) {
            initCollaborationHandler();
            collaborationHandler.initRemoteRepository(repositoriesManager.getActive());
            //collaborationHandler.setRemoteRepository(repositoriesManager.getActive());
            collaborationHandler.updateBranches();
        }
    }

    protected void updateCurrentCommitFiles() throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        ActionsOnObjectsInterface interfaceAction = (obj, path) -> {
            if(obj[fileTypeIndex].equals(FileType.FILE.toString())) {
                String filePath = path + "\\" + obj[fileNameIndex];
                File file = new File(filePath);
                currentCommitSha1Files.put(file.getCanonicalPath(), obj[sha1Index]);
            }
        };

        recursiveRunOverObjectFiles(branchesManager.getActive().getHead().getRootSha1(), wcRootPath, interfaceAction, magitRepo + "\\" + Environment.OBJECTS, magitRepo + "\\" + Environment.OBJECTS);
    }

    public void setCurrentCommitSha1Files() throws IOException {
        currentCommitSha1Files = getPathToSha1Map(wcRootPath);
    }

    public String commit(String message, Commit secondCommit) throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        if (repositoriesManager.getActive() == null)
            throw new FailedToCreateRepositoryException("Error: Please choose active repository before committing");

        if (!firstCommit) {
            findDelta();
        }

        Commit newCommit;
        if(secondCommit == null) {
            repositoriesManager.getActive().getWorkingCopy().calcRootFolderSha1();
            newCommit = new Commit(repositoriesManager.getActive().getWorkingCopy().getRootFolderSha1(), branchesManager.getActive(),
                    message, Utils.getTime(), username, firstCommit);
        }
        else{
            newCommit = new Commit(repositoriesManager.getActive().getWorkingCopy().getRootFolderSha1(), message, Utils.getTime(), username,
                    branchesManager.getActive().getHead().createHashCode(), secondCommit.createHashCode());
        }
        createObjectFiles(newCommit);
        branchesManager.getActive().setHead(newCommit);
        commitMapSha1.put(newCommit.createHashCode(), newCommit);
        firstCommit = false;
        updateHeadCommit(newCommit, repositoriesManager.getActive().getPath() + "\\" + Environment.MAGIT);
        currentCommitSha1Files = getPathToSha1Map(wcRootPath);
        return newCommit.createHashCode();
    }

    private void updateHeadCommit(Commit newCommit, String magitRepo) throws IOException, ParserConfigurationException, FailedToCreateRepositoryException {
        File commitFile = new File(magitRepo + "\\" + Environment.OBJECTS + "\\" + newCommit.createHashCode());
        if (commitFile.exists()) {
            newCommit.parseCommitFile(commitFile);
        }
        else if(commitMapSha1.containsKey(newCommit.createHashCode())){
            newCommit = commitMapSha1.get(newCommit.createHashCode());
        }
        branchesManager.getActive().setHead(newCommit);
        branchesManager.getActive().createFile(magitRepo + "\\" + Environment.BRANCHES);
        Utils.createTxtFile(magitRepo + "\\" + Environment.BRANCHES, branchesManager.getActive().getName(),
                branchesManager.getActive().getHead().createHashCode());
    }

    private void findDelta() throws IOException {
        List<String> newFiles = getNewFiles();
        List<String> deletedFiles = getDeletedFiles();
        List<String> modifiedFiles = getEditedFiles();
        if (modifiedFiles.isEmpty() && newFiles.isEmpty() && deletedFiles.isEmpty()) // no changes, create only commit file
            return;

        updateModifiedFiles(modifiedFiles);
        updateDeletedFiles(deletedFiles);
        updateNewFiles(newFiles);
        repositoriesManager.getActive().getWorkingCopy().calcRootFolderSha1();
    }

    public boolean getChanges() throws IOException {
        List<String> newFiles = getNewFiles();
        List<String> deletedFiles = getDeletedFiles();
        List<String> modifiedFiles = getEditedFiles();
        if (modifiedFiles.isEmpty() && newFiles.isEmpty() && deletedFiles.isEmpty()) // no open changes
            return true;
        else
            return false;
    }

    private void updateModifiedFiles(List<String> modifiedFiles) throws IOException {
        DeltaInterface deltaDealer = (filePath, directory) -> {
            if (directory.getIncludedFiles().containsKey(filePath.getName())) {
                FileData matchedFileData = directory.getIncludedFiles().get(filePath.getName());
                if (matchedFileData.getFilePointer() instanceof Blob) {
                    Blob fileBlob = ((Blob) matchedFileData.getFilePointer());
                    fileBlob.setContent(Utils.readFile(filePath));
                    matchedFileData.setSha1(fileBlob.createHashCode());
                    matchedFileData.setModifiedBy(username);
                    matchedFileData.setModificationDate(Utils.getTime());
                }
                return true;
            }
            return false;
        };

        for (String file : modifiedFiles)
            updateFilesHierarchy(new File(file), repositoriesManager.getActive().getWorkingCopy().getRootFolder(), deltaDealer);
    }

    private void updateDeletedFiles(List<String> deletedFiles) throws IOException {
        DeltaInterface deltaDealer = (filePath, directory) -> {
            if (directory.getIncludedFiles().containsKey(filePath.getName())) {
                FileData matchedFileData = directory.getIncludedFiles().get(filePath.getName());
                if (matchedFileData.getFilePointer() instanceof Blob) {
                    directory.removeFromFolder(filePath.getName());
                }
                return true;
            }
            return false;
        };

        for (String file : deletedFiles)
            updateFilesHierarchy(new File(file), repositoriesManager.getActive().getWorkingCopy().getRootFolder(), deltaDealer);
    }

    private void updateNewFiles(List<String> newFiles) throws IOException {
        DeltaInterface deltaDealer = (filePath, directory) -> {
            if (directory.getName().equals(filePath.getParentFile().getName())) {
                {
                    Blob newBlob = new Blob(Utils.readFile(filePath));
                    directory.addFileToFolder(newBlob, username, filePath.getName());
                }
                return true;
            }
            return false;
        };

        for (String file : newFiles)
            updateFilesHierarchy(new File(file), repositoriesManager.getActive().getWorkingCopy().getRootFolder(), deltaDealer);
    }

    private Folder updateFilesHierarchy(File filePath, Folder directory, DeltaInterface deltaDealer) throws IOException {
        if (deltaDealer.updateDelta(filePath, directory))
            return directory;

        Folder parent = null;
        for (FileData data : directory.getIncludedFiles().values()) {
            if (data.getType() == FileType.DIRECTORY)
                parent = updateFilesHierarchy(filePath, (Folder) data.getFilePointer(), deltaDealer);
        }

        if (parent != null) {
            FileData parentData = directory.getIncludedFiles().get(parent.getName());
            parentData.setModificationDate(Utils.getTime());
            parentData.setModifiedBy(username);
            parentData.setSha1(parentData.getFilePointer().createHashCode());
        }

        return directory;
    }

    public List<String> getNewFiles() throws IOException {
        Map<String, String> wcSha1Files = getPathToSha1Map(wcRootPath);
        if(firstCommit)
            return new ArrayList<>(wcSha1Files.keySet());

        return (List<String>) CollectionUtils.subtract(wcSha1Files.keySet(), currentCommitSha1Files.keySet());
    }

    public List<String> getDeletedFiles() throws IOException {
        if (firstCommit)
            return new ArrayList<>();
        Map<String, String> wcSha1Files = getPathToSha1Map(wcRootPath);
        List<String> deletedFiles = (List<String>) CollectionUtils.subtract(currentCommitSha1Files.keySet(), wcSha1Files.keySet());
        return deletedFiles;
    }

    public List<String> getEditedFiles() throws IOException {
        if (firstCommit)
            return new ArrayList<>();
        Map<String, String> wcSha1Files = getPathToSha1Map(wcRootPath);
        return compareFiles(wcSha1Files, currentCommitSha1Files);
    }

    public List<String> compareFiles(Map<String, String> wcSha1Files,  Map<String, String> currentCommitSha1Files){
        //Files that were edited since last commit
        List<String> editedFiles = new ArrayList<>();
        List<String> fileNames = (List<String>) CollectionUtils.intersection(wcSha1Files.keySet(), currentCommitSha1Files.keySet());
        for (String fileName : fileNames) {
            if (!currentCommitSha1Files.get(fileName).equals(wcSha1Files.get(fileName))) {
                editedFiles.add(fileName);
            }
        }
        return editedFiles;

    }

    public Map<String, String> getPathToSha1Map(String folder) throws IOException {
        Stream<Path> walk = Files.walk(Paths.get(folder));
        List<String> currentFiles = walk.filter(Files::isRegularFile)
                .map(Path::toString).collect(Collectors.toList());
        Map<String, String> pathToSha1 = new TreeMap<>();
        for (String f : currentFiles) {
            File file = new File(f);
            if(!file.getParent().contains(".magit")) {
                String sha1 = Utils.createGitFile(file, username).createHashCode();
                pathToSha1.put(file.getCanonicalPath(), sha1);
            }
        }
        walk.close();
        return pathToSha1;
    }

    private void createObjectFiles(Commit newCommit) throws IOException, FailedToCreateRepositoryException {
        File objects = new File(repositoriesManager.getActive().getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS);
        repositoriesManager.getActive().getWorkingCopy().initRootFolder(repositoriesManager.getActive().getPath(), username);
        createObjectsRecursive(repositoriesManager.getActive().getWorkingCopy().getRootFolder());
        Utils.createZipFile(objects.getAbsolutePath(), repositoriesManager.getActive().getWorkingCopy().getRootFolderSha1(),
                repositoriesManager.getActive().getWorkingCopy().getRootFolder().getContent());
        newCommit.setRootSha1(repositoriesManager.getActive().getWorkingCopy().getRootFolderSha1());
        Utils.createZipFile(objects.getAbsolutePath(), newCommit.createHashCode(), newCommit.getContent());
    }

    private void createObjectsRecursive(Folder mainFolder) throws IOException, FailedToCreateRepositoryException {
        for (Map.Entry<String, FileData> file : mainFolder.getIncludedFiles().entrySet()) {
            if (file.getValue().getType() == FileType.FILE) {
                Utils.createZipFile(repositoriesManager.getActive().getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS, file.getValue().getSha1(), file.getValue().getFilePointer().getContent());
            } else {
                createObjectsRecursive((Folder) file.getValue().getFilePointer());
            }
        }

        Utils.createZipFile(repositoriesManager.getActive().getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS, mainFolder.createHashCode(), mainFolder.getContent());
    }

    public void changeActiveRepository(String repoPath, String repoName) throws FailedToCreateRepositoryException, IOException, ParserConfigurationException {
        File magit = new File(repoPath + "\\" + Environment.MAGIT);
        if (!magit.exists())
            throw new FailedToCreateRepositoryException("Failed to change the active repository since .magit folder " +
                    "doesn't exist in: " + repoPath);

        initNewRepository(repoName, repoPath, false, false);
    }

    public boolean checkout(String newBranch, boolean deleteUncommitted) throws
            FailedToCreateRepositoryException, IOException, ParserConfigurationException, FailedToCreateBranchException {
        if (!branchesManager.isBranchExists(newBranch)) {
            throw new FailedToCreateRepositoryException("Branch name doesn't exists");
        }

        if(branchesManager.getBranch(newBranch).getHead().getRootSha1() == null){
            throw new FailedToCreateRepositoryException("Branch has no head commit");
        }

        if(branchesManager.getBranch(newBranch).getIsRemote()){
            throw new FailedToCreateBranchException("Checkout cannot be done on remote branch");
        }

        if (preCheckoutVerification() || deleteUncommitted) {
            if(new File(wcRootPath).exists())
                Utils.deleteDirectory(new File(wcRootPath));
            branchesManager.setActive(branchesManager.getBranch(newBranch));
            Utils.createTxtFile(magitRepo + "\\" + Environment.BRANCHES, HEAD, branchesManager.getActive().getName());
            branchFilesToWC();
            return true;
        } else {
            return false;
        }
    }

    protected void branchFilesToWC() throws IOException, ParserConfigurationException, FailedToCreateRepositoryException {
        wcRootPath = repositoriesManager.getActive().getPath();
        String commitSha1 = Utils.readFile(new File(magitRepo + "\\" + Environment.BRANCHES +
                "\\" + branchesManager.getActive().getName() + ".txt"));

        String commitFile = magitRepo + "\\" + Environment.OBJECTS + "\\" + commitSha1;
        File commitZip = new File(commitFile);
        if (commitZip.exists() && commitZip.isFile()) {
            Commit lastCommit = new Commit();
            lastCommit.parseCommitFile(commitZip);
            if (!branchesManager.getActive().getHead().createHashCode().equals(lastCommit.createHashCode()))
                branchesManager.getActive().setHead(lastCommit);
        } else {
            if (!commitSha1.equals(""))
                throw new FailedToCreateRepositoryException("Head commit zip file [" + commitZip.getName() + "] was not found in objects folder");
            else {
                firstCommit = true;
                System.out.println("Initializing empty repository");
                return;
            }
        }

        ActionsOnObjectsInterface action = createFilesFromObjects();

        recursiveRunOverObjectFiles(branchesManager.getActive().getHead().getRootSha1(), wcRootPath, action, magitRepo + "\\" + Environment.OBJECTS, magitRepo + "\\" + Environment.OBJECTS);
        repositoriesManager.getActive().getWorkingCopy().initRootFolder(repositoriesManager.getActive().getPath(), username);
        currentCommitSha1Files = getPathToSha1Map(wcRootPath);
    }

    public ActionsOnObjectsInterface createFilesFromObjects() {
        ActionsOnObjectsInterface actionInterface = (obj, path) ->
        {
            if (obj[fileTypeIndex].equals(FileType.FILE.toString())) {
                File blobFile = new File(magitRepo + "\\" + Environment.OBJECTS + "\\" + obj[sha1Index]);
                List<String> content = Utils.readZipFile(blobFile);
                String fullContent = content.stream().map(n -> String.valueOf(n))
                        .collect(Collectors.joining("\r\n"));
                Utils.writeFile(new File(path + "\\" + obj[fileNameIndex]), fullContent);
            } else {
                Utils.createNewDirectory(path, obj[fileNameIndex]);
            }
        };

        return actionInterface;
    }

    public void recursiveRunOverObjectFiles(String sha1, String path, ActionsOnObjectsInterface actionInterface, String objectsRepo, String remoteObjPath) throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        File zip = new File(objectsRepo + "\\" + sha1);
        if (zip.exists()) {
            if(!new File(remoteObjPath + "\\" + sha1).exists())
                Files.copy(new File(objectsRepo + "\\" + sha1).toPath(), new File(remoteObjPath + "\\" + sha1).toPath());
            List<String[]> files = Utils.createObjectsFromFile(zip, ",");
            for (String[] obj : files) {
                if(!new File(remoteObjPath + "\\" + obj[sha1Index]).exists())
                    Files.copy(new File(objectsRepo + "\\" + obj[sha1Index]).toPath(), new File(remoteObjPath + "\\" + obj[sha1Index]).toPath());
                if(actionInterface != null)
                    actionInterface.doAction(obj, path);
                if (obj[fileTypeIndex].equals(FileType.DIRECTORY.toString())) {
                    recursiveRunOverObjectFiles(obj[sha1Index], path + "\\" + obj[fileNameIndex], actionInterface, objectsRepo, remoteObjPath);
                }
            }
        }
    }

    private boolean preCheckoutVerification() throws IOException {
        if(!new File(wcRootPath).exists())
            return true;
        return getChanges();
    }

    public List<Map<String,String>> listOfBranches() throws FailedToCreateBranchException {
        if(magitRepo == null)
            throw new FailedToCreateBranchException("Error! Repository is empty. " +
                    "Please activate a repository or create a new one");
        List<Map<String, String>> branchMap = branchesManager.listOfBranches();
        return branchMap;
    }

    public void createBranchObjects(String magitBranches, String remoteBranch) throws IOException, ParserConfigurationException{
        File[] branchFiles = Utils.getAllFilesInDirectory(magitBranches);
        boolean areThereBranches = false;
        for(File branch : branchFiles){
            if(!branch.getName().equals(HEAD + ".txt")){
                if(branch.isFile() && branch.length() > 0) {
                    areThereBranches = true;
                    String commitSh1 = Utils.readFile(branch);
                    if (!commitSh1.equals("")) {
                        getCommitsRecursively(commitSh1);
                        String fileNameWithOutExt = FilenameUtils.removeExtension(branch.getName());
                        if(remoteBranch != null)
                            fileNameWithOutExt = remoteBranch + "\\" + fileNameWithOutExt;
                        Branch newBranch = new Branch(commitMapSha1.get(commitSh1), fileNameWithOutExt, remoteBranch != null);
                        if(!branchesManager.isBranchExists(newBranch.getName()))
                            branchesManager.addItem(newBranch);
                    }
                }
            }
        }

        firstCommit = areThereBranches ? false : true;
    }

    public String getActiveBranch(){
        return branchesManager.getActive().getName();
    }

    public void addBranch(String branchName, String headCommit) throws FailedToCreateBranchException, IOException, FailedToCreateRepositoryException {
        if(magitRepo == null)
            throw new FailedToCreateBranchException(
            "Error! Repository is empty. Please activate a repository or create a new one");
        if(branchesManager.isBranchExists(branchName))
            throw new FailedToCreateBranchException("Error! Branch with the same name already exists");
        Commit head = branchesManager.getActive().getHead();
        if(headCommit == null) {
            headCommit = Utils.readFile(new File(magitRepo + "\\" + Environment.BRANCHES + "\\" +
                    branchesManager.getActive().getName() + ".txt"));
        }
        Utils.createTxtFile(magitRepo + "\\" + Environment.BRANCHES, branchName, headCommit);
        Branch newBranch = new Branch(head, branchName);
        newBranch.setCommitSha1(headCommit);
        branchesManager.addItem(newBranch);
    }

    public void addNewBranchOnCommit(String branch, String commit) throws IOException, FailedToCreateRepositoryException {
        Utils.createTxtFile(magitRepo + "\\" + Environment.BRANCHES, branch, commit);
        Branch newBranch = new Branch(commitMapSha1.get(commit), branch);
        branchesManager.addItem(newBranch);
    }

    public void deleteBranch(String branchName) throws FailedToCreateBranchException, IOException {
        if(magitRepo == null)
            throw new FailedToCreateBranchException("Error! Repository is empty. Please activate a repository or create a new one");
        if(branchName.equals(branchesManager.getActive().getName()))
            throw new FailedToCreateBranchException("Error! Can't delete the head branch");
        if(branchesManager.isBranchExists(branchName)){
            if(!Utils.deleteFile(magitRepo + "\\" + Environment.BRANCHES + "\\" + branchName + ".txt") &&
                    new File(magitRepo + "\\" + Environment.BRANCHES + "\\" + branchName + ".txt").exists()) {
                throw new FailedToCreateBranchException("Failed to delete the branch file");
            }
            else
                branchesManager.deleteBranch(branchName);
        }
        else
            throw new FailedToCreateBranchException("Error! Branch name doesn't exists");
    }

    protected void initSha1ToCommitMap() throws IOException, ParserConfigurationException {
        if(!firstCommit) {
            for (Branch branch : branchesManager.getBranches()) {
                getCommitsRecursively(branch.getHead().createHashCode());
            }
        }
    }

    public List<Map<String,String>> commitHistory() throws FailedToCreateRepositoryException {
        if(magitRepo == null)
            throw new FailedToCreateRepositoryException("Repository is empty. Please create a new one or activate");
        String currentCommit = branchesManager.getActive().getHead().createHashCode();
        List<Map<String,String>> commitList = new TreeList<>();
        getCommitHistoryRecursively(currentCommit, commitList);
        return commitList;
    }

    private void getCommitsRecursively(String commitSha1) throws IOException, ParserConfigurationException {
        File commit = new File(magitRepo + "\\" + Environment.OBJECTS + "\\" + commitSha1);
        if(commit.exists()){
            Commit commitObj = new Commit();
            commitObj.parseCommitFile(new File(magitRepo + "\\" + Environment.OBJECTS + "\\" + commitSha1));
            if(commitObj.getPreviousCommit() != null)
                getCommitsRecursively(commitObj.getPreviousCommit());
            commitMapSha1.put(commitObj.createHashCode(), commitObj);
        }
    }

    private void getCommitHistoryRecursively(String commitSha1, List<Map<String,String>> commitList){
        File commit = new File(magitRepo + "\\" + Environment.OBJECTS + "\\" + commitSha1);
        if(commit.exists()){
            Commit commitObj = commitMapSha1.get(commitSha1);
            Map<String,String> mapCommit = new HashMap<>();
            mapCommit.put("SHA1", commitSha1);
            mapCommit.put("Message", commitObj.getMessage());
            mapCommit.put("Creation Data", commitObj.getCreationDate());
            mapCommit.put("Created By", commitObj.getCreatedBy());
            commitList.add(mapCommit);
            if(commitObj.getPreviousCommit() != null)
                getCommitHistoryRecursively(commitObj.getPreviousCommit(), commitList);
        }
    }

    public List<WCFileNode> createFilesTree() throws ParserConfigurationException, IOException, FailedToCreateRepositoryException {
        String commitSha1 = branchesManager.getActive().getHead().createHashCode();
        String rootSha1 = commitMapSha1.get(commitSha1).getRootSha1();
        List<WCFileNode> tree = new ArrayList<>();
        final List<WCFileNode>[] rootLevel = new List[]{tree};
        tree.add(new WCFileNode(repositoriesManager.getActive().getName(), repositoriesManager.getActive().getPath()));
        rootLevel[0] = tree.get(0).getNodes();
        ActionsOnObjectsInterface actionInterface = (obj, path) ->
        {
            WCFileNode node = new WCFileNode(obj[fileNameIndex], findPath(obj[sha1Index], rootSha1));
            rootLevel[0].add(node);
            //setFileStatus(node);
            if(obj[fileTypeIndex].equals(FileType.DIRECTORY.toString())){
                rootLevel[0] = rootLevel[0].get(rootLevel[0].size() - 1).getNodes();
            }
        };

        recursiveRunOverObjectFiles(rootSha1, magitRepo + "\\" + Environment.OBJECTS, actionInterface, magitRepo + "\\" + Environment.OBJECTS, magitRepo + "\\" + Environment.OBJECTS);
        /*repositoriesManager.getActive().getWorkingCopy().initRootFolder(wcRootPath, username);
        WorkingCopy wc = repositoriesManager.getActive().getWorkingCopy();
        getWCJSON(wc.getRootFolder(), rootLevel);*/
        return tree;
    }

//    private void setFileStatus(WCFileNode node) throws IOException {
//        List<String> edited = getEditedFiles();
//        List<String> newFiles = getNewFiles();
//        List<String> deleted = getDeletedFiles();
//        if(edited.contains(node.getFilePath()))
//            node.setFileStatus(PRStatus.MODIFIED);
//        else if(deleted.contains(node.getFilePath()))
//            node.setFileStatus(PRStatus.DELETED);
//        else if(newFiles.contains(node.getFilePath()))
//            node.setFileStatus(PRStatus.NEW);
//    }
//
//    private String getFilePathInWC(String fileName){
//        WorkingCopy wc = repositoriesManager.getActive().getWorkingCopy();
//        for(String path : wc.getWcFiles()){
//            File f = new File(path);
//            String name = f.getName();
//            if(name.equals(fileName))
//                return path;
//        }
//        return null;
//    }

    public  List<Map<String, String>> commitFilesDetails (String commitSha1) throws FailedToCreateRepositoryException, IOException, ParserConfigurationException {
        if(branchesManager.getActive().getHead().getRootSha1() == null && !firstCommit)
            throw new FailedToCreateRepositoryException("No previous commits were detected");
        //String rootSha1 = branchesManager.getActive().getHead().getRootSha1();
        if(!commitMapSha1.containsKey(commitSha1)){
            throw new FailedToCreateRepositoryException("Commit sha1 was not found");
        }

        String rootSha1 = commitMapSha1.get(commitSha1).getRootSha1();
        List<Map<String, String>> filesData = new ArrayList<>();

        if(firstCommit)
            return filesData;

        ActionsOnObjectsInterface actionInterface = (obj, path) ->
        {
            Map<String, String> newItem = new HashMap<>();
            newItem.put("File Path", obj[fileNameIndex]);
            newItem.put("Type", obj[fileTypeIndex]);
            newItem.put("SHA1", obj[sha1Index]);
            newItem.put("Last Modifier", obj[modifiedByIndex]);
            newItem.put("Modification Date", obj[modificationDateIndex]);
            filesData.add(newItem);
        };

        recursiveRunOverObjectFiles(rootSha1, magitRepo + "\\" + Environment.OBJECTS, actionInterface, magitRepo + "\\" + Environment.OBJECTS, magitRepo + "\\" + Environment.OBJECTS);

        for(Map<String , String> item : filesData){
            String path = findPath(item.get("SHA1"), rootSha1);
            item.put("File Path", path);
        }

        return filesData;
    }

    public static String findPath(String sha1, String rootSha1){
        List<String> pathList = new ArrayList<>();
        Map<Integer, String> indexToSha1 = new HashMap<>();
        pathList.add(wcRootPath);
        int index = searchFile(sha1, rootSha1, pathList, 0);
        if(index != -1) {
            return pathList.get(index);
        }

        return null;
    }

    private static int searchFile(String sha1, String rootSha1, List<String> path, int index) {
        File root = new File(magitRepo + "\\" + Environment.OBJECTS + "\\" + rootSha1);
        if (root.exists()) {
            List<String[]> files = Utils.createObjectsFromFile(root, ",");
            for (String[] obj : files) {
                if (obj[sha1Index].equals(sha1)) {
                    String pathAppend = path.get(index) + "\\" + obj[fileNameIndex];
                    path.add(index, pathAppend);
                    return index;
                }
            }
            for (String[] obj : files) {
                if (obj[fileTypeIndex].equals(FileType.DIRECTORY.toString())) {
                    String pathAppend = path.get(index) + "\\" + obj[fileNameIndex];
                    path.add(index, pathAppend);
                    if(obj[sha1Index].equals(sha1))
                        return index;
                    int val = searchFile(sha1, obj[sha1Index], path, index);
                    if(val != -1)
                        return val;
                    else index++;
                }
            }
        }

        return -1;
    }

    public String resetBranch(String newCommitSha1, boolean toDeleteChanges) throws FailedToCreateBranchException, IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        if (magitRepo == null)
            throw new FailedToCreateRepositoryException("Error! Repository is empty. Please activate a repository or create a new one");

        File commitObj = new File(magitRepo + "\\" + Environment.OBJECTS + "\\" + newCommitSha1);
        if (commitObj.exists()) {
            if (!commitMapSha1.containsKey(newCommitSha1)) {
                return "ERROR: The given SHA1 doesn't represent commit file";
            }
            if (!getChanges() && !toDeleteChanges) {
                throw new FailedToCreateBranchException("There are open changes in the WC");
            } else { // replace commit file in WC
                branchesManager.getActive().setHead(commitMapSha1.get(newCommitSha1));
                Utils.writeFile(new File(magitRepo + "\\" + Environment.BRANCHES + "\\" + branchesManager.getActive().getName() + ".txt"), newCommitSha1);
                    Utils.deleteDirectory(new File(wcRootPath));
                branchFilesToWC();
            }
        } else {
            return "ERROR: The given SHA1 was not found in magit objects folder";
        }

        return "Branch reset was performed successfully!";
    }

    public static String getParentCommit(String childCommit) {
        if(commitMapSha1.containsKey(childCommit)) {
            return commitMapSha1.get(childCommit).getPreviousCommit();
        }

        return null;
    }

    public MergeResult merge(String branchName) throws IOException, ParserConfigurationException, FailedToMergeException {
        mergeEngine = new MergeHandler(commitMapSha1);
        if(branchesManager.isBranchExists(branchName)) {
            return mergeEngine.merge(branchesManager.getActive(), branchesManager.getBranch(branchName));
        }
        else {
            throw new FailedToMergeException("No branch with the name [" + branchName + "]");
        }
    }

    public List<String> getConflicts() {
        if(mergeEngine.areThereConflicts()) {
            return mergeEngine.getConflictsFiles();
        }
        return null;
    }

    public Map<String, String> getConflictFilesContent(String file) throws IOException {
        return mergeEngine.getConflictFilesContent(file);
    }

    public void solveConflict(String file, String content) throws IOException, FailedToCreateRepositoryException {
        mergeEngine.solveConflict(file, content);
    }

    public static String getFileContent(String sha1) {
        String content = "";
        try{
            if(sha1 != null){
                File file = new File(Engine.getMagitRepo() + "\\" + Environment.OBJECTS + "\\" + sha1);
                content = Utils.readFile(file);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return content;
    }

    public String commitOpenChanges(String message, String branchName) throws IOException, FailedToMergeException, ParserConfigurationException, FailedToCreateRepositoryException {
        if(mergeEngine.areThereConflicts()){
            throw new FailedToMergeException("There are conflicts in the WC, please solve them before committing");
        }
        mergeEngine.updateWCwithOpenChanges();
        return commitMerge(message, branchName);
    }

    private String commitMerge(String message, String branchName) throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        return commit(message, branchesManager.getBranch(branchName).getHead());
    }

    protected void initCollaborationHandler(){
        if(collaborationHandler == null)
            collaborationHandler = new CollaborationHandler(repositoriesManager, this);
    }

    public void clone(String remotePath, String localPath, String name) throws Exception {
        initCollaborationHandler();
        String remoteName = repositoriesManager.getRepositoryByPath(remotePath) == null ?
                new File(remotePath).getName() : repositoriesManager.getRepositoryByPath(remotePath).getName();
        initNewRepository(remoteName, remotePath, false, false);
        collaborationHandler.clone(remotePath, localPath, name);
        repositoriesManager.setActive(collaborationHandler.getLocalRepository());
        if(!repositoriesManager.getActive().isLocalRepository())
            repositoriesManager.getActive().setRepositoryReference(remotePath);
    }

    public void fetch() throws Exception {
        if(collaborationHandler == null)
            throw new Exception("Please clone before fetching");
        collaborationHandler.fetch();
    }

    public boolean pull() throws Exception {
        if(collaborationHandler == null)
            throw new Exception("Please clone before pulling");
        boolean openChanges = getChanges();
        if(openChanges)
            collaborationHandler.pull();
        return openChanges;
    }

    public boolean push() throws IOException, ParserConfigurationException {
        if(collaborationHandler.getOpenChangesInRR()) {
            try {
                return collaborationHandler.push();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (FailedToCreateRepositoryException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void checkoutWithRTB(String branch) throws IOException, FailedToCreateRepositoryException, FailedToCreateBranchException, ParserConfigurationException {
        collaborationHandler.remoteTrackingBranchFromRemoteBranch(branch);
        checkout(branchesManager.getActive().getName(), false);
    }

    public void pushToRR() throws Exception {
        if(collaborationHandler == null)
            throw new Exception("Collaboration Handler is empty, is this a forked repository?");
        collaborationHandler.pushBranchToRR(branchesManager.getActive().getName());
    }

    private Map<String, String> getCommitPathToSha1(Commit commit) throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        Map<String, String> pathToSha1 = new HashMap<>();

        ActionsOnObjectsInterface actionInterface = (obj, path) ->
        {
            if (obj[fileTypeIndex].equals(FileType.FILE.toString())) {
                File blobFile = new File(magitRepo + "\\" + Environment.OBJECTS + "\\" + obj[sha1Index]);
                String content = Utils.readFile(blobFile);
                Blob blob = new Blob(content);
                pathToSha1.put(path + "\\" + obj[fileNameIndex], blob.createHashCode());
            }
        };

        recursiveRunOverObjectFiles(commit.getRootSha1(), repositoriesManager.getActive().getPath(),
                actionInterface, magitRepo + "\\" + Environment.OBJECTS,
                magitRepo + "\\" + Environment.OBJECTS);

        return pathToSha1;
    }

    public CommitDelta getDeltaToPreviousCommit(Commit commit) throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        Map<String, String> pathToSha1CurrentCommit = getCommitPathToSha1(commit);
        return prevCommitDelta(commit, pathToSha1CurrentCommit);
    }

    private CommitDelta prevCommitDelta(Commit rootCommit, Map<String, String> pathToSha1CurrentCommit)
            throws ParserConfigurationException, IOException, FailedToCreateRepositoryException {
        Commit prevCommit = null;
        Commit secondPrevCommit = null;
        CommitDelta prevCommitDelta = null;
        if(rootCommit.getPreviousCommit() != null && !rootCommit.getPreviousCommit().equals("null")) {
            prevCommit = getCommitObject(rootCommit.getPreviousCommit());
        }
        if(!rootCommit.getSecondPreviousCommit().equals("")) {
            secondPrevCommit = getCommitObject(rootCommit.getSecondPreviousCommit());
        }
        if(prevCommit != null) {
            Map<String, String> pathToSha1PreviousCommit = getCommitPathToSha1(prevCommit);
            Map<String, String> pathToSha1SecondPrevCommit = new HashMap<>();
            if(secondPrevCommit != null) {
                prevCommitDelta = new CommitDelta(rootCommit.getPreviousCommit(), rootCommit.getSecondPreviousCommit());
                pathToSha1SecondPrevCommit = getCommitPathToSha1(secondPrevCommit);

                List<String> deletedFilePath = getDeletedFiles(pathToSha1CurrentCommit, pathToSha1SecondPrevCommit);
                prevCommitDelta.setDeletedFilesSecondCommit(getSha1ToPathMap(deletedFilePath, pathToSha1CurrentCommit));

                List<String> editedFilePath = getEditedFiles(pathToSha1CurrentCommit, pathToSha1SecondPrevCommit);
                prevCommitDelta.setEditedFiledSecondCommit(getSha1ToPathMap(editedFilePath, pathToSha1CurrentCommit));

                List<String> newFilesPath = getNewFiles(pathToSha1CurrentCommit, pathToSha1SecondPrevCommit);
                prevCommitDelta.setNewFilesSecondCommit(getSha1ToPathMap(newFilesPath, pathToSha1CurrentCommit));
            }
            else
                prevCommitDelta = new CommitDelta(rootCommit.getPreviousCommit());
            List<String> deletedFilePath = getDeletedFiles(pathToSha1CurrentCommit, pathToSha1PreviousCommit);
            prevCommitDelta.setDeletedFiles(getSha1ToPathMap(deletedFilePath, pathToSha1CurrentCommit));

            List<String> editedFilePath = getEditedFiles(pathToSha1CurrentCommit, pathToSha1PreviousCommit);
            prevCommitDelta.setEditedFiled(getSha1ToPathMap(editedFilePath, pathToSha1CurrentCommit));

            List<String> newFilesPath = getNewFiles(pathToSha1CurrentCommit, pathToSha1PreviousCommit);
            prevCommitDelta.setNewFiles(getSha1ToPathMap(newFilesPath, pathToSha1CurrentCommit));        }

        return prevCommitDelta;
    }

    private Map<String, String> getSha1ToPathMap(List<String> filePaths, Map<String, String> pathToSha1CurrentCommit){
        Map<String, String> sha1ToPathMap = new HashMap<>();
        for(String path : filePaths){
            String sha1 = pathToSha1CurrentCommit.get(path);
            sha1ToPathMap.put(sha1, path);
        }

        return sha1ToPathMap;
    }

    private Commit getCommitObject(String sha1) throws IOException, ParserConfigurationException {
        Commit commit = new Commit();
        File prevCommitFile = new File(magitRepo + "\\" + Environment.OBJECTS + "\\" + sha1);
        if(commitMapSha1.containsKey(sha1)){
            commit = commitMapSha1.get(sha1);
        }
        else if(prevCommitFile.exists()){
            commit.parseCommitFile(prevCommitFile);
        }
        return commit;
    }

    private List<String> getNewFiles(Map<String, String> currentCommit, Map<String, String> prevCommit) throws IOException {
        return (List<String>) CollectionUtils.subtract(currentCommit.keySet(), prevCommit.keySet());
    }

    private List<String> getDeletedFiles(Map<String, String> currentCommit, Map<String, String> prevCommit) throws IOException {
        return (List<String>) CollectionUtils.subtract(prevCommit.keySet(), currentCommit.keySet());
    }

    private List<String> getEditedFiles(Map<String, String> currentCommit, Map<String, String> prevCommit) throws IOException {
        return compareFiles(currentCommit, prevCommit);
    }

    public String getRemoteRepositoryOwner() throws Exception {
        if(collaborationHandler == null)
            throw new Exception("Collaboration handler is not initialized!");
        return collaborationHandler.getRemoteRepositoryOwner();
    }

    public void deltaCommitPR(PullRequest PR) throws IOException, ParserConfigurationException, FailedToCreateRepositoryException {
        String base = PR.getBaseBranch();
        String target = PR.getTargetBranch();
        Map<String, String> newFiles = new HashMap<>();
        Map<String, String> deletedFiles = new HashMap<>();
        Map<String, String> editedFiles = new HashMap<>();
        Map<String, String> sha1ToContent = new HashMap<>();
        getCommitDelta(base, target, newFiles, deletedFiles, editedFiles, sha1ToContent);
        PR.setCommitDelta(newFiles, deletedFiles, editedFiles, sha1ToContent);
    }

    private void getCommitDelta(String base, String target, Map<String, String> newFiles,
                                Map<String, String> deletedFiles,Map<String, String> editedFiles,
                                Map<String, String> sha1ToContent) throws ParserConfigurationException, IOException, FailedToCreateRepositoryException {
        Commit targetCommit = branchesManager.getBranch(target).getHead();
        Commit baseCommit = branchesManager.getBranch(base).getHead();
        while(!targetCommit.createHashCode().equals(baseCommit.createHashCode())) {
            String t = targetCommit.createHashCode();
            String b = baseCommit.createHashCode();
            CommitDelta deltaCommit = getDeltaToPreviousCommit(targetCommit);
            if (deltaCommit != null) {
                addMapToMap(newFiles, deltaCommit.getNewFiles());
                addMapToMap(newFiles, deltaCommit.getNewFilesSecondCommit());
                addMapToMap(editedFiles, deltaCommit.getEditedFiled());
                addMapToMap(editedFiles, deltaCommit.getEditedFiledSecondCommit());
                addMapToMap(deletedFiles, deltaCommit.getDeletedFiles());
                addMapToMap(deletedFiles, deltaCommit.getDeletedFilesSecondCommit());
            }
            if (commitMapSha1.containsKey(targetCommit.getPreviousCommit()))
                targetCommit = commitMapSha1.get(targetCommit.getPreviousCommit());
            else
                break;
        }
        setSha1ToContent(newFiles, editedFiles, sha1ToContent);
    }

    private void addMapToMap(Map<String, String> origMap, Map<String, String> mapToCopy){
        if(mapToCopy != null && origMap != null) {
            for (Map.Entry<String, String> entry : mapToCopy.entrySet()) {
                String pathWithoutPrefix = entry.getValue().substring(entry.getValue().lastIndexOf(username) + username.length() + 1);
                origMap.put(entry.getKey(), pathWithoutPrefix);
            }
        }
    }

    private void setSha1ToContent(Map<String, String> newFiles,
                                  Map<String, String> editedFiles,  Map<String, String> sha1ToContent){
        if(newFiles != null) {
            for (Map.Entry<String, String> entry : newFiles.entrySet()) {
                String content = getFileContent(entry.getKey());
                sha1ToContent.put(entry.getKey(), content);
            }
        }

        if(editedFiles != null) {
            for (Map.Entry<String, String> entry : editedFiles.entrySet()) {
                String content = getFileContent(entry.getKey());
                sha1ToContent.put(entry.getKey(), content);
            }
        }
    }
}
