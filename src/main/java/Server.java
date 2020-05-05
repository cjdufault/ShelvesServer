import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {

    private static final int PORT = 5000; // temporary port for testing
    private static final String DATABASE_URL = "jdbc:sqlite:tasksDB.sqlite";
    private static final Path AUTH_FILE_PATH = Path.of("auth.txt");

    public static void main(String[] args) {
        Database tasksDB = new Database(DATABASE_URL);
        ServerSideAuthentication auth = new ServerSideAuthentication(AUTH_FILE_PATH);

        try {
            // setup http server
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            RequestHandler handler = new RequestHandler(tasksDB, auth);

            server.createContext("/test_connection", handler);
            server.createContext("/request_nonce", handler);
            server.createContext("/get_all_tasks", handler);
            server.createContext("/get_complete_tasks", handler);
            server.createContext("/get_incomplete_tasks", handler);
            server.createContext("/get_task", handler);
            server.createContext("/search", handler);
            server.createContext("/get_dependencies", handler);
            server.createContext("/get_dependents", handler);
            server.createContext("/remove_task", handler);
            server.createContext("/complete_task", handler);
            server.createContext("/add_task", handler);
            server.createContext("/add_dependency", handler);
            server.createContext("/remove_dependency", handler);
            server.createContext("/update_claim", handler);

            server.setExecutor(new ThreadPoolExecutor
                    (4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100)));
            server.start();
            System.out.printf("ShelvesServer listening on port %s\n", PORT);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
