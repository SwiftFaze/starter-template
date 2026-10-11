package com.swiftfaze.veil.testing.quality;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads every {@code <method>} with its COMPLEXITY and LINE counters out of a JaCoCo XML report. */
final class JacocoReportParser {

    private JacocoReportParser() {
    }

    static List<MethodCoverage> parse(Path jacocoXml) throws IOException, ParserConfigurationException, SAXException {
        Document doc = XmlDocuments.parse(jacocoXml);
        List<MethodCoverage> methods = new ArrayList<>();
        NodeList classes = doc.getElementsByTagName("class");
        for (int c = 0; c < classes.getLength(); c++) {
            Element classElement = (Element) classes.item(c);
            addMethods(classElement, methods);
        }
        return methods;
    }

    private static void addMethods(Element classElement, List<MethodCoverage> methods) {
        String className = classElement.getAttribute("name");
        NodeList methodElements = classElement.getElementsByTagName("method");
        for (int m = 0; m < methodElements.getLength(); m++) {
            methods.add(toMethod(className, (Element) methodElements.item(m)));
        }
    }

    private static MethodCoverage toMethod(String className, Element method) {
        int[] complexity = counter(method, "COMPLEXITY");
        int[] lines = counter(method, "LINE");
        return new MethodCoverage(className, method.getAttribute("name"),
                complexity[0] + complexity[1], lines[0], lines[1]);
    }

    /** Returns {missed, covered} for the counter of the given type, or {0, 0} if absent. */
    private static int[] counter(Element method, String type) {
        NodeList counters = method.getElementsByTagName("counter");
        for (int i = 0; i < counters.getLength(); i++) {
            Element counter = (Element) counters.item(i);
            if (type.equals(counter.getAttribute("type"))) {
                return new int[] {
                    Integer.parseInt(counter.getAttribute("missed")),
                    Integer.parseInt(counter.getAttribute("covered"))
                };
            }
        }
        return new int[] {0, 0};
    }
}
