import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import model.Event;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.index.IndexRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;

import com.google.common.base.Function;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class Acceptor {

	private static final Logger LOGGER = Logger.getLogger("Acceptor");
	static List<WeakReference<Thread>> threads = new ArrayList<WeakReference<Thread>>();
	// public static Mongo mongo;
	private Node elasticNode;
	private Client elasticClient;

	public final Thread processorThread = new Thread(new Runnable() {

		@Override
		public void run() {
			// DB db = mongo.getDB("log");
			// DBCollection coll = db.getCollection("log");
			while (true) {
				try {
					Event event = queue.take();
					// insertIntoCollection(coll, event);
					insertIntoES(event);
				} catch (InterruptedException e) {
					// disregard that
				} catch (IOException e) {
					LOGGER.error(e, e);
				}
			}
		}
	});
	static BlockingQueue<Event> queue = new LinkedBlockingQueue<Event>();

	public Acceptor(int port) {
		try {
			// mongo = new Mongo("localhost");
			elasticNode = nodeBuilder().node();
			elasticClient = elasticNode.client();
			processorThread.start();
			try {

				ServerSocket serverSocket = new ServerSocket(port);

				while (true) {
					final Socket socket = serverSocket.accept();
					LOGGER.info("Accepted:" + socket);

					Thread t = new Thread(new SocketRunnable(socket, queue, doneFunction));
					threads.add(new WeakReference<Thread>(t));
					t.start();
				}
			} catch (IOException e) {
				LOGGER.error(e);
			}

		} catch (Exception e) {
			LOGGER.fatal(e, e);
		} finally {
			if (elasticNode != null)
				elasticNode.close();
		}
	}

	protected void insertIntoES(Event event) throws IOException {
		Long milis = System.nanoTime();

		

		IndexRequestBuilder elasticIndex = elasticClient.prepareIndex("log", "events");
		XContentBuilder builderMain = jsonBuilder().startObject().field("logger", event.getLogger())
				.field("level", event.getLevel()).field("thread", event.getThread())
				.field("timestamp", event.getTimestamp()).field("message", event.getMessage())
				.field("throwable", event.getThrowable());

		/*XContentBuilder builderProperties = jsonBuilder().startObject();
		for (String items : event.getProperties().keySet()) {
			builderProperties.field(items, event.getProperties().get(items));
		}
		builderProperties.endObject();*/
		builderMain.array("properties",event.getProperties().keySet().toArray());
		
		builderMain = builderMain.endObject();
		
		String string = builderMain.string();
		
		IndexResponse response = elasticIndex.setSource(builderMain).execute().actionGet();

		Long delta = System.nanoTime() - milis;
		LOGGER.info("insert took: " + delta / 1000000.0 + " ms");
	}

	static final Function<Pair<Thread, Socket>, Void> doneFunction = new Function<Pair<Thread, Socket>, Void>() {

		@Override
		public synchronized Void apply(Pair<Thread, Socket> arg) {
			Iterator<WeakReference<Thread>> iterator = threads.iterator();
			while (iterator.hasNext()) {
				WeakReference<Thread> item = iterator.next();
				if (arg.getLeft().equals(item.get()) || item.get() == null) {
					iterator.remove();
				}
			}
			IOUtils.closeQuietly(arg.getRight());
			return null;
		}
	};

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ServiceLocator.acceptor = new Acceptor(8111);
	}

	protected static void insertIntoCollection(DBCollection coll, Event event) {
		Long milis = System.nanoTime();
		BasicDBObject doc = new BasicDBObject();

		doc.put("logger", event.getLogger());
		doc.put("level", event.getLevel());
		doc.put("thread", event.getThread());
		doc.put("timestamp", event.getTimestamp());
		doc.put("message", event.getMessage());
		doc.put("throwable", event.getThrowable());
		BasicDBObject props = new BasicDBObject();
		for (String items : event.getProperties().keySet()) {
			props.put(items, event.getProperties().get(items));
		}
		doc.put("properties", props);

		coll.insert(doc);
		Long delta = System.nanoTime() - milis;
		LOGGER.info("insert took: " + delta / 1000000.0 + " ms");
	}
}
