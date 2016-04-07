package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.log;

public class VBIndex implements BaseIndex {

	@Override
	public PostingList readPosting(FileChannel fc) throws IOException {
		/*
		 * TODO: Your code here
		 */
        ByteBuffer buf = ByteBuffer.allocate(INTSIZE * 2); // term id and postings length
        int ret = fc.read(buf);
        if (ret == 0 || ret == -1 ){
            return null;
        }
        buf.flip();
        int termId = buf.getInt();
        int size = buf.getInt();
        buf = ByteBuffer.allocate(size);
        fc.read(buf);
        buf.flip();
        byte[] rv = new byte[buf.limit()];
        buf.get(rv);
        List<Integer> postings = decode(rv);
        gapDecode(postings);

        return new PostingList(termId, postings);
	}

	@Override
	public void writePosting(FileChannel fc, PostingList p) throws IOException{
		/*
		 * TODO: Your code here
		 */
        gapEncode(p.getList());
        byte[] vb = encode(p.getList());
        ByteBuffer buf = ByteBuffer.allocate((INTSIZE*2) + vb.length);
        buf.putInt(p.getTermId());
        buf.putInt(vb.length);
        buf.put(vb);
        buf.flip();
        fc.write(buf);
	}

    private void gapEncode(List<Integer> docIds) {
        int curr = 0, prev;
        for (int i = 0; i < docIds.size(); i++) {
            prev = curr;
            curr = docIds.get(i);
            docIds.set(i, curr - prev);
        }
    }

    public void gapDecode(List<Integer> encodedIds) {
        for (int i = 1; i < encodedIds.size(); i++) {
            encodedIds.set(i, encodedIds.get(i - 1) + encodedIds.get(i));
        }
    }


    private static byte[] encodeNumber(int n) {
        if (n == 0) {
            return new byte[]{0};
        }
        int i = (int) (log(n) / log(128)) + 1;
        byte[] rv = new byte[i];
        int j = i - 1;
        do {
            rv[j--] = (byte) (n % 128);
            n /= 128;
        } while (j >= 0);
        rv[i - 1] += 128;
        return rv;
    }

    public static byte[] encode(List<Integer> numbers) {
        ByteBuffer buf = ByteBuffer.allocate(numbers.size() * (Integer.SIZE / Byte.SIZE));
        for (Integer number : numbers) {
            buf.put(encodeNumber(number));
        }
        buf.flip();
        byte[] rv = new byte[buf.limit()];
        buf.get(rv);
        return rv;
    }

    public static List<Integer> decode(byte[] byteStream) {
        List<Integer> numbers = new ArrayList<Integer>();
        int n = 0;
        for (byte b : byteStream) {
            if ((b & 0xff) < 128) {
                n = 128 * n + b;
            } else {
                int num = (128 * n + ((b - 128) & 0xff));
                numbers.add(num);
                n = 0;
            }
        }
        return numbers;
    }
}
