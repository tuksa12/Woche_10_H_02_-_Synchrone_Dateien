package pgdp.sync;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class FileSyncUtil {

	private FileSyncUtil() {

	}

	/**
	 * Converts a path of a file using the given folder into a relative URI String
	 */
	static String pathToRelativeUri(Path folder, Path fileInFolder) {
		return folder.toUri().relativize(fileInFolder.toUri()).toString();
	}

	/**
	 * Converts relative URI String using the given folder into the file path.
	 */
	static Path relativeUriToPath(Path folder, String fileInFolderRelativeUri) {
		Path folderAbs = folder.toAbsolutePath();
		Path newPath = Path.of(folderAbs.toUri().resolve(URI.create(fileInFolderRelativeUri)));
		if (!newPath.startsWith(folderAbs))
			throw new IllegalArgumentException(
					"file " + fileInFolderRelativeUri + " is not localed in the folder " + folderAbs);
		return newPath;
	}

	/**
	 * Makes the program sleep for five seconds. Returns <code>false</code> if our
	 * program was woken up externally while sleeping and <code>true</code> when the
	 * program slept five seconds successfully.
	 */
	static boolean sleepFiveSeconds() {
		try {
			TimeUnit.SECONDS.sleep(5);
			return true;
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
