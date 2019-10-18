package logic.manager.XmlHandler;
import logic.manager.Exceptions.FailedToCreateRepositoryException;
import logic.manager.Exceptions.XmlParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.concurrent.Callable;

public class CallableXml implements Callable<Void> {
    private XmlAdapter systemEngine;

    public Void call() throws Exception {
        callXmlLoad();
        return null;
    }

    public void setXmlEngine(XmlAdapter systemEngine){
        this.systemEngine = systemEngine;
    }

    private void callXmlLoad() throws XmlParseException, FailedToCreateRepositoryException, ParserConfigurationException, IOException {
        systemEngine.loadXML(systemEngine.getUsername());
    }
}
