import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private Map<String, String> users = new HashMap<>();
    private String currentUser;
    private String filePath = "users.txt";

    public void loadUsers(String filePath) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        while((line = br.readLine()) != null){
            if(line.isEmpty())  continue;
            String[] parts = line.split(":");
            users.put(parts[0], parts[1]);
        }
        br.close();
    }

    public void signup(String userName, String password, String filePath) throws IOException{
        if(users.containsKey(userName)){
            throw new IllegalArgumentException("User with "+userName+" already exists. Try login");
        }
        users.put(userName, password);
        BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true));
        bw.write(userName+":"+password+"\n");
        bw.close();
    }

    public boolean login(String userName, String password){
        return users.containsKey(userName) && users.get(userName).equals(password);
    }

    public boolean userExists(String userName){
        return users.containsKey(userName);
    }

    public void setCurrentUser(String currentUser){
        this.currentUser = currentUser;
    }

    public String getCurrentUser(){
        return currentUser;
    }

    public void removeUser(String userName, String password) throws IOException{
        if(!users.containsKey(userName) || !users.get(userName).equals(password)){
            throw new RuntimeException("Wrong password!");
        }
        users.remove(userName);
        File inputFile = new File(filePath);
        File tempFile = new File("temp.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(userName+":"+password)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
        inputFile.delete();
        tempFile.renameTo(inputFile);
    }

    public boolean isValidPassword(String password){
        return users.get(currentUser).equals(password);
    }
}
