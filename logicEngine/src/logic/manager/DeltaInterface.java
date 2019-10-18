package logic.manager;
import logic.modules.Folder;
import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface DeltaInterface {
    public boolean updateDelta(File filePath, Folder parent) throws IOException;
}
