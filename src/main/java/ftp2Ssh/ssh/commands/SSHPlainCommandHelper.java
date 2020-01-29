package ftp2Ssh.ssh.commands;

import java.util.Arrays;
import java.util.Base64;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ftp2Ssh.ssh.ISSHCommandHelper;

@Component("PlainCommandHelper")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SSHPlainCommandHelper implements ISSHCommandHelper {
	
	protected String prepareFileToRetr = "base64 -w {{CHUNK_SIZE}} {{FILE}} | split -l 1 - {{TMP}}{{DS}}";
	protected String retrPiece = "cat {{FILE}}";
	protected String storePiece = "echo {{PIECE}} >>{{TMPFILE}}";
	protected String joinStoredPieces = "base64 -d {{TMPFILE}} >>{{FILE}}";
	protected String echo = "echo";
	protected String cd = "cd";
	protected String pwd = "pwd";
	protected String lsla = "ls -la -R";
	protected String lsw1 = "ls -w1";
	protected String size = "stat {{FILE}} | grep -oEh \"Size: [0-9]+\" | cut -b 7-";
	protected String mkdir = "mkdir";
	protected String rmrf = "rm -rf";
	protected String mv = "mv {{FROM}} {{TO}}";
	protected String exit = "logout";
	protected String DS = "/";
	protected String spawn;
	protected String tmpFolder = "/tmp";
	protected String usernameReg = null;
	protected int piecesStored = 0;
	protected int chunkSize = 1000; 
	
	protected String[] _screenArgs(String[] args) {
		return Arrays.stream(args).map(arg -> "\"" + arg.replaceAll("([\\$\"`])", "\\\\$1") + "\"").toArray(String[]::new);
	}
	
	protected String[] screenArgs(String... args) { 
		return this._screenArgs(args);
	}
	
	protected String screenArg(String arg) { 
		return this.screenArgs(arg)[0];
	}
	
	protected String screenArgNoQuotes(String arg) { 
		return arg.replaceAll("([\\$`])", "\\\\$1");
	}

	@Override
	public String prepareFileToRetr(String fileName, String tmpFolder) {
		String[] screened = this.screenArgs(fileName, tmpFolder);
		return prepareFileToRetr
				.replace("{{CHUNK_SIZE}}", Integer.toString(chunkSize))
				.replace("{{FILE}}", screened[0])
				.replace("{{TMP}}", screened[1])
				.replace("{{DS}}", DS()); 
	}

	@Override
	public String retrPiece(String fileName) {
		return retrPiece.replace("{{FILE}}", screenArg(fileName));
	}

	@Override
	public String storePiece(byte[] piece, String tmpFolder) {
		String encoded = Base64.getEncoder().encodeToString(piece);
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

	@Override
	public String joinStoredPieces(String tmpFolder, String outputFile) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<piecesStored;i++) {
			String[] screened = screenArgs(tmpFolder + DS + Integer.toString(i), outputFile);
			builder.append(joinStoredPieces
					.replace("{{TMPFILE}}", screened[0])
					.replace("{{FILE}}", screened[1])
					
			).append("\n");
		}
		return builder.toString();
	}

	@Override
	public String echo(String arg) {
		return echo + " "+ screenArg(arg);
	}

	@Override
	public String cd(String arg) {
		return cd + " " + screenArg(arg) ;
	}

	@Override
	public String pwd() {
		return pwd;
	}

	@Override
	public String lsla(String arg) {
		return lsla + " " + screenArg(arg);
	}

	@Override
	public String lsw1(String arg) {
		return lsw1 + " " + screenArg(arg);
	}

	@Override
	public String size(String arg) {
		return size.replace("{{FILE}}", screenArg(arg));
	}

	@Override
	public String mkdir(String arg) {
		return mkdir + " " + screenArg(arg);
	}

	@Override
	public String rmrf(String arg) {
		return rmrf + " " + screenArg(arg);
	}

	@Override
	public String mv(String from, String to) {
		return mv.replace("{{FROM}}", screenArg(from))
				.replace("{{TO}}", screenArg(to));
	}

	@Override
	public String exit() {
		return exit;
	}

	@Override
	public String DS() {
		return DS;
	}

	@Override
	public String tmpFolder() {
		return tmpFolder;
	}
	
	@Override
	public String getSpawnCmd(String login, String password) throws Exception {
		String res = spawn;
		if (usernameReg != null) {
			Pattern re = Pattern.compile(usernameReg);
			Matcher match = re.matcher(login);
			if (!match.find()) {
				throw new Exception("login incorrect");
			}
			re = Pattern.compile("\\{\\{USER\\[(\\d+)\\]\\}\\})");
			Matcher spawnMatch = re.matcher(spawn);
			while(spawnMatch.find()) {
				int index = Integer.parseInt(spawnMatch.group(1));
				res = res
						.replace(spawnMatch.group(), screenArgNoQuotes(match.group(index)));
			}
		}
		return res
				.replace("{{USER}}", screenArgNoQuotes(login))
				.replace("{{PASV}}", screenArgNoQuotes(password));
	}

	
	@Autowired(required = false)
	@Qualifier("prepareFileToRetr")
	public void setPrepareFileToRetr(String prepareFileToRetr) {
		if (prepareFileToRetr == null) { return; }
		this.prepareFileToRetr = prepareFileToRetr;
	}

	@Autowired(required = false)
	@Qualifier("retrPiece")
	public void setRetrPiece(String retrPiece) {
		if (retrPiece == null) { return; }
		this.retrPiece = retrPiece;
	}

	@Autowired(required = false)
	@Qualifier("storePiece")
	public void setStorePiece(String storePiece) {
		if (storePiece == null) { return; }
		this.storePiece = storePiece;
	}

	@Autowired(required = false)
	@Qualifier("joinStoredPieces")
	public void setJoinStoredPieces(String joinStoredPieces) {
		if (joinStoredPieces == null) { return; }
		this.joinStoredPieces = joinStoredPieces;
	}

	@Autowired(required = false)
	@Qualifier("echo")
	public void setEcho(String echo) {
		if (echo == null) { return; }
		this.echo = echo;
	}

	@Autowired(required = false)
	@Qualifier("cd")
	public void setCd(String cd) {
		if (cd == null) { return; }
		this.cd = cd;
	}

	@Autowired(required = false)
	@Qualifier("pwd")
	public void setPwd(String pwd) {
		if (pwd == null) { return; }
		this.pwd = pwd;
	}

	@Autowired(required = false)
	@Qualifier("lsla")
	public void setLsla(String lsla) {
		if (lsla == null) { return; }
		this.lsla = lsla;
	}

	@Autowired(required = false)
	@Qualifier("lsw1")
	public void setLsw1(String lsw1) {
		if (lsw1 == null) { return; }
		this.lsw1 = lsw1;
	}

	@Autowired(required = false)
	@Qualifier("size")
	public void setSize(String size) {
		if (size == null) { return; }
		this.size = size;
	}

	@Autowired(required = false)
	@Qualifier("mkdir")
	public void setMkdir(String mkdir) {
		if (mkdir == null) { return; }
		this.mkdir = mkdir;
	}

	@Autowired(required = false)
	@Qualifier("rmrf")
	public void setRmrf(String rmrf) {
		if (rmrf == null) { return; }
		this.rmrf = rmrf;
	}

	@Autowired(required = false)
	@Qualifier("mv")
	public void setMv(String mv) {
		if (mv == null) { return; }
		this.mv = mv;
	}

	@Autowired(required = false)
	@Qualifier("exit")
	public void setExit(String exit) {
		if (exit == null) { return; }
		this.exit = exit;
	}

	@Autowired(required = false)
	@Qualifier("DS")
	public void setDS(String dS) {
		if (dS == null) { return; }
		DS = dS;
	}

	@Autowired(required = false)
	@Qualifier("tmpFolder")
	public void setTmpFolder(String tmpFolder) {
		if (tmpFolder == null) { return; }
		this.tmpFolder = tmpFolder;
	}

	@Autowired
	@Qualifier("chunkSize")
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
	
	@Autowired
	@Qualifier("spawn")
	public void setSpawn(String spawn) {
		this.spawn = spawn;
	}
	
	@Autowired(required = false)
	@Qualifier("loginReg")
	public void setUsernameReg(String usernameReg) {
		this.usernameReg = usernameReg;
	}

}
