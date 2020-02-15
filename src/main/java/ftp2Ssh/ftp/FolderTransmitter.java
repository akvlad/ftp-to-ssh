package ftp2Ssh.ftp;

import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import ftp2Ssh.SpringContext;
import ftp2Ssh.configuration.ConfigurationProvider;
import ftp2Ssh.ssh.AsyncCmdRes;
import ftp2Ssh.ssh.ISSHCommandHelper;
import ftp2Ssh.ssh.ISSHLayerPool;

public class FolderTransmitter {
	
	public static interface FolderTransmitedListener {
		public void onFolderTransmitted();
	}
	public static class Folder {
		public String name;
		public String[] files;
	}
	
	private LinkedBlockingQueue<Future<Folder>> folders = new LinkedBlockingQueue<>();
	private FolderTransmitedListener listener;
	private ISSHCommandHelper helper;
	private ISSHLayerPool pool;
	private Boolean isClosed = false;
	
	public FolderTransmitter(ISSHCommandHelper helper, ISSHLayerPool pool) {
		this.helper = helper;
		this.pool = pool;
	}
	
	
	public void ProcessFolder(Future<Folder> folderName) {
		folders.add(folderName);
	}
	
	public int getQueueSize() {
		return folders.size();
	}
	
	public void process(BlockingQueue<Future<AsyncCmdRes>> results) {
		try {
			while (!getClosed()) {
				Future<Folder> folderName = folders.poll(30, TimeUnit.SECONDS);
				if (folderName == null) {
					throw new TimeoutException("folder getting error");
				}
				String[] files = folderName.get().files;
				Queue<Future<AsyncCmdRes>> internalResults = new LinkedList<>();
				for (String file : files) {
					Future<AsyncCmdRes> result = pool.cmdA(helper.retrPiece(folderName.get().name+helper.DS()+file.trim()));
					internalResults.add(result);
					results.add(result);
					if (getClosed()) {
						return;
					}
				}
				for (Future<AsyncCmdRes> contents : internalResults) {
					contents.get();
					if (getClosed()) {
						return;
					}
				}
				pool.cmdA(helper.rmrf(folderName.get().name));
				if (listener != null) {
					listener.onFolderTransmitted();
				}
			}
		} catch (TimeoutException | InterruptedException | ExecutionException e) {
			if (e.getCause() instanceof EOFException) {
				results.add(ConcurrentUtils.constantFuture(new AsyncCmdRes((EOFException)e.getCause(), null)));
				return;
			}
			results.add(ConcurrentUtils.constantFuture(new AsyncCmdRes(e, null)));
		} catch (Exception e) {
			results.add(ConcurrentUtils.constantFuture(new AsyncCmdRes(e, null)));
		}
	}
	
	public boolean getClosed() {
		synchronized (isClosed) {
			return isClosed;
		}
	}
	
	public void close() {
		synchronized (isClosed) {
			isClosed = true;
		}
	}

	public FolderTransmitedListener getListener() {
		return listener;
	}

	public void setListener(FolderTransmitedListener listener) {
		this.listener = listener;
	}
	

}
