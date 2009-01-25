package org.xidea.lite.parser;

import static org.xidea.lite.Template.ELSE_TYPE;
import static org.xidea.lite.Template.FOR_TYPE;
import static org.xidea.lite.Template.IF_TYPE;
import static org.xidea.lite.Template.VAR_TYPE;
import static org.xidea.lite.Template.CAPTRUE_TYPE;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xidea.el.json.JSONEncoder;

public class CoreXMLNodeParser implements NodeParser {
	private static final String TEMPLATE_NAMESPACE = "http://www.xidea.org/ns/template";
	private static final String TEMPLATE_NAMESPACE_CORE = "http://www.xidea.org/ns/template/core";

	public static boolean isCoreNS(String prefix, String url) {
		return ("c".equals(prefix) && ("#".equals(url) || "#core".equals(url)))
				|| TEMPLATE_NAMESPACE.equals(url)
				|| TEMPLATE_NAMESPACE_CORE.equals(url);
	}

	private static Log log = LogFactory.getLog(CoreXMLNodeParser.class);
	private XMLParser parser;

	public CoreXMLNodeParser(XMLParser parser) {
		this.parser = parser;
	}

	public Node parseNode(Node node, ParseContext context) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element el = (Element) node;
			String prefix = el.getPrefix();
			String namespaceURI = el.getNamespaceURI();
			if (namespaceURI != null && isCoreNS(prefix, namespaceURI)) {
				String name = el.getLocalName();
				int depth = context.getDepth();
				context.setDepth(depth - 1);
				if ("choose".equals(name)) {
					return parseChooseTag(node, context);
				} else if ("elseif".equals(name) || "else-if".equals(name) || "elif".equals(name)) {
					return parseElseIfTag(node, context,true);
				} else if ("else".equals(name)) {
					return parseElseIfTag(node, context,false);
				} else if ("if".equals(name)) {
					return parseIfTag(node, context);
				} else if ("out".equals(name)) {
					return parseOutTag(node, context);
				} else if ("include".equals(name)) {
					return parseIncludeTag(node, context);
				} else if ("for".equals(name) || "forEach".equals(name)
						|| "for-each".equals(name)) {
					return parseForTag(node, context);
				} else if ("var".equals(name)) {
					return parseVarTag(node, context);
				} else if ("json".equals(name)) {
					return parseJSONTag(node, context);
				}
				context.setDepth(depth);
			}
		}
		return node;
	}

	public String getAttribute(Node node, String... keys) {
		Element el = (Element) node;
		for (String key : keys) {
			if (el.hasAttribute(key)) {
				return el.getAttribute(key);
			}
		}
		return null;
	}

	private Object toEL(String value) {
		value = value.trim();
		if (value.startsWith("${") && value.endsWith("}")) {
			value = value.substring(2, value.length() - 1);
		} else {
			value = JSONEncoder.encode(value);
		}
		return parser.optimizeEL(value);
	}

	private Object getAttributeEL(Node node, String key) {
		String value = getAttribute(node, key);
		return toEL(value);

	}

	public Node parseIncludeTag(Node node, ParseContext context) {
		String var = getAttribute(node, "var");
		String path = getAttribute(node, "path");
		String xpath = getAttribute(node, "xpath");
		String xslt = getAttribute(node, "xslt");
		String name = getAttribute(node, "name");
		Node doc = node.getOwnerDocument();
		URL parentURL = context.getCurrentURL();
		try {
			if (name != null) {
				DocumentFragment cachedNode = parser.toDocumentFragment(node,
						node.getChildNodes());
				context.put("#" + name, cachedNode);
			}
			if (var != null) {
				Node next = node.getFirstChild();
				context.append(new Object[] { CAPTRUE_TYPE, var });
				if (next != null) {
					do {
						this.parser.parseNode(next, context);
					} while ((next = next.getNextSibling()) != null);
				}
				context.append(END);
			}
			if (path != null) {
				if (path.startsWith("#")) {
					doc = (Node) context.get(path);
					String uri;
					if (doc instanceof Document) {
						uri = ((Document) doc).getDocumentURI();
					} else {
						uri = doc.getOwnerDocument().getDocumentURI();
					}
					if (uri != null) {
						try {
							context.setCurrentURL(new URL(uri));
						} catch (Exception e) {
						}
					}
				} else {
					doc = this.parser
							.loadXML(new URL(parentURL, path), context);
				}
			}

			if (xpath != null) {
				doc = this.parser.selectNodes(xpath, doc);
			}
			if (xslt != null) {
				Source xsltSource;
				if (xslt.startsWith("#")) {
					Node node1 = ((Node) context.get(xslt));
					Transformer transformer = javax.xml.transform.TransformerFactory
							.newInstance().newTransformer();
					DOMResult result = new DOMResult();
					if (node1.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
						node1 = node1.getFirstChild();
						while (node1.getNodeType() != Node.ELEMENT_NODE) {
							node1 = node1.getNextSibling();
						}
					}
					transformer.transform(new DOMSource(node1), result);
					xsltSource = new javax.xml.transform.dom.DOMSource(result
							.getNode());
				} else {
					xsltSource = new javax.xml.transform.stream.StreamSource(
							new URL(parentURL, xslt).openStream());
				}

				// create an instance of TransformerFactory
				Transformer transformer = javax.xml.transform.TransformerFactory
						.newInstance().newTransformer(xsltSource);
				// javax.xml.transform.TransformerFactory
				// .newInstance().set
				// transformer.setNamespaceContext(parser.createNamespaceContext(doc.getOwnerDocument()));

				Source xmlSource;
				if (doc.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
					Element root = doc.getOwnerDocument().createElement("root");
					root.appendChild(doc);
					xmlSource = new DOMSource(root);
				} else {
					xmlSource = new DOMSource(doc);
				}
				DOMResult result = new DOMResult();

				transformer.transform(xmlSource, result);
				doc = result.getNode();
			}
			this.parser.parseNode(doc, context);
			return null;
		} catch (Exception e) {
			log.error(e);
			return null;
		} finally {
			context.setCurrentURL(parentURL);
		}
	}

	Node parseIfTag(Node node, ParseContext context) {
		Node next = node.getFirstChild();
		Object test = getAttributeEL(node, "test");
		context.append(new Object[] { IF_TYPE, test });
		if (next != null) {
			do {
				this.parser.parseNode(next, context);
			} while ((next = next.getNextSibling()) != null);
		}
		context.append(END);
		return null;
	}

	Node parseElseIfTag(Node node, ParseContext context, boolean reqiiredTest) {
		context.removeLastEnd();
		Node next = node.getFirstChild();
		if (((Element) node).hasAttribute("test")) {
			Object test = getAttributeEL(node, "test");
			context.append(new Object[] { ELSE_TYPE, test });
		} else if(reqiiredTest) {
			throw new IllegalArgumentException("@test is required");
		} else {
			context.append(new Object[] { ELSE_TYPE, null });
		}

		if (next != null) {
			do {
				this.parser.parseNode(next, context);
			} while ((next = next.getNextSibling()) != null);
		}
		context.append(END);
		return null;
	}

	Node parseElseTag(Node node, ParseContext context) {
		context.removeLastEnd();
		Node next = node.getFirstChild();
		context.append(new Object[] { ELSE_TYPE, null });
		if (next != null) {
			do {
				this.parser.parseNode(next, context);
			} while ((next = next.getNextSibling()) != null);
		}
		context.append(END);
		return null;
	}

	Node parseChooseTag(Node node, ParseContext context) {
		Node next = node.getFirstChild();
		boolean first = true;
		String whenTag = "when";
		String elseTag = "otherwise";
		if (next != null) {
			do {
				if (next instanceof Element) {
					Element el = (Element) next;
					if (el.getNamespaceURI().equals(node.getNamespaceURI())) {
						if (whenTag.equals(el.getLocalName())) {
							if (first) {
								first = false;
								parseIfTag(next, context);
							} else {
								parseElseIfTag(next, context,true);
							}
						} else if (next.getLocalName().equals(elseTag)) {
							parseElseTag(next, context);
						} else {
							throw new RuntimeException(
									"choose 只接受when，otherwise 节点");
						}
					}
				}
			} while ((next = next.getNextSibling()) != null);
		}
		return null;
	}

	Node parseForTag(Node node, ParseContext context) {
		Node next = node.getFirstChild();
		Object items = getAttributeEL(node, "items");
		String var = getAttribute(node, "var");
		String status = getAttribute(node, "status");
		context.append(new Object[] { FOR_TYPE, var, items, status });
		if (next != null) {
			do {
				this.parser.parseNode(next, context);
			} while ((next = next.getNextSibling()) != null);
		}
		context.append(END);
		return null;
	}

	Node parseVarTag(Node node, ParseContext context) {
		String name = getAttribute(node, "name");
		String value = getAttribute(node, "value");
		if (value == null) {
			Node next = node.getFirstChild();
			context.append(new Object[] { CAPTRUE_TYPE, name });
			if (next != null) {
				do {
					this.parser.parseNode(next, context);
				} while ((next = next.getNextSibling()) != null);
			}
			context.append(END);
		} else {
			context.append(new Object[] { VAR_TYPE, toEL(value), name });
		}
		return null;
	}

	Node parseJSONTag(Node node, ParseContext context) {
		String var = getAttribute(node, "var");
		String file = getAttribute(node, "file");
		String encoding = getAttribute(node, "encoding", "charset");
		String content = getAttribute(node, "content");
		if (file != null) {
			try {
				InputStream in = new URL(context.getCurrentURL(), file)
						.openStream();
				InputStreamReader reader = new InputStreamReader(in,
						encoding == null ? "utf-8" : encoding);
				StringBuilder sbuf = new StringBuilder();
				char[] cbuf = new char[1024];
				int c;
				while ((c = reader.read(cbuf)) >= 0) {
					sbuf.append(cbuf, 0, c);
				}
				content = sbuf.toString();
			} catch (Exception e) {
				if (log.isWarnEnabled()) {
					log.warn("json文件读取失败", e);
				}
			}
		}
		if (content == null) {
			// Node next = node.getFirstChild();
			// context.append(new Object[] { VAR_TYPE, var, null });
			// if (next != null) {
			// do {
			// this.parser.parseNode(next, context);
			// } while ((next = next.getNextSibling()) != null);
			// }
			// context.append(END);
			content = node.getTextContent();
		}
		context
				.append(new Object[] { VAR_TYPE, parser.optimizeEL(content),
						var });
		return null;
	}

	Node parseOutTag(Node node, ParseContext context) {
		String value = getAttribute(node, "value");
		List<Object> result = this.parser.parseText(value, false, false, 0);
		context.appendList(result);
		return null;
	}

}