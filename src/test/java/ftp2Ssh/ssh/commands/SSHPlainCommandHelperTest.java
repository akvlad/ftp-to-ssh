package ftp2Ssh.ssh.commands;

import java.util.Arrays;
import java.util.Base64;

import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

public class SSHPlainCommandHelperTest {
	
	private SSHPlainCommandHelper helper;
	
	@Before
	public void init() {
		helper = new SSHPlainCommandHelper();
	}
	
	@Test
	public void prepareFileToRetrTest() {
		String cmd = helper.prepareFileToRetr("file", "/tmp");
		Assert.assertEquals(cmd, "base64 -w 1000 \"file\" | split -l 1 - \"/tmp\"/");
	}
	
	@Test
	public void prepareFileToRetrIncapsTest() {
		String cmd = helper.prepareFileToRetr("`pwd`", "$( rm -rf / )");
		Assert.assertEquals(cmd, "base64 -w 1000 \"\\`pwd\\`\" | split -l 1 - \"\\$( rm -rf / )\"/");
	}
	
	@Test
	public void retrPieceTest() {
		Assert.assertEquals(helper.retrPiece("file"), "cat \"file\"");
	}

	@Test
	public void storePieceTest() {
		Assert.assertEquals(
				"echo \"" + Base64.getEncoder().encodeToString(new byte[] {1,2,3,4,5}) + "\" >>\"/tmp/0\"\n", 
				helper.storePiece(new byte[] {1,2,3,4,5}, "/tmp")
		);
		
		byte[] piece = new byte[2048];
		Arrays.fill(piece, (byte)0);
		String encoded = Base64.getEncoder().encodeToString(piece);
		
		Assert.assertEquals(
				"echo \"" + encoded    + "\" >>\"/tmp/1\"\n",
				helper.storePiece(piece, "/tmp")
		);
	}
	
	@Test
	public void joinStoredPiecesTest() {
		helper.storePiece(new byte[] {1,2,3,4,5}, "/tmp");
		helper.storePiece(new byte[] {1,2,3,4,5}, "/tmp");
		Assert.assertEquals(
			"base64 -d \"/tmp/0\" >>\"file\"\n" +
			"base64 -d \"/tmp/1\" >>\"file\"\n", 
			helper.joinStoredPieces("/tmp", "file"));
	}

	@Test
	public void echoTest() {
		Assert.assertEquals(
				"echo \"ABCDABCD\"",
				helper.echo("ABCDABCD")
		);
	}

	@Test
	public void cdTest() {
		Assert.assertEquals(
				"cd \"..\"", 
				helper.cd("..")
		);
	}

	@Test
	public void pwdTest() {
		Assert.assertEquals(
				"pwd", 
				helper.pwd()
		);
	}

	@Test
	public void lsla() {
		Assert.assertEquals("ls -la -R \"dir\"", helper.lsla("dir"));
	}

	@Test
	public void lsw1() {
		Assert.assertEquals("ls -w1 \"dir\"", helper.lsw1("dir"));
	}

	@Test
	public void size() {
		Assert.assertEquals("stat \"file\" | grep -oEh \"Size: [0-9]+\" | cut -b 7-", helper.size("file"));
	}

	@Test
	public void mkdir() {
		Assert.assertEquals("mkdir \"file\"", helper.mkdir("file"));
	}

	@Test
	public void rmrf() {
		Assert.assertEquals("rm -rf \"tmp\"", helper.rmrf("tmp"));
	}

	@Test
	public void mv() {
		Assert.assertEquals("mv \"f1\" \"f2\"", helper.mv("f1", "f2"));
	}

	@Test
	public void exit() {
		Assert.assertEquals("logout", helper.exit());
	}

	@Test
	public void DS() {
		Assert.assertEquals("/", helper.DS());
	}

	@Test
	public void tmpFolder() {
		Assert.assertEquals("/tmp", helper.tmpFolder());
	}

}
