import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RequestHandler implements HttpHandler {

    private Database tasksDB;

    RequestHandler(Database tasksDB){
        this.tasksDB = tasksDB;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // gather info about the request
        String method = exchange.getRequestMethod();
        String requestURI = exchange.getRequestURI().toString(); // the url the client sent

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
                response = handleGetRequest(requestKeyword, requestArgument);
                responseCode = 200;
                break;
            }
            case "POST": {
                response = handlePostRequest(requestKeyword, in);
                responseCode = 200;
                break;
            }
            default: {
                response = "501 Not Implemented";
                responseCode = 501;
                break;
            }
        }

        // send the response
        exchange.sendResponseHeaders(responseCode, response.getBytes().length);
        out.write(response.getBytes());
        out.close();
    }

    private String handleGetRequest(String requestKeyword, String requestArgument){
        List<Task> returnedTasks = new ArrayList<>();

        switch (requestKeyword.toLowerCase()){
            case "test_connection":{
                // send a status code and confirmation that this server is a ShelvesServer
                return "{\"status_code\":0,\"service_name\":\"ShelvesServer\"}";
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
                    return "{\"status_code\":1}";
                }
                break;
            }
            // get tasks whose names contain the query
            case "search":
                if (requestArgument != null){
                    returnedTasks = tasksDB.search(requestArgument);
                }
                else {
                    return "{\"status_code\":1}";
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
                    return "{\"status_code\":1}";
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
                    return "{\"status_code\":1}";
                }
                break;
            }
            case "remove_task": {
                if (requestArgument != null){
                    int ID = Integer.parseInt(requestArgument);

                    if (tasksDB.removeTask(ID)){
                        return "{\"status_code\":0}";
                    }
                    return "{\"status_code\":1}";
                }
            }
            case "complete_task": {
                if (requestArgument != null){
                    int ID = Integer.parseInt(requestArgument);

                    if (tasksDB.completeTask(ID)){
                        return "{\"status_code\":0}";
                    }
                    return "{\"status_code\":1}";
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

        return json.toString();
    }

    // TODO: add a key that the admin can use to protect access to these
    private String handlePostRequest(String requestKeyword, BufferedReader in) throws IOException{
        StringBuilder payloadBuilder = new StringBuilder();
        while (in.ready()){
            payloadBuilder.append((char) in.read());
        }
        String payload = payloadBuilder.toString();

        switch (requestKeyword){
            case "add_task": {
                Task newTask = parseTaskJSON(payload);

                if (tasksDB.addTask(newTask)){
                    return "{\"status_code\":0}";
                }
                return "{\"status_code\":1}";
            }
            case "add_dependency": {
                JSONObject json = new JSONObject(payload);
                int dependentID = json.getInt("dependentID");
                int dependencyID = json.getInt("dependencyID");

                if (tasksDB.addDependency(dependentID, dependencyID)){
                    return "{\"status_code\":0}";
                }
                return "{\"status_code\":1}";
            }
            case "remove_dependency": {
                JSONObject json = new JSONObject(payload);
                int dependentID = json.getInt("dependentID");
                int dependencyID = json.getInt("dependencyID");

                if (tasksDB.removeDependency(dependentID, dependencyID)){
                    return "{\"status_code\":0}";
                }
                return "{\"status_code\":1}";
            }
            case "update_claim": {
                JSONObject json = new JSONObject(payload);
                int ID = json.getInt("ID");
                String claimedByEmail = json.getString("claimedByEmail");

                if (tasksDB.updateClaim(ID, claimedByEmail)){
                    return "{\"status_code\":0}";
                }
                return "{\"status_code\":1}";
            }
        }
        return "{\"status_code\":1}";
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
