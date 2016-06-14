package gov.va.isaac.sync.git.gitblit;

import java.io.IOException;

/**

 */
public class GitBlitException extends IOException {

	private static final long serialVersionUID = 1L;

	public GitBlitException(String message) {
		super(message);
	}

	public GitBlitException(Throwable cause) {
		super(cause);
	}

	/**
	 * Exception to indicate that the client should prompt for credentials
	 * because the requested action requires authentication.
	 */
	public static class UnauthorizedException extends GitBlitException {

		private static final long serialVersionUID = 1L;

		public UnauthorizedException(String message) {
			super(message);
		}
	}

	/**
	 * Exception to indicate that the requested action can not be executed by
	 * the specified user.
	 */
	public static class ForbiddenException extends GitBlitException {

		private static final long serialVersionUID = 1L;

		public ForbiddenException(String message) {
			super(message);
		}
	}

	/**
	 * Exception to indicate that the requested action has been disabled on the
	 * Gitblit server.
	 */
	public static class NotAllowedException extends GitBlitException {

		private static final long serialVersionUID = 1L;

		public NotAllowedException(String message) {
			super(message);
		}
	}

	/**
	 * Exception to indicate that the requested action can not be executed by
	 * the server because it does not recognize the request type.
	 */
	public static class UnknownRequestException extends GitBlitException {

		private static final long serialVersionUID = 1L;

		public UnknownRequestException(String message) {
			super(message);
		}
	}
}
