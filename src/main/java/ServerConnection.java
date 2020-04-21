import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ServerConnection implements Runnable{

    private static final String database_URL = "jdbc:sqlite:tasksDB.sqlite";

    private Database tasksDB = new Database(database_URL);
    private Socket connection;

    ServerConnection(Socket connection){
        this.connection = connection;
    }

    // handles each client connection
    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            // reads from client connection
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            // writes to client connection
            out = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()), true);

            // read the header in from the BufferedReader
            StringBuilder headerBuilder = new StringBuilder();

            String headerLine = in.readLine();
            while (headerLine.length() != 0){
                headerBuilder.append(headerLine);
                headerBuilder.append("\n");
                headerLine = in.readLine();
            }
            String header = headerBuilder.toString();

            // grab the first line and split it up by the spaces
            String[] splitHeaderLineOne = header.split("\n")[0].split(" ");

            String method = splitHeaderLineOne[0]; // e.g. GET, POST, etc
            String[] request = splitHeaderLineOne[1].substring(1).split("/");
            String requestKeyword = request[0]; // indicates what kind of request this is

            String requestArgument = null;
            if (request.length > 1) requestArgument = request[1];

            switch (method.toUpperCase()) {
                case "GET": {
                    handleGetRequest(requestKeyword, requestArgument, out);
                    break;
                }
                case "POST": {
                    handlePostRequest(requestKeyword, in, out);
                    break;
                }
                default: {
                    out.println("501 Not Implemented");
                    break;
                }
            }
        }
        catch (IOException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                connection.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void handleGetRequest(String requestKeyword, String requestArgument, PrintWriter out){
        List<Task> returnedTasks = new ArrayList<>();

        switch (requestKeyword.toLowerCase()){
            // get all tasks
            case "get_all_tasks": {
                returnedTasks = tasksDB.getAllTasks();
                break;
            }
            // get complete tasks
            case "get_complete_tasks": {
                returnedTasks = tasksDB.getCompleteTasks();
                break;
            }
            // get incomplete tasks
            case "get_incomplete_tasks": {
                returnedTasks = tasksDB.getIncompleteTasks();
                break;
            }
            // get a specific task by ID
            case "get_task": {
                if (requestArgument != null) {
                    int getID = Integer.parseInt(requestArgument);
                    Task getTask = tasksDB.getTask(getID);
                    if (getTask != null) {
                        returnedTasks.add(tasksDB.getTask(getID));
                    }
                }
                else {
                    out.println("No argument provided");
                }
                break;
            }
            // get tasks whose names contain the query
            case "search":
                if (requestArgument != null){
                    returnedTasks = tasksDB.search(requestArgument);
                }
                else {
                    out.println("No argument provided");
                }
                break;
            // get a task's dependencies (tasks it depends on)
            case "get_dependencies": {
                if (requestArgument != null) {
                    int ID = Integer.parseInt(requestArgument);
                    List<String> dependencies = tasksDB.getDependencies(ID);
                    if (dependencies != null) {
                        for (String dependency : dependencies) {
                            int dependencyID = Integer.parseInt(dependency);
                            Task dependencyTask = tasksDB.getTask(dependencyID);
                            returnedTasks.add(dependencyTask);
                        }
                    }
                }
                else {
                    out.println("No argument provided");
                }
                break;
            }
            // get a task's dependents (tasks that depend on it)
            case "get_dependents": {
                if (requestArgument != null) {
                    int ID = Integer.parseInt(requestArgument);
                    List<String> dependents = tasksDB.getDependents(ID);
                    if (dependents != null) {
                        for (String dependent : dependents) {
                            int dependentID = Integer.parseInt(dependent);
                            Task dependentTask = tasksDB.getTask(dependentID);
                            returnedTasks.add(dependentTask);
                        }
                    }
                }
                else {
                    out.println("No argument provided");
                }
                break;
            }
            case "remove_task": {
                if (requestArgument != null){
                    int ID = Integer.parseInt(requestArgument);
                    tasksDB.removeTask(ID);
                }
                break;
            }
            case "complete_task": {
                if (requestArgument != null){
                    int ID = Integer.parseInt(requestArgument);
                    tasksDB.completeTask(ID);
                }
                break;
            }
        }

        JSONObject json;

        // special case for testing connection w/o making a list of tasks
        if (requestKeyword.equals("test_connection")){
            json = new JSONObject("{\"status_code\":0}");
        }
        else {
            // convert all tasks to JSON objects
            List<JSONObject> jsonObjects = new ArrayList<>();
            for (Task task : returnedTasks){
                jsonObjects.add(task.toJSON());
            }

            json = makeOutputJSON(jsonObjects);
        }

        out.println(json);
    }

    // TODO: add a key that the admin can use to protect access to these
    private void handlePostRequest(String requestKeyword, BufferedReader in, PrintWriter out) throws IOException{
        StringBuilder payloadBuilder = new StringBuilder();
        while (in.ready()){
            payloadBuilder.append((char) in.read());
        }
        String payload = payloadBuilder.toString();

        switch (requestKeyword){
            case "add_task": {
                Task newTask = parseTaskJSON(payload);
                tasksDB.addTask(newTask);
                break;
            }
            case "add_dependency": {
                JSONObject json = new JSONObject(payload);
                int dependentID = json.getInt("dependentID");
                int dependencyID = json.getInt("dependencyID");
                tasksDB.addDependency(dependentID, dependencyID);
                break;
            }
            case "remove_dependency": {
                JSONObject json = new JSONObject(payload);
                int dependentID = json.getInt("dependentID");
                int dependencyID = json.getInt("dependencyID");
                tasksDB.removeDependency(dependentID, dependencyID);
                break;
            }
            case "update_claim": {
                JSONObject json = new JSONObject(payload);
                int ID = json.getInt("ID");
                String claimedByEmail = json.getString("claimedByEmail");
                tasksDB.updateClaim(ID, claimedByEmail);
            }
        }
    }

    // takes the JSON from the client and makes a Task out of it
    private Task parseTaskJSON(String payload){
        JSONObject json = new JSONObject(payload);

        // the easy ones
        int ID = json.getInt("ID");
        String taskName = json.getString("taskName");
        String description = json.getString("description");
        Date dateCreated = new Date(json.getLong("dateCreated"));
        Date dateDue = new Date(json.getLong("dateDue"));

        // convert the JSONArrays to Lists
        List<String> requirements = new ArrayList<>();
        for (Object requirement : json.getJSONArray("requirements")){
            requirements.add(requirement.toString());
        }
        List<String> dependencies = new ArrayList<>();
        for (Object dependency : json.getJSONArray("dependencies")){
            dependencies.add(dependency.toString());
        }
        List<String> dependents = new ArrayList<>();
        for (Object dependent : json.getJSONArray("dependents")){
            dependents.add(dependent.toString());
        }

        return new Task(ID, taskName, description, requirements, dateCreated,
                dateDue, false, dependencies, dependents);
    }

    // makes the JSONObject that will be sent to the client when they request task info
    private JSONObject makeOutputJSON(List<JSONObject> jsonObjects){
        JSONObject jsonOut = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        // status code is 0 if there were tasks found, 1 if not
        if (jsonObjects.size() > 0){
            jsonOut.put("status_code", 0);
        }
        else {
            jsonOut.put("status_code", 1);
        }

        // add all JSONObjects to JSONArray
        for (JSONObject json : jsonObjects){
            jsonArray.put(json);
        }

        jsonOut.put("results", jsonArray);
        return jsonOut;
    }
}
