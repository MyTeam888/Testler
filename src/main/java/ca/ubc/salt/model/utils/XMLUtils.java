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

    // public static void main(String[] args) throws SAXException, IOException
    // {
    //
    // getVarIndex(FileUtils.getVars("testSubtract-39.xml"), "");
    // }
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

    public static NodeList getNodeList(String stateName)
    {
	String state = FileUtils.getState(stateName);
	// Document document = builder.parse(Settings.tracePaths + "/" +
	// stateName);
	if (state.equals(""))
	    return null;
	try
	{
	    Document document = XMLUtils.builder.parse(new InputSource(new StringReader(state)));
	    NodeList nodeList = document.getDocumentElement().getChildNodes();
	    return nodeList;
	} catch (SAXException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    System.out.println(stateName);
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
