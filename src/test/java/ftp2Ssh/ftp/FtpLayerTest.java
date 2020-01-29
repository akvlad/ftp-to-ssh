package ftp2Ssh.ftp;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.MockUtil;
import org.mockito.junit.MockitoJUnitRunner;

import expectj.TimeoutException;
import ftp2Ssh.ssh.AsyncCmdRes;
import ftp2Ssh.ssh.ISSHCommandHelper;
import ftp2Ssh.ssh.ISSHCommandHelperFactory;
import ftp2Ssh.ssh.ISSHLayerPool;
import ftp2Ssh.ssh.ISSHLayerPooled;
import ftp2Ssh.ssh.ISSHPoolFactory;

@RunWith(MockitoJUnitRunner.class)
public class FtpLayerTest {
	
	private FTPLayer layer;
	@Mock
	private ISSHPoolFactory poolFactoryMock;
	@Mock
	private ISSHCommandHelperFactory sshCommandHelperFactoryMock;
	@Mock
	private ISSHCommandHelper sshCommandHelperMock;
	@Mock
	private ISSHLayerPool sshLayerPoolMock;
	@Mock
	private ISSHLayerPooled sshLayerPooledMock;
	@Mock
	private FTPResponseListener ftpResponseListenerMock;
	@Mock
	private IFTPDataSock ftpDataSockMock;
	@Mock
	private IFTPRequestContext ctx;
	
	private void verifyResponse(String response) {
		Mockito.verify(ftpResponseListenerMock).onResponse(response);
	}
	
	private void verifyResponseEq(String response) {
		Mockito.verify(ftpResponseListenerMock).onResponse(Mockito.eq(response));
	}
	
	@Before
	public void initFTPLayer() throws IOException, InterruptedException, TimeoutException {
		layer = new FTPLayer();
		
		MockitoAnnotations.initMocks(this);
		
		Mockito.when(sshCommandHelperMock.DS()).thenReturn("/");
		Mockito.when(sshCommandHelperMock.tmpFolder()).thenReturn("/tmp");
		
		Mockito.when(poolFactoryMock.createPool(Mockito.any())).thenReturn(sshLayerPoolMock);
		Mockito.when(sshLayerPoolMock.acquire()).thenReturn(sshLayerPooledMock);
		Mockito.when(sshCommandHelperFactoryMock.getHelper()).thenReturn(sshCommandHelperMock);
		Mockito.when(sshCommandHelperFactoryMock.getDecoder()).thenReturn(Base64.getDecoder()::decode);
		
		layer.setSSHPoolFactory(poolFactoryMock);
		layer.setCmdHelperFactory(sshCommandHelperFactoryMock);
		layer.addResponseListener(ftpResponseListenerMock);
		layer.setDataSock(ftpDataSockMock);
		
		Mockito.when(sshCommandHelperMock.exit()).thenReturn("logout");
	}
	
	@Test
	public  void loginTest() {
		try {
			Mockito.when(sshCommandHelperMock.getSpawnCmd(Mockito.anyString(), Mockito.anyString())).thenReturn(new String[] {"SPAWN"});
			layer.CMD("USER a\n", ctx);
			verifyResponseEq("331 OK\r\n");
			layer.CMD("PASS b\n", ctx);
			Mockito.verify(sshCommandHelperMock).getSpawnCmd(Mockito.eq("a"), Mockito.eq("b"));
			Mockito.verify(poolFactoryMock).createPool(Mockito.eq(new String[] {"SPAWN"}));
			verifyResponseEq("230 OK\r\n");
			Mockito.reset(ftpResponseListenerMock);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}  
	
	@Test
	public void ftpQUITTest() throws IOException, InterruptedException, TimeoutException {
		this.loginTest();
		layer.CMD("QUIT", ctx);
		Mockito.verify(sshLayerPoolMock).close("logout");
	}
	
	@Test
	public  void ftpSYSTTest() {
		this.loginTest();
		layer.CMD("SYST", ctx);
		verifyResponseEq("215 UNIX Type: L8\r\n");
	}
	
	@Test
	public void ftpOPTSTest() throws FTPException {
		this.loginTest();
		layer.CMD("OPTS UTF8 ON", ctx);
		verifyResponseEq("200 OK.\r\n");
		layer.CMD("OPTS ASCII ON", ctx);
		verifyResponseEq("451 Sorry\r\n");
    }
	
	
	@Test
	public  void loginErrorTest() throws Exception {
		Mockito.when(sshLayerPoolMock.acquire()).thenThrow(new IOException("TEST EXCEPTION"));
		Mockito.when(sshCommandHelperMock.getSpawnCmd(Mockito.anyString(), Mockito.anyString())).thenReturn(new String[] {"SPAWN"});
		layer.CMD("USER a\n", ctx);
		verifyResponseEq("331 OK\r\n");
		layer.CMD("PASS b\n", ctx);
		Mockito.verify(sshCommandHelperMock).getSpawnCmd(Mockito.eq("a"), Mockito.eq("b"));
		Mockito.verify(poolFactoryMock).createPool(Mockito.eq(new String[] {"SPAWN"}));
		Mockito.verify(ftpResponseListenerMock).onResponse(Mockito.startsWith("451 TEST EXCEPTION"));
	} 
	
	@Test
	public void ftpNOOPTest() throws IOException, InterruptedException, TimeoutException {
		loginTest();
		Mockito.when(sshCommandHelperMock.echo(Mockito.eq("HI"))).thenReturn("echo HI");
		layer.CMD("NOOP", ctx);
		verifyResponseEq("200 OK.\r\n");
		Mockito.when(sshLayerPooledMock.cmd("echo HI")).thenThrow(new IllegalStateException("TEST"));
		layer.CMD("NOOP", ctx);
		Mockito.verify(ftpResponseListenerMock).onResponse(Mockito.startsWith("451 TEST"));
    }
	
	@Test
	public  void ftpTYPETest() {
		loginTest();
		layer.CMD("TYPE i", ctx);
		verifyResponseEq("200 Type set to I\r\n");
		layer.CMD("TYPE a", ctx);
		verifyResponseEq("200 Type set to A\r\n");
    }
	
	@Test
	public void ftpCDUPTest() throws IOException, InterruptedException, TimeoutException {
		loginTest();
		Mockito.when(sshCommandHelperMock.cd(Mockito.eq(".."))).thenReturn("TEST");
		layer.CMD("CDUP", ctx);
		Mockito.verify(sshLayerPooledMock).cmd("TEST");
		verifyResponseEq("200 OK.\r\n");
    }
	
	@Test
	public void ftpPWDTest() throws IOException, InterruptedException, TimeoutException {
		loginTest();
		Mockito.when(sshCommandHelperMock.pwd()).thenReturn("TEST");
		Mockito.when(sshLayerPooledMock.cmd("TEST")).thenReturn("/TEST");
		layer.CMD("PWD", ctx);
		Mockito.verify(sshLayerPooledMock).cmd("TEST");
		verifyResponseEq("257 /TEST\r\n");
    }
	
	@Test
	public void ftpCWDTest() throws IOException, InterruptedException, TimeoutException {
		loginTest();
		Mockito.when(sshCommandHelperMock.cd("TEST")).thenReturn("TEST");
        layer.CMD("CWD TEST", ctx);
        Mockito.verify(sshLayerPooledMock).cmd("TEST");
		verifyResponseEq("250 OK\r\n");
    }
	
	@Test
	public void ftpPORTTest() {
		loginTest();
		layer.CMD("PORT 192,168,0,1,255,255", ctx);
		Mockito.verify(ftpDataSockMock).activeMode("192.168.0.1", 0xFFFF);
		verifyResponseEq("200 Get port.\r\n");
	}
	
	@Test
	public void ftpPASVTest() {
		loginTest();
		Mockito.when(ftpDataSockMock.getHost()).thenReturn("192.168.0.1");
		Mockito.when(ftpDataSockMock.getPort()).thenReturn(0xFF0A);
		layer.CMD("PASV", ctx);
		verifyResponseEq("227 Entering Passive Mode (192,168,0,1,255,10).\r\n");
	}
	
	@Test
	public void ftpLISTTest() throws IOException, InterruptedException, TimeoutException {
		loginTest();
		Mockito.when(sshCommandHelperMock.lsla("TEST")).thenReturn("ls -la");
		Mockito.when(sshLayerPooledMock.cmd("ls -la")).thenReturn("-rw-rw-r--.   1 root root 297202 лип 29 16:10 artifacts.xml");
		layer.CMD("LIST TEST", ctx);
		verifyResponseEq("150 Here comes the directory listing.\r\n");
		Mockito.verify(ftpDataSockMock).write("-rw-rw-r--   1 root root 297202 лип 29 16:10 artifacts.xml\r\n");
	}
	
	@Test
	public void ftpSIZETest() throws IOException, InterruptedException, TimeoutException {
		loginTest();
		Mockito.when(sshCommandHelperMock.size("TEST")).thenReturn("TEST");
		Mockito.when(sshCommandHelperMock.size("EMPTY")).thenReturn("EMPTY");
		Mockito.when(sshLayerPooledMock.cmd("TEST")).thenReturn("TEST");
		Mockito.when(sshLayerPooledMock.cmd("EMPTY")).thenReturn("");
		layer.CMD("SIZE TEST", ctx);
		verifyResponseEq("213 TEST\r\n");
		layer.CMD("SIZE EMPTY", ctx);
		verifyResponseEq("213 0\r\n");
	}
	
	@Test
	public void ftpMKDTest() {
        loginTest();
        Mockito.when(sshCommandHelperMock.mkdir("TEST")).thenReturn("TEST");
        layer.CMD("MKD TEST", ctx);
        verifyResponseEq("257 Directory created.\r\n");
    }
	
	@Test
	public void ftpRMDTest() {
		loginTest();
        Mockito.when(sshCommandHelperMock.rmrf("TEST")).thenReturn("TEST");
        layer.CMD("RMD TEST", ctx);
        verifyResponseEq("250 Deleted\r\n");
        Mockito.reset(ftpResponseListenerMock);
        layer.CMD("DELE TEST", ctx);
        verifyResponseEq("250 Deleted\r\n");
    }
	
	@Test
	public void ftpMoveTest() throws IOException, InterruptedException, TimeoutException {
		loginTest();
		Mockito.when(sshCommandHelperMock.mv("TEST1", "TEST2")).thenReturn("TEST");
		layer.CMD("RNFR TEST1", ctx);
		verifyResponseEq("350 Ready.\r\n");
		layer.CMD("RNTO TEST2", ctx);
		Mockito.verify(sshCommandHelperMock).mv("TEST1", "TEST2");
		Mockito.verify(sshLayerPooledMock).cmd("TEST");
		verifyResponseEq("250 File renamed.\r\n");
	}
	
	@Test
	public void ftpRETRTest() throws IOException, InterruptedException, TimeoutException {
		loginTest();
		Mockito.when(sshCommandHelperMock.mkdir(Mockito.anyString())).thenReturn("mkdir TEST");
		Mockito.when(sshCommandHelperMock.lsw1(Mockito.anyString())).thenReturn("ls -w1 TEST");
		Mockito.when(sshLayerPooledMock.cmd("ls -w1 TEST")).thenReturn("F1\nF2\n");
		
		Mockito.when(sshCommandHelperMock.retrPiece(Mockito.endsWith("F1"))).thenReturn("RETR F1");
		Mockito.when(sshCommandHelperMock.retrPiece(Mockito.endsWith("F2"))).thenReturn("RETR F2");
		
		Mockito.when(sshLayerPoolMock.cmdA("RETR F1")).thenReturn(
				ConcurrentUtils.constantFuture(
						new AsyncCmdRes(null, Base64.getEncoder().encodeToString(new byte[] {0,0,0,0,0,1}))
				)
		);
		Mockito.when(sshLayerPoolMock.cmdA("RETR F2")).thenReturn(
				ConcurrentUtils.constantFuture(
						new AsyncCmdRes(null, Base64.getEncoder().encodeToString(new byte[] {0,0,0,0,0,2}))
				)
		);
		
		layer.CMD("RETR TEST", ctx);
		
		InOrder order = Mockito.inOrder(ftpDataSockMock, ftpResponseListenerMock);
		
		order.verify(ftpResponseListenerMock).onResponse("150 Opening data connection.\r\n");
		order.verify(ftpDataSockMock).write(new byte[] {0,0,0,0,0,1});
		order.verify(ftpDataSockMock).write(new byte[] {0,0,0,0,0,2});
		order.verify(ftpResponseListenerMock).onResponse("226 Transfer complete.\r\n");
				
	}
	
	@Test
	public void ftpRETRUnsuccessfullTest() throws IOException, InterruptedException, TimeoutException {
		loginTest();
		Mockito.when(sshCommandHelperMock.mkdir(Mockito.anyString())).thenReturn("mkdir TEST");
		Mockito.when(sshCommandHelperMock.lsw1(Mockito.anyString())).thenReturn("ls -w1 TEST");
		Mockito.when(sshLayerPooledMock.cmd("ls -w1 TEST"))
			.thenThrow(new IOException("TEST"))
			.thenReturn("F1\nF2\n");
		
		layer.CMD("RETR TEST", ctx);
		
		verifyResponseEq("451 TEST\r\n");
		
		Mockito.reset(ftpResponseListenerMock);
		
		Mockito.when(sshCommandHelperMock.retrPiece(Mockito.endsWith("F1"))).thenReturn("RETR F1");
		Mockito.when(sshCommandHelperMock.retrPiece(Mockito.endsWith("F2"))).thenReturn("RETR F2");
				
		Mockito.when(sshLayerPoolMock.cmdA("RETR F1")).thenReturn(
				ConcurrentUtils.constantFuture(
						new AsyncCmdRes(null, Base64.getEncoder().encodeToString(new byte[] {0,0,0,0,0,1}))
				)
		);
		Mockito.when(sshLayerPoolMock.cmdA("RETR F2")).thenReturn(
				ConcurrentUtils.constantFuture(
						new AsyncCmdRes(new IOException("TEST 2"), null)
				)
		);
		
		layer.CMD("RETR TEST", ctx);
		
		verifyResponseEq("150 Opening data connection.\r\n");
		verifyResponseEq("451 TEST 2\r\n");
				
	}
	
	@Test
	public void ftpSTORTest() throws Exception {
        loginTest();
        Mockito.when(sshCommandHelperMock.mkdir(Mockito.anyString())).thenReturn("mkdir TMP");
        Mockito.when(sshCommandHelperMock.storePiece(Mockito.any(), Mockito.anyString())).thenReturn("STORE PIECE");
        Mockito.when(sshLayerPoolMock.cmdA("STORE PIECE")).thenReturn(
        		ConcurrentUtils.constantFuture(
        				new AsyncCmdRes(null, null)
        		)
        );
        Mockito.when(sshCommandHelperMock.joinStoredPieces(Mockito.anyString(), Mockito.anyString())).thenReturn("TEST");
        Mockito.when(sshCommandHelperMock.rmrf(Mockito.anyString())).thenReturn("TEST");
        Mockito.when(ftpDataSockMock.read(Mockito.anyInt()))
        	.thenReturn(new byte[] {0,0,0,0,0,1})
        	.thenReturn(new byte[] {0,0,0,0,0,2})
        	.thenReturn(null);
        
        layer.CMD("STOR TEST", ctx);
        
        InOrder order = Mockito.inOrder(sshCommandHelperMock, ftpResponseListenerMock);
        
        order.verify(ftpResponseListenerMock).onResponse("150 Opening data connection.\r\n");
        order.verify(sshCommandHelperMock).storePiece(Mockito.eq(new byte[] {0,0,0,0,0,1}), Mockito.anyString());
        order.verify(sshCommandHelperMock).storePiece(Mockito.eq(new byte[] {0,0,0,0,0,2}), Mockito.anyString());
        order.verify(ftpResponseListenerMock).onResponse("226 Transfer complete.\r\n");
    }
	
	@Test
	public void ftpSTORTestUnsuccessfull() throws Exception {
        loginTest();
        Mockito.when(sshCommandHelperMock.mkdir(Mockito.anyString())).thenReturn("mkdir TMP");
        Mockito.when(sshLayerPooledMock.cmd("mkdir TMP"))
        	.thenThrow(new IOException("TEST 1"))
        	.thenReturn(null);
        Mockito.when(ftpDataSockMock.read(Mockito.anyInt()))
        	.thenThrow(new IOException("TEST 2"));
        
        layer.CMD("STOR TEST", ctx);
        
        verifyResponseEq("451 TEST 1\r\n");
        
        layer.CMD("STOR TEST", ctx);
        
        verifyResponseEq("150 Opening data connection.\r\n");
        verifyResponseEq("451 TEST 2\r\n");
    }

}
