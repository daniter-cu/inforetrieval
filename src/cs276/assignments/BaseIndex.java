package cs276.assignments;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public interface BaseIndex {
	
	public PostingList readPosting (RandomAccessFile fc) throws IOException;
	
	public void writePosting (RandomAccessFile fc, PostingList p) throws IOException;
}
