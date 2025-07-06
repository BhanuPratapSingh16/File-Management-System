import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Superblock {
    int magicNumber = 0x23D4F8;
    int blockSize;
    int numBlocks;
    int fatStartBlock;
    int fatSize;
    int fatBlocksCount;
    int fatBlockSize;
    int rootDirStartBlock;

    public Superblock(){}

    public Superblock(int blockSize, int numBlocks) {
        this.blockSize = blockSize;
        this.numBlocks = numBlocks;
        this.fatStartBlock = 1;
        this.fatSize = (numBlocks * 4);
        this.fatBlocksCount = (int) Math.ceil((double) fatSize / blockSize);
        this.fatBlockSize = this.fatSize / this.fatBlocksCount;
        this.rootDirStartBlock = fatStartBlock + fatBlocksCount;
    }

    public byte[] serialize(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try{
            dos.writeInt(magicNumber);
            dos.writeInt(blockSize);
            dos.writeInt(numBlocks);

            dos.writeInt(fatStartBlock);
            dos.writeInt(fatSize);
            dos.writeInt(fatBlockSize);
            dos.writeInt(fatBlocksCount);
            dos.writeInt(rootDirStartBlock);
            return baos.toByteArray();
        }
        catch(IOException e){
            throw new RuntimeException("Error during serialization ", e);
        }
    }

    public static Superblock deserialize(byte[] buffer){
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        DataInputStream dis = new DataInputStream(bais);

        try{
            Superblock sb = new Superblock();
            sb.magicNumber = dis.readInt();
            sb.blockSize  = dis.readInt();
            sb.numBlocks = dis.readInt();

            sb.fatStartBlock = dis.readInt();
            sb.fatSize = dis.readInt();
            sb.fatBlockSize = dis.readInt();
            sb.fatBlocksCount = dis.readInt();
            sb.rootDirStartBlock = dis.readInt();
            
            return sb;
        }
        catch(IOException e){
            throw new RuntimeException("Error during deserialization", e);
        }
    }
}
