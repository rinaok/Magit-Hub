package logic.manager.XmlHandler;

import logic.manager.Exceptions.XmlParseException;
import logic.manager.generated.*;
import logic.modules.FileType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

public class XmlParser {

    private static Set<String> idBlobSet = new HashSet<>();
    private static Set<String> idFolderSet = new HashSet<>();
    private static Set<String> idCommitSet = new HashSet<>();

    public static MagitRepository loadXML(String stringXML, String userName) throws XmlParseException {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(MagitRepository.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            MagitRepository magitRepository = (MagitRepository) jaxbUnmarshaller.unmarshal(new StringReader(stringXML));
            magitRepository.setLocation("c:\\magit-ex3\\" + userName + "\\" + magitRepository.getName());
            fileErrorValidations(magitRepository);
            return magitRepository;
        }
        catch (JAXBException | IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private static void fileErrorValidations(MagitRepository xml) throws IOException, XmlParseException {
        idBlobSet.clear();
        idCommitSet.clear();
        idFolderSet.clear();
        uniqueIdValidation(xml);
        validateFolderItems(xml);
        folderPointsItselfValidation(xml);
        commitRootFolderValidation(xml);
        branchToCommitValidation(xml);
        headBranchValidation(xml);
        checkTrackingAfterRemote(xml);
    }

    private static void checkTrackingAfterRemote(MagitRepository xml) throws XmlParseException {
        for(MagitSingleBranch magitBranch : xml.getMagitBranches().getMagitSingleBranch()){
            if(magitBranch.isTracking()){
                String trackingAfter = magitBranch.getTrackingAfter();
                for(MagitSingleBranch magitBranchRemote : xml.getMagitBranches().getMagitSingleBranch()){
                    if(trackingAfter.equals(magitBranchRemote.getName())){
                        if(!magitBranchRemote.isIsRemote())
                            throw new XmlParseException("The branch [" + magitBranch.getName() + "] is tracking" +
                                    " after [" + trackingAfter + "] which is not a remote branch");
                    }
                }
            }
        }
    }

    private static void uniqueIdValidation(MagitRepository xml) throws XmlParseException {
        idFolderSet = new HashSet<>();
        idBlobSet = new HashSet<>();
        for(MagitBlob blob : xml.getMagitBlobs().getMagitBlob()){
            if(idBlobSet.contains(blob.getId())){
                throw new XmlParseException("Xml is invalid: " +
                        "there are 2 folder objects which have the same ID [" + blob.getId() + "]");
            }
            idBlobSet.add(blob.getId());
        }

        for(MagitSingleFolder folder : xml.getMagitFolders().getMagitSingleFolder()){
            if(idFolderSet.contains(folder.getId())){
                throw new XmlParseException("Xml is invalid: " +
                        "there are 2 blobs objects which have the same ID [" + folder.getId() + "]");
            }
            idFolderSet.add(folder.getId());
        }
    }

    private static void validateFolderItems(MagitRepository xml) throws XmlParseException{
        for(MagitSingleFolder folder : xml.getMagitFolders().getMagitSingleFolder()){
            for(Item item : folder.getItems().getItem()){
                if(item.getType().equals("folder")){
                    if(!idFolderSet.contains(item.getId()))
                        throw new XmlParseException("The folder [" + folder.getId() + "]" +
                                "points to folder ID " + item.getId() + " which does not exist");
                }
                else{
                    if(!idBlobSet.contains(item.getId()))
                        throw new XmlParseException("The folder [" + folder.getId() + "]" +
                                " points to blob ID " + item.getId() + " which does not exist");
                }
            }
        }
    }

    private static void folderPointsItselfValidation(MagitRepository xml) throws XmlParseException {
        for (MagitSingleFolder folder : xml.getMagitFolders().getMagitSingleFolder()) {
            for (Item item : folder.getItems().getItem()) {
                if (item.getType().equals(FileType.DIRECTORY.toString())) {
                    if (item.getId().equals(folder.getId()))
                        throw new XmlParseException("The folder [" + folder.getName() + "]" +
                                "points to itself - ID " + item.getId());
                }
            }
        }
    }

    private static void commitRootFolderValidation(MagitRepository xml) throws XmlParseException{
        for(MagitSingleCommit commit : xml.getMagitCommits().getMagitSingleCommit()){
            idCommitSet.add(commit.getId());
            if(!idFolderSet.contains(commit.getRootFolder().getId()))
                throw new XmlParseException("Commit ID [" + commit.getId() + "] " +
                        "points to root folder ID which doesn't exist in the the folder objects [ID "
                        + commit.getRootFolder().getId());
            for(MagitSingleFolder folder : xml.getMagitFolders().getMagitSingleFolder()){
                if(commit.getRootFolder().getId().equals(folder.getId()))
                    if(!folder.isIsRoot()){
                        throw new XmlParseException("Commit ID [" + commit.getId() + "]" +
                                "points to folder ID [" + folder.getId() + "] which isn't root folder");
                    }
            }
        }
    }

    private static void branchToCommitValidation(MagitRepository xml) throws XmlParseException{
        if(idCommitSet.size() == 0){
            for(MagitSingleBranch branch : xml.getMagitBranches().getMagitSingleBranch()){
                if(!branch.getPointedCommit().getId().equals("")){
                    throw new XmlParseException("Branch [" + branch.getName() + "] " +
                            "points to commit ID [" + branch.getPointedCommit().getId() + "]" +
                            " which doesn't exist in the commit objects");
                }
            }
        }
        else {
            for (MagitSingleBranch branch : xml.getMagitBranches().getMagitSingleBranch()) {
                if (!idCommitSet.contains(branch.getPointedCommit().getId()))
                    throw new XmlParseException("Branch [" + branch.getName() + "]" +
                            "points to commit ID [" + branch.getPointedCommit().getId() + "]" +
                            " which doesn't exist in the commit objects");
            }
        }
    }

    private static void headBranchValidation(MagitRepository xml) throws XmlParseException{
        String headBranch = xml.getMagitBranches().getHead();
        boolean isExist = false;
        for(MagitSingleBranch branch : xml.getMagitBranches().getMagitSingleBranch()){
            if(branch.getName().equals(headBranch)) {
                isExist = true;
                break;
            }
        }
        if(!isExist)
            throw new XmlParseException("Head branch points to branch [" + headBranch + "] does not exist");
    }
}
