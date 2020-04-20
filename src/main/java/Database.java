import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Database {

    private String database_URL;

    Database(String url){
        database_URL = url;

        try (Connection connection = DriverManager.getConnection(database_URL)){
            Statement statement = connection.createStatement();

            String createTable = "CREATE TABLE IF NOT EXISTS Tasks (" +
                                    "ID INTEGER PRIMARY KEY, " +
                                    "taskName TEXT NOT NULL, " +
                                    "description TEXT, " +
                                    "requirements TEXT, " +
                                    "dateCreated NUMBER NOT NULL," +
                                    "dateDue NUMBER," +
                                    "dateComplete NUMBER," +
                                    "isComplete INT CHECK ( isComplete == 1 or isComplete == 0 )," + // 0: false, 1: true
                                    "claimedByEmail TEXT," + // email address for person who has claimed the task
                                    "dependencies TEXT," +
                                    "dependents TEXT)";
            statement.execute(createTable);
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    /** ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~GET METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ **/

    // search for all tasks whose names contain query
    public List<Task> search(String query){
        try (Connection connection = DriverManager.getConnection(database_URL)){
            String searchQuery = "SELECT * FROM Tasks WHERE taskName LIKE ?";
            PreparedStatement preparedStatement = connection.prepareStatement(searchQuery);

            preparedStatement.setString(1, "%" + query + "%"); // "%" means "any # of any character
            ResultSet results = preparedStatement.executeQuery();

            return parseResultSet(results);
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    // get task with a specific ID
    public Task getTask(int ID){
        try (Connection connection = DriverManager.getConnection(database_URL)){
            String getTaskQuery = "SELECT * FROM Tasks WHERE ID == ?";
            PreparedStatement preparedStatement = connection.prepareStatement(getTaskQuery);

            preparedStatement.setInt(1, ID);
            ResultSet results = preparedStatement.executeQuery();
            return makeTask(results);
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    // loads all tasks from the database and returns them as a list of Task objects
    public List<Task> getAllTasks(){
        try (Connection connection = DriverManager.getConnection(database_URL)){
            Statement statement = connection.createStatement();

            String getTasksQuery = "SELECT * FROM Tasks";
            ResultSet results = statement.executeQuery(getTasksQuery);

            return parseResultSet(results);
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    // loads just complete tasks
    public List<Task> getCompleteTasks(){
        try (Connection connection = DriverManager.getConnection(database_URL)){
            Statement statement = connection.createStatement();

            String getTasksQuery = "SELECT * FROM Tasks WHERE isComplete == 1";
            ResultSet results = statement.executeQuery(getTasksQuery);

            return parseResultSet(results);
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    // loads just incomplete tasks
    public List<Task> getIncompleteTasks(){
        try (Connection connection = DriverManager.getConnection(database_URL)){
            Statement statement = connection.createStatement();

            String getTasksQuery = "SELECT * FROM Tasks WHERE isComplete == 0";
            ResultSet results = statement.executeQuery(getTasksQuery);

            return parseResultSet(results);
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getDependencies(int ID){
        try (Connection connection = DriverManager.getConnection(database_URL)){
            String selectDependencies = "SELECT dependencies FROM Tasks WHERE ID == ?";
            PreparedStatement prepareSelectDependencies = connection.prepareStatement(selectDependencies);
            prepareSelectDependencies.setInt(1, ID);
            ResultSet selectDependenciesResults = prepareSelectDependencies.executeQuery();
            return new ArrayList<>(convertStringToList(selectDependenciesResults.getString("dependencies")));
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<String> getDependents(int ID){
        try (Connection connection = DriverManager.getConnection(database_URL)){
            String selectDependents = "SELECT dependents FROM Tasks WHERE ID == ?";
            PreparedStatement prepareSelectDependents = connection.prepareStatement(selectDependents);
            prepareSelectDependents.setInt(1, ID);
            ResultSet selectDependentsResults = prepareSelectDependents.executeQuery();
            return new ArrayList<>(convertStringToList(selectDependentsResults.getString("dependents")));
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public int getDatabaseCount(){
        try(Connection connection = DriverManager.getConnection(database_URL)){
            Statement statement = connection.createStatement();
            String getCount = "SELECT COUNT() FROM (SELECT * FROM Tasks)";

            ResultSet results = statement.executeQuery(getCount);

            return results.getInt("Count()");
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return -1;
    }

    /** ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ADD METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ **/

    public void addTask(Task task){
        int count = getDatabaseCount();

        try (Connection connection = DriverManager.getConnection(database_URL)){
            String addTaskUpdate = "INSERT INTO Tasks " +
                    "(taskName, description, requirements, dateCreated, dateDue, isComplete, claimedByEmail, dependencies, dependents) " +
                    "VALUES (?,?,?,?,?,?,'',?,?)";

            PreparedStatement preparedStatement = connection.prepareStatement(addTaskUpdate);

            preparedStatement.setString(1, task.getTaskName());
            preparedStatement.setString(2, task.getDescription());
            preparedStatement.setString(3, convertListToString(task.getRequirements()));
            preparedStatement.setLong(4, task.getDateCreated().getTime());
            preparedStatement.setLong(5, task.getDateDue().getTime());

            if (task.getComplete()) {
                preparedStatement.setInt(6, (1));
            }
            else {
                preparedStatement.setInt(6, (0));
            }
            preparedStatement.setString(7, "");
            preparedStatement.setString(8, "");

            preparedStatement.executeUpdate();

            for (String dependency : task.getDependencies()) {
                addDependency(count + 1, Integer.parseInt(dependency));
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    // designate that a task depends on another task
    public void addDependency(int dependentID, int dependencyID){ // dependent task relies on dependency task
        // get the existing dependencies that the dependent task has
        List<String> dependencies = getDependencies(dependentID);

        // get the existing dependents that the dependency has
        List<String> dependents = getDependents(dependencyID);

        if (!dependencies.contains(Integer.toString(dependencyID))) { // check if dependency already exists
            // add the dependent to the dependency's dependents and the dependency to the dependent's dependencies
            dependencies.add(Integer.toString(dependencyID));
            dependents.add(Integer.toString(dependentID));

            // update the dependencies column of the dependent task
            updateDependencies(dependentID, dependencies);

            // update the dependents column of the dependency task
            updateDependents(dependencyID, dependents);
        }
    }

    /** ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~REMOVE METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ **/

    // remove the task with the specified ID
    public void removeTask(int ID){
        Task task = getTask(ID);

        // clear all dependents and dependencies of this task
        for (String dependency: task.getDependencies()){
            removeDependency(ID, Integer.parseInt(dependency));
        }
        for (String dependent: task.getDependents()){
            removeDependency(Integer.parseInt(dependent), ID);
        }

        // delete from database
        try (Connection connection = DriverManager.getConnection(database_URL)){
            String removeTaskUpdate = "DELETE FROM Tasks WHERE ID == ?";
            PreparedStatement preparedStatement = connection.prepareStatement(removeTaskUpdate);

            preparedStatement.setInt(1, ID);

            preparedStatement.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void removeDependency(int dependentID, int dependencyID){
        // get the existing dependencies that the dependent task has
        List<String> dependencies = getDependencies(dependentID);

        // get the existing dependents that the dependency has
        List<String> dependents = getDependents(dependencyID);

        if (dependencies.contains(Integer.toString(dependencyID))) { // check if dependency actually exists
            // remove tasks from each other's lists
            dependencies.remove(Integer.toString(dependencyID));
            dependents.remove(Integer.toString(dependentID));

            // update each task
            updateDependencies(dependentID, dependencies);
            updateDependents(dependencyID, dependents);
        }
    }

    /** ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~UPDATE METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ **/

    // mark a task as complete
    public void completeTask(int ID){
        try (Connection connection = DriverManager.getConnection(database_URL)) {
            String update = "UPDATE Tasks SET isComplete = 1, dateComplete = ? WHERE ID == ?";
            PreparedStatement preparedStatement = connection.prepareStatement(update);

            long dateComplete = new java.util.Date().getTime();
            preparedStatement.setLong(1, dateComplete);
            preparedStatement.setInt(2, ID);
            preparedStatement.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // updates the email column for the person who has claimed the task
    public void updateClaim(int ID, String claimedByEmail){
        try (Connection connection = DriverManager.getConnection(database_URL)){
            String update = "UPDATE Tasks SET claimedByEmail = ? WHERE ID == ?";
            PreparedStatement preparedStatement = connection.prepareStatement(update);

            preparedStatement.setString(1, claimedByEmail);
            preparedStatement.setInt(2, ID);
            preparedStatement.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    // only used internally when adding or removing dependencies
    private void updateDependencies(int ID, List<String> dependencies){
        try (Connection connection = DriverManager.getConnection(database_URL)){
            String updateDependencies = "UPDATE Tasks SET dependencies = ? WHERE ID == ?";
            PreparedStatement prepareUpdateDependencies = connection.prepareStatement(updateDependencies);
            prepareUpdateDependencies.setString(1, convertListToString(dependencies));
            prepareUpdateDependencies.setInt(2, ID);
            prepareUpdateDependencies.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    // only used internally when adding or removing dependencies
    private void updateDependents(int ID, List<String> dependents){
        try (Connection connection = DriverManager.getConnection(database_URL)){
            String updateDependents = "UPDATE Tasks SET dependents = ? WHERE ID == ?";
            PreparedStatement prepareUpdateDependents = connection.prepareStatement(updateDependents);
            prepareUpdateDependents.setString(1, convertListToString(dependents));
            prepareUpdateDependents.setInt(2, ID);
            prepareUpdateDependents.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    /** ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~HELPER METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ **/

    // creates a Task object for each result in a ResultSet
    private List<Task> parseResultSet(ResultSet results) throws SQLException{
        List<Task> tasks = new ArrayList<>();

        while (results.next()){
            tasks.add(makeTask(results));
        }

        return tasks;
    }

    // creates Task objects from results of a database query
    private Task makeTask(ResultSet results) throws SQLException{
        int ID = results.getInt("ID");
        String taskName = results.getString("taskName");
        String description = results.getString("description");
        List<String> requirements = convertStringToList(results.getString("requirements"));
        Date dateCreated = new Date(results.getLong("dateCreated"));
        Date dateDue = new Date(results.getLong("dateDue"));
        boolean isComplete = results.getInt("isComplete") == 1;
        String claimedByEmail = results.getString("claimedByEmail");

        List<String> dependencies = convertStringToList(results.getString("dependencies"));
        List<String> dependents = convertStringToList(results.getString("dependents"));

        if (isComplete){
            Date dateComplete = new Date(results.getLong("dateComplete"));
            return new Task(ID, taskName, description, requirements, dateCreated,
                    dateDue, dateComplete, true, claimedByEmail, dependencies, dependents);
        }
        else if (claimedByEmail == null){
            return new Task(ID, taskName, description, requirements, dateCreated,
                    dateDue, false, dependencies, dependents);
        }
        else{
            return new Task(ID, taskName, description, requirements, dateCreated,
                    dateDue, false, claimedByEmail, dependencies, dependents);
        }
    }

    // to convert the reqs array into a comma-separated string to be stored in DB
    private String convertListToString(List<String> list){
        StringBuilder output = new StringBuilder();

        for (String string : list){
            output.append(string).append(",");
        }

        return output.toString();
    }

    // to convert reqs string back to an array
    private List<String> convertStringToList(String string){
        if (!string.equals("")) {
            return Arrays.asList(string.split(","));
        }
        return new ArrayList<>();
    }
}
