package pgdp.sync;

import javax.sound.sampled.Port;
import java.nio.file.Path;

public class FileSyncServer extends FileSyncParticipant{
    int port;

    public FileSyncServer(Path folder, int port) {
        super(folder);
        this.port = port;
    }

    @Override
    public void run() {

    }
}
