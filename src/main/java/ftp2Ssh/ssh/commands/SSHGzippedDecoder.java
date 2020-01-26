package ftp2Ssh.ssh.commands;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;

import ftp2Ssh.ssh.IB64Decoder;

public class SSHGzippedDecoder implements IB64Decoder {
	
	@Override
	public byte[] decode(String b64) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(b64.replace("\n", "").replace("\r", "")));
			GZIPInputStream gzis = new GZIPInputStream(bais);
			return IOUtils.toByteArray(gzis);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
