import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RequestHandler implements HttpHandler {

    private static final String STATUS_OK = "{\"status_code\":0}\n";
    private static final String STATUS_NOT_OK = "{\"status_code\":1}\n";

    private final Database tasksDB;
    private final ServerSideAuthentication auth;

    RequestHandler(Database tasksDB, ServerSideAuthentication auth){
        this.tasksDB = tasksDB;
        this.auth = auth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // gather info about the request
        String method = exchange.getRequestMethod();
        String requestURI = exchange.getRequestURI().toString(); // the url the client sent
        InetAddress remoteAddress = exchange.getRemoteAddress().getAddress();

        BufferedReader in = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        OutputStream out = exchange.getResponseBody();

        // split the url the client sent to find what kind of response it wants, and an argument, if any
        String[] pathComponents = requestURI.substring(1).split("/");
        String requestKeyword = pathComponents[0];
        String requestArgument = null;
        if (pathComponents.length > 1){
            requestArgument = pathComponents[1];
        }

        String response;
        int responseCode;

        // use the handling method appropriate to the request to make the response
        switch (method.toUpperCase()) {
            case "GET": {
                response = handleGetRequest(requestKeyword, requestArgument, remoteAddress);
                responseCode = 200;
                break;
            }
            case "POST": {
                response = handlePostRequest(requestKeyword, in, remoteAddress);
                responseCode = 200;
                break;
            }
            default: {
                response = "501 Not Implemented";
                responseCode = 501;
                break;
            }
        }

        // configure and send headers, then send the response body
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseCode, response.getBytes().length);
        out.write(response.getBytes());
        out.close();
    }

    private String handleGetRequest(String requestKeyword, String requestArgument, InetAddress remoteAddress){
        List<Task> returnedTasks = new ArrayList<>();

        switch (requestKeyword.toLowerCase()){
            case "test_connection": {
                // send a status code and confirmation that this server is a ShelvesServer
                return "{\"status_code\":0,\"service_name\":\"ShelvesServer\"}\n";
            }
            case "request_nonce": {
                return auth.getNonce(remoteAddress);
            }
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
                    return STATUS_NOT_OK;
                }
                break;
            }
            // get tasks whose names contain the query
            case "search":
                if (requestArgument != null){
                    returnedTasks = tasksDB.search(requestArgument);
                }
                else {
                    return STATUS_NOT_OK;
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
                    return STATUS_NOT_OK;
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
                    return STATUS_NOT_OK;
                }
                break;
            }
        }

        JSONObject json;

        // convert all tasks to JSON objects
        List<JSONObject> jsonObjects = new ArrayList<>();
        for (Task task : returnedTasks){
            jsonObjects.add(task.toJSON());
        }

        json = makeOutputJSON(jsonObjects);
        return json.toString();
    }

    private String handlePostRequest(String requestKeyword, BufferedReader in, InetAddress remoteAddress) throws IOException{
        String authHash = in.readLine(); // first line of the request body will be the pw hash
        boolean authenticated = auth.checkCredentials(authHash, remoteAddress);

        if (authenticated) { // only allow access if authenticated
            // read the remainder of the request body
            StringBuilder payloadBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                payloadBuilder.append(line);
            }
            String payload = payloadBuilder.toString();

            switch (requestKeyword) {
                case "auth": { // confirms that the submitted credential is valid; does no other action
                    return STATUS_OK;
                }
                case "add_task": {
                    Task newTask = parseTaskJSON(payload);

                    if (tasksDB.addTask(newTask)) {
                        return STATUS_OK;
                    }
                    return STATUS_NOT_OK;
                }
                case "add_dependency": {
                    JSONObject json = new JSONObject(payload);
                    int dependentID = json.getInt("dependentID");
                    int dependencyID = json.getInt("dependencyID");

                    if (tasksDB.addDependency(dependentID, dependencyID)) {
                        return STATUS_OK;
                    }
                    return STATUS_NOT_OK;
                }
                case "remove_dependency": {
                    JSONObject json = new JSONObject(payload);
                    int dependentID = json.getInt("dependent_id");
                    int dependencyID = json.getInt("dependency_id");

                    if (tasksDB.removeDependency(dependentID, dependencyID)) {
                        return STATUS_OK;
                    }
                    return STATUS_NOT_OK;
                }
                case "update_claim": {
                    JSONObject json = new JSONObject(payload);
                    int ID = json.getInt("id");
                    String claimedByEmail = json.getString("claimed_by_email");

                    if (tasksDB.updateClaim(ID, claimedByEmail)) {
                        return STATUS_OK;
                    }
                    return STATUS_NOT_OK;
                }
                case "remove_task": {
                    JSONObject json = new JSONObject(payload);
                    int ID = json.getInt("id");

                    if (tasksDB.removeTask(ID)) {
                        return STATUS_OK;
                    }
                    return STATUS_NOT_OK;
                }
                case "complete_task": {
                    JSONObject json = new JSONObject(payload);
                    int ID = json.getInt("id");

                    if (tasksDB.completeTask(ID)) {
                        return STATUS_OK;
                    }
                    return STATUS_NOT_OK;
                }
            }
        }
        return STATUS_NOT_OK;
    }

    // takes the JSON from the client and makes a Task out of it
    private Task parseTaskJSON(String payload){
        JSONObject json = new JSONObject(payload);

        // the easy ones
        int ID = json.getInt("id");
        String taskName = json.getString("task_name");
        String description = json.getString("description");
        Date dateCreated = new Date(json.getLong("date_created"));
        Date dateDue = new Date(json.getLong("date_due"));

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
                dateDue, false, false, dependencies, dependents);
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
