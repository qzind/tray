package qz.utils;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import qz.App;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class XmlUtilities {

    /**
     * Create xml1-compatible document that's common for macOS plists, system_profiler, etc.
     */
    public static Document createXmlDocument(InputStream is, boolean validating) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf  = DocumentBuilderFactory.newInstance();

        // don't let the <!DOCTYPE> fail parsing per https://github.com/qzind/tray/issues/809
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        // fix erroneous "\r\n", ignored unless setValidating(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setValidating(validating);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        // Resolve DTDs from installer/assets/dtd if present
        builder.setEntityResolver((publicId, systemId) -> {
            String fileName = Paths.get(systemId).getFileName().toString();
            Path relativeAsset = Paths.get("installer/assets/dtd");
            InputStream inputStream = App.class.getResourceAsStream(relativeAsset.resolve(fileName).toString());
            return inputStream != null ? new InputSource(inputStream) : null;
        });

        Document doc = builder.parse(is);
        doc.normalizeDocument();
        return doc;
    }

    public static Document createXmlDocument(InputStream is) throws ParserConfigurationException, IOException, SAXException {
        return  createXmlDocument(is, true);
    }
}
