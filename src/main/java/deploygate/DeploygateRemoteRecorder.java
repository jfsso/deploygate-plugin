package deploygate;

import hudson.model.BuildListener;
import jenkins.security.MasterToSlaveCallable;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang.StringUtils;

/**
 * Code for sending a build to Deploygate which can run on a master or slave.
 */
public class DeploygateRemoteRecorder extends MasterToSlaveCallable {
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
		uploadRequest.file = identifyApp();

		listener.getLogger().println(uploadRequest.file);

		DeploygateUploader uploader = new DeploygateUploader();
		return uploader.upload(uploadRequest);
	}

	private File identifyApp() {
		if (pathSpecified) {
			return new File(uploadRequest.filePath);
		} else {
			File workspaceDir = new File(uploadRequest.filePath);
			File possibleIpa = DeploygateRemoteRecorder.findApp(workspaceDir);
			return possibleIpa != null ? possibleIpa : workspaceDir;
		}
	}

	public static File findApp(File root) {
		for (File file : root.listFiles()) {
			if (file.isDirectory()) {
				File ipaResult = findApp(file);
				if (ipaResult != null)
					return ipaResult;
			} else if (file.getName().endsWith(".apk") || file.getName().endsWith(".ipa")) {
				return file;
			}
		}
		return null;
	}
}
