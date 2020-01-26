package ftp2Ssh.ssh.commands;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ftp2Ssh.ssh.IB64Decoder;


@Component("GzippedCommandHelper")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SSHGzippedCommandHelper extends SSHPlainCommandHelper {
	
	public SSHGzippedCommandHelper() {
		super();
		joinStoredPieces = "base64 -d {{TMPFILE}} | gzip -d -c - >>{{FILE}}";
		prepareFileToRetr = "split -b {{CHUNK_SIZE}} --additional-suffix .split {{FILE}} {{TMP}}{{DS}} && gzip {{TMP}}{{DS}}* && for l in `ls {{TMP}}`; do base64 {{TMP}}{{DS}}$l >{{TMP}}{{DS}}$l.b64; done && rm {{TMP}}{{DS}}*.split.gz";
		
	}
	
	@Override
	public String storePiece(byte[] piece, String tmpFolder) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gz  = null;
		String encoded = null;
		try {
			gz = new GZIPOutputStream(baos);
			gz.write(piece);
			gz.flush();
			gz.finish();
			encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try { if (gz != null) gz.close(); } catch (Exception e) {}
			try { baos.close(); } catch (Exception e) {}
		}
		
		StringBuilder builder = new StringBuilder();
		String[] screened = null;
		synchronized (this) {
			screened = screenArgs(tmpFolder + DS + Integer.toString(piecesStored));
			piecesStored++;
		}
		
			builder.append(storePiece
					.replace("{{PIECE}}", "\""+encoded+"\"")
					.replace("{{TMPFILE}}", screened[0])
			).append("\n");
		
		return builder.toString();
	}
}
