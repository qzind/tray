package qz.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import qz.App;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
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

    public static Document createXmlDocument(Path absolutePath) throws ParserConfigurationException, IOException, SAXException {
        return createXmlDocument(Files.newInputStream(absolutePath));
    }

    public static Document createXmlDocument(Path absolutePath, boolean validating) throws ParserConfigurationException, IOException, SAXException {
        return createXmlDocument(Files.newInputStream(absolutePath), validating);
    }

    /**
     * Attempts to coerce the specified root xml attribute and return the mutated doc
     */
    public static String setSvgAttribute(Path absolutePath, String rootNode, String attribute, String value) throws IOException {
        try {
            Document doc = XmlUtilities.createXmlDocument(absolutePath, false);
            NodeList svgList = doc.getElementsByTagName("svg");
            if (svgList.getLength() > 0) {
                Element svgNode = (Element)svgList.item(0);

                // Some attributes (such as "style") are additive, most are not
                String existingAttribute = switch(attribute) {
                    case "style" -> svgNode.getAttribute(attribute).trim();
                    default -> "";
                };

                svgNode.setAttribute(attribute, existingAttribute +
                        (existingAttribute.isEmpty()? "":";") + value);

                // Convert the updated XML Document back to a String
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(doc), new StreamResult(writer));

                return writer.toString();
            }
            throw new IOException("XML is missing a root '" + rootNode + "' node");
        } catch(Exception e) {
            throw new IOException(e);
        }
    }
}
