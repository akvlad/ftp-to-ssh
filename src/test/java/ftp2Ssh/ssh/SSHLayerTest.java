package ftp2Ssh.ssh;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import expectj.TimeoutException;
import ftp2Ssh.ssh.impl.SSHLayer;


public class SSHLayerTest {
	
	@Mock
	public ISSHCommandHelper sshCommandHelper;
	@Mock
	public ISSHCommandHelperFactory sshCommandHelperFactory;
	
	public SSHLayer layer;
	
	@Before
	public void init() throws IOException, TimeoutException, InterruptedException {
		MockitoAnnotations.initMocks(this);
		Mockito.when(sshCommandHelperFactory.getHelper()).thenReturn(sshCommandHelper);
		Mockito.when(sshCommandHelper.echo(Mockito.anyString())).then((InvocationOnMock invocation) -> {
				return "echo " + invocation.getArgument(0, String.class);
		});
		layer = new SSHLayer("bash", sshCommandHelperFactory);
	}
	
	
	@Test
	public void SSHLayerCmdTest() throws IOException, TimeoutException, InterruptedException {
		String out = layer.cmd("ls -la");
		String expected = TestBase.getLsLa();
		Assert.assertEquals(expected, out);
		out = layer.cmd("ls -la");
		Assert.assertEquals(expected, out);
		layer.close("exit");
	}
}
