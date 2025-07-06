import java.nio.ByteBuffer;

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
        this.cur = new Directory("/", sb.rootDirStartBlock, -1);  // Default directory is root
    }

    public static FileSystem mount(Disk disk, int blockSize, int numBlocks) {
        byte[] readSuperblock = disk.readBlock(0);
        int magicNumber = ByteBuffer.wrap(readSuperblock).getInt();

        if(magicNumber != 0x23D4F8){
            Superblock sb = new Superblock(blockSize, numBlocks);
            FatTable fat = new FatTable(numBlocks, sb.rootDirStartBlock+1);
            FileSystem fs = new FileSystem(disk, sb, fat);

            Directory root = new Directory("/", sb.rootDirStartBlock, -1);
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
        return cur.name;
    } 
    
    public void createDirectory(String name){
        if(!checkValidName(name, false)){
            throw new IllegalArgumentException("Invalid directory name: " + name);
        }
        if(dirExists(name)){
            throw new RuntimeException("Directory with name : "+name+" already exists.");
        }
        int startBlock = fatTable.getFreeBlock();
        fatTable.setEOF(startBlock);
        byte[] serializedFatTable = fatTable.serialize();
        disk.writeBlock(sb.fatStartBlock, serializedFatTable);

        Directory newDirectory = new Directory(name, startBlock, cur.startBlock);
        byte[] serializedNewDir = newDirectory.serialize();
        disk.writeBlock(startBlock, serializedNewDir);

        cur.childrenBlocks.add(startBlock);
        byte[] serializedCurDir = cur.serialize();
        disk.writeBlock(cur.startBlock, serializedCurDir);
    }

    public void changeDirectory(String dirName){
        Directory curDirectory = (Directory)resolvePath(dirName);
        cur = curDirectory;
        save();
    }

    public void listDirectory(){
        System.out.println("Listing directories of : "+cur.name);
        for(int block:cur.childrenBlocks){
            byte[] dirData = disk.readBlock(block);
            if(ByteBuffer.wrap(dirData).getInt() == 0){
                Directory dir = Directory.deserialize(dirData);
                System.out.println("Dir : "+dir.name+" Start block : "+dir.startBlock+" PArent dir : "+dir.parentBlock);
            }
            else{
                MyFile file = MyFile.deserialize(dirData);
                System.out.println("File : "+file.name+" Start block : "+file.startBlock+" Parent dir : "+file.parentBlock+" Size : "+file.size);
            }
        }
    }

    public void deleteDirectory(String name){
        Directory current = cur;
        Directory dir = (Directory)resolvePath(name);
        cur = current;
        if(dir.isRoot){
            throw new RuntimeException("Cannot delete root directory.");
        }
        if(dir.childrenBlocks.size()>0){
            throw new RuntimeException("Cannot delete Directory : "+dir.name+" because it is not empty");
        }
        
        disk.writeBlock(dir.startBlock, new byte[sb.blockSize]);
        Directory parent = (Directory)getParentDirectory(dir, true);
        parent.childrenBlocks.remove((Integer)dir.startBlock);
        disk.writeBlock(parent.startBlock, parent.serialize());
        
        fatTable.freeBlocks(dir.startBlock);
        disk.writeBlock(sb.fatStartBlock,  fatTable.serialize());

        if(cur.startBlock == dir.startBlock || cur.startBlock == parent.startBlock){
            cur = parent;
        }
    }

    public void renameDirectory(String oldName, String newName){
        if(!dirExists(oldName)){
            throw new RuntimeException("Directory with name "+oldName+" does not exists");
        }
        if(dirExists(newName)){
            throw new RuntimeException("Directory with name : "+newName+" alreadye exists.");
        }
        Directory dir = getDirectory(oldName);
        dir.name = newName;
        disk.writeBlock(dir.startBlock, dir.serialize());
    }


    public void createFile(String name){
        if(!checkValidName(name, true)){
            throw new IllegalArgumentException("Invalid file name: " + name);
        }
        if(fileExists(name)){
            throw new RuntimeException("File with name : "+name+" already exists.");
        }
        int startBlock = fatTable.getFreeBlock();

        fatTable.setEOF(startBlock);
        byte[] serializedFatTable = fatTable.serialize();
        disk.writeBlock(sb.fatStartBlock, serializedFatTable);

        MyFile newFile = new MyFile(name, startBlock, cur.startBlock, 0);
        byte[] serializedNewFile = newFile.serialize();
        disk.writeBlock(startBlock, serializedNewFile);

        cur.childrenBlocks.add(startBlock);
        byte[] serializedCurDir = cur.serialize();
        disk.writeBlock(cur.startBlock, serializedCurDir);
    }

    public void writeFile(String name, byte[] data){
        if(data == null || data.length == 0){
            throw new IllegalArgumentException("Data cannot be null or empty.");
        }
        MyFile file = (MyFile)resolvePath(name);
        int startBlock = file.startBlock;
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

        file.size = dataSize;
        byte[] serializedFile = file.serialize();
        disk.writeBlock(startBlock, serializedFile);

        byte[] serializedFatTable = fatTable.serialize();
        disk.writeBlock(sb.fatStartBlock, serializedFatTable);
    }

    public byte[] readFile(String name){
        MyFile file = (MyFile)resolvePath(name);
        int startBlock = file.startBlock;
        byte[] fileData = new byte[file.size];
        int bytesRead = 0;
        int curBlock = fatTable.getNextBlock(startBlock);
        while(curBlock != -1 && bytesRead < file.size){
            byte[] blockData = disk.readBlock(curBlock);
            int blockSize = Math.min(sb.blockSize, file.size - bytesRead);
            System.arraycopy(blockData, 0, fileData, bytesRead, blockSize);
            bytesRead += blockSize;
            curBlock = fatTable.getNextBlock(curBlock);
        }
        return fileData;
    }

    public void appendFile(String name, byte[] data){
        if(!fileExists(name)){
            throw new RuntimeException("File with name : "+name+" does not exist.");
        }
        MyFile file = getFile(name);
        int startBlock = file.startBlock;
        int curBlock = startBlock;

        while(true){
            int nextBlock = fatTable.getNextBlock(curBlock);
            if(nextBlock == -1) break;
            curBlock = nextBlock;
        }

        int bytesWritten = 0;
        int dataSize = data.length;
        int lastBlockSize = file.size % sb.blockSize;

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
        file.size += dataSize;
        disk.writeBlock(startBlock, file.serialize());
        disk.writeBlock(sb.fatStartBlock, fatTable.serialize());
    }

    public void deleteFile(String name){
        if(!fileExists(name)){
            throw new RuntimeException("File with name : "+name+" does not exist.");
        }
        MyFile file = getFile(name);
        int startBlock = file.startBlock;

        int curBlock = startBlock;
        fatTable.freeBlocks(curBlock);

        cur.childrenBlocks.remove((Integer)startBlock);
        disk.writeBlock(cur.startBlock, cur.serialize());

        disk.writeBlock(startBlock, new byte[sb.blockSize]);

        disk.writeBlock(sb.fatStartBlock, fatTable.serialize());
    }


    public void renameFile(String oldname, String newName){
        if(!fileExists(oldname)){
            throw new RuntimeException("File with oldname : "+oldname+" does not exist.");
        }
        if(fileExists(newName)){
            throw new RuntimeException("File with name : "+newName+" already exists.");
        }
        MyFile file = getFile(oldname);
        file.name = newName;
        disk.writeBlock(file.startBlock, file.serialize());
    }

    public void moveFile(String oldPath, String newPath){
        Directory current = cur;
        MyFile file = (MyFile)resolvePath(oldPath);
        String newFileName = getFileName(newPath);
        String newDirPath = getParentPath(newPath);

        Directory targetDir = newDirPath.equals(cur.name) ? cur:(Directory)resolvePath(newDirPath);

        if(fileExists(newFileName)){
            cur = current;
            throw new RuntimeException("File with name : "+newFileName+" already exists in target directory");
        }

        Directory oldParent = (Directory)getParentDirectory(file, false);
        if(oldParent.name.equals(targetDir.name)){
            renameFile(file.name, newFileName);
            return;
        }
        oldParent.childrenBlocks.remove((Integer)file.startBlock);
        disk.writeBlock(oldParent.startBlock, oldParent.serialize());

        file.name = newFileName;
        file.parentBlock = targetDir.startBlock;
        disk.writeBlock(file.startBlock, file.serialize());

        targetDir.childrenBlocks.add(file.startBlock);
        disk.writeBlock(targetDir.startBlock, targetDir.serialize());
    }

    public void copyFile(String oldPath, String newPath){
        MyFile file = (MyFile)resolvePath(oldPath);
        
        int curBlock = fatTable.getNextBlock(file.startBlock);
        int fileSize = file.size;
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

        Directory targetDir = newDirPath.equals(cur.name) ? cur:(Directory)resolvePath(newDirPath);

        Directory current = cur;
        if(fileExists(newFileName)){
            cur = current;
            throw new RuntimeException("File with name : "+newFileName+" already exists in target directory");
        }
        
        cur = targetDir;
        createFile(newFileName);
        writeFile(newFileName, data);
        cur = current;
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
                if(currentDir.parentBlock == -1){ind++;  continue;}
                currentDir = (Directory)getParentDirectory(currentDir, false);
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
        for(int block:cur.childrenBlocks){
            byte[] data = disk.readBlock(block);
            Directory dir = Directory.deserialize(data);
            String dirName = dir.name;
            if(dirName.equals(name)){
                return dir;
            }
        }
        return null;
    }

    private Directory getDirectory(String name, Directory current){
        for(int block:current.childrenBlocks){
            byte[] data = disk.readBlock(block);
            if(ByteBuffer.wrap(data).getInt() == 0){
                Directory dir = Directory.deserialize(data);
                String dirName = dir.name;
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
            int parentBlock = current.parentBlock;
            return Directory.deserialize(disk.readBlock(parentBlock));
        }
        else{
            MyFile file = (MyFile)cur;
            int parentBlock = file.parentBlock;
            return Directory.deserialize(disk.readBlock(parentBlock));
        }
    }

    private boolean dirExists(String name){
        return getDirectory(name) != null;
    }

    private MyFile getFile(String name){
        for(int block:cur.childrenBlocks){
            byte[] data = disk.readBlock(block);
            if(ByteBuffer.wrap(data).getInt() == 1){
                MyFile file = MyFile.deserialize(data);
                String fileName = file.name;
                if(fileName.equals(name)){
                    return file;
                }
            }
        }
        return null;
    }

    private MyFile getFile(String name, Directory cur){
        for(int block:cur.childrenBlocks){
            byte[] data = disk.readBlock(block);
            MyFile file = MyFile.deserialize(data);
            String fileName = file.name;
            if(fileName.equals(name)){
                return file;
            }
        }
        return null;
    }

    private boolean fileExists(String name){
        return getFile(name) != null;
    }

    private boolean checkValidName(String name, boolean isFile){
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
        if(lastSlash < 0)  return cur.name;
        return path.substring(0, lastSlash);
    }
}
