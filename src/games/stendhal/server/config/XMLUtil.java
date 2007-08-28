/*
 * @(#) src/games/stendhal/server/config/XMLUtil.java
 *
 * $Id$
 */

package games.stendhal.server.config;

//
//

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML utility methods for DOM reading.
 */
public class XMLUtil {
	//
	// XMLUtil
	//

	/**
	 * Get all the direct children elements of an element.
	 *
	 *
	 */
	public static List<Element> getElements(final Element parent) {
		LinkedList<Element> list = new LinkedList<Element>();

		Node node = parent.getFirstChild();

		while(node != null) {
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				list.add((Element) node);
			}

			node = node.getNextSibling();
		}

		return list;
	}


	/**
	 * Get all the direct children elements of an element that have a
	 * specific tag name.
	 *
	 *
	 */
	public static List<Element> getElements(final Element parent, final String name) {
		LinkedList<Element> list = new LinkedList<Element>();

		Node node = parent.getFirstChild();

		while(node != null) {
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;

				if(element.getTagName().equals(name)) {
					list.add(element);
				}
			}

			node = node.getNextSibling();
		}

		return list;
	}


	public static String getText(final Element element) {
		StringBuffer sbuf = new StringBuffer();

		getText(element, sbuf, false);

		return sbuf.toString();
	}


	public static void getText(final Element element, final StringBuffer sbuf, final boolean decend) {
		Node node = element.getFirstChild();

		while(node != null) {
			switch(node.getNodeType()) {
				case Node.TEXT_NODE:
					sbuf.append(node.getNodeValue());
					break;

				case Node.ELEMENT_NODE:
					if(decend) {
						getText((Element) node, sbuf, decend);
					}

					break;
			}

			node = node.getNextSibling();
		}
	}


	public static Document parse(InputStream in) throws SAXException, IOException {
		return parse(new InputSource(in));
	}


	public static Document parse(InputSource is) throws SAXException, IOException {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

			return builder.parse(is);
		} catch(ParserConfigurationException ex) {
			throw new IllegalArgumentException("DOM parser configuration error: " + ex.getMessage());
		}
	}
}
