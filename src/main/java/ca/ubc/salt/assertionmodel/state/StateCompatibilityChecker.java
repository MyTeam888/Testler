package ca.ubc.salt.assertionmodel.state;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ca.ubs.salt.assertionModel.utils.FileUtils;
import ca.ubs.salt.assertionModel.utils.Settings;

public class StateCompatibilityChecker
{
    HashMap<List<String>, Set<String>> varStateSet = new HashMap<List<String>, Set<String>>();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;

    public static void main(String[] args) throws SAXException, IOException
    {
	StateCompatibilityChecker scc = new StateCompatibilityChecker();
//	scc.processState("testSubtract-17.xml");
	 scc.populateVarStateSet();
	System.out.println(scc.varStateSet);
    }

    public StateCompatibilityChecker()
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

    private void populateVarStateSet()
    {
	File folder = new File(Settings.tracePaths);
	String[] traces = folder.list();

	for (String state : traces)
	{
	    try
	    {
		processState(state);
	    } catch (SAXException e)
	    {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (IOException e)
	    {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    }

    private void processState(String stateName) throws SAXException, IOException
    {
	String state = FileUtils.getState(stateName);
	// Document document = builder.parse(Settings.tracePaths + "/" +
	// stateName);
	Document document = builder.parse(new InputSource(new StringReader(state)));
	NodeList nodeList = document.getDocumentElement().getChildNodes();

	for (int i = 0; i < nodeList.getLength(); i++)
	{
	    Node object = nodeList.item(i);
	    if (object instanceof Element)
	    {
		processObject(object, new LinkedList<String>(), stateName);
	    }
	}
    }

    private void processObject(Node object, LinkedList<String> key, String stateName)
    {

	key.add(object.getNodeName());

	NodeList children = object.getChildNodes();
	if (children.getLength() == 0)
	{
	    key.add(object.getNodeValue());
	    addToList(key, stateName);
	    key.removeLast();

	} else
	{
	    for (int i = 0; i < children.getLength(); i++)
	    {
		processObject(children.item(i), key, stateName);
	    }
	}

	key.removeLast();
    }

    private void addToList(List<String> key, String stateName)
    {
	Set<String> states = varStateSet.get(key);
	if (states != null)
	{
	    states.add(stateName);
	} else
	{
	    states = new HashSet<String>();
	    states.add(stateName);
	    // TODO check if you need to clone key object!
	    varStateSet.put(new LinkedList<String>(key), states);
	}
    }
}
