package logic.manager;

import logic.manager.Exceptions.FailedToCreateRepositoryException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@FunctionalInterface
public interface ActionsOnObjectsInterface {

    void doAction(String[] params, String path) throws IOException, FailedToCreateRepositoryException, ParserConfigurationException;
}
