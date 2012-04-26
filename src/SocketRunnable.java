import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import model.Event;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

import com.google.common.base.Function;

public class SocketRunnable implements Runnable {
	private static final Logger LOGGER = Logger.getLogger("SocketRunnable");
	private Socket socket;
	private BlockingQueue<Event> queue;
	public final Function<Pair<Thread, Socket>, Void> doneFunction;

	public SocketRunnable(Socket socket, BlockingQueue<Event> queue, Function<Pair<Thread, Socket>, Void> doneFunction) {
		this.socket = socket;
		this.queue = queue;
		this.doneFunction = doneFunction;
	}

	@Override
	public void run() {
		BufferedReader reader = null;
		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			SAXParser parser = factory.newSAXParser();

			reader = prepareReaderFromStream(socket.getInputStream());
			InputSource inputSource = new InputSource(reader);
			inputSource.setSystemId("file://" + new File(".").getAbsolutePath());
			inputSource.setEncoding("UTF-8");

			parser.parse(inputSource, new XMLHandler(acceptFunction));

		} catch (Exception e) {
			LOGGER.error(e, e);
		} finally {
			IOUtils.closeQuietly(reader);
			doneFunction.apply(Pair.of(Thread.currentThread(), this.socket));
		}

	}

	public final Function<Event, Void> acceptFunction = new Function<Event, Void>() {

		@Override
		public Void apply(Event evt) {
			boolean ok = false;
			while (!ok) {
				try {
					SocketRunnable.this.queue.put(evt);
					ok = true;
				} catch (InterruptedException e) {
					// disregard this
				}
			}
			return null;
		}
	};

	private BufferedReader prepareReaderFromStream(InputStream inputStream) {
		ByteArrayInputStream baisStart = new ByteArrayInputStream(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?><log4j:messages>".getBytes());
		ByteArrayInputStream baisEnd = new ByteArrayInputStream("\r\n</log4j:messages>\r\n".getBytes());
		SequenceInputStream targetInputStream = new SequenceInputStream(new ArrayEnumeration(new InputStream[] {
				baisStart, inputStream, baisEnd }));
		return new BufferedReader(new InputStreamReader(targetInputStream));
	}
}
