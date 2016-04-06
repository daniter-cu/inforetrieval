package cs276.assignments;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BasicIndex implements BaseIndex{

	@Override
	public PostingList readPosting(RandomAccessFile fc) throws IOException{
		/*
		 * TODO: Your code here
		 *       Read and return the postings list from the given file.
		 */
		int termId;
		try {
			termId = fc.readInt();
		} catch (EOFException e) {
			return null;
		}
		PostingList pl = new PostingList(termId);
		int size = fc.readInt();
		for(int i = 0; i < size; i++){
			pl.getList().add(fc.readInt());
		}
		return pl;
	}

	@Override
	public void writePosting(RandomAccessFile fc, PostingList p) throws IOException {
		/*
		 * TODO: Your code here
		 *       Write the given postings list to the given file.
		 */
		fc.writeInt(p.getTermId()); // write the termId
		fc.writeInt(p.getList().size()); // write size of posting list
		for(Integer post: p.getList()){
			fc.writeInt(post);
		}
	}
}
