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

    public void setCreation(String creation) {
        this.creation = creation;
    }

    public Directory(String name, int startBlock, int parentBlock) {
        this.name = name;
        this.startBlock = startBlock;
        this.parentBlock = parentBlock;
        if (parentBlock == -1) {
            this.isRoot = true;
        } else {
            this.isRoot = false;
        }
    }

    public byte[] serialize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeInt(0);
            dos.writeInt(parentBlock);
            dos.writeUTF(name);
            dos.writeInt(startBlock);

            dos.writeBoolean(isRoot);

            dos.writeInt(childrenBlocks.size());
            for (int block : childrenBlocks) {
                dos.writeInt(block);
            }
            dos.writeUTF(creation);
            dos.writeInt(subFiles);
            dos.writeInt(subFolders);

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

            Directory directory = new Directory(name, startBlock, parentBlock);
            directory.isRoot = dis.readBoolean();

            int childrenCount = dis.readInt();
            for (int i = 0; i < childrenCount; i++) {
                directory.childrenBlocks.add(dis.readInt());
            }
            directory.creation = dis.readUTF();
            directory.subFiles = dis.readInt();
            directory.subFolders = dis.readInt();

            return directory;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
