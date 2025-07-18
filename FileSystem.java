import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import FAT.Directory;
import FAT.FatTable;
import FAT.MyFile;

public class FileSystem {
    private final Disk disk;
    public Superblock sb;
    public FatTable fatTable;
    public Directory cur;

    public FileSystem(Disk disk, Superblock sb, FatTable fatTable) {
        this.disk = disk;
        this.sb = sb;
        this.fatTable = fatTable;
        this.cur = new Directory("/", sb.rootDirStartBlock, -1, "root");  // Default directory is root
    }

    public static FileSystem mount(Disk disk, int blockSize, int numBlocks) {
        byte[] readSuperblock = disk.readBlock(0);
        int magicNumber = ByteBuffer.wrap(readSuperblock).getInt();

        if(magicNumber != 0x23D4F8){
            Superblock sb = new Superblock(blockSize, numBlocks);
            FatTable fat = new FatTable(numBlocks, sb.rootDirStartBlock+1);
            FileSystem fs = new FileSystem(disk, sb, fat);

            Directory root = new Directory("/", sb.rootDirStartBlock, -1, "root");
            fat.setEOF(sb.rootDirStartBlock);
            fs.cur = root;
            byte[] rootData = root.serialize();
            disk.writeBlock(sb.rootDirStartBlock, rootData);
            fs.save();
            return fs;
        }

        Superblock sb = Superblock.deserialize(readSuperblock);

        byte[] fatData = new byte[sb.fatSize];
        for(int i=0;i<sb.fatBlocksCount;i++){
            byte[] block = disk.readBlock(i+sb.fatStartBlock);
            System.arraycopy(block, 0, fatData, i * sb.fatBlockSize, Math.min(sb.blockSize, fatData.length - i * sb.blockSize));
        }
        FatTable fTable = FatTable.deserialize(fatData, sb.rootDirStartBlock+1);

        byte[] data = disk.readBlock(sb.rootDirStartBlock);
        Directory root = Directory.deserialize(data);
        FileSystem fs = new FileSystem(disk, sb, fTable);
        fs.cur = root;
        return fs;
    }

    public void save(){
        disk.writeBlock(0, sb.serialize());
        byte[] fatData = fatTable.serialize();
        for(int i=0;i<sb.fatBlocksCount;i++){
            byte[] blockData = new byte[sb.blockSize];
            int len = Math.min(sb.blockSize, fatData.length - i * sb.blockSize);
            System.arraycopy(fatData, i * sb.blockSize, blockData, 0, len);
            disk.writeBlock(i+sb.fatStartBlock, blockData);
        }
    }

    public String getCurrentDirectory() {
        return cur.getName();
    } 
    
    public void createDirectory(String name, String currentUser, String userName){
        if(!checkValidName(name, false)){
            throw new IllegalArgumentException("Invalid directory name: " + name);
        }
        if(dirExists(name)){
            throw new RuntimeException("Directory with name : "+name+" already exists.");
        }

        if(!hasPermission(cur, currentUser, 'w', true)){
            throw new RuntimeException("Permission denied. Owner access required");
        }

        int startBlock = fatTable.getFreeBlock();
        fatTable.setEOF(startBlock);
        byte[] serializedFatTable = fatTable.serialize();
        disk.writeBlock(sb.fatStartBlock, serializedFatTable);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String currentDateTime = LocalDateTime.now().format(formatter);

        Directory newDirectory = new Directory(name, startBlock, cur.getStartBlock(), userName);
        newDirectory.setCreation(currentDateTime);
        newDirectory.setSubFiles(0);
        newDirectory.setSubFolders(0);
        byte[] serializedNewDir = newDirectory.serialize();
        disk.writeBlock(startBlock, serializedNewDir);

        cur.getChildrenBlocks().add(startBlock);
        cur.setSubFolders(cur.getSubFolders()+1);
        byte[] serializedCurDir = cur.serialize();
        disk.writeBlock(cur.getStartBlock(), serializedCurDir);
    }

    public void changeDirectory(String dirName){
        Directory curDirectory = (Directory)resolvePath(dirName);
        cur = curDirectory;
        save();
    }

    public void listDirectory(String userName){
        System.out.println("Listing files and directories of : "+cur.getName());

        if(!hasPermission(cur, userName, 'x', true)){
            throw new RuntimeException("Permission denied. Owner access required");
        }
        
        for(int block:cur.getChildrenBlocks()){
            byte[] dirData = disk.readBlock(block);
            if(ByteBuffer.wrap(dirData).getInt() == 0){
                Directory dir = Directory.deserialize(dirData);
                System.out.println("Dir : "+dir.getName());
            }
            else{
                MyFile file = MyFile.deserialize(dirData);
                System.out.println("File : "+file.getName());
            }
        }
    }

    public void deleteDirectory(String name, String currentUser){
        Directory current = cur;
        Directory dir = (Directory)resolvePath(name);
        cur = current;
        if(dir.isRoot()){
            throw new RuntimeException("Cannot delete root directory.");
        }
        if(dir.getChildrenBlocks().size()>0){
            throw new RuntimeException("Cannot delete Directory : "+dir.getName()+" because it is not empty");
        }

        Directory parent = (Directory)getParentDirectory(dir, true);

        if(!hasPermission(parent, currentUser, 'w', true)){
            throw new RuntimeException("Permission denied. Owner access required");
        }
        
        parent.getChildrenBlocks().remove((Integer)dir.getStartBlock());
        parent.setSubFolders(parent.getSubFolders()-1);
        disk.writeBlock(parent.getStartBlock(), parent.serialize());
        
        fatTable.freeBlocks(dir.getStartBlock());
        disk.writeBlock(sb.fatStartBlock,  fatTable.serialize());

        if(cur.getStartBlock() == dir.getStartBlock() || cur.getStartBlock() == parent.getStartBlock()){
            cur = parent;
        }
    }

    public void renameDirectory(String oldName, String newName, String currentUser){
        if(!dirExists(oldName)){
            throw new RuntimeException("Directory with name "+oldName+" does not exists");
        }
        if(dirExists(newName)){
            throw new RuntimeException("Directory with name : "+newName+" alreadye exists.");
        }
        if(!hasPermission(cur, currentUser, 'w', true)){
            throw new RuntimeException("Permission denied. Owner access required");
        }
        
        Directory dir = getDirectory(oldName);
        dir.setName(newName);
        disk.writeBlock(dir.getStartBlock(), dir.serialize());
    }

    public void displayDirectoryInfo(){
        System.out.println("Created : "+cur.getCreation());
        System.out.println("Contains : "+cur.getSubFiles()+" files, "+cur.getSubFolders()+" folders");
        System.out.println("Owner : "+cur.getOwner());
    }

    public void createFile(String name, String currentUser){
        if(!checkValidName(name, true)){
            throw new IllegalArgumentException("Invalid file name: " + name);
        }
        if(fileExists(name)){
            throw new RuntimeException("File with name : "+name+" already exists.");
        }
        if(!hasPermission(cur, currentUser, 'w', true)){
            throw new RuntimeException("Permission denied. Owner access required");
        }
        int startBlock = fatTable.getFreeBlock();

        fatTable.setEOF(startBlock);
        byte[] serializedFatTable = fatTable.serialize();
        disk.writeBlock(sb.fatStartBlock, serializedFatTable);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String currentDateTime = LocalDateTime.now().format(formatter);

        MyFile newFile = new MyFile(name, startBlock, cur.getStartBlock(), 0, currentDateTime, currentDateTime, currentDateTime,currentUser);
        byte[] serializedNewFile = newFile.serialize();
        disk.writeBlock(startBlock, serializedNewFile);

        cur.getChildrenBlocks().add(startBlock);
        cur.setSubFiles(cur.getSubFiles()+1);
        byte[] serializedCurDir = cur.serialize();
        disk.writeBlock(cur.getStartBlock(), serializedCurDir);
    }

    public void writeFile(String name, byte[] data, String currentUser){
        if(data == null || data.length == 0){
            throw new IllegalArgumentException("Data cannot be null or empty.");
        }
        MyFile file = (MyFile)resolvePath(name);
        if(!hasPermission(file, currentUser, 'w', false)){
            throw new RuntimeException("Permission denied. Owner access required");
        }
        int startBlock = file.getStartBlock();
        fatTable.freeBlocks(fatTable.getNextBlock(startBlock));
        int curBlock = fatTable.getFreeBlock();
        fatTable.setNextBlock(startBlock, curBlock);
        int dataSize = data.length;
        int bytesWritten = 0;
       
        while(bytesWritten < dataSize){
            byte[] blockData = new byte[sb.blockSize];
            int blockSize = Math.min(sb.blockSize, dataSize - bytesWritten);
            System.arraycopy(data, bytesWritten, blockData, 0, blockSize);
            disk.writeBlock(curBlock, blockData);
            bytesWritten += blockSize;
            if(bytesWritten < dataSize){
                int nextBlock = fatTable.getFreeBlock();
                fatTable.setNextBlock(curBlock, nextBlock);
                curBlock = nextBlock;
            }
            else{
                fatTable.setEOF(curBlock);
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String currentDateTime = LocalDateTime.now().format(formatter);

        file.setSize(dataSize);
        file.setAccessed(currentDateTime);
        file.setModified(currentDateTime);
        byte[] serializedFile = file.serialize();
        disk.writeBlock(startBlock, serializedFile);

        byte[] serializedFatTable = fatTable.serialize();
        disk.writeBlock(sb.fatStartBlock, serializedFatTable);
    }

    public byte[] readFile(String name, String currentUser){
        MyFile file = (MyFile)resolvePath(name);

        if(!hasPermission(file, currentUser, 'r', false)){
            throw new RuntimeException("Permission denied. Owner access required");
        }

        int startBlock = file.getStartBlock();
        byte[] fileData = new byte[file.getSize()];
        int bytesRead = 0;
        int curBlock = fatTable.getNextBlock(startBlock);
        while(curBlock != -1 && bytesRead < file.getSize()){
            byte[] blockData = disk.readBlock(curBlock);
            int blockSize = Math.min(sb.blockSize, file.getSize() - bytesRead);
            System.arraycopy(blockData, 0, fileData, bytesRead, blockSize);
            bytesRead += blockSize;
            curBlock = fatTable.getNextBlock(curBlock);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String currentDateTime = LocalDateTime.now().format(formatter);
        
        file.setAccessed(currentDateTime);
        disk.writeBlock(startBlock, file.serialize());

        return fileData;
    }

    public void appendFile(String name, byte[] data, String currentUser){
        if(!fileExists(name)){
            throw new RuntimeException("File with name : "+name+" does not exist.");
        }
        MyFile file = getFile(name);
        
        if(!hasPermission(file, currentUser, 'w', false)){
            throw new RuntimeException("Permission denied. Owner access required");
        }

        int startBlock = file.getStartBlock();
        int curBlock = startBlock;

        while(true){
            int nextBlock = fatTable.getNextBlock(curBlock);
            if(nextBlock == -1) break;
            curBlock = nextBlock;
        }

        int bytesWritten = 0;
        int dataSize = data.length;
        int lastBlockSize = file.getSize() % sb.blockSize;

        if(lastBlockSize > 0){
            byte[] lastBlockData = disk.readBlock(curBlock);
            int bytesToWrite = Math.min(sb.blockSize - lastBlockSize, dataSize);
            System.arraycopy(data, 0, lastBlockData, lastBlockSize, bytesToWrite);
            disk.writeBlock(curBlock, lastBlockData);
            bytesWritten += bytesToWrite;
        }
        while(bytesWritten<dataSize){
            int nextBlock = fatTable.getFreeBlock();
            fatTable.setNextBlock(curBlock, nextBlock);
            curBlock = nextBlock;
            int bytesToWrite = Math.min(sb.blockSize, dataSize - bytesWritten);
            byte[] blockData = new byte[sb.blockSize];
            System.arraycopy(data, bytesWritten, blockData, 0, bytesToWrite);
            disk.writeBlock(curBlock, blockData);
            bytesWritten += bytesToWrite;
        }

        fatTable.setEOF(curBlock);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String currentDateTime = LocalDateTime.now().format(formatter);

        file.setSize(file.getSize()+dataSize);
        file.setAccessed(currentDateTime);
        file.setModified(currentDateTime);
        disk.writeBlock(startBlock, file.serialize());
        disk.writeBlock(sb.fatStartBlock, fatTable.serialize());
    }

    public void deleteFile(String name, String userName){
        if(!fileExists(name)){
            throw new RuntimeException("File with name : "+name+" does not exist.");
        }
        MyFile file = getFile(name);
        Directory parentDirectory = getParentDirectory(file, false);

        if(!hasPermission(parentDirectory, userName, 'w', true)){
            throw new RuntimeException("Permission denied. Owner access required.");
        }
        
        int startBlock = file.getStartBlock();
        int curBlock = startBlock;
        fatTable.freeBlocks(curBlock);
        
        parentDirectory.getChildrenBlocks().remove((Integer)startBlock);
        parentDirectory.setSubFiles(parentDirectory.getSubFiles()-1);
        disk.writeBlock(parentDirectory.getStartBlock(), parentDirectory.serialize());

        disk.writeBlock(sb.fatStartBlock, fatTable.serialize());

        if(cur.getStartBlock() == parentDirectory.getStartBlock()){
            cur = parentDirectory;
        }
    }


    public void renameFile(String oldname, String newName, String userName){
        if(!fileExists(oldname)){
            throw new RuntimeException("File with oldname : "+oldname+" does not exist.");
        }
        if(fileExists(newName)){
            throw new RuntimeException("File with name : "+newName+" already exists.");
        }
        MyFile file = getFile(oldname);

        if(!hasPermission(cur, userName, 'w', true)){
            throw new RuntimeException("Permission denied. Owner access required.");
        }

        file.setName(newName);
        disk.writeBlock(file.getStartBlock(), file.serialize());
    }

    public void moveFile(String oldPath, String newPath, String userName){
        Directory current = cur;
        MyFile file = (MyFile)resolvePath(oldPath);
        String newFileName = getFileName(newPath);
        String newDirPath = getParentPath(newPath);

        Directory targetDir = newDirPath.equals(cur.getName()) ? cur:(Directory)resolvePath(newDirPath);

        if(fileExists(newFileName)){
            cur = current;
            throw new RuntimeException("File with name : "+newFileName+" already exists in target directory");
        }

        Directory oldParent = (Directory)getParentDirectory(file, false);

        if(!hasPermission(oldParent, userName, 'w', true) || !hasPermission(targetDir, userName, 'w', true)){
            throw new RuntimeException("Permission denied.");
        }

        if(oldParent.getName().equals(targetDir.getName())){
            renameFile(file.getName(), newFileName, userName);
            return;
        }
        oldParent.getChildrenBlocks().remove((Integer)file.getStartBlock());
        oldParent.setSubFiles(oldParent.getSubFiles()-1);
        disk.writeBlock(oldParent.getStartBlock(), oldParent.serialize());

        file.setName(newFileName);
        file.setParentBlock(targetDir.getStartBlock());
        disk.writeBlock(file.getStartBlock(), file.serialize());

        targetDir.getChildrenBlocks().add(file.getStartBlock());
        targetDir.setSubFiles(targetDir.getSubFiles()+1);
        disk.writeBlock(targetDir.getStartBlock(), targetDir.serialize());
    }

    public void copyFile(String oldPath, String newPath, String currentUser){
        MyFile file = (MyFile)resolvePath(oldPath);
        
        int curBlock = fatTable.getNextBlock(file.getStartBlock());
        int fileSize = file.getSize();
        byte[] data = new byte[fileSize];
        int bytesWritten = 0;
        while (bytesWritten < fileSize && curBlock != -1) {
            byte[] blockData = disk.readBlock(curBlock);
            int len = Math.min(sb.blockSize, fileSize - bytesWritten);
            System.arraycopy(blockData, 0, data, bytesWritten, len);
            bytesWritten += len;
            curBlock = fatTable.getNextBlock(curBlock);
        }

        String newFileName = getFileName(newPath);
        String newDirPath = getParentPath(newPath);

        Directory targetDir = newDirPath.equals(cur.getName()) ? cur:(Directory)resolvePath(newDirPath);

        Directory current = cur;
        if(fileExists(newFileName)){
            cur = current;
            throw new RuntimeException("File with name : "+newFileName+" already exists in target directory");
        }
        
        cur = targetDir;
        createFile(newFileName, currentUser);
        if(data.length != 0 && data != null)
        writeFile(newFileName, data, currentUser);
        cur = current;
    }

    public void displayFileInfo(String name){
        if(!fileExists(name)){
            throw new RuntimeException("File with name : "+name+" does not exist.");
        }
        MyFile file = getFile(name);
        System.out.println("File Size : "+file.getSize()+" bytes");
        System.out.println("Created : "+file.getCreation());
        System.out.println("Modified : "+file.getModified());
        System.out.println("Accessed : "+file.getAccessed());
        System.out.println("Owner : "+file.getOwner());
    }

    public void recursiveDelete(String dirName, String currentUser){
        Directory current = cur;
        Directory dir = (Directory)resolvePath(dirName);
        
        List<Integer> childrenBlocks = new ArrayList<>(dir.getChildrenBlocks());
        for(int block:childrenBlocks){
            byte[] blockData = disk.readBlock(block);
            if(ByteBuffer.wrap(blockData).getInt() == 1){
                MyFile file = MyFile.deserialize(blockData);
                deleteFile(file.getName(), currentUser);
            }
            else{
                Directory subDir = Directory.deserialize(blockData);
                recursiveDelete(subDir.getName(), currentUser);
            }
        }
        cur = current;
        deleteDirectory(dirName, currentUser);
    }

    // Helper functions
    private Object resolvePath(String path){
        if (path == null || path.isEmpty()) return cur;
        Directory currentDir = cur;
        if(path.startsWith("/")){
            currentDir = Directory.deserialize(disk.readBlock(sb.rootDirStartBlock));
        }
        String[] parts = path.split("/");
        int ind = 0;

        for (String part:parts) {
            if (part.isEmpty()) {
                ind++;
                continue;
            }
            if(part.equals("..")){
                if(currentDir.getParentBlock() == -1){ind++;  continue;}
                currentDir = (Directory)getParentDirectory(currentDir, true);
            }
            else{
                Directory nextDir = getDirectory(part, currentDir);
                if (nextDir == null && ind != parts.length-1) {
                    throw new RuntimeException("Directory not found: " + part);
                }
                else if(nextDir == null && ind == parts.length-1){
                    MyFile file = getFile(part, currentDir);
                    if(file == null){
                        throw new RuntimeException("Cannot find specified file path : "+path);
                    }
                    return file;
                }
                currentDir = nextDir;
            }
            ind++;
        }
        return cur = currentDir;
    }

    private Directory getDirectory(String name){
        for(int block:cur.getChildrenBlocks()){
            byte[] data = disk.readBlock(block);
            if(ByteBuffer.wrap(data).getInt() == 0){
                Directory dir = Directory.deserialize(data);
                String dirName = dir.getName();
                if(dirName.equals(name)){
                    return dir;
                }
            }
        }
        return null;
    }

    private Directory getDirectory(String name, Directory current){
        for(int block:current.getChildrenBlocks()){
            byte[] data = disk.readBlock(block);
            if(ByteBuffer.wrap(data).getInt() == 0){
                Directory dir = Directory.deserialize(data);
                String dirName = dir.getName();
                if(dirName.equals(name)){
                    return dir;
                }
            }
        }
        return null;
    }

    private Directory getParentDirectory(Object cur, boolean isDirectory){
        if(isDirectory){
            Directory current = (Directory)cur;
            int parentBlock = current.getParentBlock();
            return Directory.deserialize(disk.readBlock(parentBlock));
        }
        else{
            MyFile file = (MyFile)cur;
            int parentBlock = file.getParentBlock();
            return Directory.deserialize(disk.readBlock(parentBlock));
        }
    }

    private boolean dirExists(String name){
        return getDirectory(name) != null;
    }

    private MyFile getFile(String name){
        for(int block:cur.getChildrenBlocks()){
            byte[] data = disk.readBlock(block);
            if(ByteBuffer.wrap(data).getInt() == 1){
                MyFile file = MyFile.deserialize(data);
                String fileName = file.getName();
                if(fileName.equals(name)){
                    return file;
                }
            }
        }
        return null;
    }

    private MyFile getFile(String name, Directory cur){
        for(int block:cur.getChildrenBlocks()){
            byte[] data = disk.readBlock(block);
            if(ByteBuffer.wrap(data).getInt() == 1){
                MyFile file = MyFile.deserialize(data);
                String fileName = file.getName();
                if(fileName.equals(name)){
                    return file;
                }
            }
        }
        return null;
    }

    private boolean fileExists(String name){
        return getFile(name) != null;
    }

    private boolean checkValidName(String name, boolean isFile){
        if(name.startsWith(" ")){
            throw new IllegalArgumentException("Invalid name : "+name);
        }
        if(name == null || name.isEmpty() || name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("Invalid directory name: " + name);
        }
        if(name.contains(".") && !isFile) {
            throw new IllegalArgumentException("Directory name cannot contain '.': " + name);
        }
        if(name.contains("/") && isFile){
            throw new IllegalArgumentException("File name cannot contain '/': " + name);
        }
        if(isFile && name.endsWith(".")){
            throw new IllegalArgumentException("File name cannot end with '.': " + name);
        }
        if(isFile && name.contains(" ")){
            throw new IllegalArgumentException("File name cannot contain blank spaces");
        }
        return true;
    }

    private String getFileName(String path){
        String[] parts = path.split("/");
        if(parts.length == 0){
            throw new RuntimeException("Invalid path");
        }
        return parts[parts.length-1];
    }

    private String getParentPath(String path){
        int lastSlash = path.lastIndexOf('/');
        if(lastSlash == 0)  return "/";
        if(lastSlash < 0)  return cur.getName();
        return path.substring(0, lastSlash);
    }

    private boolean hasPermission(Object obj, String userName, char mode, boolean isDir){
        if(userName.equals("root")){
            return true;
        }
        String perms;
        String owner;

        if(isDir){
            Directory dir = (Directory) obj;
            perms = dir.getPermissions();
            owner = dir.getOwner();
        }
        else{
            MyFile file = (MyFile) obj;
            perms = file.getPermissions();
            owner = file.getOwner();
        }
        int index = owner.equals(userName)?0:3;
        for(int i=index;i<index+3;i++){
            if(perms.charAt(i) == mode)
                return true;
        }
        return false;
    }
}
