package gov.va.isaac.sync.git.gitblit.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import gov.va.isaac.sync.git.gitblit.GitBlitException.ForbiddenException;
import gov.va.isaac.sync.git.gitblit.GitBlitException.NotAllowedException;
import gov.va.isaac.sync.git.gitblit.GitBlitException.UnauthorizedException;
import gov.va.isaac.sync.git.gitblit.GitBlitException.UnknownRequestException;
import gov.va.isaac.sync.git.gitblit.models.RepositoryModel;
import gov.va.isaac.sync.git.gitblit.utils.RpcUtils.AccessPermission;

/**
 * Utility methods for json calls to a Gitblit server.
 *
 * @author James Moger
 *
 */
public class JsonUtils {

	public static final Type REPOSITORIES_TYPE = new TypeToken<Map<String, RepositoryModel>>() {
	}.getType();

	/**
	 * Creates JSON from the specified object.
	 *
	 * @param o
	 * @return json
	 */
	public static String toJsonString(Object o) {
		String json = gson().toJson(o);
		return json;
	}

	/**
	 * Sends a JSON message.
	 *
	 * @param url
	 *            the url to write to
	 * @param json
	 *            the json message to send
	 * @param username
	 * @param password
	 * @return the http request result code
	 * @throws {@link IOException}
	 */
	public static int sendJsonString(String url, String json, String username, char[] password)
			throws IOException {
		try {
			byte[] jsonBytes = json.getBytes(ConnectionUtils.CHARSET);
			URLConnection conn = ConnectionUtils.openConnection(url, username, password);
			conn.setRequestProperty("Content-Type", "text/plain;charset=" + ConnectionUtils.CHARSET);
			conn.setRequestProperty("Content-Length", "" + jsonBytes.length);

			// write json body
			OutputStream os = conn.getOutputStream();
			os.write(jsonBytes);
			os.close();

			int status = ((HttpURLConnection) conn).getResponseCode();
			return status;
		} catch (IOException e) {
			if (e.getMessage().indexOf("401") > -1) {
				// unauthorized
				throw new UnauthorizedException(url);
			} else if (e.getMessage().indexOf("403") > -1) {
				// requested url is forbidden by the requesting user
				throw new ForbiddenException(url);
			} else if (e.getMessage().indexOf("405") > -1) {
				// requested url is not allowed by the server
				throw new NotAllowedException(url);
			} else if (e.getMessage().indexOf("501") > -1) {
				// requested url is not recognized by the server
				throw new UnknownRequestException(url);
			}
			throw e;
		}
	}

	// build custom gson instance with GMT date serializer/deserializer
	// http://code.google.com/p/google-gson/issues/detail?id=281
	public static Gson gson(ExclusionStrategy... strategies) {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Date.class, new GmtDateTypeAdapter());
		builder.registerTypeAdapter(AccessPermission.class, new AccessPermissionTypeAdapter());
		if (strategies != null && strategies.length > 0) {
			builder.setExclusionStrategies(strategies);
		}
		return builder.create();
	}

	private static class AccessPermissionTypeAdapter implements JsonSerializer<AccessPermission>, JsonDeserializer<AccessPermission> {

		private AccessPermissionTypeAdapter() {
		}

		@Override
		public synchronized JsonElement serialize(AccessPermission permission, Type type,
				JsonSerializationContext jsonSerializationContext) {
			return new JsonPrimitive(permission.code);
		}

		@Override
		public synchronized AccessPermission deserialize(JsonElement jsonElement, Type type,
				JsonDeserializationContext jsonDeserializationContext) {
			return AccessPermission.fromCode(jsonElement.getAsString());
		}
	}
	public static class GmtDateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
		private final DateFormat dateFormat;

		public GmtDateTypeAdapter() {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

		@Override
		public synchronized JsonElement serialize(Date date, Type type,
				JsonSerializationContext jsonSerializationContext) {
			synchronized (dateFormat) {
				String dateFormatAsString = dateFormat.format(date);
				return new JsonPrimitive(dateFormatAsString);
			}
		}

		@Override
		public synchronized Date deserialize(JsonElement jsonElement, Type type,
				JsonDeserializationContext jsonDeserializationContext) {
			try {
				synchronized (dateFormat) {
					Date date = dateFormat.parse(jsonElement.getAsString());
					return new Date((date.getTime() / 1000) * 1000);
				}
			} catch (ParseException e) {
				throw new JsonSyntaxException(jsonElement.getAsString(), e);
			}
		}
	}


}
