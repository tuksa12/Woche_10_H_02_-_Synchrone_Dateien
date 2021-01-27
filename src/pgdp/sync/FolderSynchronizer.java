package pgdp.sync;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FolderSynchronizer {
	Path folder;

	public FolderSynchronizer(Path folder) {
		this.folder = folder;
	}

	public Map<String, Instant> scan() throws IOException {
		Map<String, Instant > result = new HashMap<>();
		Files.walk(folder)
				.forEach(file -> {
					try {
						result.put(FileSyncUtil.pathToRelativeUri(folder,file),Files.getLastModifiedTime(file).toInstant());
					} catch (IOException ignored) {
					}
				});
		return result;
	}

	public boolean updateIfNewer(String fileInFolderRelativeUri, FileContent fileContent) throws IOException {
		boolean contains = Files.walk(folder)
				.anyMatch(file -> file.getFileName().toString().equals(fileInFolderRelativeUri));
		if (!contains){
			Path path = FileSyncUtil.relativeUriToPath(Files.createDirectories(folder), fileInFolderRelativeUri);
		} else {
			Files.walk(folder);
		}
//		Files.walk(folder)
//				.filter(file -> file.equals(path))
//				.forEach(file -> {
//					try {
//						if(Files.getLastModifiedTime(file).toInstant().isBefore(fileContent.getLastModifiedTime())){
//							Files.cr
//							Files.setLastModifiedTime(file, FileTime.from(fileContent.getLastModifiedTime()));
//						}
//					} catch (IOException e) {
//					}
//				});
		return false;
	}

	public FileContent getFileContent(String fileInFolderRelativeUri) throws IOException {
		Path path = FileSyncUtil.relativeUriToPath(folder,fileInFolderRelativeUri);
		List<String> content = new ArrayList<>();
		Files.walk(folder)
			.forEach(file -> {
					content.add(file.getFileName().toString());
			});
		Instant instant = null;
		try{
			instant = Files.getLastModifiedTime(path).toInstant();
		} catch (IOException ignored) {
		}
		return new FileContent(instant,content);
	}
}
