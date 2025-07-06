package FAT;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Directory {
    public String name;
    public int startBlock;
    public boolean isRoot;
    public int parentBlock;
    public List<Integer> childrenBlocks = new ArrayList<>();

    public Directory(String name, int startBlock, int parentBlock){
        this.name = name;
        this.startBlock = startBlock;
        this.parentBlock = parentBlock;
        if(parentBlock == -1){
            this.isRoot = true;
        }
        else{
            this.isRoot = false;
        }
    }

    public byte[] serialize(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try{
            dos.writeInt(0);
            dos.writeInt(parentBlock);
            dos.writeUTF(name);
            dos.writeInt(startBlock);
            dos.writeBoolean(isRoot);

            dos.writeInt(childrenBlocks.size());
            for(int block : childrenBlocks){
                dos.writeInt(block);
            }

            return baos.toByteArray();
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static Directory deserialize(byte[] data){
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        try{
            dis.readInt();
            int parentBlock = dis.readInt();
            String name = dis.readUTF();
            int startBlock = dis.readInt();

            Directory directory = new Directory(name, startBlock, parentBlock);
            directory.isRoot = dis.readBoolean();

            int childrenCount = dis.readInt();
            for(int i=0;i<childrenCount;i++){
                directory.childrenBlocks.add(dis.readInt());
            }

            return directory;
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
