package ftp2Ssh.ssh.commands;

import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SSHGzippedDecoderTest {
	private SSHGzippedDecoder decoder;
	
	@Before
	public void init() {
		decoder = new SSHGzippedDecoder();
	}
	
	@Test
	public void decode() throws UnsupportedEncodingException {
		Assert.assertArrayEquals(
				"12345\n".getBytes("UTF8"), 
				decoder.decode("H4sICCc5I14AAzEAMzQyNjHlAgDmrx0mBgAAAA==")
		);
	}
}
