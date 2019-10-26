package logic.manager.Merge;
import logic.manager.Engine;
import logic.manager.Environment;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Utils;
import logic.modules.*;
import puk.team.course.magit.ancestor.finder.AncestorFinder;
import puk.team.course.magit.ancestor.finder.MappingFunctionFailureException;

import javax.rmi.CORBA.Util;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.ResponseCache;
import java.util.*;

public class MergeHandler {
    private final static int BIT_ARR_SIZE = 6;
    private final static int EXISTS_IN_OURS = 0;
    private final static int EXISTS_IN_THEIRS = 1;
    private final static int EXISTS_IN_ANCESTOR = 2;
    private final static int EQUALS_OURS_ANCESTOR = 3;
    private final static int EQUALS_THEIRS_ANCESTOR = 4;
    private final static int EQUALS_OURS_THEIRS = 5;
    private AncestorFinder ancestorFinder;
    private Map<String, String> ourFilePathToSha1;
    private Map<String, String> otherFilePathToSha1;
    private Map<String, String> ancestorFilePathToSha1;
    private Map<String, MergeOptions> openChanges;
    private Map<String, MergeOptions> conflicts;

    public MergeHandler(Map<String, Commit> commitMapSha1) {
        Map<String, Representative> mapper = new HashMap<>();
        for(Map.Entry<String, Commit> entry : commitMapSha1.entrySet()){
            mapper.put(entry.getKey(), new Representative(entry.getValue()));
        }
        ancestorFinder = new AncestorFinder(c -> mapper.get(c));
    }

    public MergeResult merge(Branch ourBranch, Branch otherBranch) throws IOException, ParserConfigurationException {
        String ancestor = getAncestor(ourBranch.getHead().createHashCode(), otherBranch.getHead().createHashCode());
        MergeResult result = MergeResult.FAILURE;
        if(ancestor != null) {
            result = fastForwardMerge(ourBranch, otherBranch, ancestor);
            if (result == MergeResult.REGULAR_MERGE) {
                Commit ancestorCommit = new Commit();
                ancestorCommit.parseCommitFile(new File(Engine.getMagitRepo() + "\\" + Environment.OBJECTS + "\\" + ancestor));
                Map<String, BitSet> mappedChanges = mapFilesChanges(ourBranch.getHead().getRootSha1(), otherBranch.getHead().getRootSha1(), ancestorCommit.getRootSha1());
                openChanges = calcChanges(mappedChanges, MergeOptions.OPEN_CHANGES);
                conflicts = calcChanges(mappedChanges, MergeOptions.CONFLICTS);
            }
        }
        return result;
    }

    private MergeResult fastForwardMerge(Branch ourBranch, Branch otherBranch, String ancestor){
        if(ancestor.equals(ourBranch.getHead().createHashCode())){
            ourBranch.setHead(otherBranch.getHead());
            return MergeResult.FAST_FORWARD_MERGE_CONTAINED;
        }
        else if(ancestor.equals(otherBranch.getHead().createHashCode())){
            return MergeResult.FAST_FORWARD_MERGE_CONCEALED;
        }
        return MergeResult.REGULAR_MERGE;
    }

    public boolean areThereConflicts() {
        if(conflicts == null || conflicts.size() == 0)
            return false;
        return true;
    }

    public List<String> getConflictsFiles() { return new ArrayList<>(conflicts.keySet()); }

    private String getAncestor(String commit1, String commit2) {
        String ancestor = null;
        try {
            ancestor = ancestorFinder.traceAncestor(commit1, commit2);
        }
        catch(MappingFunctionFailureException e){
            e.printStackTrace();
        }
        finally {
            return ancestor;
        }
    }

    private Map<String, MergeOptions> calcChanges(Map<String, BitSet> mappedFiles, EnumSet<MergeOptions> optionCategories) {
        Map<String, MergeOptions> changes = new HashMap<>();
        for (Map.Entry<String, BitSet> file : mappedFiles.entrySet()) {
            MergeOptions mergeOption = MergeOptions.values()[(int) MergeOptions.convert(file.getValue())];
            if (optionCategories.contains(mergeOption)) {
                changes.put(file.getKey(), mergeOption);
            }
        }
        return changes;
    }

    private Map<String, BitSet> mapFilesChanges(String ourRoot, String otherRoot, String ancestorRoot) throws IOException, ParserConfigurationException {
        Map<String, BitSet> mappedChanges = new HashMap<>();
        ourFilePathToSha1 = new HashMap<>();
        otherFilePathToSha1 = new HashMap<>();
        ancestorFilePathToSha1 = new HashMap<>();

        mapFilesInFolder(ourRoot, ourFilePathToSha1);
        mapFilesInFolder(otherRoot, otherFilePathToSha1);
        mapFilesInFolder(ancestorRoot, ancestorFilePathToSha1);

        setBitsForExistingFiles(EXISTS_IN_OURS, ourFilePathToSha1, mappedChanges);
        setBitsForExistingFiles(EXISTS_IN_THEIRS, otherFilePathToSha1, mappedChanges);
        setBitsForExistingFiles(EXISTS_IN_ANCESTOR, ancestorFilePathToSha1, mappedChanges);

        setBitsForModifiedFiles(EQUALS_OURS_ANCESTOR, ourFilePathToSha1, ancestorFilePathToSha1, mappedChanges);
        setBitsForModifiedFiles(EQUALS_THEIRS_ANCESTOR, otherFilePathToSha1, ancestorFilePathToSha1, mappedChanges);
        setBitsForModifiedFiles(EQUALS_OURS_THEIRS, ourFilePathToSha1, otherFilePathToSha1, mappedChanges);

        return mappedChanges;
    }

    private void mapFilesInFolder(String rootSha1, Map<String, String> mappedFiles) throws IOException, ParserConfigurationException {
        Folder ourRootFolder = new Folder("");
        ourRootFolder.parseFolderFile(new File(Engine.getMagitRepo() + "\\" + Environment.OBJECTS + "\\" + rootSha1));
        pathToSha1(ourRootFolder, rootSha1, mappedFiles);
    }

    private void pathToSha1(Folder folder, String rootSha1, Map<String, String> filePathToSha1) throws IOException, ParserConfigurationException {
        for (Map.Entry<String, FileData> file : folder.getIncludedFiles().entrySet()) {
            if (file.getValue().getType() == FileType.FILE) {
                filePathToSha1.put(Engine.findPath(file.getValue().getSha1(), rootSha1), file.getValue().getSha1());
            }
            else {
                Folder innerFolder = (Folder)file.getValue().getFilePointer();
                innerFolder.parseFolderFile(new File(Engine.getMagitRepo() + "\\" + Environment.OBJECTS + "\\" + file.getValue().getSha1()));
                pathToSha1(innerFolder, rootSha1, filePathToSha1);
            }
        }
    }

    private void setBitsForExistingFiles(int bitIndex, Map<String,String> pathToSha1, Map<String, BitSet> openChanges) throws IOException, ParserConfigurationException {
        for (Map.Entry<String, String> file : pathToSha1.entrySet()) {
            BitSet mergeBytes = new BitSet(BIT_ARR_SIZE);
            if (openChanges.containsKey(file.getKey())) {
                mergeBytes = openChanges.get(file.getKey());
            }
            mergeBytes.set(bitIndex);
            openChanges.put(file.getKey(), mergeBytes);
        }
    }

    private void setBitsForModifiedFiles(int bitIndex, Map<String,String> folderFiles, Map<String,String> ancestorFiles,
                                         Map<String, BitSet> openChanges) {
        for (Map.Entry<String, String> file : folderFiles.entrySet()) {
            if (ancestorFiles.containsKey(file.getKey())) {
                if (ancestorFiles.get(file.getKey()).equals(file.getValue())) {
                    BitSet mergeBytes = new BitSet(BIT_ARR_SIZE);
                    if (openChanges.containsKey(file.getKey())) {
                        mergeBytes = openChanges.get(file.getKey());
                    }
                    mergeBytes.set(bitIndex);
                    openChanges.put(file.getKey(), mergeBytes);
                }
            }
        }
    }

    public Map<String, String> getConflictFilesContent(String file) throws IOException {
        Map<String, String> fileSideToContent = new HashMap<>();
        if(conflicts.containsKey(file)) {
            String ourSha1 = ourFilePathToSha1.get(file);
            String theirSha1 = otherFilePathToSha1.get(file);
            String ancestorSha1 = ancestorFilePathToSha1.get(file);
            fileSideToContent.put("ours", Engine.getFileContent(ourSha1));
            fileSideToContent.put("theirs", Engine.getFileContent(theirSha1));
            fileSideToContent.put("ancestor", Engine.getFileContent(ancestorSha1));
            return fileSideToContent;
        }
        else {
            return null;
        }
    }

    public void solveConflict(String file, String content) throws IOException, FailedToCreateRepositoryException {
        if (conflicts.containsKey(file)) {
            Utils.writeFile(new File(file), content);
            conflicts.remove(file);
            openChanges.put(file, MergeOptions.CONFLICT_SOLVED);
        }
    }

    public void updateWCwithOpenChanges() throws IOException, FailedToCreateRepositoryException {
        if(openChanges != null) {
            for (Map.Entry<String, MergeOptions> openChange : openChanges.entrySet()) {
                if (MergeOptions.DELETE_FILE.contains(openChange.getValue()))
                    deleteFile(openChange.getKey());
                else if(!openChange.getValue().equals(MergeOptions.CONFLICT_SOLVED))
                    handleFile(openChange.getKey(), openChange.getValue());
            }
            openChanges.clear();
        }
    }

    private void deleteFile(String file) throws IOException {
        Utils.deleteFile(file);
    }

    private void handleFile(String file, MergeOptions addFileOption) throws IOException, FailedToCreateRepositoryException {
        String content = "";
        switch (addFileOption){
            case EXISTS_ONLY_IN_OURS:
            case EDITED_ONLY_ON_OURS_AND_EXISTS_IN_ALL:
            case EXISTS_AND_EQUAL_IN_ALL:
                {
                    content = Utils.readFile(new File(Engine.getMagitRepo() + "\\" + Environment.OBJECTS + "\\" + ourFilePathToSha1.get(file)));
                    break;
                }
            case EXISTS_ONLY_IN_THEIRS:
            case EDITED_ONLY_ON_THEIRS_AND_EXISTS_IN_ALL:
                {
                    content = Utils.readFile(new File(Engine.getMagitRepo() + "\\" + Environment.OBJECTS + "\\" + otherFilePathToSha1.get(file)));
                    break;
                }
        }
        File newFile = new File(file);
        Utils.writeFile(newFile, content);
    }
}
