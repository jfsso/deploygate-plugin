package deploygate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.parser.JSONParser;

/**
 * A testflight uploader
 */
public class DeploygateUploader implements Serializable {
	static class UploadRequest implements Serializable {
		String userName;
		String filePath;
		String apiToken;
		String buildNotes;
		String distributionKey;
		File file;
		String proxyHost;
		String proxyUser;
		String proxyPass;
		int proxyPort;
	}

	public Map upload(UploadRequest ur) throws IOException,
			org.json.simple.parser.ParseException {

		DefaultHttpClient httpClient = new DefaultHttpClient();

		// Configure the proxy if necessary
		if (ur.proxyHost != null && !ur.proxyHost.isEmpty() && ur.proxyPort > 0) {
			Credentials cred = null;
			if (ur.proxyUser != null && !ur.proxyUser.isEmpty())
				cred = new UsernamePasswordCredentials(ur.proxyUser,
						ur.proxyPass);

			httpClient.getCredentialsProvider().setCredentials(
					new AuthScope(ur.proxyHost, ur.proxyPort), cred);
			HttpHost proxy = new HttpHost(ur.proxyHost, ur.proxyPort);
			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
					proxy);
		}

		HttpHost targetHost = new HttpHost("deploygate.com", 443, "https");
		HttpPost httpPost = new HttpPost(String.format("/api/users/%s/apps", ur.userName));
		FileBody fileBody = new FileBody(ur.file);

		MultipartEntity entity = new MultipartEntity();
		entity.addPart("token", new StringBody(ur.apiToken));
		entity.addPart("message", new StringBody(ur.buildNotes));
		if(ur.distributionKey != null && ! ur.distributionKey.isEmpty()) {
			entity.addPart("distribution_key", new StringBody(ur.distributionKey));
		}
        entity.addPart("file", fileBody);
		httpPost.setEntity(entity);

		HttpResponse response = httpClient.execute(targetHost, httpPost);
		HttpEntity resEntity = response.getEntity();

		InputStream is = resEntity.getContent();

		// Improved error handling.
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200) {
			String responseBody = new Scanner(is).useDelimiter("\\A").next();
			throw new UploadException(statusCode, responseBody, response);
		}

		JSONParser parser = new JSONParser();

		return (Map) parser
				.parse(new BufferedReader(new InputStreamReader(is)));
	}
}
