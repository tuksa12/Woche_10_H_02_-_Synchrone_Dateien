package pgdp.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class FileSyncClient extends FileSyncParticipant {

	// Server commands
	static final String UPDATE = "UPDATE";
	static final String GET = "GET";
	static final String LIST = "LIST";

	// Status responses
	static final String SUCCESS = "SUCCESS";
	static final String FAILED = "FAILED";

	private final String host;
	private final int port;
	private BufferedReader in;
	private PrintWriter out;

	public FileSyncClient(Path folder, String host, int port) {
		super(folder);
		this.host = host;
		this.port = port;
	}

	@Override
	public void run() {
		try (Socket client = new Socket(host, port)) {
			// Pass UTF-8 for both IO streams as the charset we want to use
			in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
			// Initialize with auto-flush
			out = new PrintWriter(client.getOutputStream(), true, StandardCharsets.UTF_8);
			while (true) {
				executeNextCommand();
			}
		} catch (Exception e) {
			System.err.println("Server connection failed: " + e);
		}
	}

	private void executeNextCommand() throws IOException {
		String command = in.readLine();
		switch (command) {
			case LIST -> sendFileListToServer();
			case GET -> sendFileContentToServer();
			case UPDATE -> updateFileFromServer();
			default -> System.err.print("Unknown command from server: " + command);
		}
	}

	private void sendFileListToServer() {
		Map<String, Instant> files;
		try {
			files = folderSynchronizer().scan();
		} catch (Exception e) {
			System.err.println("Folder scan failed: " + e);
			out.println(FAILED);
			return;
		}
		out.println(SUCCESS);
		out.println(files.size());
		for (Entry<String, Instant> file : files.entrySet()) {
			out.println(file.getKey());
			out.println(file.getValue());
		}
	}

	private void sendFileContentToServer() throws IOException {
		String file = in.readLine();
		FileContent fileContent;
		try {
			fileContent = folderSynchronizer().getFileContent(file);
		} catch (Exception e) {
			System.err.println("File read of " + file + " failed: " + e);
			out.println(FAILED);
			return;
		}
		out.println(SUCCESS);
		sendFileContent(fileContent);
	}

	private void updateFileFromServer() throws IOException {
		String file = in.readLine();
		recieveAndUpdateFile(file);
	}

	void recieveAndUpdateFile(String file) throws IOException {
		Instant lastModTime = Instant.parse(in.readLine());
		int lineCount = Integer.parseInt(in.readLine());
		List<String> lines = new ArrayList<>();
		for (int i = 0; i < lineCount; i++) {
			lines.add(in.readLine());
		}
		FileContent fileContent = new FileContent(lastModTime, lines);
		try {
			boolean updated = folderSynchronizer().updateIfNewer(file, fileContent);
			if (updated)
				System.out.println("File " + file + " updated");
		} catch (Exception e) {
			System.err.println("File update of " + file + " failed: " + e);
		}
	}

	void sendFileContent(FileContent fileContent) {
		out.println(fileContent.getLastModifiedTime());
		out.println(fileContent.getLines().size());
		for (String line : fileContent.getLines()) {
			out.println(line);
		}
	}

	public static void main(String[] args) {
		if (args.length != 3)
			throw new IllegalArgumentException("Invalid number of arguments");
		Path folder = Path.of(args[0]);
		String host = args[1];
		int port = Integer.parseInt(args[2]);
		new FileSyncClient(folder, host, port).run();
	}
}
