import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        Disk disk = new Disk(2048, 100);
        FileSystem fs = FileSystem.mount(disk, 2048, 100);
        UserManager userManager = new UserManager();
        userManager.loadUsers("users.txt");
        run(disk, fs, userManager);
    }

    public static void run(Disk disk, FileSystem fs, UserManager userManager) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while(true){
            System.out.print("Choose Login/Signup(l/s) : ");
            String inp = reader.readLine().toLowerCase().trim();
            if(inp.length() != 1)   continue;
            char ch = inp.charAt(0);
            if(ch == 'l'){
                System.out.print("Enter user name : ");
                String userName = reader.readLine().trim();
                System.out.print("Enter password : ");
                String password = reader.readLine().trim();
                if(userManager.login(userName, password)){
                    System.out.println("Login successful.");
                    userManager.setCurrentUser(userName);
                    fs.changeDirectory(userName);
                    break;
                }
                else{
                    System.out.println("Invalid username or password. Try signup");
                }
            }
            else if(ch == 's'){
                System.out.print("Enter user name : ");
                String userName = reader.readLine().trim();
                if(userManager.userExists(userName)){
                    System.out.println("User "+userName+" already exists. Try login");
                    continue;
                }
                System.out.print("Enter password : ");
                String password = reader.readLine().trim();
                try{
                    userManager.signup(userName, password, "users.txt");
                    System.out.println("Successful signup and login");
                    fs.createDirectory(userName, "root", userName);
                    fs.changeDirectory(userName);
                    userManager.setCurrentUser(userName);
                }
                catch(IOException e){
                    e.printStackTrace();
                }
                break;
            }
        }

        System.out.println("Type 'help' for commands list");
        while(true){
            System.out.print(">> ");
            String command = reader.readLine();
            if (command == null || command.trim().isEmpty()) {
                continue;
            }
            if(command.equalsIgnoreCase("exit")) {
                System.out.println("Exiting...");
                break;
            }
            String[] parts = command.split(" ");
            String cmd = parts[0].toLowerCase();
            try{
            switch (cmd) {
                case "pwd":{
                    System.out.println("Current directory: " + fs.getCurrentDirectory());
                    break;
                }
                case "cd":{
                    String dirName = "";
                    for(int i=1;i<parts.length;i++){
                        if(i==1)    dirName = parts[i];
                        else{
                            dirName += " "+parts[i];
                        }
                    }
                    fs.changeDirectory(dirName);
                    break;
                }
                case "ls":{
                    if(parts.length != 1){
                        System.out.println("Usage: ls");
                        break;
                    }
                    fs.listDirectory(userManager.getCurrentUser());
                    break;
                }
                case "mkdir":{
                    String newDirName = "";
                    for(int i=1;i<parts.length;i++){
                        if(i==1)    newDirName = parts[i];
                        else{
                            newDirName += " "+parts[i];
                        }
                    }
                    fs.createDirectory(newDirName, userManager.getCurrentUser(), userManager.getCurrentUser());
                    break;
                }
                case "rmdir":{
                    String dirName = "";
                    for(int i=1;i<parts.length;i++){
                        if(i==1)    dirName = parts[i];
                        else{
                            dirName += " "+parts[i];
                        }
                    }
                    fs.deleteDirectory(dirName, userManager.getCurrentUser());
                    break;
                }
                case "infodir":{
                    if(parts.length != 1){
                        System.out.println("Usage: infodir");
                        break;
                    }
                    fs.displayDirectoryInfo();
                    break;
                }
                case "touch":{
                    if(parts.length != 2){
                        System.out.println("Usage: touch <file_name>");
                        break;
                    }
                    String newFileName = parts[1];
                    fs.createFile(newFileName, userManager.getCurrentUser());
                    break;
                }
                case "cat":{
                    if(parts.length != 2){
                        System.out.println("Usage: cat <file_name>");
                        break;
                    }
                    String fileName = parts[1];
                    byte[] data = fs.readFile(fileName);
                    System.out.println("Contents of " + fileName + ":");
                    if (data != null) {
                        System.out.println(new String(data));
                    } else {
                        System.out.println("File not found or empty.");
                    }
                    break;
                }
                case "write":{
                    if(parts.length != 2){
                        System.out.println("Usage: write <file_name>");
                        break;
                    }
                    String fileName = parts[1];
                    System.out.println("Enter data to write (type 'exit' on a new line to finish):");
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    String fileContent = "";
                    while(true){
                        String line = br.readLine();
                        if(line.equalsIgnoreCase("exit")) {
                            break;
                        }
                        fileContent += line + "\n";
                    }
                    fileContent = fileContent.substring(0, fileContent.length()-1);
                    fs.writeFile(fileName, fileContent.getBytes(), userManager.getCurrentUser());
                    break;
                }
                case "rm":{
                    if(parts.length != 2){
                        System.out.println("Usage: rm <file_name>");
                        break;
                    }
                    String fileName = parts[1];
                    fs.deleteFile(fileName, userManager.getCurrentUser());
                    break;
                }
                case "ren":{
                    if(parts.length != 3){
                        System.out.println("Usage: ren <old_name> <new_name>");
                        break;
                    }
                    String oldName = parts[1];
                    String newName = parts[2];
                    fs.renameFile(oldName, newName, userManager.getCurrentUser());
                    break;
                }
                case "append":{
                    if(parts.length < 2){
                        System.out.println("Usage: append <file_name>");
                        break;
                    }
                    String fileName = parts[1];
                    System.out.println("Enter data to append (type 'exit' on a new line to finish):");
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    String appendData = "";
                    while(true){
                        String line = br.readLine();
                        if(line.equalsIgnoreCase("exit")) {
                            break;
                        }
                        appendData += line + "\n";
                    }
                    appendData = appendData.substring(0, appendData.length()-1);
                    fs.appendFile(fileName, appendData.getBytes(), userManager.getCurrentUser());
                    break;
                }
                case "mv":{
                    String[] split = command.split("\"");
                    if(split.length != 4){
                        System.out.println("Usage mv <\"old file path\"> <\"new file path\">");
                        break;
                    }
                    String oldPath = split[1];
                    String newPath = split[3];
                    fs.moveFile(oldPath, newPath, userManager.getCurrentUser());
                    break;
                }
                case "cp":{
                    String[] split = command.split("\"");
                    if(split.length != 4){
                        System.out.println("Usage cp <\"old file path\"> <\"new file path\">");
                        break;
                    }
                    String oldPath = split[1];
                    String newPath = split[3];
                    fs.copyFile(oldPath, newPath, userManager.getCurrentUser());
                    break;
                }
                case "info":{
                    if(parts.length != 2){
                        System.out.println("Usage : info <file>");
                        break;
                    }
                    String fileName = parts[1];
                    fs.displayFileInfo(fileName);
                    break;
                }
                case "help":
                    System.out.println("Available commands:");
                    System.out.println("pwd - Display the present working directory");
                    System.out.println("cd <directory> - Change directory");
                    System.out.println("ls - List files and directories");
                    System.out.println("mkdir <directory> - Create a new directory");
                    System.out.println("rmdir <directory> - Remove a directory");
                    System.out.println("infodir <directory_name> - Displays information about directory_name");
                    System.out.println("touch <file> - Create a new file");
                    System.out.println("cat <file> - Read a file");
                    System.out.println("write <file> - Write data to a file");
                    System.out.println("ren <file> - Rename a file");
                    System.out.println("rm <file> - Remove a file");
                    System.out.println("mv <\"old file path\"> <\"new file path\"> - Move a file");
                    System.out.println("cp <\"old file path\"> <\"new file path\"> - Copy a file");
                    System.out.println("info <file> - Displays information about file");
                    System.out.println("exit - Exit the file system");
                    break;
                default:
                    System.out.println("Unknown command: " + cmd);
                    break;
                }
            }
            catch(RuntimeException e){
                System.out.println("Error : "+e.getMessage());
            }
        }
    }
}
