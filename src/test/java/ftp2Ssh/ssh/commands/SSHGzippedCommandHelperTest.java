package ftp2Ssh.ssh.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

public class SSHGzippedCommandHelperTest {
	private SSHGzippedCommandHelper helper;
	
	@Before 
	public void init() {
		helper = new SSHGzippedCommandHelper();
	}
	
	@Test
	public void storePiece() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);
		gzos.write(new byte[] {1,2,3,4,5});
		gzos.flush();
		gzos.finish();
		byte[] piece = baos.toByteArray();
		gzos.close();
		baos.close();
		Assert.assertEquals(
				"echo \""+ Base64.getEncoder().encodeToString(piece) + "\" >>\"/tmp/0\"\n", 
				helper.storePiece(new byte[] {1,2,3,4,5}, "/tmp")
		);
		
	}

}
