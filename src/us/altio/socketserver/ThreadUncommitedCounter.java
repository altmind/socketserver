package us.altio.socketserver;
import java.lang.ref.WeakReference;


public class ThreadUncommitedCounter implements Comparable<ThreadUncommitedCounter> {
	Long uncommitedRecords;
	WeakReference<Thread> thread;
	@Override
	public int compareTo(ThreadUncommitedCounter o) {
		return this.uncommitedRecords.compareTo(o.uncommitedRecords);
	}
	
}
