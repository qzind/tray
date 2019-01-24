package qz.printer.status;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;

/**
 * Created by kyle on 4/27/17.
 */
public class CupsStatusHandler extends AbstractHandler {

    private static String lastGuid;

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        baseRequest.setHandled(true);
        if (request.getReader().readLine() != null) {
            try {
                XMLInputFactory factory = XMLInputFactory.newInstance();
                XMLEventReader eventReader = factory.createXMLEventReader(request.getReader());
                parseXML(eventReader);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void parseXML(XMLEventReader eventReader) throws XMLStreamException {
        boolean isDescription = false, isGuid = false, isFirstGuid = true, running = true;
        String firstGuid = "";
        String description = "";

        while(eventReader.hasNext() && running) {
            XMLEvent event = eventReader.nextEvent();
            switch(event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement startElement = event.asStartElement();
                    String qName = startElement.getName().getLocalPart();
                    if ("description".equalsIgnoreCase(qName)) {
                        isDescription = true;
                    }
                    if ("guid".equalsIgnoreCase(qName)) {
                        isGuid = true;
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    Characters characters = event.asCharacters();
                    if (isDescription) {
                        description = characters.getData();
                        isDescription = false;
                    }
                    if (isGuid) {
                        String guid = characters.getData();
                        if (isFirstGuid) {
                            firstGuid = guid;
                            isFirstGuid = false;
                        }
                        if (guid.equals(lastGuid)) {
                            running = false;
                            break;
                        } else {
                            String printerName = StringUtils.substringBeforeLast(description, "\"");
                            printerName = StringUtils.substringAfter(printerName, "\"");
                            if (!printerName.isEmpty() && StatusMonitor.isListeningTo(printerName)) {
                                StatusMonitor.statusChanged(CupsUtils.getStatuses(printerName));
                            }
                        }
                        isGuid = false;
                    }
                    break;
            }
        }

        lastGuid = firstGuid;
    }
}
