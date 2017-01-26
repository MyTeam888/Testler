package ca.ubc.salt.model.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLUtils
{
    public static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    public static DocumentBuilder builder;
    public static Transformer transformer = null;
    static
    {
	try
	{
	    builder = factory.newDocumentBuilder();
	} catch (ParserConfigurationException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static List<String> getVars(String varXML)
    {
	List<String> vars = new LinkedList<String>();
	try
	{
	    Document document = XMLUtils.builder.parse(new InputSource(new StringReader(varXML)));
	    NodeList nodeList = document.getDocumentElement().getChildNodes();

	    for (int i = 0; i < nodeList.getLength(); i++)
	    {
		Node object = nodeList.item(i);
		if (object instanceof Element)
		{
		    Element e = (Element) object;
		    vars.add(e.getTextContent());
		}
	    }
	} catch (SAXException e1)
	{
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	} catch (IOException e1)
	{
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
	return vars;
    }
    
    /**
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    public static String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in))) return ""; // vacancy test.
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                (current == 0xA) ||
                (current == 0xD) ||
                ((current >= 0x20) && (current <= 0xD7FF)) ||
                ((current >= 0xE000) && (current <= 0xFFFD)) ||
                ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    } 

    public static NodeList getNodeList(String stateName)
    {
	String state = FileUtils.getState(stateName);
	// Document document = builder.parse(Settings.tracePaths + "/" +
	// stateName);
	if (state.equals(""))
	    return null;
	
	// added by tsigalko18
//	state = stripNonValidXMLCharacters(state);
	
	try
	{
	    Document document = XMLUtils.builder.parse(new InputSource(new StringReader(state)));
	    NodeList nodeList = document.getDocumentElement().getChildNodes();
	    return nodeList;
	} catch (SAXException e)
	{
	    // TODO Auto-generated catch block
		Settings.consoleLogger.error("Problems with state " + stateName);
	    e.printStackTrace();
	    return null;
	} catch (IOException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return null;
	}
    }

    public static String getXMLString(Node node)
    {
	try
	{
	    if (transformer == null)
	    {
		transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    }
	    StreamResult result = new StreamResult(new StringWriter());
	    DOMSource source = new DOMSource(node);
	    transformer.transform(source, result);
	    String xmlString = result.getWriter().toString();
	    return xmlString;

	} catch (TransformerException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return null;

    }

}
