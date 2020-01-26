package ftp2Ssh.ssh;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;

import expectj.TimeoutException;
import ftp2Ssh.ssh.impl.SSHLayer;
import ftp2Ssh.ssh.impl.SSHLayerPool;

public class SSHLayerPoolTest {
	
	@Mock
	private ISSHCommandHelperFactory sshCommandHelperFactory;
	
	@Mock
	private ISSHCommandHelper sshCommandHelper;
	
	private SSHLayerPool pool;
	
	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		
		Mockito.when(sshCommandHelperFactory.getHelper()).thenReturn(sshCommandHelper);
		Mockito.when(sshCommandHelper.echo(Mockito.anyString())).then((InvocationOnMock invocation) -> {
			return "echo " + invocation.getArgument(0, String.class);
		});
	}
	
	@Test
	public void acquireTest() throws IOException, InterruptedException, TimeoutException {
		pool = new SSHLayerPool("bash", 1);
		pool.setSshCmdHelperFactory(sshCommandHelperFactory);
		ISSHLayerPooled pooled = pool.acquire();
		String out = pooled.cmd("ls -la");
		Assert.assertEquals(out, TestBase.getLsLa());
		pooled.release();
		pool.close("exit");
	}
	
	@Test
	public void doubleAcquireTest() throws IOException, InterruptedException, TimeoutException {
		pool = new SSHLayerPool("bash", 2);
		pool.setSshCmdHelperFactory(sshCommandHelperFactory);
		ISSHLayerPooled pooled1 = pool.acquire();
		String out = pooled1.cmd("ls -la");
		Assert.assertEquals(out, TestBase.getLsLa());
		ISSHLayerPooled pooled2 = pool.acquire();
		out = pooled2.cmd("ls -la");
		Assert.assertEquals(out, TestBase.getLsLa());
		pooled1.release();
		pooled2.release();
		pool.close("exit");
	}
	
	@Test
	public void insufficientLayersTest() throws IOException, InterruptedException, TimeoutException {
		pool = new SSHLayerPool("bash", 1);
		pool.setSshCmdHelperFactory(sshCommandHelperFactory);
		ISSHLayerPooled pooled1 = pool.acquire();
		pool.setTimeoutMSec(1000);
		Assert.assertThrows(IllegalStateException.class, () -> {
			pool.acquire();			
		});
		pooled1.release();
		pool.close("exit");
	}
	
	@Test
	public void queuedAcquiresTest() throws IOException, InterruptedException, TimeoutException {
		pool = new SSHLayerPool("bash", 1);
		pool.setSshCmdHelperFactory(sshCommandHelperFactory);
		ISSHLayerPooled pooled1 = pool.acquire();
		pooled1.release();
		ISSHLayerPooled pooled2 = pool.acquire();
		pooled2.release();
		pool.close("exit");
	}
	
	@Test
	public void forceCloseTest() throws IOException, InterruptedException, TimeoutException {
		pool = new SSHLayerPool("bash", 1);
		pool.setSshCmdHelperFactory(sshCommandHelperFactory);
		ISSHLayerPooled pooled1 = pool.acquire();
		Assert.assertThrows(IllegalStateException.class, () -> {
			pool.close("exit");
		});
		pool.forceClose("exit");
		Assert.assertTrue(pooled1.isClosed());
	}
	
	@Test
	public void wrongCmdCloseTest() throws IOException, InterruptedException, TimeoutException {
		pool = new SSHLayerPool("bash", 1);
		pool.setSshCmdHelperFactory(sshCommandHelperFactory);
		ISSHLayerPooled pooled1 = pool.acquire();
		pool.forceClose("echo 1");
		Assert.assertTrue(pooled1.isClosed());
	}
	
	@Test
	public void cmdATest() throws IOException, InterruptedException, TimeoutException, ExecutionException {
		pool = new SSHLayerPool("bash", 1);
		pool.setSshCmdHelperFactory(sshCommandHelperFactory);
		Future<AsyncCmdRes> f = pool.cmdA("ls -la");
		AsyncCmdRes res = f.get();
		Assert.assertEquals(res.res, TestBase.getLsLa());
	} 
	
	@Test
	public void cmdAOverflowTest() throws IOException, InterruptedException, TimeoutException, ExecutionException {
		pool = new SSHLayerPool("bash", 1);
		pool.setSshCmdHelperFactory(sshCommandHelperFactory);
		Future<AsyncCmdRes> f1 = pool.cmdA("ls -la");
		Future<AsyncCmdRes> f2 = pool.cmdA("ls -la");
		AsyncCmdRes res1 = f1.get(),
				res2 = f2.get();
		Assert.assertEquals(res1.res, TestBase.getLsLa());
		Assert.assertEquals(res2.res, TestBase.getLsLa());
		pool.close("exit");
	}
	
	@Test
	public void cmdAInsufficientExecutorsTest() throws IOException, InterruptedException, TimeoutException, ExecutionException {
		pool = new SSHLayerPool("bash", 1);
		pool.setSshCmdHelperFactory(sshCommandHelperFactory);
		pool.setTimeoutMSec(1000);
		pool.acquire();
		Assert.assertThrows(IllegalStateException.class, () -> {
			pool.cmdA("ls -la");
		});
		pool.forceClose("exit");
	}
	
	@Test
	public void sshFailedTest() throws IOException, InterruptedException, TimeoutException {
		pool = new SSHLayerPool("date", 1);
		pool.setSshCmdHelperFactory(sshCommandHelperFactory);
		pool.setTimeoutMSec(1000);
		Assert.assertThrows(IllegalStateException.class, () -> {
			pool.acquire();			
		});
		
	}
}
