/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

/**
 * {@link MavenPublish}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class MavenPublish extends Task<Integer>
{
	private static Logger log = LoggerFactory.getLogger(MavenPublish.class);

	String groupId_;
	String artifactId_;
	String version_;
	File pomFile_;
	File[] dataFiles_;
	String url_;
	String username_;
	String psswrd_;

	public MavenPublish(String groupId, String artifactId, String version, File pomFile,
			File[] dataFiles, String url, String username, String psswrd) throws Exception
	{
		groupId_ = groupId;
		artifactId_ = artifactId;
		version_ = version;
		pomFile_ = pomFile;
		dataFiles_ = dataFiles;
		url_ = url;
		username_ = username;
		psswrd_ = psswrd;
	}

	private void writeChecksumFile(File file, String type) throws IOException, InterruptedException, ExecutionException
	{
		updateMessage("Calculating Checksum for " + file.getName());

		Task<String> gen =  ChecksumGenerator.calculateChecksum(type, file);
		gen.messageProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				updateMessage(newValue);
			}
		});
		gen.progressProperty().addListener(new ChangeListener<Number>()
		{
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
			{
				updateProgress(gen.getWorkDone(), gen.getTotalWork());
			}
		});

		WorkExecutors.get().getExecutor().execute(gen);
		String checksum = gen.get();

		updateMessage("Writing checksum file");

		Files.write(new File(file.getParentFile(), file.getName() + "." + type.toLowerCase()).toPath(),
				(checksum + "  " + file.getName()).getBytes(), StandardOpenOption.WRITE,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		updateMessage("");
		updateProgress(-1, 0);
	}

	private void putFile(File file, String targetFileName) throws Exception
	{
		String groupIdTemp = groupId_.replaceAll("\\.", "//");
		URL url = new URL(url_ + (url_.endsWith("/") ? "" : "/") + groupIdTemp + "/" + artifactId_ + "/" + version_
				+ "/" + (targetFileName == null ? file.getName() : targetFileName));

		log.info("Uploading " + file.getAbsolutePath() + " to " + url.toString());

		updateMessage("Uploading " + file.getName());
		updateProgress(0, file.length());

		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		if (username_.length() > 0 || psswrd_.length() > 0)
		{
			String encoded = Base64.getEncoder().encodeToString((username_ + ":" + psswrd_).getBytes());
			httpCon.setRequestProperty("Authorization", "Basic " + encoded);
		}
		httpCon.setDoOutput(true);
		httpCon.setRequestMethod("PUT");
		httpCon.setConnectTimeout(30 * 1000);
		httpCon.setReadTimeout(60 * 60 * 1000);
		long fileLength = file.length();
		httpCon.setFixedLengthStreamingMode(fileLength);

		byte[] buf = new byte[8192];
		long loopCount = 0;
		int read = 0;

		try (OutputStream out = httpCon.getOutputStream();
				FileInputStream fis = new FileInputStream(file);)
		{
			while ((read = fis.read(buf, 0, buf.length)) > 0)
			{
				//update every MB
				if (loopCount++ % 128 == 0)
				{
					updateProgress((loopCount * 8192l), fileLength);
					updateMessage("Uploading " + file.getName() + " - " + (loopCount * 8192l) + " / " + fileLength);
				}
				out.write(buf, 0, read);
			}
			out.flush();
		}

		StringBuilder sb = new StringBuilder();

		try (InputStream is = httpCon.getInputStream();)
		{
			read = 0;
			byte[] buffer = new byte[1024];
			CharBuffer cBuffer = ByteBuffer.wrap(buffer).asCharBuffer();
			while (read != -1)
			{
				read = is.read(buffer);
				if (read > 0)
				{
					sb.append(cBuffer, 0, read);
				}
			}
		}

		httpCon.disconnect();
		if (sb.toString().trim().length() > 0)
		{
			throw new Exception("The server reported an error during the publish operation:  " + sb.toString());
		}
		log.info("Upload Successful");
		updateMessage("");
		updateProgress(-1, 0);
	}

	/**
	 * @see javafx.concurrent.Task#call()
	 */
	@Override
	protected Integer call() throws Exception
	{
		updateProgress(-1, 0);

		updateMessage("Creating Checksum Files");
		writeChecksumFile(pomFile_, "MD5");
		writeChecksumFile(pomFile_, "SHA1");

		for (File f : dataFiles_)
		{
			writeChecksumFile(f, "MD5");
			writeChecksumFile(f, "SHA1");
		}

		updateMessage("Uploading data files");
		for (File f : dataFiles_)
		{
			//TODO check maven upload order
			putFile(f, null);
			putFile(new File(f.getParentFile(), f.getName() + ".md5"), null);
			putFile(new File(f.getParentFile(), f.getName() + ".sha1"), null);
		}

		updateMessage("Uploading pom files");
		putFile(pomFile_, "pom");
		putFile(new File(pomFile_.getParentFile(), pomFile_.getName() + ".md5"), "pom.md5");
		putFile(new File(pomFile_.getParentFile(), pomFile_.getName() + ".sha1"), "pom.sha1");

		updateMessage("Publish Complete");
		updateProgress(10, 10);
		return 0;
	}
}
