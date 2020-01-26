package ftp2Ssh.tcp.management;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;

import ftp2Ssh.ftp.FTPLayer;
import ftp2Ssh.ftp.FTPLayerFactory;

public class CommandAdapterTest {
	
	private CommandAdapter cmdAdapter;
	private ManagementServerInit serverInit;
	@Mock
	private FTPLayer layer;
	@Mock
	private FTPLayerFactory ftpLayerFactory;
	
	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		cmdAdapter = new CommandAdapter();
		serverInit = new ManagementServerInit();
		serverInit.setCommandAdapter(cmdAdapter);
		cmdAdapter.setFtpLayerFactory(ftpLayerFactory);
		Mockito.when(ftpLayerFactory.getFTPLayer()).thenReturn(layer);
		cmdAdapter.setFtpLayerFactory(ftpLayerFactory);
	}
	
	@Test
	public void cmdTest() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		Executor exec = Executors.newSingleThreadExecutor();
		FutureTask<Object> ft = new FutureTask<Object>((Callable<Object>) () -> {
			serverInit.run();
			return null;
		});
		serverInit.setHost("127.0.0.1");
		serverInit.setPort(8080);
		exec.execute(() -> {
			ft.run();
		});
		Socket sock = null;
		Thread.sleep(1000);
		try {
		sock = new Socket("127.0.0.1", 8080);
		sock.getOutputStream().write("USER a\n".getBytes(StandardCharsets.UTF_8));
		sock.getOutputStream().flush();
		sock.getOutputStream().write("USER b".getBytes(StandardCharsets.UTF_8));
		sock.getOutputStream().flush();
		Thread.sleep(500);
		sock.getOutputStream().write("USER c\n".getBytes(StandardCharsets.UTF_8));
		sock.getOutputStream().flush();
		Thread.sleep(100);
		} finally {
			if (sock != null) sock.close();
			serverInit.close();
			ft.get(1000, TimeUnit.MILLISECONDS);
			
		}
		
		
		Mockito.verify(layer).CMD(Mockito.eq("USER a"), Mockito.any());
		Mockito.verify(layer).CMD(Mockito.eq("USER b"), Mockito.any());
		Mockito.verify(layer).CMD(Mockito.eq("USER c"), Mockito.any());
		Mockito.verify(ftpLayerFactory, Mockito.times(1)).getFTPLayer();
		
	}

}
