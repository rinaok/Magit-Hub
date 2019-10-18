package logic.manager.XmlHandler;
import logic.manager.Environment;
import logic.manager.Repository;
import logic.manager.generated.*;
import logic.modules.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XmlGenerator {

    private String branchMagitPath;
    private String objectsMagitPath;
    private Repository repository;
    private File xmlFile;
    private MagitRepository magitRepository;
    private MagitCommits magitCommitsObject;
    private MagitFolders magitFoldersObject;
    private MagitBlobs magitBlobsObjects;
    private MagitBranches magitBranchesObject;
    private int foldersCounter;
    private int blobsCounter;
    private int commitsCounter;

    XmlGenerator(){
        foldersCounter = 0;
        blobsCounter = 0;
        commitsCounter = 0;
        commitsCounter = 0;
        magitBlobsObjects = new MagitBlobs();
        magitCommitsObject = new MagitCommits();
        magitFoldersObject = new MagitFolders();
        magitBranchesObject = new MagitBranches();
    }

    public void createRepositoryXml(String filePath, Repository repository) throws JAXBException, IOException, ParserConfigurationException {
        if(repository == null)
            throw new ParserConfigurationException("Repository is empty, please choose an active repository");
        this.repository = repository;
        this.xmlFile = new File(filePath);
        branchMagitPath = repository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES;
        objectsMagitPath = repository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.OBJECTS;
        JAXBContext jc = JAXBContext.newInstance(MagitRepository.class);
        createMagitRepository();
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.marshal(magitRepository, xmlFile);
    }

    private void createMagitRepository() throws IOException, ParserConfigurationException {
        createMagitXmlObjects();
        magitRepository = new MagitRepository();
        magitRepository.setMagitBlobs(magitBlobsObjects);
        magitRepository.setName(repository.getName());
        magitRepository.setLocation(repository.getPath());
        magitRepository.setMagitBranches(magitBranchesObject);
        magitRepository.setMagitCommits(magitCommitsObject);
        magitRepository.setMagitFolders(magitFoldersObject);
    }

    private void createMagitXmlObjects() throws IOException, ParserConfigurationException {
        magitRepository = new MagitRepository();
        magitRepository.setLocation(repository.getPath());
        magitRepository.setName(repository.getName());
        parseBranchFiles();
    }

    private void parseBranchFiles() throws IOException, ParserConfigurationException {
        String branchMagitPath = repository.getPath() + "\\" + Environment.MAGIT + "\\" + Environment.BRANCHES;
        Stream<Path> walk = Files.walk(Paths.get(branchMagitPath));
        List<String> branches = walk.filter(Files::isRegularFile)
                .map(Path::toString).collect(Collectors.toList());
        for (String path : branches) {
            if(!path.contains("head")) {
                File file = new File(path);
                Branch newBranch = new Branch();
                newBranch.setName(file.getName());
                copyBranchToMagitBranch(newBranch);
            }
        }
    }

    private void copyBranchToMagitBranch(Branch toCopy) throws IOException, ParserConfigurationException {
        MagitSingleBranch magitBranch = new MagitSingleBranch();
        magitBranch.setName(toCopy.getName());
        Commit headCommit = new Commit();
        File branchFile = new File(branchMagitPath + "\\" + toCopy.getName());
        String commitSha1 = toCopy.parseBranchFile(branchFile);
        headCommit.parseCommitFile(new File(objectsMagitPath + "\\" + commitSha1));
        MagitSingleBranch.PointedCommit pointedCommit = new MagitSingleBranch.PointedCommit();
        pointedCommit.setId(String.valueOf(++commitsCounter));
        magitBranch.setPointedCommit(pointedCommit);
        copyCommitToMagitCommit(headCommit, pointedCommit.getId());
        magitBranchesObject.getMagitSingleBranch().add(magitBranch);
    }

    private void copyCommitToMagitCommit(Commit toCopy, String Id) throws IOException, ParserConfigurationException {
        MagitSingleCommit magitCommit = new MagitSingleCommit();
        magitCommit.setId(Id);
        RootFolder root = new RootFolder();
        root.setId(String.valueOf(++foldersCounter));
        magitCommit.setRootFolder(root);
        magitCommit.setMessage(toCopy.getMessage());
        magitCommit.setAuthor(toCopy.getCreatedBy());
        magitCommit.setDateOfCreation(toCopy.getCreationDate());
        copyFolderToMagitFolder(toCopy.getRootSha1(), root.getId(), toCopy.getCreatedBy(), toCopy.getCreationDate(), true);
        magitCommitsObject.getMagitSingleCommit().add(magitCommit);
    }

    private void copyFolderToMagitFolder(String Sha1, String Id, String lastUpdater, String lastUpdate, boolean isRoot)
            throws IOException, ParserConfigurationException {
        File file = new File(objectsMagitPath + "\\" + Sha1);
        Folder folder = new Folder("");
        folder.parseFolderFile(file);
        MagitSingleFolder magitFolder = new MagitSingleFolder();
        magitFolder.setId(Id);
        if(isRoot)
            magitFolder.setIsRoot(isRoot);
        magitFolder.setLastUpdater(lastUpdater);
        magitFolder.setLastUpdateDate(lastUpdate);
        if (folder.getIncludedFiles().size() > 0) {
            MagitSingleFolder.Items items = new MagitSingleFolder.Items();
            magitFolder.setItems(items);
            for (Map.Entry<String, FileData> item : folder.getIncludedFiles().entrySet()) {
                Item itemMagit = new Item();
                items.getItem().add(itemMagit);
                if (item.getValue().getType().equals(FileType.FILE)) {
                    itemMagit.setId(String.valueOf(++blobsCounter));
                    itemMagit.setType("blob");
                    copyBlobToMagitBlob(itemMagit.getId(), item.getValue());
                }
                else{
                    itemMagit.setId(String.valueOf(++foldersCounter));
                    itemMagit.setType("folder");
                    copyFolderToMagitFolder(item.getValue().getSha1(), itemMagit.getId(), item.getValue().getModifiedBy(),
                            item.getValue().getModificationDate(), false);
                }
            }
        }

        magitFoldersObject.getMagitSingleFolder().add(magitFolder);
    }

    private void copyBlobToMagitBlob(String Id, FileData fileData) {
        MagitBlob magitBlob = new MagitBlob();
        magitBlob.setId(Id);
        magitBlob.setName(fileData.getName());
        magitBlob.setLastUpdater(fileData.getModifiedBy());
        magitBlob.setLastUpdateDate(fileData.getModificationDate());
        String content = fileData.getFilePointer().getContent();
        content = content.replaceAll("\r?\n", "\n");
        magitBlob.setContent(content);
        magitBlobsObjects.getMagitBlob().add(magitBlob);
    }
}