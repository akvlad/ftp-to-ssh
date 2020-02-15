package stp2Ssh.integration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.Test;

import ftp2Ssh.SpringContext;
import ftp2Ssh.configuration.ConfigurationProvider;
import ftp2Ssh.ssh.ISSHPoolFactory;
import ftp2Ssh.tcp.management.ManagementServerInit;

public class Integration {
	
	@Test
	public void test() throws UnknownHostException, IOException, InterruptedException {
		SpringContext.getContext().getBean(ConfigurationProvider.class).setTimeoutSec(5);
		SpringContext.getContext().getBean(ConfigurationProvider.class).setMaxLayers(10);
		ManagementServerInit init = SpringContext.getContext().getBean(ManagementServerInit.class);
		ExecutorService server = Executors.newFixedThreadPool(1);
		server.execute(() -> {
			try {
				init.run();
			} catch (Throwable e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		});
		Thread.sleep(1000);
		FTPClient ftp = new FTPClient();
		ftp.connect("127.0.0.1", 8888);
		ftp.login("a", "b");
		FTPFile[] files = ftp.listFiles();
		for (FTPFile f : files) {
			System.out.println(f.getName());
		}
		byte[] longFile = new byte[1000000];
		for (int i = 0; i < 1000000; i++) {
			longFile[i] = 0;
		}
		long start = (new Date()).getTime();
		for (int j = 0; j < 5; j++) {
			ftp.removeDirectory("FILE1");
			boolean rep = ftp.storeFile("FILE1", new ByteArrayInputStream("HELLO \n".getBytes(StandardCharsets.UTF_8)));
			assert (rep);
			ByteArrayOutputStream oStream = new ByteArrayOutputStream();
			rep = ftp.retrieveFile("FILE1", oStream);
			assert (rep);
			assertArrayEquals("HELLO \n".getBytes(StandardCharsets.UTF_8), oStream.toByteArray());
			oStream.reset();
			rep = ftp.storeFile("LONGFILE", new ByteArrayInputStream(longFile));
			assert (rep);
			rep = ftp.retrieveFile("LONGFILE", oStream);
			assert (rep);
			assertArrayEquals(longFile, oStream.toByteArray());
			rep = ftp.removeDirectory("FILE1");
			assert(rep);
			rep = ftp.removeDirectory("LONGFILE");
			assert(rep);
		}
		System.out.printf("Time: %d sec\n", ((new Date()).getTime() - start) / 1000);
		
		//HUGE FILE TEST
		
		
		init.close();

	}
	
	@Test
	public void hugeFileTest() throws IOException, InterruptedException {
		if (System.getenv("HUGE_TESTS") != "1") {
			return;
		}
		SpringContext.getContext().getBean(ConfigurationProvider.class).setTimeoutSec(5);
		SpringContext.getContext().getBean(ConfigurationProvider.class).setMaxLayers(10);
		SpringContext.getContext().getBean(ConfigurationProvider.class).setChunkSize(50000);
		ManagementServerInit init = SpringContext.getContext().getBean(ManagementServerInit.class);
		
		ExecutorService server = Executors.newFixedThreadPool(1);
		server.execute(() -> {
			try {
				init.run();
			} catch (Throwable e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		});
		Thread.sleep(1000);
		FTPClient ftp = new FTPClient();
		ftp.connect("127.0.0.1", 8888);
		ftp.login("a", "b");
		
		long start = (new Date()).getTime();
		boolean rep = false;

		rep = ftp.storeFile("HUGEFILE", new InputStream() {
			
			int readBytes = 0;
			
			@Override
			public int read() throws IOException {
				if (readBytes >= 100000000) {
					return -1;
				}
				readBytes++;
				return 'A' + (readBytes % 26);
			}
		});
		assert (rep);
		int time1 = (int) (((new Date()).getTime() - start) / 1000);
		
		
		start = (new Date()).getTime();
		ByteArrayOutputStream oStream = new ByteArrayOutputStream();
		rep = ftp.retrieveFile("HUGEFILE", oStream);
		assert(rep);
		System.out.printf("Send Time:    %d sec\n", time1);
		System.out.printf("Receive Time: %d sec\n", ((new Date()).getTime() - start) / 1000);
		
		
		byte[] buff = oStream.toByteArray();
		assert(buff.length == 100000000);
		int i=0;
		for (byte b : oStream.toByteArray()) {
			i++;
			assert(b == 'A' + (i%26));
		}
		
		init.close();
	}
}
