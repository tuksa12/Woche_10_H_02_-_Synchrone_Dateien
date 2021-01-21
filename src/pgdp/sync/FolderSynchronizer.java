package pgdp.sync;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

public class FolderSynchronizer {

	public FolderSynchronizer(Path folder) {

	}

	public Map<String, Instant> scan() throws IOException {
		return null;
	}

	public boolean updateIfNewer(String fileInFolderRelativeUri, FileContent fileContent) throws IOException {
		return false;
	}

	public FileContent getFileContent(String fileInFolderRelativeUri) throws IOException {
		return null;
	}
}
