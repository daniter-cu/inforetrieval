package cs276.assignments;

import cs276.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.*;

public class Index {

	// Term id -> (position in index file, doc frequency) dictionary
	private static Map<Integer, Pair<Long, Integer>> postingDict 
		= new TreeMap<Integer, Pair<Long, Integer>>();
	// Doc name -> doc id dictionary
	private static Map<String, Integer> docDict
		= new TreeMap<String, Integer>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict
		= new TreeMap<String, Integer>();
	// Block queue
	private static LinkedList<File> blockQueue
		= new LinkedList<File>();

	private static Map<Integer, PostingList> postings
		= new HashMap<Integer, PostingList>();

	// Total file counter
	private static int totalFileCount = 0;
	// Document counter
	private static int docIdCounter = 1;
	// Term counter
	private static int wordIdCounter = 0;
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list to the given file 
	 * You should record the file position of this posting list
	 * so that you can read it back during retrieval
	 * 
	 * */
	private static void writePosting(RandomAccessFile fc, PostingList posting)
			throws IOException {
		/*
		 * TODO: Your code here
		 *	 
		 */
        postingDict.put(posting.getTermId(),
                new Pair<Long,Integer>(fc.getFilePointer(), posting.getList().size()));
        index.writePosting(fc.getChannel(), posting);
	}

	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 3) {
			System.err
					.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
			return;
		}

		/* Get index */
		String className = "cs276.assignments." + args[0] + "Index";
		try {
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get root directory */
		String root = args[1];
		File rootdir = new File(root);
		if (!rootdir.exists() || !rootdir.isDirectory()) {
			System.err.println("Invalid data directory: " + root);
			return;
		}

		/* Get output directory */
		String output = args[2];
		File outdir = new File(output);
		if (outdir.exists() && !outdir.isDirectory()) {
			System.err.println("Invalid output directory: " + output);
			return;
		}

		if (!outdir.exists()) {
			if (!outdir.mkdirs()) {
				System.err.println("Create output directory failure");
				return;
			}
		}

		/* BSBI indexing algorithm */
		File[] dirlist = rootdir.listFiles();

		/* For each block */
		for (File block : dirlist) {
			File blockFile = new File(output, block.getName());
			blockQueue.add(blockFile);

			File blockDir = new File(root, block.getName());
			File[] filelist = blockDir.listFiles();
			
			/* For each file */
			for (File file : filelist) {
				++totalFileCount;
				String fileName = block.getName() + "/" + file.getName();
                int docId = docIdCounter++;
				docDict.put(fileName, docId);
				
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split("\\s+");
					for (String token : tokens) {
						/*
						 * TODO: Your code here
						 *       For each term, build up a list of
						 *       documents in which the term occurs
						 */
						int termId;
						if(!termDict.containsKey(token)){
							wordIdCounter++;
							termDict.put(token, wordIdCounter);
							termId = wordIdCounter;
						}else{
							termId = termDict.get(token);
						}
						PostingList posting;
						if(postings.containsKey(termId)){
							posting = postings.get(termId);
						} else {
							posting = new PostingList(termId);
							postings.put(termId, posting);
						}
                        if(posting.getList().size() == 0
                                || posting.getList().get(posting.getList().size()-1) != docId) {
                            posting.getList().add(docId);
                        }
					}
				}
				reader.close();
			}

			/* Sort and output */
            List<Map.Entry<Integer,PostingList>> sortedpostings
                    = new ArrayList<Map.Entry<Integer,PostingList>>(postings.entrySet());

            Collections.sort(
                    sortedpostings
                    ,   new Comparator<Map.Entry<Integer,PostingList>>() {
                        public int compare(Map.Entry<Integer,PostingList> a, Map.Entry<Integer,PostingList> b) {
                            return Integer.compare(a.getKey(), b.getKey());
                        }
                    }
            );

			if (!blockFile.createNewFile()) {
                System.err.println("Create new block failure.");
                return;
            }
			
			RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");
			
			/*
			 * TODO: Your code here
			 *       Write all posting lists for all terms to file (bfc)
			 */
            for(Map.Entry<Integer,PostingList> entry : sortedpostings){
                index.writePosting(bfc.getChannel(), entry.getValue());
            }

			bfc.close();
			postings.clear();
		}

		/* Required: output total number of files. */
		System.out.println(totalFileCount);

		/* Merge blocks */
		while (true) {
			if (blockQueue.size() <= 1)
				break;

			File b1 = blockQueue.removeFirst();
			File b2 = blockQueue.removeFirst();
			
			File combfile = new File(output, b1.getName() + "+" + b2.getName());
			if (!combfile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}

			RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
			RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
			RandomAccessFile mf = new RandomAccessFile(combfile, "rw");
			 
			/*
			 * TODO: Your code here
			 *       Combine blocks bf1 and bf2 into our combined file, mf
			 *       You will want to consider in what order to merge
			 *       the two blocks (based on term ID, perhaps?).
			 *       
			 */
            PostingList a = index.readPosting(bf1.getChannel());
            PostingList b = index.readPosting(bf2.getChannel());
            while(true){
                if(a == null){
                    while(b != null){
                        writePosting(mf, b);
                        b = index.readPosting(bf2.getChannel());
                    }
                    break;
                }
                if(b == null){
                    while(a != null){
                        writePosting(mf, a);
                        a = index.readPosting(bf1.getChannel());
                    }
                    break;
                }

                if (a.getTermId() < b.getTermId()){
                    writePosting(mf, a);
                    a = index.readPosting(bf1.getChannel());
                } else if (a.getTermId() > b.getTermId()) {
                    writePosting(mf, b);
                    b = index.readPosting(bf2.getChannel());
                } else { // they are equal, we have to merge them
                    a.getList().addAll(b.getList()); // note that all docs have to be unique
                    Collections.sort(a.getList());
                    writePosting(mf, a);
                    a = index.readPosting(bf1.getChannel());
                    b = index.readPosting(bf2.getChannel());
                }
            }
			
			bf1.close();
			bf2.close();
			mf.close();
			b1.delete();
			b2.delete();
			blockQueue.add(combfile);
		}

		/* Dump constructed index back into file system */
		File indexFile = blockQueue.removeFirst();
		indexFile.renameTo(new File(output, "corpus.index"));

		BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(
				output, "term.dict")));
		for (String term : termDict.keySet()) {
			termWriter.write(term + "\t" + termDict.get(term) + "\n");
		}
		termWriter.close();

		BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(
				output, "doc.dict")));
		for (String doc : docDict.keySet()) {
			docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
		}
		docWriter.close();

		BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(
				output, "posting.dict")));
		for (Integer termId : postingDict.keySet()) {
			postWriter.write(termId + "\t" + postingDict.get(termId).getFirst()
					+ "\t" + postingDict.get(termId).getSecond() + "\n");
		}
		postWriter.close();
	}

}
