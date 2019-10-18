package logic.manager;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.modules.Blob;
import logic.modules.Folder;
import logic.modules.GitFile;
import org.apache.commons.io.FileUtils;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Utils {

    public static void createNewDirectory(String path, String name) throws FailedToCreateRepositoryException {
        String dirFile = path + "\\" + name;
        if(path!=null && !"".equals(dirFile.trim())) {
            File repoFile = new File(dirFile);
            if (!repoFile.exists()) {
                try{
                    FileUtils.forceMkdir(repoFile);
                }
                catch(IOException ex){
                    throw new FailedToCreateRepositoryException(ex.toString());
                }
            }
            else {
                throw new FailedToCreateRepositoryException("Folder " + dirFile + " already exists in the current path");
            }
        }
        else
        {
            throw new FailedToCreateRepositoryException("Invalid folder, path is empty. Please insert a valid path");
        }
    }

    public static void writeFile(File sourceFile, String content) throws IOException, FailedToCreateRepositoryException {
        if(!sourceFile.getParentFile().exists())
            Utils.createNewDirectory(sourceFile.getParentFile().getParent(), sourceFile.getParentFile().getName());
        //BufferedWriter writer = null;
        FileWriter fileWriter = new FileWriter(sourceFile);
        if(content == null)
            content = "";
        try {
            //writer = new BufferedWriter(new FileWriter(sourceFile));
            fileWriter.write(content);
        }
        catch(IOException ex){
            System.out.println("An Error occurred while writing to file: " + sourceFile.getName());
        }
        finally {
            try {
                fileWriter.close();
            }
            catch (IOException ex)
            {
                System.out.println("Something went wrong...");
            }
        }
    }

    public static void createZipFile(String path, String fileName, String content) throws IOException, FailedToCreateRepositoryException {
        String sourceFile = path + "\\" + fileName + ".txt";
        File source = new File(sourceFile);
        if(!source.exists()) {
            writeFile(source, content);
            FileOutputStream fos = new FileOutputStream(path + "\\" + fileName);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFile);
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            zipOut.close();
            fis.close();
            fos.close();
            source.delete();
        }
    }

    public static void createTxtFile(String path, String fileName, String content) throws IOException, FailedToCreateRepositoryException {
        String sourceFile = path + "\\" + fileName + ".txt";
        File source = new File(sourceFile);
        writeFile(source, content);
    }

    public static File[] getAllFilesInDirectory(String path) {
        File dir = new File(path);
        return dir.listFiles();
    }

    public static void deleteDirectory(File path) throws IOException {
        Stream<Path> walk = Files.walk(Paths.get(path.getCanonicalPath()));
        List<String> currentFiles = walk.filter(x->!x.toAbsolutePath().toString().contains(".magit"))
                .map(Path::toString).collect(Collectors.toList());

        for(String filePath : currentFiles) {
            File file = new File(filePath);
            if (file.isDirectory() && !path.getCanonicalPath().equals(filePath))
                FileUtils.deleteDirectory(file);
            else
                file.delete();
        }
    }

    public static void deleteRepository(File path) throws IOException {
        FileUtils.forceDelete(path);
    }

    public static boolean deleteFile(String filePath) throws IOException{
        File toDelete = new File(filePath);
        boolean isDeleted = toDelete.delete();
        if(isDeleted){
            Stream<Path> walk = Files.walk(Paths.get(toDelete.getParent()));
            List<String> currentFiles = walk.map(Path::toString).collect(Collectors.toList());
            if(currentFiles == null || currentFiles.size() == 1)
                deleteDirectory(new File(toDelete.getParent()));
        }
        return toDelete.delete();
    }

    public static String getTime(){
        Calendar currentTime = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyy-HH:mm:ss:sss");
        return dateFormat.format(currentTime.getTime());
    }

    public static String readFile(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        if(file.getCanonicalPath().contains(Environment.MAGIT + "\\" + Environment.OBJECTS)) {
            lines = readZipFile(file);
        }
        else{
            //Path path = Paths.get(file.getCanonicalPath());
            //lines = Files.readAllLines(path);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;
            while ((st = br.readLine()) != null)
                lines.add(st);
            br.close();
        }
        return String.join("\r\n", lines);
    }

    public static List<String> readZipFile(File file) throws IOException {
        InputStream theFile = new FileInputStream(file.getCanonicalPath());
        ZipInputStream zipStream = new ZipInputStream(theFile);
        zipStream.getNextEntry();
        Scanner sc = new Scanner(zipStream);
        List<String> lines = new ArrayList<>();
        while (sc.hasNextLine()) {
            lines.add(sc.nextLine());
        }
        sc.close();
        return lines;
    }

    public static List<String[]> readObjectFileIntoArray(File file, String delimiter) throws IOException {
        List<String> lines = readZipFile(file);
        List<String[]> objects = new ArrayList<>();
        for(String line : lines){
            objects.add(line.split(delimiter));
        }
        return objects;
    }

    public static List<String[]> createObjectsFromFile(File file, String delimiter) {
        List<String[]> objects = new ArrayList<>() ;
        try {
            objects = readObjectFileIntoArray(file, delimiter);
        }
        catch (IOException ignored){
        }
        finally
        {
            return objects;
        }
    }

    public static GitFile createGitFile(File file, String username) throws IOException {
        if (file.isFile()) {
            Blob newBlob = new Blob(Utils.readFile(file));
            return newBlob;
        } else { // directory
            Folder newFolder = new Folder(file.getName());
            File[] fileInDir = Utils.getAllFilesInDirectory(file.getCanonicalPath());
            for (File f : fileInDir) {
                if (f.isFile()) {
                    newFolder.addFileToFolder(new Blob(Utils.readFile(f)), username, f.getName());
                } else {
                    Folder subFolder = new Folder(f.getName());
                    newFolder.addFileToFolder(subFolder, username, f.getName());
                }
                createGitFile(f, username);
            }
            return newFolder;
        }
    }

    public static void copyDirectory(String source, String destination, FileFilter filters){
        File srcDir = new File(source);
        File destDir = new File(destination);
        try {
            FileUtils.copyDirectory(srcDir, destDir, filters);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
