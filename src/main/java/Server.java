import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    private static final int PORT = 5000; // temporary port for testing
    private AtomicBoolean running = new AtomicBoolean();

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    void start(){
        running.set(true);

        try {
            ServerSocket serverConnection = new ServerSocket(PORT);
            System.out.println(String.format("ShelvesServer listening on Port %s", PORT));

            while (running.get()){
                ServerConnection connection = new ServerConnection(serverConnection.accept());

                // starts a new thread for each client connection
                Thread connectionThread = new Thread(connection);
                connectionThread.start();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
