package src.main.java.misc;

import java.util.Date;

//This class is used to assist user initial Input
//by indicating users' states to solve the concurrency problem
public class UserInitialState {
    private int stateIndex;
    private String userName;
    private String[] stateList = {"id", "age", "gender", 
            "weight", "height", "desiredWeight", "goaldate"};
    
    String id;
    int age;
    String gender;
    int weight;
    int height;
    int desiredWeight;
    String goalDate;
    
    public UserInitialState(String name) {
        this.stateIndex = 0;
        this.userName = name;
    }
    public String getState() {
        return this.stateList[stateIndex];
    }
    public String getName() {
        return this.userName;
    }
    public void moveState() {
        this.stateIndex++;
    }
}
