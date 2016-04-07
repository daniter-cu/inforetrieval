package cs276.assignments;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class BasicIndex implements BaseIndex{

	static final int INTSIZE = (Integer.SIZE/Byte.SIZE);

	@Override
	public PostingList readPosting(FileChannel fc) throws IOException{
		/*
		 * TODO: Your code here
		 *       Read and return the postings list from the given file.
		 */

		ByteBuffer buf = ByteBuffer.allocate(INTSIZE * 2); // term id and postings length
		int ret = fc.read(buf);
		if (ret == 0 || ret == -1 ){
			return null;
		}
		buf.flip();
		int termId = buf.getInt();
		int size = buf.getInt();
		ArrayList<Integer> postings = new ArrayList<Integer>(size);
		buf = ByteBuffer.allocate(INTSIZE* size);
		fc.read(buf);
		buf.flip();
		for(int i = 0; i < size; i++){
			postings.add(buf.getInt());
		}

		return new PostingList(termId, postings);
	}

	@Override
	public void writePosting(FileChannel fc, PostingList p) throws IOException {
		/*
		 * TODO: Your code here
		 *       Write the given postings list to the given file.
		 */
		ByteBuffer buf = ByteBuffer.allocate(INTSIZE * (p.getList().size()+2));
		buf.putInt(p.getTermId());
		buf.putInt(p.getList().size());
		for(Integer post: p.getList()){
			buf.putInt(post);
		}
		buf.flip();
		int ret = fc.write(buf);
		assert(ret == INTSIZE * (p.getList().size()+2));
	}
}
