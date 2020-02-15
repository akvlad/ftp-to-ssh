package ftp2Ssh.ssh;

public interface ISSHCommandHelper {
	String prepareFileToRetr(String fileName, String tmpFolder);
	String retrPiece(String fileName);
	
	String storePiece(byte[] piece, String tmpFolder);
	String joinStoredPieces(String tmpFolder, String outputFile);
	
	String echo(String arg);
	String cd(String arg);
	String pwd();
	String lsla(String arg);
	String lsw1(String arg);
	String size(String arg);
	String mkdir(String arg);
	String rmrf(String arg);
	String mv(String from, String to);
	String exit();
	
	String DS();
	String tmpFolder();
	String[] getSpawnCmd(String login, String password) throws Exception;
	String slice(String from, String to, long slice_size, long slices, long offset);
}
