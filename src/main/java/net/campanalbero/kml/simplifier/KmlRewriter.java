package net.campanalbero.kml.simplifier;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class KmlRewriter {
	private static final XPath XPATH = XPathFactory.newInstance().newXPath();

	private final int limit;
	private final Document doc;
	private final File out;

	public KmlRewriter(String input, String output, int limit) throws ParserConfigurationException, SAXException, IOException {
		out = new File(output);
		this.limit = limit;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		doc = builder.parse(new File(input));
	}

	private void save() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(out));
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void init() {
		Element root = doc.getDocumentElement();
		NodeList list = root.getElementsByTagName("Placemark");
		for (int i = 0; i < list.getLength() - 1; i++) {
			addCache(list.item(i), list.item(i + 1));
		}
	}

	private Line generateLine(Node initialNode, Node terminalNode) {
		Vertex v0 = generateVertex(initialNode);
		Vertex v1 = generateVertex(terminalNode);
		return new Line(v0, v1);
	}

	private Vertex generateVertex(Node placemark) {
		String raw = null;
		try {
			raw = XPATH.evaluate("Point/coordinates", placemark);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		String[] coordinates = raw.split(",");
		return new Vertex(coordinates);
	}

	private void addCache(Node initialNode, Node terminalNode) {
		Line line = generateLine(initialNode, terminalNode);

		Node childNode = doc.createElement("length");
		childNode.setTextContent(Double.valueOf(line.getLength()).toString());
		initialNode.appendChild(childNode);
	}

	private void updateCache(Node initialNode, Node terminalNode) {
		Line line = generateLine(initialNode, terminalNode);

		Node newNode = doc.createElement("length");
		newNode.setTextContent(Double.valueOf(line.getLength()).toString());
		try {
			Node oldNode = (Node) XPATH.evaluate("length", initialNode, XPathConstants.NODE);
			initialNode.replaceChild(newNode, oldNode);

			System.out.println("updated: " + XPATH.evaluate("name", initialNode)); // logger
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	private void removeCache(Node placemark) {
		try {
			Node node = (Node) XPATH.evaluate("length", placemark, XPathConstants.NODE);
			placemark.removeChild(node);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	private void remove(Node target) {
		try {
			System.out.println("remove: " + XPATH.evaluate("name", target));
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}

		Node parent = target.getParentNode();
		parent.removeChild(target);
	}

	private int searchMinLengthIndex(NodeList list) {
		double min = Double.MAX_VALUE;
		int index = -1;
		for (int i = 1; i < list.getLength() - 1; i++) {
			Node placemark = list.item(i);
			double length = 0;
			try {
				length = Double.valueOf(XPATH.evaluate("length", placemark));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (min > length) {
				min = length;
				index = i;
			}
		}
		return index;
	}

	private void execute() {
		Element root = doc.getDocumentElement();
		NodeList list = root.getElementsByTagName("Placemark");

		while (list.getLength() > limit) {
			System.out.println();
			System.out.println("current total number of points : " + list.getLength());

			int index = searchMinLengthIndex(list);

			remove(list.item(index));

			if (index >= 2) {
				updateCache(list.item(index - 1), list.item(index));
			}
		}
	}

	private void clean() {
		Element root = doc.getDocumentElement();
		NodeList list = root.getElementsByTagName("Placemark");
		for (int i = 1; i < list.getLength() - 1; i++) {
			removeCache(list.item(i));
		}
	}

	public void run() {
		init();
		execute();
		clean();
		save();
	}
}
