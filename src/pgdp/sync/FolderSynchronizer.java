package pgdp.sync;

import java.io.*;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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
						if(Files.isRegularFile(file)){
							result.put(FileSyncUtil.pathToRelativeUri(folder,file),Files.getLastModifiedTime(file).toInstant());
						}
					} catch (IOException ignored) {
					}
				});
		return result;
	}

	public boolean updateIfNewer(String fileInFolderRelativeUri, FileContent fileContent) throws IOException {
		if(fileInFolderRelativeUri.equals("") || fileContent == null){
			return false;
		}
//		if(!Files.exists(folder)){
//			 Path k = Files.createDirectories(folder);
//		}

		boolean containsFile = Files.walk(folder)
				.anyMatch(file -> FileSyncUtil.pathToRelativeUri(folder,file).equals(fileInFolderRelativeUri));

		if(containsFile){
			Path file = FileSyncUtil.relativeUriToPath(folder, fileInFolderRelativeUri);
			if (Files.getLastModifiedTime(file).toInstant().isBefore(fileContent.getLastModifiedTime())){
					BufferedWriter writer = new BufferedWriter(new FileWriter(file.toString()));
					writer.write(fileContent.getLines().toString());
					Files.setLastModifiedTime(file,FileTime.from(fileContent.getLastModifiedTime()));
			}
		} else{
			Path newFile = Files.createDirectories(folder);
			BufferedWriter writer = new BufferedWriter(new FileWriter(newFile.toString()));
			writer.write(fileContent.getLines().toString());
			Files.setLastModifiedTime(newFile,FileTime.from(fileContent.getLastModifiedTime()));
		}

		return Files.walk(folder)
				.anyMatch(file -> FileSyncUtil.pathToRelativeUri(folder,file).equals(fileInFolderRelativeUri));
	}


//	public boolean updateIfNewer(String fileInFolderRelativeUri, FileContent fileContent) throws IOException {
//		Path filePath = FileSyncUtil.relativeUriToPath(folder,fileInFolderRelativeUri);
//		boolean contains = Files.walk(folder)
//				.anyMatch(file -> file.getFileName().equals(filePath));
//		if (!contains){
//			//Files.createDirectories(folder,Files.getAttribute(filePath,fileContent.getLines().toString()));
//		} else {
//			Files.walk(folder)
//			.filter(file -> file.getFileName().equals(filePath))
//			.map(file -> {
//				try {
//					if(Files.getLastModifiedTime(file).toInstant().isBefore(fileContent.getLastModifiedTime())){
//						Files.setAttribute(file,fileContent.getLines().toString(),fileContent.getLastModifiedTime());
//						Files.setLastModifiedTime(file, FileTime.from(fileContent.getLastModifiedTime()));
//					}
//				} catch (IOException e) {
//				}
//			return true;});
//		}
////		Files.walk(folder)
////				.filter(file -> file.equals(path))
////				.forEach(file -> {
////					try {
////						if(Files.getLastModifiedTime(file).toInstant().isBefore(fileContent.getLastModifiedTime())){
////							Files.cr
////							Files.setLastModifiedTime(file, FileTime.from(fileContent.getLastModifiedTime()));
////						}
////					} catch (IOException e) {
////					}
////				});
//		return true;
//	}
	public FileContent getFileContent(String fileInFolderRelativeUri) throws IOException {
		List<FileContent> result = Files.walk(folder)
				.filter(file -> FileSyncUtil.pathToRelativeUri(folder,file).equals(fileInFolderRelativeUri))
				.map(file -> {
					try {
						 List<String> content = Files.readAllLines(file);
						 Instant lastModifiedDate = Files.getLastModifiedTime(file).toInstant();
						 return new FileContent(lastModifiedDate,content);
					} catch (IOException e) {
						return new FileContent(null,null);//Mudar isso
					}
				})
				.collect(Collectors.toList());
		return result.get(0);
	}

//	public FileContent getFileContent(String fileInFolderRelativeUri) throws IOException {
//		Path path = FileSyncUtil.relativeUriToPath(folder,fileInFolderRelativeUri);
//		List<String> content = new ArrayList<>();
//		Files.walk(folder)
//			.forEach(file -> {
//					content.add(file.toFile().toString());//Antes estava file.getFileName().toString(),testar se funciona
//			});
//		Instant instant = null;
//		try{
//			instant = Files.getLastModifiedTime(path).toInstant();
//		} catch (IOException ignored) {
//		}
//		return new FileContent(instant,content);
//	}
}
