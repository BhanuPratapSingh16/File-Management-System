import java.io.File;
import java.io.RandomAccessFile;

public class Disk {
    private final RandomAccessFile disk;
    private final int blockSize;
    private final int numBlocks;
    final String filePath = "disk.dat";

    public Disk(int blockSize, int numBlocks){
        try{
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            this.disk = new RandomAccessFile(file, "rw");
            this.blockSize = blockSize;
            this.numBlocks = numBlocks;
            disk.setLength((long) blockSize * numBlocks);
        }
        catch (Exception e) {
            throw new RuntimeException("Error during initialization", e);
        }
    }

    public byte[] readBlock(int blockNumber){
        if(blockNumber < 0 || blockNumber >= numBlocks){
            throw new RuntimeException("Block number out of bounds : "+blockNumber);
        }
        try{
            byte[] buffer = new byte[blockSize];
            disk.seek((long)blockNumber * blockSize);
            disk.readFully(buffer);
            return buffer;
        }
        catch (Exception e) {
            throw new RuntimeException("Error reading block " + blockNumber, e);
        }
    }

    public void writeBlock(int blockNumber, byte[] data){
        if(blockNumber < 0 || blockNumber >= numBlocks){
            throw new RuntimeException("Block number out of bounds : "+blockNumber);
        }

        try{
            disk.seek((long)blockNumber * blockSize);
            disk.write(data);
        }
        catch (Exception e) {
            throw new RuntimeException("Error writing block " + blockNumber, e);
        }
    }

    public void close() {
        try {
            disk.close();
        } catch (Exception e) {
            throw new RuntimeException("Error closing disk", e);
        }
    }
}
