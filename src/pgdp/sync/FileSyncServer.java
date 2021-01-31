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
            initiate();
            while (true) {
                executeNextCommand();
            }
        } catch (Exception e) {
            System.err.println("Client connection failed: " + e);
        }
    }

    private void initiate(){
        out.println(LIST);
    }

    private void executeNextCommand() throws IOException {
        String command = in.readLine();
        switch (command) {
            case SUCCESS -> getFileListFromClient();
            default -> System.err.print("Unknown command from client: " + command);
        }
    }

    private void getFileListFromClient() {
        ArrayList<String> uri = new ArrayList<>();
        ArrayList<Instant> lastModifiedTime = new ArrayList<>();
        try {
            Map<String, Instant> filesFromServer = folderSynchronizer().scan();
            int filesNumber= in.read();
            for (int i = 0; i < filesNumber; i++) {
                uri.add(in.readLine());
                lastModifiedTime.add(Instant.parse(in.readLine()));

            } if(uri.size() > filesFromServer.size()){
                out.println(GET);
                for (int i = 0; i < uri.size(); i++) {
                    if(!filesFromServer.keySet().contains(uri.get(i))){
                        out.println(uri.get(i));
                        updateFileFromServer(uri.get(i));
                    } else if(filesFromServer.get(uri.get(i)).isBefore(lastModifiedTime.get(i))){
                        out.println(uri.get(i));
                        updateFileFromServer(uri.get(i));
                    }
                }
            } else if(uri.size() < filesFromServer.size()){
                List<String> keys = new ArrayList<>(filesFromServer.keySet());
                out.println(UPDATE);
                for (int i = 0; i < filesFromServer.size(); i++) {
                    if(!uri.contains(keys.get(i))){
                        out.println(keys.get(i));
                        sendFileContentToClient(keys.get(i));
                    } else if(filesFromServer.get(uri.get(i)).isAfter(lastModifiedTime.get(i))){
                        out.println(uri.get(i) + "\n" + uri.get(i) + " wurde auf Server editiert");
                        sendFileContentToClient(uri.get(i));
                    }
                }
            } else {
                List<String> keys = new ArrayList<>(filesFromServer.keySet());
                for (int i = 0; i < filesFromServer.size(); i++) {
                    if(filesFromServer.get(uri.get(i)).isBefore(lastModifiedTime.get(i))){
                        out.println(GET);
                        out.println(uri.get(i));
                        updateFileFromServer(uri.get(i));
                    }else if (filesFromServer.get(uri.get(i)).isAfter(lastModifiedTime.get(i))) {
                        out.println(UPDATE);
                        out.println(uri.get(i) + "\n" + uri.get(i) + " wurde auf Server editiert");
                        sendFileContentToClient(uri.get(i));
                    }
                }
            }
            FileSyncUtil.sleepFiveSeconds();
            initiate();
        } catch (IOException e) {
            System.err.println("Not a valid file number.");
        }
    }

    private void sendFileContentToClient(String file) throws IOException {
        FileContent fileContent;
        try {
            fileContent = folderSynchronizer().getFileContent(file);
        } catch (Exception e) {
            System.err.println("File read of " + file + " failed: " + e);
            out.println(FAILED);
            return;
        }
        sendFileContent(fileContent);
    }

    private void updateFileFromServer(String file) throws IOException {
        if(in.readLine().equals(SUCCESS)){
            receiveAndUpdateFile(file);
        }
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
            folderSynchronizer().updateIfNewer(file, fileContent);
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
