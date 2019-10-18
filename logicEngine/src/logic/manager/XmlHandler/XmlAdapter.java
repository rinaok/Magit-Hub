package logic.manager.XmlHandler;
import logic.manager.Engine;
import logic.manager.Environment;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Exceptions.XmlParseException;
import logic.manager.Repository;
import logic.manager.Utils;
import logic.manager.generated.*;
import logic.modules.*;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlAdapter extends Engine {

    private final static String ROOT = "root";
    private Map<String, FileData> XmlblobsAndFolders;
    private Map<String, Commit> commitMapXml = new HashMap<>(); // commit ID to commit object
    private String stringXML;

    public void setFilePath(String stringXML){
        this.stringXML = stringXML;
    }

    public void loadXML(String userName) throws FailedToCreateRepositoryException, XmlParseException, IOException, ParserConfigurationException {
        MagitRepository magitRepository = XmlParser.loadXML(stringXML, userName);
        XmlblobsAndFolders = new HashMap<>();
        if (magitRepository == null)
            throw new XmlParseException("Failed to parse xml");
        initNewRepositoryXml(magitRepository);
        Map<String, MagitSingleFolder> magitFolders = createFolders(magitRepository);
        createSha1ZipObjects();
        createBranchFilesFromXml(magitRepository);
        branchFilesToWC();
        initSha1ToCommitMap();
        if (!firstCommit)
            updateCurrentCommitFiles();
    }

    private void createSha1ZipObjects() throws IOException, FailedToCreateRepositoryException {
        for(Map.Entry<String, FileData> file : XmlblobsAndFolders.entrySet()){
            file.getValue().setSha1(file.getValue().getFilePointer().createHashCode());
            Utils.createZipFile(magitRepo + "\\" + Environment.OBJECTS, file.getValue().getSha1(),
                    file.getValue().getFilePointer().getContent());
        }
    }

    private Map<String, MagitSingleFolder> createFolders(MagitRepository magitRepository) {
        Map<String, MagitBlob> magitBlobs = new HashMap<>();
        Map<String, MagitSingleFolder> magitFolders = new HashMap<>();
        for (MagitBlob magitBlob : magitRepository.getMagitBlobs().getMagitBlob()) {
            magitBlobs.put(magitBlob.getId(), magitBlob);
        }
        for (MagitSingleFolder magitFolder : magitRepository.getMagitFolders().getMagitSingleFolder()) {
            magitFolders.put(magitFolder.getId(), magitFolder);
        }
        for (MagitSingleFolder magitSingleFolder : magitRepository.getMagitFolders().getMagitSingleFolder()) {
            if(magitSingleFolder.isIsRoot()){
                Folder folder = new Folder(ROOT);
                FileData fileData = new FileData(magitSingleFolder, folder);
                XmlblobsAndFolders.put(FileType.DIRECTORY + "_" + magitSingleFolder.getId(), fileData);
                createFolderRec(magitSingleFolder, magitBlobs, magitFolders, folder);
            }
        }
        return magitFolders;
    }

    private void createFolderRec(MagitSingleFolder magitSingleFolder, Map<String, MagitBlob> magitBlobs,
                                 Map<String, MagitSingleFolder> magitFolders, Folder rootFolder) {
        for (Item item: magitSingleFolder.getItems().getItem()) {
            if(item.getType().equals("blob")){
                MagitBlob magitBlob = magitBlobs.get(item.getId());
                Blob blob = new Blob(magitBlob.getContent());
                FileData fileData = new FileData(magitBlob, blob);
                XmlblobsAndFolders.put(FileType.FILE + "_" + magitBlob.getId(), fileData);
                rootFolder.addFileToFolder(blob, fileData.getModifiedBy(), fileData.getName());
            }
            if(item.getType().equals("folder")){
                MagitSingleFolder magitFolder = magitFolders.get(item.getId());
                Folder folder = new Folder(magitFolder.getName());
                FileData fileData = new FileData(folder, magitFolder.getLastUpdater(), magitFolder.getName());
                XmlblobsAndFolders.put(FileType.DIRECTORY + "_" + magitFolder.getId(), fileData);
                rootFolder.addFileToFolder(folder, fileData.getModifiedBy(), fileData.getName());
                createFolderRec(magitFolder, magitBlobs, magitFolders, folder);
            }
        }
    }

    private void createBranchFilesFromXml(MagitRepository xml) throws IOException, FailedToCreateRepositoryException {
        branchesManager.clear();
        for(MagitSingleBranch branch : xml.getMagitBranches().getMagitSingleBranch()){
            String commitId = branch.getPointedCommit().getId();
            String headCommit = createCommitsFromXml(xml, commitId);
            Branch newBranch = new Branch(commitMapXml.get(FileType.COMMIT + "_" + branch.getPointedCommit().getId()), branch.getName());
            branchesManager.addItem(newBranch);
            Utils.writeFile(new File(magitRepo + "\\" + Environment.BRANCHES + "\\" + branch.getName() + ".txt"), headCommit);
            if(branch.getName().equals(xml.getMagitBranches().getHead())){
                branchesManager.setActive(newBranch);
            }
            // check tracking branch
            if(branch.isTracking()) {
                newBranch.setTracking(branch.getTrackingAfter());
                Utils.writeFile(new File(magitRepo + "\\" + Environment.BRANCHES + "\\" + branch.getTrackingAfter() + ".txt"), headCommit);
            }
            else if(branch.isIsRemote()){
                newBranch.setIsRemote();
            }
        }
        createCommitFiles();
        repositoriesManager.setActive(new Repository(xml.getName(), xml.getLocation(),
                branchesManager.getActive().getName(), username));
    }

    private String createCommitsFromXml(MagitRepository xml, String commitId) {
        Commit newCommit = null;
        String prevCommit = null;
        for(MagitSingleCommit commit : xml.getMagitCommits().getMagitSingleCommit()) {
            if (commit.getId().equals(commitId)) {
                if (commit.getPrecedingCommits() != null && commit.getPrecedingCommits().getPrecedingCommit().size() > 0) {
                    List<PrecedingCommits.PrecedingCommit> prevCommits = commit.getPrecedingCommits().getPrecedingCommit();
                    prevCommit = createCommitsFromXml(xml, prevCommits.get(0).getId());
                }
                FileData rootFolder = XmlblobsAndFolders.get(FileType.DIRECTORY + "_" + commit.getRootFolder().getId());
                newCommit = new Commit(rootFolder.getSha1(), commit.getMessage(), commit.getDateOfCreation(), commit.getAuthor(), prevCommit);
                commitMapXml.put(FileType.COMMIT + "_" + commit.getId(), newCommit);
            }
        }
        if(newCommit != null)
            return newCommit.createHashCode();
        else
            return null;
    }

    private void createCommitFiles() throws IOException, FailedToCreateRepositoryException {
        for(Map.Entry<String, Commit> commit : commitMapXml.entrySet()){
            Utils.createZipFile(magitRepo + "\\" + Environment.OBJECTS, commit.getValue().createHashCode(), commit.getValue().getContent());
        }
    }

    public void deleteRepo() throws IOException {
        if(magitRepo != null) {
            File repo = new File(magitRepo).getParentFile();
            if (repo.exists())
                Utils.deleteRepository(repo);
        }
    }

    private void initNewRepositoryXml(MagitRepository xmlRepo) throws FailedToCreateRepositoryException, IOException {
        magitRepo = xmlRepo.getLocation() + "\\" + Environment.MAGIT.toString();
        File repo = new File(xmlRepo.getLocation());
        Repository newRepo = new Repository(xmlRepo.getName(), xmlRepo.getLocation(), ROOT, getUsername());
        repositoriesManager.addItem(newRepo);
        repositoriesManager.setActive(newRepo);
        Utils.createNewDirectory(repo.getParent(), repo.getName());
        Utils.createNewDirectory(magitRepo, Environment.OBJECTS.toString());
        Utils.createNewDirectory(magitRepo, Environment.BRANCHES.toString());
        Utils.writeFile(new File(magitRepo + "\\" + Environment.BRANCHES + "\\" + HEAD + ".txt"), xmlRepo.getMagitBranches().getHead());
//        repositoriesManager.setActive(new Repository(xmlRepo.getName(), xmlRepo.getLocation(),
//            branchesManager.getActive().getName(), username));
        if(xmlRepo.getMagitRemoteReference() != null) {
            if (xmlRepo.getMagitRemoteReference().getLocation() != null) {
                initCollaborationHandler();
                collaborationHandler.setLocalRepository(newRepo);
                if (repositoriesManager.getRepository(xmlRepo.getMagitRemoteReference().getName()) != null) {
                    collaborationHandler.setRemoteRepository(repositoriesManager.getRepository(xmlRepo.getMagitRemoteReference().getName()));
                } else {
                    Repository remote = new Repository(xmlRepo.getMagitRemoteReference().getName(), xmlRepo.getMagitRemoteReference().getLocation(),
                            ROOT, username);
                    remote.getWorkingCopy().initRootFolder(remote.getPath(), username);
                    repositoriesManager.addItem(remote);
                    collaborationHandler.setRemoteRepository(remote);
                }
            }
        }
    }

    public boolean isRepositoryExists(String xml, String userName) throws XmlParseException {
        MagitRepository xmlMagit = XmlParser.loadXML(xml, userName);
        if(xmlMagit != null) {
            File newRepo = new File(xmlMagit.getLocation());
            return newRepo.exists();
        }
        return false;
    }

    public String exportRepositoryToXml(String filePath){
        XmlGenerator xmlGenerator = new XmlGenerator();
        try {
            xmlGenerator.createRepositoryXml(filePath, repositoriesManager.getActive());
            return "Repository was exported to: [" + filePath + "]";
        }
        catch (JAXBException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
