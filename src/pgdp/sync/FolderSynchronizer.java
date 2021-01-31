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
	final Path folder;

	//Constructor
	public FolderSynchronizer(Path folder) {
		this.folder = folder;
	}

	public Map<String, Instant> scan() throws IOException {
		Map<String, Instant > result = new HashMap<>();
		Files.walk(folder)//Walk through the folder
				.forEach(file -> {
					try {
						if(Files.isRegularFile(file)){//If it is a regularFile, add the file to the map
							result.put(FileSyncUtil.pathToRelativeUri(folder,file),Files.getLastModifiedTime(file).toInstant());
						}
					} catch (IOException ignored) {//Ignores the error, to continue the "walk"
					}
				});
		return result;
	}

	//I couldn't manage to make updateIfNewer work, but the idea behind should be more or less in the right direction
	public boolean updateIfNewer(String fileInFolderRelativeUri, FileContent fileContent) throws IOException {
		//The next two if statements I did to try to fix the artemis test error
		if(fileInFolderRelativeUri.equals("") || fileContent == null){
			return false;
		}
		if(!Files.exists(folder)){
			 Files.createDirectories(folder);
		}

		boolean containsFile = Files.walk(folder)//Tries to find the file in the folder
				.anyMatch(file -> FileSyncUtil.pathToRelativeUri(folder,file).equals(fileInFolderRelativeUri));

		if(containsFile){
			Path file = FileSyncUtil.relativeUriToPath(folder, fileInFolderRelativeUri);
			if (Files.getLastModifiedTime(file).toInstant().isBefore(fileContent.getLastModifiedTime())){//Compares the lastModifiedTime of both files
					BufferedWriter writer = new BufferedWriter(new FileWriter(file.toString()));
					writer.write(fileContent.getLines().toString());
					Files.setLastModifiedTime(file,FileTime.from(fileContent.getLastModifiedTime()));
			}
		} else{//Creates a new file because the folder doesn't have the file
			Path newFile = Files.createDirectories(folder);
			BufferedWriter writer = new BufferedWriter(new FileWriter(newFile.toString()));
			writer.write(fileContent.getLines().toString());
			Files.setLastModifiedTime(newFile,FileTime.from(fileContent.getLastModifiedTime()));
		}

		return Files.walk(folder)//Returns the if the file was add to the folder. With the method working, should return true
				.anyMatch(file -> FileSyncUtil.pathToRelativeUri(folder,file).equals(fileInFolderRelativeUri));
	}


	public FileContent getFileContent(String fileInFolderRelativeUri) throws IOException {
		List<FileContent> result = Files.walk(folder)
				.filter(file -> FileSyncUtil.pathToRelativeUri(folder,file).equals(fileInFolderRelativeUri))//Filter to find the specific file in the folder
				.map(file -> {
					try {
						 List<String> content = Files.readAllLines(file);
						 Instant lastModifiedDate = Files.getLastModifiedTime(file).toInstant();
						 return new FileContent(lastModifiedDate,content);//Returns the file content of the specific file
					} catch (IOException e) {
						return new FileContent(null,null);//If there is a error, it will return an empty FileContent
					}
				})
				.collect(Collectors.toList());

		return result.get(0);//Just a easy way to pick the first and only element present in the list
	}

}
