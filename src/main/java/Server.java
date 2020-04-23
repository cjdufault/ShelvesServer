import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {

    private static final int PORT = 5000; // temporary port for testing
    private static final String DATABASE_URL = "jdbc:sqlite:tasksDB.sqlite";

    public static void main(String[] args) {
        Database tasksDB = new Database(DATABASE_URL);

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            server.createContext("/test_connection", new RequestHandler(tasksDB));
            server.createContext("/get_all_tasks", new RequestHandler(tasksDB));
            server.createContext("/get_complete_tasks", new RequestHandler(tasksDB));
            server.createContext("/get_incomplete_tasks", new RequestHandler(tasksDB));
            server.createContext("/get_task", new RequestHandler(tasksDB));
            server.createContext("/search", new RequestHandler(tasksDB));
            server.createContext("/complete_task", new RequestHandler(tasksDB));

            server.start();
            System.out.printf("ShelvesServer listening on port %s", PORT);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
