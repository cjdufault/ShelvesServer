import org.json.JSONObject;
import java.util.Date;
import java.util.List;

public class Task {

    private int ID;
    private String taskName;
    private String description;
    private List<String> requirements;
    private Date dateCreated;
    private Date dateDue;
    private Date dateComplete;
    private boolean isComplete;
    private boolean isClaimed;
    private String claimedByEmail;
    private List<String> dependencies; // list of the IDs of tasks that this task depends on
    private List<String> dependents; // list of the IDs of tasks that depend on this task

    // for new tasks
    Task(String taskName, String description, List<String> requirements,
         Date dateDue, boolean isComplete, List<String> dependencies, List<String> dependents){
        this.taskName = taskName;
        this.description = description;
        this.requirements = requirements;
        this.dateCreated = new Date();
        this.dateDue = dateDue;
        this.isComplete = isComplete;
        this.dependencies = dependencies; // tasks this task depends on
        this.dependents = dependents; // tasks that depend on this task
    }

    // for unfinished, unclaimed tasks loaded from DB
    Task(int taskID, String taskName, String description, List<String> requirements, Date dateCreated, Date dateDue,
         boolean isComplete, boolean isClaimed, List<String> dependencies, List<String> dependents){
        this.ID = taskID;
        this.taskName = taskName;
        this.description = description;
        this.requirements = requirements;
        this.dateCreated = dateCreated;
        this.dateDue = dateDue;
        this.isComplete = isComplete;
        this.isClaimed = isClaimed;
        this.dependencies = dependencies;
        this.dependents = dependents;
    }

    // for unfinished, claimed tasks loaded from DB
    Task(int taskID, String taskName, String description, List<String> requirements, Date dateCreated, Date dateDue,
         boolean isComplete, boolean isClaimed, String claimedByEmail, List<String> dependencies, List<String> dependents){
        this.ID = taskID;
        this.taskName = taskName;
        this.description = description;
        this.requirements = requirements;
        this.dateCreated = dateCreated;
        this.dateDue = dateDue;
        this.isComplete = isComplete;
        this.isClaimed = isClaimed;
        this.claimedByEmail = claimedByEmail;
        this.dependencies = dependencies;
        this.dependents = dependents;
    }

    // for finished tasks loaded from DB
    Task(int taskID, String taskName, String description, List<String> requirements, Date dateCreated, Date dateDue, Date dateComplete,
         boolean isComplete, boolean isClaimed, String claimedByEmail, List<String> dependencies, List<String> dependents){
        this.ID = taskID;
        this.taskName = taskName;
        this.description = description;
        this.requirements = requirements;
        this.dateCreated = dateCreated;
        this.dateDue = dateDue;
        this.dateComplete = dateComplete;
        this.isComplete = isComplete;
        this.isClaimed = isClaimed;
        this.claimedByEmail = claimedByEmail;
        this.dependencies = dependencies;
        this.dependents = dependents;
    }

    public int getID() { return ID; }
    public String getTaskName() { return taskName; }
    public String getDescription() { return description; }
    public List<String> getRequirements() { return requirements; }
    public Date getDateCreated() { return dateCreated; }
    public Date getDateDue() { return dateDue; }
    public Date getDateComplete() { return dateComplete; }
    public boolean getComplete() { return isComplete; }
    public boolean getClaimed() { return isClaimed; }
    public String getClaimedByEmail() { return claimedByEmail; }
    public List<String> getDependencies() { return dependencies; }
    public List<String> getDependents() { return dependents; }

    public JSONObject toJSON(){
        JSONObject json = new JSONObject();

        json.put("id", ID);
        json.put("task_name", taskName);
        json.put("description", description);
        json.put("requirements", requirements);
        json.put("date_created", dateCreated.getTime());
        json.put("date_due", dateDue.getTime());
        json.put("is_complete", isComplete);
        json.put("is_claimed", isClaimed);
        json.put("dependencies", dependencies);
        json.put("dependents", dependents);

        if (isComplete) {
            json.put("date_complete", dateComplete.getTime());
            json.put("claimed_by_email", claimedByEmail);
            json.put("dependencies", dependencies);
            json.put("dependents", dependents);
        }
        else if (isClaimed){
            json.put("claimed_by_email", claimedByEmail);
        }

        return json;
    }
}
