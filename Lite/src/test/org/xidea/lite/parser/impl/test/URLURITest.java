package org.xidea.lite.parser.impl.test;


import java.io.File;
import java.net.URI;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.test.LiteTestUtil;


public class URLURITest {

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testResourceContext() throws Exception{
		String base = "http://lh:8080/test";
		String base2 = "lite:///";
		ParseContext rc =LiteTestUtil.buildParseContext(new URI(base));
		Assert.assertEquals(base2+"test.xml", rc.createURI("test.xml").toString());
		Assert.assertEquals(base2+"test.xml", rc.createURI("./test.xml").toString());
		
		base = "http://lh:8080/test/aa/bb/";
		rc = LiteTestUtil.buildParseContext(new URI(base));
		rc.setCurrentURI(new URI(base));
		System.out.println(rc.getCurrentURI());
		Assert.assertEquals("http://lh:8080/test/aa/bb/test.xml", rc.createURI("test.xml").toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../../test.xml").toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../.././test.xml").toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI(".././../test.xml").toString());
//		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../././../test.xml").toString());
		
	
	}
	@Test
	public void testURIRelative() throws Exception{
		System.out.println(new File(".").toURI().getPath());
		String t = "classpath:///aa/bb/cc/.././../dd";
		System.out.println(new URI(t).normalize().getPath());
		System.out.println(new URI(t).resolve("/1.xx"));
		System.out.println(new URI(t).resolve("xx:./test.x"));
	}
	@Test
	public void testURIFile(){
		File f = new File("C:/1.txt");;
		System.out.println(f.toURI());
		System.out.println(ReflectUtil.map(f.toURI()));
		System.out.println(ReflectUtil.map(new File(".").toURI()));
	}

}
