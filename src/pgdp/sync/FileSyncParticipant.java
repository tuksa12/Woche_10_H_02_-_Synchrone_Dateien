package pgdp.sync;

import java.nio.file.Files;
import java.nio.file.Path;

abstract class FileSyncParticipant {

	private final FolderSynchronizer folderSynchronizer;

	FileSyncParticipant(Path folder) {
		if (Files.notExists(folder))
			throw new IllegalArgumentException("synchronization folder does not exist");
		folderSynchronizer = new FolderSynchronizer(folder);
	}

	public abstract void run();

	final FolderSynchronizer folderSynchronizer() {
		return folderSynchronizer;
	}
}
