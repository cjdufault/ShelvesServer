import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class testDB {

    private Database testDatabase;
    private static final String test_url = "jdbc:sqlite:testDB.sqlite";
    private String errorMessage;


    @Before
    public void start(){
        clearTable();
        testDatabase = new Database(test_url);
    }

    @Test
    public void testGetAllTasks(){
        clearTable();
        populateTable();
        List<Task> allTasks = testDatabase.getAllTasks();

        try {
            errorMessage = "getAllTasks() returned null";
            assert allTasks != null;
            errorMessage = "getAllTasks() didn't return the right number of tasks";
            assert allTasks.size() == 5;
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    @Test
    public void testGetCompleteTasks(){
        clearTable();
        populateTable();
        testDatabase.completeTask(3);
        List<Task> completeTasks = testDatabase.getCompleteTasks();

        try {
            errorMessage = "getCompleteTasks() returned null";
            assert completeTasks != null;
            errorMessage = "getCompleteTasks() didn't return the right number of tasks";
            assert completeTasks.size() == 1;
            errorMessage = "getCompleteTasks() returned an incomplete task";
            assert completeTasks.get(0).getComplete();
            errorMessage = "getCompleteTasks() returned the wrong task";
            assert completeTasks.get(0).getID() == 3;
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    @Test
    public void testGetIncompleteTasks(){
        clearTable();
        populateTable();
        testDatabase.completeTask(3);
        List<Task> incompleteTasks = testDatabase.getIncompleteTasks();

        try {
            errorMessage = "getIncompleteTasks() returned null";
            assert incompleteTasks != null;
            errorMessage = "getIncompleteTasks() didn't return the right number of tasks";
            assert incompleteTasks.size() == 4;
            errorMessage = "getIncompleteTasks() returned a complete task";
            for (Task task: incompleteTasks) {
                assert !task.getComplete();
            }
            errorMessage = "The ID of the 4th task returned by getIncompleteTasks() should have been 5, but it wasn't.";
            assert incompleteTasks.get(3).getID() == 5;
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    @Test
    public void testGetTask(){
        clearTable();
        populateTable();
        Task task = testDatabase.getTask(3);

        try {
            errorMessage = "getTask() didn't returned null";
            assert task != null;
            errorMessage = "getTask() returned the wrong task";
            assert task.getID() == 3;
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    @Test
    public void testSearch(){
        clearTable();
        populateTable();
        String expectedName = "Test Row #3";
        List<Task> result = testDatabase.search(expectedName);

        try {
            errorMessage = "search() returned null";
            assert result != null;
            errorMessage = "search() returned the wrong number of tasks";
            assert result.size() == 1;
            errorMessage = "search() returned the wrong task";
            assert result.get(0).getTaskName().equals(expectedName);
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    @Test
    public void testCompleteTask(){
        clearTable();
        populateTable();
        testDatabase.completeTask(3);
        Task task = testDatabase.getTask(3);

        try {
            errorMessage = "Couldn't find the task that completeTask() was applied to";
            assert task != null;
            errorMessage = "completeTask() failed to mark the task as complete";
            assert task.getComplete();
            errorMessage = "No dateComplete was found for the task completeTask() was applied to";
            assert task.getDateComplete() != null;
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    @Test
    public void testRemoveTask(){
        clearTable();
        populateTable();
        testDatabase.removeTask(3);

        try {
            errorMessage = "removeTask() failed to remove the task";
            assert testDatabase.getTask(3) == null; // will raise "SQLException: ResultSet closed"; this is fine
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    @Test
    public void testGetDatabaseCount(){
        clearTable();
        populateTable();
        int count = testDatabase.getDatabaseCount();

        try {
            errorMessage = "getDatabaseCount() returned the wrong number of items in the database";
            assert count == 5;
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    @Test
    public void testUpdateClaim(){
        clearTable();
        populateTable();
        String expectedEmail = "shevek@abbenay.org";
        testDatabase.updateClaim(3, expectedEmail);
        Task task = testDatabase.getTask(3);

        try {
            errorMessage = "updateClaim() didn't properly add the claimant's email address to the database";
            assert task.getClaimedByEmail().equals(expectedEmail);
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    @Test
    public void testAddDependency(){
        clearTable();
        populateTable();
        testDatabase.addDependency(3, 1);
        testDatabase.addDependency(3, 2);

        Task dependency1 = testDatabase.getTask(2);
        Task dependency2 = testDatabase.getTask(1);
        Task dependent = testDatabase.getTask(3);

        List<String> expectedDependencies = new ArrayList<>();
        expectedDependencies.add("1");
        expectedDependencies.add("2");

        List<String> expectedDependents = new ArrayList<>();
        expectedDependents.add("3");

        try{
            errorMessage = "addDependency() failed to add the expected dependencies";
            assert dependent.getDependencies().equals(expectedDependencies);

            errorMessage = "addDependency() failed to add the dependent task to its dependencies' dependents lists";
            assert dependency1.getDependents().equals(expectedDependents);
            assert dependency2.getDependents().equals(expectedDependents);
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    @Test
    public void testRemoveDependency(){
        clearTable();
        populateTable();
        testDatabase.addDependency(3, 1);
        testDatabase.removeDependency(3,1);

        Task dependency = testDatabase.getTask(1);
        Task dependent = testDatabase.getTask(3);

        try{
            errorMessage = "The dependent task was not removed from the dependency's dependents";
            assert dependency.getDependents().size() == 0;
            errorMessage = "The dependency was not removed from the dependent task's dependencies";
            assert dependent.getDependencies().size() == 0;
        }
        catch (AssertionError e){
            System.out.println(errorMessage);
            throw e;
        }
    }

    private void populateTable(){
        for (int i = 0; i < 5; i++) {
            List<String> requirements = new ArrayList<>();
            requirements.add("Req 1");
            requirements.add("Req 2");
            requirements.add("Req 3");
            Task task = new Task(String.format("Test Row #%s", i + 1), "This is a test",
                    requirements, new Date(), false, new ArrayList<>(), new ArrayList<>());
            testDatabase.addTask(task);
        }
    }

    private void clearTable(){
        try (Connection connection = DriverManager.getConnection(test_url)){
            Statement statement = connection.createStatement();

            String clearTableUpdate = "DELETE FROM Tasks";

            statement.executeUpdate(clearTableUpdate);
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }
}
