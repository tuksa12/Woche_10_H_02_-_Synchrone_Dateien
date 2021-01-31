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
//The server will not work when the updateIfNewer is called, but the the idea should be more or less right
public class FileSyncServer extends FileSyncParticipant{
    // Status responses
    static final String UPDATE = "UPDATE";
    static final String GET = "GET";
    static final String LIST = "LIST";

    // Client responses
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
    public void run() {//The same as in the client, with the addition of the helpMethod initiate()
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
    //Initiates the "conversation" by printing "LIST"
    private void initiate(){
        out.println(LIST);
    }

    private void executeNextCommand() throws IOException {
        String command = in.readLine();
        switch (command) {
            case SUCCESS -> getFileListFromClient();
            default -> System.err.print("Unknown command from client: " + command);//Case Failed
        }
    }

    private void getFileListFromClient() {//Get the information from the client
        ArrayList<String> uri = new ArrayList<>();//ArrayList of URI from the user
        ArrayList<Instant> lastModifiedTime = new ArrayList<>();//ArrayList of lastModifiedTime from the user
        try {
            Map<String, Instant> filesFromServer = folderSynchronizer().scan();//Files from the server
            int filesNumber= in.read();//Number of files that the client has
            for (int i = 0; i < filesNumber; i++) {//Loop to receive the input from the client and store in the two ArrayLists. It will repeat for the number of "filesNumber"
                uri.add(in.readLine());
                lastModifiedTime.add(Instant.parse(in.readLine()));

            } if(uri.size() > filesFromServer.size()){//If client has more files -> GET
                out.println(GET);
                for (int i = 0; i < uri.size(); i++) {
                    if(!filesFromServer.keySet().contains(uri.get(i))){//If the server doesn't have the file, get the file
                        out.println(uri.get(i));
                        updateFileFromServer(uri.get(i));
                    } else if(filesFromServer.get(uri.get(i)).isBefore(lastModifiedTime.get(i))){//if the server has the file, but not the most recent version
                        out.println(uri.get(i));
                        updateFileFromServer(uri.get(i));
                    }
                }
            } else if(uri.size() < filesFromServer.size()){//If server has more files -> UPDATE
                List<String> keys = new ArrayList<>(filesFromServer.keySet());
                out.println(UPDATE);
                for (int i = 0; i < filesFromServer.size(); i++) {
                    if(!uri.contains(keys.get(i))){//If the client doesn't have the file, send the file
                        out.println(keys.get(i));
                        sendFileContentToClient(keys.get(i));
                    } else if(filesFromServer.get(uri.get(i)).isAfter(lastModifiedTime.get(i))){//if the client has the file, but not the most recent version
                        out.println(uri.get(i) + "\n" + uri.get(i) + " wurde auf Server editiert");
                        sendFileContentToClient(uri.get(i));
                    }
                }
            } else {//If the sizes of files are equal
                for (int i = 0; i < filesFromServer.size(); i++) {
                    if(filesFromServer.get(uri.get(i)).isBefore(lastModifiedTime.get(i))){//if the server has the file, but not the most recent version
                        out.println(GET);
                        out.println(uri.get(i));
                        updateFileFromServer(uri.get(i));
                    }else if (filesFromServer.get(uri.get(i)).isAfter(lastModifiedTime.get(i))) {//if the client has the file, but not the most recent version
                        out.println(UPDATE);
                        out.println(uri.get(i) + "\n" + uri.get(i) + " wurde auf Server editiert");
                        sendFileContentToClient(uri.get(i));
                    }
                }
            }
            FileSyncUtil.sleepFiveSeconds();//Step 10
            initiate();
        } catch (IOException e) {
            System.err.println("Error while working with the files.");
        }
    }

    private void sendFileContentToClient(String file) throws IOException {//Same idea as the method in the client, bt with a parameter
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

    private void updateFileFromServer(String file) throws IOException {//Same idea as the method in the client, but with a parameter and the if statement
        if(in.readLine().equals(SUCCESS)){
            receiveAndUpdateFile(file);
        }
    }

    void receiveAndUpdateFile(String file) throws IOException {//Same as in client
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

    void sendFileContent(FileContent fileContent) {//Same as in client
        out.println(fileContent.getLastModifiedTime());
        out.println(fileContent.getLines().size());
        for (String line : fileContent.getLines()) {
            out.println(line);
        }
    }

    public static void main(String[] args) {//Main method but with only two parameters
        if (args.length != 2)
            throw new IllegalArgumentException("Invalid number of arguments");
        Path folder = Path.of(args[0]);
        int port = Integer.parseInt(args[1]);
        new FileSyncServer(folder, port).run();
    }
}
