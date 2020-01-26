package ftp2Ssh.ssh;

import java.io.IOException;
import java.io.InputStream;

public class TestBase {
	public static String getLsLa() throws IOException, InterruptedException {
		Process proc = Runtime.getRuntime().exec("ls -la");
		proc.waitFor();
		InputStream expectedstream = proc.getInputStream();
		byte[] buff = new byte[expectedstream.available()];
		expectedstream.read(buff);
		return new String(buff);
	}
}
