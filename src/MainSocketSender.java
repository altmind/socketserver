import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.io.IOUtils;

public class MainSocketSender {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Socket echoSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		try {
			echoSocket = new Socket("localhost", 8111);
			echoSocket.setKeepAlive(true);
			echoSocket.setSoTimeout(1000);
			out = new PrintWriter(echoSocket.getOutputStream(), true);
			while(true)
			{
				out.println("ZOMG!\r\n");
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			IOUtils.closeQuietly(out);
		}
	}

}
