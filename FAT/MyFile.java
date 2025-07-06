package FAT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MyFile {
    public String name;
    public int startBlock;
    public int parentBlock;
    public int size;

    public MyFile(String name, int startBlock, int parentBlock, int size){
        this.name = name;
        this.startBlock = startBlock;
        this.parentBlock = parentBlock;
        this.size = size;
    }

    public byte[] serialize(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try{
            dos.writeInt(1);
            dos.writeInt(parentBlock);
            dos.writeUTF(name);
            dos.writeInt(startBlock);
            dos.writeInt(size);
            return baos.toByteArray();
        }
        catch(IOException e){
            throw new RuntimeException("Error during serialization : ", e);
        }
    }

    public static MyFile deserialize(byte[] data){
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        try{
            dis.readInt();
            int parentBlock = dis.readInt();
            String name = dis.readUTF();
            int startBlock = dis.readInt();
            int size = dis.readInt();
            return new MyFile(name, startBlock, parentBlock, size);
        }
        catch(IOException e){
            throw new RuntimeException("Error during deserialization : ", e);
        }
    }
}
