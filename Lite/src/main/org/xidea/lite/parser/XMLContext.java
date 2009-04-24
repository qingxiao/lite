package org.xidea.lite.parser;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xml.sax.SAXException;

public interface XMLContext {

	/**
	 * 如果compress为真，或者 reserveSpace为真 则该属性失效
	 * @param format
	 */
	public boolean isFormat();

	public void setFormat(boolean format);
	
	public int getELType();

	/**
	 * 如果 reserveSpace为真 则该属性失效
	 * @return
	 */
	public boolean isCompress();

	public void setCompress(boolean compress);

	/**
	 * 该属性为真时，compress 和 format都将失效
	 * @return
	 */
	public boolean isReserveSpace();

	public void setReserveSpace(boolean keepSpace);

	/**
	 * 开始缩进(当压缩属性和reserveSpace都不为真的时候有效)
	 * @see ParseContextImpl#beginIndent()
	 */
	public void beginIndent();


	/**
	 * 开始缩进(当压缩属性和reserveSpace都不为真的时候有效)
	 * @see ParseContextImpl#endIndent()
	 */
	public void endIndent();
	
	public Node selectNodes(String xpath, Node doc) throws XPathExpressionException;
	
	public Node transform(URL parentURL, Node doc, String xslt) throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException, IOException;
	
	public Node loadXML(URL createURL) throws SAXException, IOException;

}