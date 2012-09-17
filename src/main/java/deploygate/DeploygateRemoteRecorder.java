package deploygate;

import hudson.model.BuildListener;
import hudson.remoting.Callable;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

/**
 * Code for sending a build to TestFlight which can run on a master or slave.
 */
public class DeploygateRemoteRecorder implements Callable<Object, Throwable>,
		Serializable {
	final private boolean pathSpecified;
	final private DeploygateUploader.UploadRequest uploadRequest;
	final private BuildListener listener;

	public DeploygateRemoteRecorder(boolean pathSpecified,
			DeploygateUploader.UploadRequest uploadRequest,
			BuildListener listener) {
		this.pathSpecified = pathSpecified;
		this.uploadRequest = uploadRequest;
		this.listener = listener;
	}

	public Object call() throws Throwable {
		uploadRequest.file = identifyApk();

		listener.getLogger().println(uploadRequest.file);

		DeploygateUploader uploader = new DeploygateUploader();
		return uploader.upload(uploadRequest);
	}

	private File identifyApk() {
		if (pathSpecified) {
			return new File(uploadRequest.filePath);
		} else {
			File workspaceDir = new File(uploadRequest.filePath);
			File possibleIpa = DeploygateRemoteRecorder.findApk(workspaceDir);
			return possibleIpa != null ? possibleIpa : workspaceDir;
		}
	}

	public static File findApk(File root) {
		for (File file : root.listFiles()) {
			if (file.isDirectory()) {
				File ipaResult = findApk(file);
				if (ipaResult != null)
					return ipaResult;
			} else if (file.getName().endsWith(".apk")) {
				return file;
			}
		}
		return null;
	}
}
