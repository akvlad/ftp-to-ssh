package ftp2Ssh.tcp.data;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TCPDataSockTest {
	
	private TCPDataSock sock;
	private ServerSocket srvSock;
	private Socket clientSock;
	
	private void startActiveMode() throws UnknownHostException, IOException {
		srvSock = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));
		srvSock.setSoTimeout(300);
		String host = srvSock.getInetAddress().getHostAddress();
		int port = srvSock.getLocalPort();
		sock.activeMode(host, port);
		sock.startDataSock("127.0.0.1");
		clientSock = srvSock.accept();
	}
	
	private void stopActiveMode() throws IOException {
		clientSock.close();
		srvSock.close();
		sock.stopDataSock();
		sock.close();
	}
	
	@Before
	public void init() {
		sock = new TCPDataSock();
	}
	
	@Test
	public void passiveMode() throws UnknownHostException, IOException {
		sock.passiveMode("127.0.0.1");
		Socket client = new Socket(sock.getHost(), sock.getPort());
		client.close();
		sock.stopDataSock();
		sock.close();
	}

	@Test
	public void activeMode() throws IOException {
		startActiveMode();
		clientSock.close();
		srvSock.close();
	}

	@Test
	public void startDataSock() throws Exception {
		sock.passiveMode("127.0.0.1");
		FutureTask<Object> ft = new FutureTask<>((Callable<Object>)() -> {sock.startDataSock("127.0.0.1"); return null;});
		Executors.newSingleThreadExecutor().execute(ft::run);
		Socket client = new Socket(sock.getHost(), sock.getPort());
		ft.get();
		client.getOutputStream().write(new byte[] {1,2,3,4,5});
		Assert.assertArrayEquals(new byte[] {1,2,3,4,5}, sock.read(5));
		client.close();
		sock.stopDataSock();
		sock.close();
	}

	
	
	@Test
	public void write() throws IOException {
		startActiveMode();
		sock.write(new byte[] {1,2,3,4,5});
		byte[] actual = new byte[5];
		int read = clientSock.getInputStream().read(actual);
		stopActiveMode();
		Assert.assertArrayEquals(new byte[] {1,2,3,4,5}, actual);
		Assert.assertEquals(5, read);
	}

	@Test
	public void read() throws Exception {
		startActiveMode();
		clientSock.getOutputStream().write(new byte[] {1,2,3,4,5});
		assertArrayEquals(new byte[] {1,2,3,4,5}, sock.read(6));
		stopActiveMode();
		
	}

}
