package ftp2Ssh.ftp;

import java.io.EOFException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import ftp2Ssh.ftp.FolderTransmitter.Folder;
import ftp2Ssh.ftp.FolderTransmitter.FolderTransmitedListener;
import ftp2Ssh.ssh.ISSHCommandHelper;
import ftp2Ssh.ssh.ISSHLayerPooled;

public class FileSlicer implements FolderTransmitedListener {
	
	private String fileName;
	private long size;
	private int pointer = 0;
	private int chunkSize;
	private Executor folderExec = Executors.newFixedThreadPool(1);
	private FolderTransmitter transmitter;
	private ISSHLayerPooled mainExec;
	private ISSHCommandHelper helper;
	
	private String getTmpFolder() {
		return String.format("%s%sFTP%d", helper.tmpFolder(), helper.DS(),  (int)(Math.random() * 65535)); 
	}
	
	public FileSlicer(String name, long size, int chunkSize, 
			ISSHLayerPooled mainExec, ISSHCommandHelper helper, FolderTransmitter transmitter) {
		this.fileName = name;
		this.size = size;
		this.mainExec = mainExec;
		this.helper = helper;
		this.transmitter = transmitter;
		this.transmitter.setListener(this);
		this.chunkSize = chunkSize;
	}
	
	@Override
	public void onFolderTransmitted() {
		slice();		
	}
	
	public void slice() {
		int _pointer = pointer;
		pointer += 1;
		String tmpFolder = getTmpFolder();
		String tmpFile = getTmpFolder();
		FutureTask<Folder> res = new FutureTask<Folder>((Callable<Folder>)() -> {
			if (_pointer * chunkSize > size) {
				throw new EOFException("EOF");
			}
			mainExec.cmd(helper.slice(fileName, tmpFile, chunkSize, 1, _pointer));
			mainExec.cmd(helper.mkdir(tmpFolder));
			mainExec.cmd(helper.prepareFileToRetr(tmpFile, tmpFolder));
			mainExec.cmd(helper.rmrf(tmpFile));
			String files = mainExec.cmd(helper.lsw1(tmpFolder));
			Folder folder = new Folder();
			folder.files = files.split("\n");
			folder.name = tmpFolder;
			return folder;
		});
		folderExec.execute(() -> {
			res.run();
		});
		transmitter.ProcessFolder(res);
	}
	

}
