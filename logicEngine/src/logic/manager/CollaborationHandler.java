package logic.manager;
import logic.manager.Exceptions.FailedToCreateBranchException;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Managers.RepositoryManager;
import logic.modules.*;
import com.sun.deploy.net.FailedDownloadException;
import org.apache.commons.io.FilenameUtils;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollaborationHandler {

    private static final String DELIMITER = ",";
    private static final int REMOTE_LOCATION_INDEX = 0;
    private static final int REMOTE_NAME_INDEX = 1;
    private Repository localRepository;
    private Repository remoteRepository;
    private RepositoryManager repoManager;
    private Engine engine;

    public CollaborationHandler(RepositoryManager repoManager, Engine engine) {
        this.repoManager = repoManager;
        this.engine = engine;
        remoteRepository = null;
        localRepository = null;
    }

    public Repository getLocalRepository(){
        return localRepository;
    }

    public void setRemoteRepository(Repository repository) throws IOException, FailedToCreateRepositoryException {
        if(!new File(repository.getPath()).exists())
            throw new FailedToCreateRepositoryException("Remote repository doesn't exist on files system");
        remoteRepository = repository;
        localRepository.setRepositoryReference(remoteRepository.getPath());
        createRemoteFile();
    }

    public void setLocalRepository(Repository repository){
        localRepository = repository;
    }

    public void clone(String remotePath, String localPath, String repoName) throws Exception {
        File magit = new File(remotePath + "\\" + Environment.MAGIT);
        if (!magit.exists())
            throw new FailedToCreateRepositoryException("Failed to clone since .magit folder " +
                    "doesn't exist in: " + remotePath);
        //create a FileFilter and override its accept-method
        FileFilter excludeBranchFileFilter = new FileFilter() {
            public boolean accept(File file) {
                try {
                    if (file.getCanonicalPath().contains(".magit\\branches")) {
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        Utils.copyDirectory(remotePath, localPath, excludeBranchFileFilter);
        remoteRepository = repoManager.getRepositoryByPath(remotePath);
        if(repoManager.getRepositoryByPath(localPath) == null) {
            localRepository = new Repository(repoName, localPath, "", engine.getUsername());
            repoManager.addItem(localRepository);
        }
        else
            localRepository = repoManager.getRepositoryByPath(localPath);
        createRemoteBranch(false);
        createRemoteTrackingBranch(getHeadBranch(remoteRepository.getPath()));
        createRemoteFile();
        initNewRepository(localRepository.getPath());
        repoManager.setActive(localRepository);
        localRepository.setRepositoryReference(remotePath);
    }

    public void updateBranches() throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        for(Branch branch : engine.getBranchesManager().getBranches()){
            if(!branch.getName().contains(remoteRepository.getName())) {
                branch.setTracking(remoteRepository.getName() + "\\" + branch.getName());
                branch.setNotRemote();
            }
            else
                branch.remoteTracking();
        }
        updateRemoteBranches();
    }

    private void updateRemoteBranches() throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        File remoteFolder = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" +
                Environment.BRANCHES + "\\" + remoteRepository.getName());
        if(remoteFolder.exists()) {
            File[] remoteBranches = remoteFolder.listFiles();
            for(File branch : remoteBranches){
                String fileNameWithOutExt = remoteRepository.getName() + "\\" + FilenameUtils.removeExtension(branch.getName());
                if(engine.getBranchesManager().getBranch(fileNameWithOutExt) != null){
                    engine.getBranchesManager().getBranch(fileNameWithOutExt).setIsRemote();
                }
                else{
                    createNewBranch(fileNameWithOutExt, remoteFolder.getCanonicalPath());
                }
            }
        }
    }

    private void createRemoteFile() throws IOException, FailedToCreateRepositoryException {
        File remoteLocation = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + engine.REMOTE_LOCATION);
        if(remoteLocation.exists()) {
            String remoteDetails = Utils.readFile(remoteLocation);
            String[] remoteLocationAndName = remoteDetails.split(DELIMITER);
            if(remoteLocationAndName[REMOTE_LOCATION_INDEX].equals(remoteRepository.getPath())){
                remoteRepository.setName(remoteLocationAndName[REMOTE_NAME_INDEX]);
            }
            else{
                throw new FailedToCreateRepositoryException("The repository already contains a different" +
                        " remote repository: [" + remoteLocationAndName[REMOTE_LOCATION_INDEX] + "]");
            }
        }
        else
            Utils.createTxtFile(remoteLocation.getParent(), engine.REMOTE_LOCATION, remoteRepository.getPath() + "," + remoteRepository.getName());
    }

    private void createNewBranch(String branch, String branchPath) throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        File remoteBranchFile = new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" +
                Environment.BRANCHES + "\\" + branch + ".txt");
        String commitSha1 = Utils.readFile(remoteBranchFile);
        Utils.createTxtFile(branchPath, branch, commitSha1);
        Commit headCommit = new Commit();
        if(engine.getCommitMapSha1().containsKey(commitSha1))
            headCommit = engine.getCommitMapSha1().get(commitSha1);
        else
        headCommit.parseCommitFile(new File(remoteRepository.getPath() + "\\" + Environment.MAGIT +
                "\\" + Environment.OBJECTS + "\\" + commitSha1));
        engine.getBranchesManager().addItem(new Branch(headCommit, remoteRepository.getName() + "\\" + branch, true));
    }

    public void createRemoteTrackingBranch(String headBranch) throws IOException, FailedToCreateRepositoryException {
        String commitSha1 = engine.getBranchesManager().getBranch(remoteRepository.getName() + "\\" + headBranch).getHead().createHashCode();
        createRTB(headBranch, commitSha1);
    }

    public void remoteTrackingBranchFromRemoteBranch(String headBranch) throws IOException, FailedToCreateRepositoryException {
        String commitSha1 = engine.getBranchesManager().getBranch(headBranch).getHead().createHashCode();
        String branchName = headBranch.substring(headBranch.indexOf("\\")+1);
        createRTB(branchName, commitSha1);
    }

    private void createRTB(String headBranch, String commitSha1) throws IOException, FailedToCreateRepositoryException {
        Utils.createTxtFile(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES, headBranch, commitSha1);
        Utils.createTxtFile(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES, "head", headBranch);
        engine.getBranchesManager().addItem(new Branch(engine.getCommitMapSha1().get(commitSha1), headBranch));
        engine.getBranchesManager().getBranch(headBranch).setTracking(remoteRepository.getName());
        engine.getBranchesManager().setActive(engine.getBranchesManager().getBranch(headBranch));
    }

    public void fetch() throws Exception {
        compareFilesWC();
        getNewObjectFiles();
        createRemoteBranch(true);
        createRemoteTrackingBranch(getHeadBranch(localRepository.getPath()));
        engine.setCurrentCommitSha1Files();
    }

    private void createRemoteBranch(boolean isFetch) throws Exception {
        Stream<Path> walk = Files.walk(Paths.get(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES));
        List<Path> remoteBranches = walk.filter(Files::isRegularFile).collect(Collectors.toList());
        String branchPath = localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES + "\\" + remoteRepository.getName();
        for (Path branch : remoteBranches) {
                String fileNameWithOutExt = FilenameUtils.removeExtension(branch.getFileName().toString());
                if (!fileNameWithOutExt.equals("head")) {
                    if (engine.getBranchesManager().getBranch(remoteRepository.getName() + "\\" + fileNameWithOutExt) == null) {
                        String commitHead = Utils.readFile(new File(remoteRepository.getPath() + "\\" + Environment.MAGIT +
                                "\\" + Environment.BRANCHES + "\\" + fileNameWithOutExt + ".txt"));
                        createNewBranch(fileNameWithOutExt, branchPath);
                    } else {
                        String remoteCommitSha1 = engine.getBranchesManager().getBranch(remoteRepository.getName() + "\\" + fileNameWithOutExt).createHashCode();
                        String localCommitSha1 = Utils.readFile(new File(branchPath + "\\" + branch.getFileName().toString()));
                        if (!remoteCommitSha1.equals(localCommitSha1)) {
                            Utils.deleteFile(branchPath + "\\" + branch.getFileName().toString());
                            createNewBranch(fileNameWithOutExt, branchPath);
                        }
                    }
                }
            else {
                if(!isFetch)
                    handleHead();
            }
        }

        if(!isFetch) {
            for (Path branch : remoteBranches) {
                String fileNameWithOutExt = FilenameUtils.removeExtension(branch.getFileName().toString());
                if (!fileNameWithOutExt.equals("head"))
                    engine.getBranchesManager().deleteBranch(fileNameWithOutExt);
            }
        }
    }

    private void handleHead() throws IOException, FailedToCreateRepositoryException {
        File remoteHead = new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES + "\\" + "head.txt");
        File localHead = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES + "\\" + "head.txt");
        String headBranch = Utils.readFile(remoteHead);
        if (localHead.exists()) {
            String head = Utils.readFile(localHead);
            if (!head.equals(Utils.readFile(remoteHead))) {
                Utils.deleteFile(localHead.getCanonicalPath());
                Utils.createTxtFile(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES, "head", headBranch);
            }
        } else {
            Utils.createTxtFile(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES, "head", headBranch);
        }
    }

    public boolean getOpenChangesInRR() throws IOException, ParserConfigurationException {
        remoteRepository.getWorkingCopy().initRootFolder(remoteRepository.getPath(), remoteRepository.getUsername());
        String currentSha1 = remoteRepository.getWorkingCopy().getRootFolder().createHashCode();
        String remoteRoot = getRootCommit(remoteRepository).getRootSha1();
        return remoteRoot.equals(currentSha1);
    }

    private void compareFilesWC() throws IOException, FailedToCreateRepositoryException {
        Stream<Path> walkRemote = Files.walk(Paths.get(remoteRepository.getPath()));
        List<String> filesRemote = walkRemote.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());
        for(String f : filesRemote) {
            if (!f.contains(".magit")) {
                String localPath = localRepository.getPath() + "\\" + f.replace(remoteRepository.getPath(), "");
                File remote = new File(f);
                File local = new File(localPath);
                if (remote.exists() && local.exists()) {
                    if (!isSha1Equal(local, remote)) {
                        Utils.deleteFile(localPath);
                        writeNewFileToWC(local, remote);
                    }
                } else if (remote.exists() && !local.exists()) {
                    writeNewFileToWC(local, remote);
                }
            }
        }
    }

    private void writeNewFileToWC(File local, File remote) throws IOException, FailedToCreateRepositoryException {
        if(remote.isFile())
            Utils.writeFile(local, Utils.readFile(remote));
        else
            Utils.createNewDirectory(local.getParent(), local.getName());
        String sha1 = getSha1(remote);
        if(!new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS +
                "\\" + sha1).exists()) {
            Files.copy(new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS +
                            "\\" + sha1).toPath(),
                    new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS +
                            "\\" + sha1).toPath());
        }

    }

    private boolean isSha1Equal(File file1, File file2) throws IOException {
        String firstSha1 = Utils.createGitFile(file1, engine.getUsername()).createHashCode();
        String secondSha1 = Utils.createGitFile(file2, engine.getUsername()).createHashCode();
        return firstSha1.equals(secondSha1);
    }

    private String getSha1(File file) throws IOException {
        return Utils.createGitFile(file, engine.getUsername()).createHashCode();
    }

    private String getCurrentBranchFiles() throws IOException, ParserConfigurationException, FailedToCreateRepositoryException {
        String head = getHeadBranch(localRepository.getPath());
        Commit localHeadCommit = engine.getBranchesManager().getBranch(remoteRepository.getName() + "\\" + head).getHead();
        String remoteCommitSha1 = Utils.readFile(new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES + "\\" + head + ".txt"));
        Commit remoteHeadCommit = new Commit();
        remoteHeadCommit.parseCommitFile(new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + remoteCommitSha1));
        while(!remoteHeadCommit.createHashCode().equals(localHeadCommit.createHashCode())){
            engine.getCommitMapSha1().put(remoteHeadCommit.createHashCode(), remoteHeadCommit);
            if(!remoteHeadCommit.getRootSha1().equals(localHeadCommit.getRootSha1())){
                File objInLocal = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + remoteHeadCommit.createHashCode());
                if(!objInLocal.exists()) {
                    // copy commit obj
                    Files.copy(new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + remoteHeadCommit.createHashCode()).toPath(),
                            objInLocal.toPath());
                    // copy root folder obj
                    if(!new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + remoteHeadCommit.getRootSha1()).exists()) {
                        Files.copy(new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + remoteHeadCommit.getRootSha1()).toPath(),
                                new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + remoteHeadCommit.getRootSha1()).toPath());
                        Folder root = new Folder("");
                        root.parseFolderFile(new File(remoteRepository.getPath() + "\\" +
                                Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + remoteHeadCommit.getRootSha1()));
                        String localObjPath = localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\";
                        calcDelta(root, localRepository.getPath(), localObjPath);
                    }
                }
            }
//            else
//                break;
            remoteHeadCommit.parseCommitFile(new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS +
                    "\\" + remoteHeadCommit.getPreviousCommit()));
        }
        return remoteCommitSha1;
    }

    private void calcDelta(Folder root, String path, String objPath) throws IOException, FailedToCreateRepositoryException, ParserConfigurationException {
        for(Map.Entry<String, FileData> fd : root.getIncludedFiles().entrySet()){
            File objFile = new File(objPath + fd.getValue().getSha1());
            if(!objFile.exists()){
                File inRemote = new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + fd.getValue().getSha1());
                Files.copy((inRemote).toPath(), objFile.toPath());
                //Utils.createZipFile(objPath, fd.getValue().getSha1(), fd.getValue().getFilePointer().getContent());
                if(fd.getValue().getType() == FileType.DIRECTORY) {
                    if(!new File(path + "\\" + fd.getValue().getName()).exists())
                        Utils.createNewDirectory(path, fd.getValue().getName());
                    Folder newRoot = new Folder(fd.getValue().getName());
                    newRoot.parseFolderFile(inRemote);
                    calcDelta(newRoot, path + "\\" + fd.getValue().getName(), objPath);
                }
                else{
                    Utils.writeFile(new File(path + "\\" + fd.getValue().getName()), fd.getValue().getFilePointer().getContent());
                }
            }
        }
    }

    private void getNewObjectFiles() throws IOException {
        File remoteObj = new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS);
        File[] objInRemote = remoteObj.listFiles();
        for(File file : objInRemote){
            String fileName = file.getName();
            File inLocal = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" +
                    fileName);
            if(!inLocal.exists()){
                Files.copy(file.toPath(), inLocal.toPath());
            }
        }
    }

    public void pull() throws Exception {
        String head = getHeadBranch(localRepository.getPath());
        Branch headBranch = engine.getBranchesManager().getBranch(head);
        if(isLRPushed()) {
            if (headBranch != null) {
                if (headBranch.getTracking()) { // won't pull if the head branch is not RTB
                    String commitSha1 = getCurrentBranchFiles();
                    updateRBandRBT(commitSha1);
                    engine.setCurrentCommitSha1Files();
                }
            } else {
                throw new Exception("Head branch doesn't exists... Something went wrong");
            }
        }
        else{
            throw new Exception("Can't pull since there are changes in local repository which weren't pushed");
        }
    }

    private boolean isLRPushed() throws IOException, ParserConfigurationException {
        String localCommit = engine.getBranchesManager().getActive().getHead().createHashCode();
        File remoteCommit = new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + localCommit);
        return remoteCommit.exists();
    }

    private String getHeadBranch(String path) throws IOException {
        return Utils.readFile(new File(path + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES +
                "\\" + "head.txt"));
    }

    private void updateRBandRBT(String headCommitSha1) throws IOException, FailedToCreateRepositoryException {
        String headBranch = getHeadBranch(localRepository.getPath());
        File RBFile = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" +
                Environment.BRANCHES + "\\" + headBranch + ".txt");
        Utils.writeFile(RBFile, headCommitSha1);
        File RBTFile = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" +
                Environment.BRANCHES + "\\" + remoteRepository.getName() + "\\" + headBranch + ".txt");
        Utils.writeFile(RBTFile, headCommitSha1);
        Commit headCommit = engine.getBranchesManager().getBranch(headBranch).getHead();
        engine.getBranchesManager().getBranch(remoteRepository.getName() + "\\" + headBranch).setHead(headCommit);
    }

    public boolean push() throws IOException, ParserConfigurationException, FailedToCreateRepositoryException {
        if(!isLRSyncWithRR()){
            return false;
        }
        pushToRemote();
        updateBranchesAfterPush();
        engine.setCurrentCommitSha1Files();
        return true;
    }

    private void pushToRemote() throws IOException, ParserConfigurationException, FailedToCreateRepositoryException {
        String headBranch = getHeadBranch(localRepository.getPath());
        String rtbCommit = Utils.readFile(new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" +
                Environment.BRANCHES + "\\" + headBranch + ".txt"));
        String commitFile = localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + rtbCommit;
        File commitZip = new File(commitFile);
        if (commitZip.exists() && commitZip.isFile()) {
            Commit lrCommit = new Commit();
            lrCommit.parseCommitFile(commitZip);
            File rootFile = new File(remoteRepository.getPath());
            if(rootFile.exists())
                Utils.deleteDirectory(rootFile);
            File remoteFile = new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + commitZip.getName());
            if(!remoteFile.exists()) {
                Files.copy(commitZip.toPath(), (remoteFile).toPath());
            }
            ActionsOnObjectsInterface action = engine.createFilesFromObjects();
            String rootSha1 = lrCommit.getRootSha1();
            engine.recursiveRunOverObjectFiles(rootSha1, remoteRepository.getPath(), action,
                    localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS,
                    remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS);
            repoManager.getActive().getWorkingCopy().initRootFolder(repoManager.getActive().getPath(), engine.getUsername());
            engine.setCurrentCommitSha1Files();
        }
    }

    private void updateBranchesAfterPush() throws IOException, FailedToCreateRepositoryException {
        String headBranch = getHeadBranch(localRepository.getPath());
        File RBFile = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" +
                Environment.BRANCHES + "\\" + remoteRepository.getName() + "\\" + headBranch + ".txt");
        String commitSha1 = Utils.readFile(new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES + "\\" + headBranch + ".txt"));
        Utils.writeFile(RBFile, commitSha1);
        File branchOnRemote = new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" +
                Environment.BRANCHES + "\\" + headBranch + ".txt");
        Utils.writeFile(branchOnRemote, commitSha1);
    }

    private Commit getRootCommit(Repository repo) throws IOException, ParserConfigurationException {
        String headBranch = getHeadBranch(repo.getPath());
        String commitSha1 = Utils.readFile(new File(repo.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES + "\\" + headBranch + ".txt"));
        Commit commit = new Commit();
        commit.parseCommitFile(new File(repo.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + commitSha1));
        return commit;
    }

    private boolean isLRSyncWithRR() throws IOException {
        String localHead = getHeadBranch(localRepository.getPath());
        String remoteHead = getHeadBranch(remoteRepository.getPath());
        if(localHead.equals(remoteHead)){
            String rbCommit = Utils.readFile(new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" +
                    Environment.BRANCHES + "\\" + remoteRepository.getName() + "\\" + localHead + ".txt"));
            String rrBranchCommit = Utils.readFile(new File(remoteRepository.getPath() + "\\" + Environment.MAGIT +"\\" +
                    Environment.BRANCHES + "\\" + remoteHead + ".txt"));
            return rbCommit.equals(rrBranchCommit);
        }
        return false;
    }

    private void initNewRepository(String path) throws IOException, ParserConfigurationException, FailedToCreateRepositoryException {
        engine.initNewRepository(localRepository.getName(), path,false, true);
    }

    public void initRemoteRepository(Repository localRepository) throws FailedToCreateRepositoryException, IOException, ParserConfigurationException {
        this.localRepository = localRepository;
        File remoteRepo = new File(engine.magitRepo + "\\" + engine.REMOTE_LOCATION + ".txt");
        String remoteDetails = Utils.readFile(remoteRepo);
        String[] remoteLocationAndName = remoteDetails.split(DELIMITER);
        if(!new File(remoteLocationAndName[REMOTE_LOCATION_INDEX]).exists())
            throw new FailedToCreateRepositoryException("Remote repository doesn't exists on files system");
        if(engine.getRepositoriesManager().getRepositoryByPath(remoteLocationAndName[REMOTE_LOCATION_INDEX]) != null){
            remoteRepository = engine.getRepositoriesManager().getRepositoryByPath(remoteLocationAndName[REMOTE_LOCATION_INDEX]);
        }
        else{
            remoteRepository = new Repository(remoteLocationAndName[REMOTE_NAME_INDEX], remoteLocationAndName[REMOTE_LOCATION_INDEX],
                    "", engine.getUsername());
            remoteRepository.getWorkingCopy().initRootFolder(remoteRepository.getPath(), engine.getUsername());
            engine.getRepositoriesManager().addItem(remoteRepository);
        }
        readRemoteBranches();
        setRemoteRepository(remoteRepository);
    }

    private void readRemoteBranches() throws IOException, ParserConfigurationException {
        File remoteBranches = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" +
                Environment.BRANCHES + "\\" + remoteRepository.getName());
        if(remoteBranches.exists() && remoteBranches.isDirectory()){
            engine.createBranchObjects(remoteBranches.getCanonicalPath(), remoteRepository.getName());
        }
    }

    public void pushBranchToRR(String branchToPush) throws FailedToCreateBranchException, IOException, ParserConfigurationException, FailedToCreateRepositoryException {
        File remoteBranch = new File(remoteRepository.getPath() + "\\" + Environment.MAGIT +
                "\\" + Environment.BRANCHES + "\\" + branchToPush + ".txt");
        if (remoteBranch.exists())
            throw new FailedToCreateBranchException("The branch already exists in the remote repository");
        File branchFile = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES + "\\" + branchToPush + ".txt");
        Files.copy(branchFile.toPath(), remoteBranch.toPath());
        String commitHead = Utils.readFile(branchFile);
        File commitZip = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + commitHead);
        if (commitZip.exists() && commitZip.isFile()) {
            Commit lrCommit = new Commit();
            lrCommit.parseCommitFile(commitZip);
            createObjFilesInRemote(commitHead);
            Branch newRB = new Branch(lrCommit, remoteRepository.getName() + "\\" + branchToPush, true);
            engine.getBranchesManager().addItem(newRB);
            engine.getBranchesManager().getBranch(branchToPush).setTracking(newRB.getName());
            Utils.writeFile(new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" +
                    Environment.BRANCHES + "\\" + remoteRepository.getName() + "\\" + branchToPush + ".txt"), lrCommit.createHashCode());
        }
    }

    private void createObjFilesInRemote(String commitHead) throws IOException, ParserConfigurationException {
        Commit currentCommit = new Commit();
        File commitFile = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" +
                commitHead);
        currentCommit.parseCommitFile(commitFile);
        File commitInRemote = new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" +
                commitHead);
        while (!commitInRemote.exists()) {
            File objInLocal = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + currentCommit.createHashCode());
            if (objInLocal.exists()) {
                Files.copy(objInLocal.toPath(), commitInRemote.toPath());
                File remoteRoot =  new File(remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + currentCommit.getRootSha1());
                if(!remoteRoot.exists()) {
                    Files.copy(new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + currentCommit.getRootSha1()).toPath(),
                            remoteRoot.toPath());
                    Folder root = new Folder("");
                    root.parseFolderFile(new File(localRepository.getPath() + "\\" +
                            Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + currentCommit.getRootSha1()));
                    String localObjPath = remoteRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\";
                    calcObjDelta(root, localObjPath);
                }
            }
        }
    }

     private void calcObjDelta(Folder root, String objPath) throws IOException, ParserConfigurationException {
         for(Map.Entry<String, FileData> fd : root.getIncludedFiles().entrySet()){
             File objFile = new File(objPath + fd.getValue().getSha1());
             if(!objFile.exists()){
                 File inLocal = new File(localRepository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS + "\\" + fd.getValue().getSha1());
                 Files.copy((inLocal).toPath(), objFile.toPath());
                 if(fd.getValue().getType() == FileType.DIRECTORY) {
                     Folder newRoot = new Folder(fd.getValue().getName());
                     newRoot.parseFolderFile(inLocal);
                     calcObjDelta(newRoot, objPath);
                 }
             }
         }
     }

}


