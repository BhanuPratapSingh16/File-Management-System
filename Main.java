import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        Disk disk = new Disk(2048, 100);
        FileSystem fs = FileSystem.mount(disk, 2048, 100);
        run(disk, fs);
    }

    public static void run(Disk disk, FileSystem fs) throws IOException{
        System.out.println("Type 'help' for commands list");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
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
                    fs.listDirectory();
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
                    fs.createDirectory(newDirName);
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
                    fs.deleteDirectory(dirName);
                    break;
                }
                case "touch":{
                    if(parts.length > 2){
                        System.out.println("Usage: touch <file_name>");
                        break;
                    }
                    String newFileName = parts[1];
                    fs.createFile(newFileName);
                    break;
                }
                case "cat":{
                    if(parts.length > 2){
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
                    if(parts.length < 2){
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
                    fs.writeFile(fileName, fileContent.getBytes());
                    break;
                }
                case "rm":{
                    if(parts.length > 2){
                        System.out.println("Usage: rm <file_name>");
                        break;
                    }
                    String fileName = parts[1];
                    fs.deleteFile(fileName);
                    break;
                }
                case "ren":{
                    if(parts.length != 3){
                        System.out.println("Usage: ren <old_name> <new_name>");
                        break;
                    }
                    String oldName = parts[1];
                    String newName = parts[2];
                    fs.renameFile(oldName, newName);
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
                    fs.appendFile(fileName, appendData.getBytes());
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
                    fs.moveFile(oldPath, newPath);
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
                    fs.copyFile(oldPath, newPath);
                    break;
                }
                case "help":
                    System.out.println("Available commands:");
                    System.out.println("pwd - Display the present working directory");
                    System.out.println("cd <directory> - Change directory");
                    System.out.println("ls - List files and directories");
                    System.out.println("mkdir <directory> - Create a new directory");
                    System.out.println("rmdir <directory> - Remove a directory");
                    System.out.println("touch <file> - Create a new file");
                    System.out.println("cat <file> - Read a file");
                    System.out.println("write <file> - Write data to a file");
                    System.out.println("ren <file> - Rename a file");
                    System.out.println("rm <file> - Remove a file");
                    System.out.println("mv <\"old file path\"> <\"new file path\"> - Move a file");
                    System.out.println("cp <\"old file path\"> <\"new file path\"> - Copy a file");
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
