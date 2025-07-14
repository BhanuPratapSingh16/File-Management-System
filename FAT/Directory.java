package FAT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Directory {
    private String name;
    private int startBlock;
    private boolean isRoot;
    private int parentBlock;
    private List<Integer> childrenBlocks = new ArrayList<>();
    private String creation = "";
    private int subFiles = 0;
    private int subFolders = 0;
    private final String owner;
    private String permissions;

    public String getName() {
        return name;
    }

    public int getStartBlock() {
        return startBlock;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public int getParentBlock() {
        return parentBlock;
    }

    public List<Integer> getChildrenBlocks() {
        return childrenBlocks;
    }

    public int getSubFiles() {
        return subFiles;
    }

    public int getSubFolders() {
        return subFolders;
    }

    public String getOwner() {
        return owner;
    }

    public String getPermissions() {
        return permissions;
    }

    public String getCreation() {
        return creation;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartBlock(int startBlock) {
        this.startBlock = startBlock;
    }

    public void setIsRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    public void setParentBlock(int parentBlock) {
        this.parentBlock = parentBlock;
    }

    public void setChildrenBlocks(List<Integer> blocks) {
        this.childrenBlocks = blocks;
    }

    public void setSubFiles(int subFiles) {
        this.subFiles = subFiles;
    }

    public void setSubFolders(int subFolders) {
        this.subFolders = subFolders;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public void setCreation(String creation) {
        this.creation = creation;
    }

    public Directory(String name, int startBlock, int parentBlock, String owner) {
        this.name = name;
        this.startBlock = startBlock;
        this.parentBlock = parentBlock;
        if (parentBlock == -1) {
            this.isRoot = true;
        } else {
            this.isRoot = false;
        }
        this.owner = owner;
        this.permissions = "rwxr-x"; // owner - all, others - read and execute
    }

    public byte[] serialize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeInt(0);
            dos.writeInt(parentBlock);
            dos.writeUTF(name);
            dos.writeInt(startBlock);
            dos.writeUTF(owner);

            dos.writeBoolean(isRoot);

            dos.writeInt(childrenBlocks.size());
            for (int block : childrenBlocks) {
                dos.writeInt(block);
            }
            dos.writeUTF(creation);
            dos.writeInt(subFiles);
            dos.writeInt(subFolders);

            dos.writeUTF(permissions);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Directory deserialize(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        try {
            dis.readInt();
            int parentBlock = dis.readInt();
            String name = dis.readUTF();
            int startBlock = dis.readInt();
            String owner = dis.readUTF();

            Directory directory = new Directory(name, startBlock, parentBlock, owner);
            directory.isRoot = dis.readBoolean();

            int childrenCount = dis.readInt();
            for (int i = 0; i < childrenCount; i++) {
                directory.childrenBlocks.add(dis.readInt());
            }
            directory.creation = dis.readUTF();
            directory.subFiles = dis.readInt();
            directory.subFolders = dis.readInt();
            directory.permissions = dis.readUTF();

            return directory;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
