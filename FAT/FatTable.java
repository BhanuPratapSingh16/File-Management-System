package FAT;

import java.nio.ByteBuffer;

public class FatTable {
    final int[] fat; // 0 -> free, >0 -> next block number, -1 -> end of file, -2 -> reserved
    final int numBlocks;
    final int reservedBlocks;

    public FatTable(int numBlocks, int reservedBlocks){
        this.numBlocks = numBlocks;
        this.fat = new int[numBlocks];
        this.reservedBlocks = reservedBlocks;
        for(int i=0;i<reservedBlocks;i++){
            fat[i] = -2;
        }
    }

    public byte[] serialize(){
        ByteBuffer buffer = ByteBuffer.allocate(numBlocks*4);
        for(int val:fat){
            buffer.putInt(val);
        }
        return buffer.array();
    }

    public static FatTable deserialize(byte[] data, int reservedBlocks){
        int numBlocks = data.length / 4;
        FatTable fTable = new FatTable(numBlocks, reservedBlocks);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        for(int i=0;i<numBlocks;i++){
            fTable.fat[i] = buffer.getInt();
        }
        return fTable;
    }

    public int getFreeBlock(){
        for(int i=reservedBlocks;i<numBlocks;i++){
            if(fat[i] == 0){
                fat[i] = -3; // Mark as allocated
                return i;
            }
        }
        throw new RuntimeException("Disk space full");
    }

    public void setNextBlock(int curBlock, int nextBlock){
        if(curBlock < reservedBlocks || curBlock >= numBlocks){
            throw new IndexOutOfBoundsException("Invalid block number : "+curBlock);
        }
        if(nextBlock < reservedBlocks || nextBlock >= numBlocks){
            throw new IndexOutOfBoundsException("Invalid block number : "+nextBlock);
        }
        fat[curBlock] = nextBlock;
    }

    public int getNextBlock(int curBlock){
        if(curBlock < reservedBlocks || curBlock >= numBlocks){
            throw new IndexOutOfBoundsException("Invalid block number : "+curBlock);
        }
        return fat[curBlock];
    }

    public void freeBlocks(int startBlock){
        int curBlock = startBlock;
        while(curBlock != -1){
            int nextBlock = fat[curBlock];
            fat[curBlock] = 0;
            curBlock = nextBlock;
        }
    }

    public void setEOF(int blockNumber){
        if(blockNumber < 0 || blockNumber >= numBlocks){
            throw new RuntimeException("Invalid block number : "+blockNumber);
        }
        fat[blockNumber] = -1;
    }
}
