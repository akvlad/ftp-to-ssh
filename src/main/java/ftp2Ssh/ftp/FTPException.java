package ftp2Ssh.ftp;

public class FTPException extends Exception {
	
	private int code;
	
	public FTPException(Exception e, int code) {
		super(e);
		this.code = code;
	}
	
	public FTPException(Exception e) {
		super(e);
		this.code = 451;
	}
	
	public FTPException(String message, int code) {
		super(message);
		this.code = code;
	}
	
	public FTPException(String message) {
		super(message);
		this.code = 451;
	}

	public int getCode() {
		return code;
	}

}
