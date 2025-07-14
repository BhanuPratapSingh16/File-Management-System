package FAT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MyFile {
    private String name;
    private int startBlock;
    private int parentBlock;
    private int size;
    private String creation;
    private String modified;
    private String accessed;
    private final String owner;
    private String permissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStartBlock() {
        return startBlock;
    }

    public void setStartBlock(int startBlock) {
        this.startBlock = startBlock;
    }

    public int getParentBlock() {
        return parentBlock;
    }

    public void setParentBlock(int parentBlock) {
        this.parentBlock = parentBlock;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getCreation() {
        return creation;
    }

    public void setCreation(String creation) {
        this.creation = creation;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getAccessed() {
        return accessed;
    }

    public void setAccessed(String accessed) {
        this.accessed = accessed;
    }

    public String getOwner() {
        return owner;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public MyFile(String name, int startBlock, int parentBlock, int size, String creation, String modified, String accessed, String owner) {
        this.name = name;
        this.startBlock = startBlock;
        this.parentBlock = parentBlock;
        this.size = size;
        this.creation = creation;
        this.modified = modified;
        this.accessed = accessed;
        this.owner = owner;
        this.permissions = "rw-r--"; // owner - read and write , others - write
    }

    public byte[] serialize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeInt(1);
            dos.writeInt(parentBlock);
            dos.writeUTF(name);
            dos.writeInt(startBlock);
            dos.writeInt(size);
            dos.writeUTF(creation);
            dos.writeUTF(modified);
            dos.writeUTF(accessed);
            dos.writeUTF(owner);
            dos.writeUTF(permissions);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error during serialization : ", e);
        }
    }

    public static MyFile deserialize(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        try {
            dis.readInt();
            int parentBlock = dis.readInt();
            String name = dis.readUTF();
            int startBlock = dis.readInt();
            int size = dis.readInt();
            String creation = dis.readUTF();
            String modified = dis.readUTF();
            String accessed = dis.readUTF();
            String owner = dis.readUTF();
            MyFile file = new MyFile(name, startBlock, parentBlock, size, creation, modified, accessed, owner);
            file.permissions = dis.readUTF();
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Error during deserialization : ", e);
        }
    }
}
