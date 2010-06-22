package org.jside.webserver.action;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;

import org.jside.webserver.RequestContext;
import org.jside.webserver.RequestUtil;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parser.impl.ResourceContextImpl;

@SuppressWarnings("unchecked")
public class TemplateAction extends ResourceContextImpl {
	private static Constructor<TemplateEngine> DEFAULT_HOT_ENGINE;
	protected Constructor<TemplateEngine> hotEngine = DEFAULT_HOT_ENGINE;
	
	static {
		try {
			String hotClass = "org.xidea.lite.parser.impl.HotTemplateEngine";
			DEFAULT_HOT_ENGINE = (Constructor<TemplateEngine>) Class.forName(hotClass)
					.getConstructor(URI.class, URI.class);
		} catch (Throwable w) {
			DEFAULT_HOT_ENGINE = null;
		}
	}
	private String contentType = "text/html";
	private TemplateEngine engine;

	public TemplateAction(URI base) {
		super(base);
	}
	public TemplateAction(Constructor<TemplateEngine> hotEngine,URI base) {
		super(base);
		this.hotEngine = hotEngine;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void reset(RequestContext requestContext) {
		URI config=null;
		if (requestContext != null) {
			URI newRoot = requestContext.getResource("/");
			config = requestContext.getResource("/WEB-INF/lite.xml");
			if (newRoot != null) {
				if (base == null || !newRoot.equals(base)) {
					engine = null;
					base = newRoot;
				}
			}
		}
		if (engine == null) {
			try {
				engine = hotEngine.newInstance(base, config);
			} catch (Throwable w) {
				engine = new TemplateEngine(base);
			}
		}
	}

	public URI createURI(String path, URI parentURI) {
		if (parentURI == null) {
			URI result = getResource(path);
			if (result != null) {
				return result;
			}
			parentURI = base;
		}
		return super.createURI(path, parentURI);
	}

	public void execute() throws IOException {
		RequestContext context = RequestUtil.get();
		OutputStreamWriter out = new OutputStreamWriter(context
				.getOutputStream(), context.getEncoding());
		if (contentType != null) {
			context.setMimeType(contentType);
		}
		reset(context);
		render(context.getRequestURI(), context.getContextMap(), out);
	}

	public void render(String path, Object context, Writer out)
			throws IOException {
		engine.render(path, context, out);
	}

	protected URI getResource(String pagePath) {
		try {
			if (base == null) {
				RequestContext context = RequestUtil.get();
				URI uri = context.getResource(pagePath);
				if (uri != null) {
					uri = toExistResource(uri);
					if (uri != null) {
						return uri;
					}
				}
			} else {
				if (pagePath.length() == 0) {
					return base;
				}
				if (pagePath.startsWith("/")) {
					pagePath = pagePath.substring(1);
				}
				URI uri = base.resolve(pagePath);
				uri = toExistResource(uri);
				if (uri != null) {
					return uri;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	private URI toExistResource(URI uri) throws MalformedURLException {
		File file = null;
		if (uri.getScheme().equals("file")) {
			file = new File(uri);
			// }else if (uri.getScheme().equals("classpath")) {
		}
		if (file != null) {
			if (file.exists()) {
				return uri;
			}
		} else {
			return uri;
		}
		return null;
	}

}