package pgdp.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileSyncServer extends FileSyncParticipant{
    // Server commands
    static final String UPDATE = "UPDATE";
    static final String GET = "GET";
    static final String LIST = "LIST";

    // Status responses
    static final String SUCCESS = "SUCCESS";
    static final String FAILED = "FAILED";

    private final int port;
    private BufferedReader in;
    private PrintWriter out;

    public FileSyncServer(Path folder, int port) {
        super(folder);
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            // Pass UTF-8 for both IO streams as the charset we want to use
            in = new BufferedReader(new InputStreamReader(server.accept().getInputStream(), StandardCharsets.UTF_8));
            // Initialize with auto-flush
            out = new PrintWriter(server.accept().getOutputStream(), true, StandardCharsets.UTF_8);
            while (true) {
                executeNextCommand();
            }
        } catch (Exception e) {
            System.err.println("Client connection failed: " + e);
        }
    }

    private void executeNextCommand() throws IOException {
        String command = in.readLine();
        switch (command) {
            case LIST -> sendFileListToClient();
            case GET -> sendFileContentToClient();
            case UPDATE -> updateFileFromClient();
            default -> System.err.print("Unknown command from client: " + command);
        }
    }

    private void sendFileListToClient() {
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
        for (Map.Entry<String, Instant> file : files.entrySet()) {
            out.println(file.getKey());
            out.println(file.getValue());
        }
    }

    private void sendFileContentToClient() throws IOException {
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

    private void updateFileFromClient() throws IOException {
        String file = in.readLine();
        receiveAndUpdateFile(file);
    }

    void receiveAndUpdateFile(String file) throws IOException {
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
        if (args.length != 2)
            throw new IllegalArgumentException("Invalid number of arguments");
        Path folder = Path.of(args[0]);
        int port = Integer.parseInt(args[1]);
        new FileSyncServer(folder, port).run();
    }
}
