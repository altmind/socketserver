package us.altio.socketserver;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.Callable;


import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import us.altio.socketserver.model.Event;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Functions;

public class XMLHandler extends DefaultHandler {

	Logger LOG = Logger.getLogger(this.getClass());

	Stack<String> tagStack = new Stack<String>();

	Event event = null;

	private Function<Event, Void> callback;

	public XMLHandler(Function<Event, Void> callback) {
		super();
		this.callback = callback;
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		processCharacters(false);
		tagStack.push(qName);
		if (qName.endsWith(":event")) {
			event = new Event();
			event.setLevel(attributes.getValue("level"));
			event.setLogger(attributes.getValue("logger"));
			event.setThread(attributes.getValue("thread"));
			event.setTimestamp(attributes.getValue("timestamp") == null ? null : Long.parseLong(attributes
					.getValue("timestamp")));
		} else if (qName.endsWith(":data")) {
			event.getProperties().put(attributes.getValue("name"), attributes.getValue("value"));
		}
	}


	StringBuffer sb = new StringBuffer();

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String message = new String(ch, start, length);
		sb.append(message);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		processCharacters(true);
		String closedTag = tagStack.pop();
		if (closedTag.endsWith(":event")) {
			callback.apply(event);
		}
	}

	private void processCharacters(boolean runEvents) {
		if (runEvents && sb.length()>0 && !CharMatcher.WHITESPACE.matchesAllOf(sb.toString()))
		{
			
			if (tagStack.peek().endsWith(":message")) {
				event.setMessage(sb.toString());
			} else if (tagStack.peek().endsWith(":throwable")) {
				event.setThrowable(sb.toString());
			} else
				throw new IllegalArgumentException("Tag Stack contains invalid element " + tagStack.peek());
		}
		sb.setLength(0);
	}

	@Override
	public void error(SAXParseException paramSAXParseException) throws SAXException {
		LOG.error(paramSAXParseException);
		super.error(paramSAXParseException);
	}

	@Override
	public void fatalError(SAXParseException paramSAXParseException) throws SAXException {
		LOG.fatal(paramSAXParseException);
		super.fatalError(paramSAXParseException);
	}

}
