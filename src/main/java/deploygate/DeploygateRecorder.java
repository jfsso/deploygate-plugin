package deploygate;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.RunList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class DeploygateRecorder extends Recorder {

	private String userName;

	public String getUserName() {
		return userName;
	}

	private String apiToken;

	public String getApiToken() {
		return this.apiToken;
	}

	private String buildNotes;

	public String getBuildNotes() {
		return this.buildNotes;
	}

	private String filePath;

	public String getFilePath() {
		return this.filePath;
	}

	private String proxyHost;

	public String getProxyHost() {
		return proxyHost;
	}

	private String proxyUser;

	public String getProxyUser() {
		return proxyUser;
	}

	private String proxyPass;

	public String getProxyPass() {
		return proxyPass;
	}

	private int proxyPort;

	public int getProxyPort() {
		return proxyPort;
	}

	@DataBoundConstructor
	public DeploygateRecorder(String apiToken, String userName,
			String buildNotes, String filePath, String proxyHost,
			String proxyUser, String proxyPass, int proxyPort) {
		this.apiToken = apiToken;
		this.userName = userName;
		this.buildNotes = buildNotes;
		this.filePath = filePath;
		this.proxyHost = proxyHost;
		this.proxyUser = proxyUser;
		this.proxyPass = proxyPass;
		this.proxyPort = proxyPort;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher,
			final BuildListener listener) {
		if (build.getResult().isWorseOrEqualTo(Result.FAILURE))
			return false;

		listener.getLogger().println("Uploading to deploygate.com");

		try {
			EnvVars vars = build.getEnvironment(listener);

			boolean pathSpecified = filePath != null
					&& !filePath.trim().isEmpty();
			String expandPath;
			if (!pathSpecified)
				expandPath = "$WORKSPACE";
			else
				expandPath = filePath;

			DeploygateUploader.UploadRequest ur = createPartialUploadRequest(
					vars, expandPath);

			DeploygateRemoteRecorder remoteRecorder = new DeploygateRemoteRecorder(
					pathSpecified, ur, listener);

			final Map parsedMap;

			try {
				Object result = launcher.getChannel().call(remoteRecorder);
				parsedMap = (Map) result;
			} catch (UploadException ue) {
				listener.getLogger().println(
						"Incorrect response code: " + ue.getStatusCode());
				listener.getLogger().println(ue.getResponseBody());
				return false;
			}

			DeploygateBuildAction installAction = new DeploygateBuildAction();
			installAction.displayName = "Uploaded to DeployGate!";
			installAction.iconFileName = "package.gif";
			installAction.urlName = "https://www.deploygate.com/";
			build.addAction(installAction);

		} catch (Throwable e) {
			listener.getLogger().println(e);
			e.printStackTrace(listener.getLogger());
			return false;
		}

		return true;
	}

	private DeploygateUploader.UploadRequest createPartialUploadRequest(
			EnvVars vars, String expandPath) {
		DeploygateUploader.UploadRequest ur = new DeploygateUploader.UploadRequest();
		ur.userName = userName;
		ur.filePath = vars.expand(expandPath);
		ur.apiToken = vars.expand(apiToken);
		ur.buildNotes = vars.expand(buildNotes);
		ur.proxyHost = proxyHost;
		ur.proxyPass = proxyPass;
		ur.proxyPort = proxyPort;
		ur.proxyUser = proxyUser;
		return ur;
	}

	@Override
	public Collection<? extends Action> getProjectActions(
			AbstractProject<?, ?> project) {
		ArrayList<DeploygateBuildAction> actions = new ArrayList<DeploygateBuildAction>();
		RunList<? extends AbstractBuild<?, ?>> builds = project.getBuilds();

		Collection predicated = CollectionUtils.select(builds, new Predicate() {
			public boolean evaluate(Object o) {
				return ((AbstractBuild<?, ?>) o).getResult().isBetterOrEqualTo(
						Result.SUCCESS);
			}
		});

		ArrayList<AbstractBuild<?, ?>> filteredList = new ArrayList<AbstractBuild<?, ?>>(
				predicated);

		Collections.reverse(filteredList);
		for (AbstractBuild<?, ?> build : filteredList) {
			List<DeploygateBuildAction> testflightActions = build
					.getActions(DeploygateBuildAction.class);
			if (testflightActions != null && testflightActions.size() > 0) {
				for (DeploygateBuildAction action : testflightActions) {
					actions.add(new DeploygateBuildAction(action));
				}
				break;
			}
		}

		return actions;
	}

	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {
		public DescriptorImpl() {
			super(DeploygateRecorder.class);
			load();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types
			return true;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json)
				throws FormException {
			// XXX is this now the right style?
			req.bindJSON(this, json);
			save();
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Upload to DeployGate";
		}
	}

}
