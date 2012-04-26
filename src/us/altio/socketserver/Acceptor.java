package us.altio.socketserver;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import us.altio.socketserver.model.Event;

import com.google.common.base.Function;

public class Acceptor {

	private static final Logger LOGGER = Logger.getLogger("Acceptor");
	static List<WeakReference<Thread>> threads = new ArrayList<WeakReference<Thread>>();

	HttpSolrServer server = new HttpSolrServer("http://localhost:8983/solr/");

	public final Thread processorThread = new Thread(new Runnable() {

		@Override
		public void run() {
			while (true) {
				try {
					Event event = queue.poll(10, TimeUnit.SECONDS);
					Long ctr=0L;
					if (event!=null)
					{
						insertInIndex(event);
						ctr++;
					}
					else
					{
						commitIndex();
					}
					
					if (ctr%5000==0)
					{
						commitIndex();
					}
				} catch (InterruptedException e) {
					// disregard that
				} catch (IOException e) {
					LOGGER.error(e, e);
				} catch (SolrServerException e) {
					LOGGER.error(e, e);
				}
			}
		}

		private void commitIndex() throws SolrServerException, IOException {
			server.commit();
		}
	});
	static BlockingQueue<Event> queue = new LinkedBlockingQueue<Event>();

	public Acceptor(int port) {
		try {
			processorThread.start();
			try {

				ServerSocket serverSocket = new ServerSocket(port);

				while (true) {
					final Socket socket = serverSocket.accept();
					LOGGER.info("Accepted:" + socket);

					Thread t = new Thread(new SocketRunnable(socket, queue,
							doneFunction));
					threads.add(new WeakReference<Thread>(t));
					t.start();
				}
			} catch (IOException e) {
				LOGGER.error(e);
			}

		} catch (Exception e) {
			LOGGER.fatal(e, e);
		} finally {
			// server.close() ?
		}
	}

	protected void insertInIndex(Event event) throws IOException,
			SolrServerException {
		Long milis = System.nanoTime();

		SolrInputDocument sid = new SolrInputDocument();
		sid.addField("logger", event.getLogger());
		sid.addField("level", event.getLevel());
		sid.addField("thread", event.getThread());
		sid.addField("timestamp", event.getTimestamp());
		sid.addField("message", event.getMessage());
		sid.addField("throwable", event.getThrowable());
		UpdateResponse response = server.add(sid);

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
		//ServiceLocator.threadUncommitedCounter = new PriorityBlockingQueue<ThreadUncommitedCounter>(128);
	}

}
