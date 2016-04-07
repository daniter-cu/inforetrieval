package cs276.assignments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.*;

public class Query {

	// Term id -> position in index file
	private static Map<Integer, Long> posDict = new TreeMap<Integer, Long>();
	// Term id -> document frequency
	private static Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();
	// Doc id -> doc name dictionary
	private static Map<Integer, String> docDict = new TreeMap<Integer, String>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list with a given termID from the file 
	 * You should seek to the file position of this specific
	 * posting list and read it back.
	 * */
	private static PostingList readPosting(FileChannel fc, int termId)
			throws IOException {
		/*
		 * TODO: Your code here
		 */
		return null;
	}

	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 2) {
			System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
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

		/* Get index directory */
		String input = args[1];
		File inputdir = new File(input);
		if (!inputdir.exists() || !inputdir.isDirectory()) {
			System.err.println("Invalid index directory: " + input);
			return;
		}

		/* Index file */
		RandomAccessFile indexFile = new RandomAccessFile(new File(input,
				"corpus.index"), "r");

		String line = null;
		/* Term dictionary */
		BufferedReader termReader = new BufferedReader(new FileReader(new File(
				input, "term.dict")));
		while ((line = termReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			termDict.put(tokens[0], Integer.parseInt(tokens[1]));
		}
		termReader.close();

		/* Doc dictionary */
		BufferedReader docReader = new BufferedReader(new FileReader(new File(
				input, "doc.dict")));
		while ((line = docReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
		}
		docReader.close();

		/* Posting dictionary */
		BufferedReader postReader = new BufferedReader(new FileReader(new File(
				input, "posting.dict")));
		while ((line = postReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
			freqDict.put(Integer.parseInt(tokens[0]),
					Integer.parseInt(tokens[2]));
		}
		postReader.close();

		/* Processing queries */
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		/* For each query */
		while ((line = br.readLine()) != null) {
			/*
			 * TODO: Your code here
			 *       Perform query processing with the inverted index.
			 *       Make sure to print to stdout the list of documents
			 *       containing the query terms, one document file on each
			 *       line, sorted in lexicographical order.
			 */
			LinkedList<PostingList> postings = new LinkedList<PostingList>();
			String[] queries = line.split("\\s+");
			for (String word : queries) {
				indexFile.seek(posDict.get(termDict.get(word)));
				postings.add(index.readPosting(indexFile.getChannel()));
			}

			PostingList pl = mergePostings(postings);
			ArrayList<String> files = new ArrayList<String>();
			for(Integer i  : pl.getList()){
				if(docDict.get(i) == null){
					System.err.println(i);
				}
				files.add(docDict.get(i));
			}

			assert(files.size() != 0);
			Collections.sort(files);
			for(String f : files){
				System.out.println(f);
			}
		}
		br.close();
		indexFile.close();
	}

	private static PostingList mergePostings(LinkedList<PostingList> postings) {
		ArrayList<PostingList> remove = new ArrayList<>();
		for(int i = 0; i < postings.size(); i++){
			for(int j = i+1; j < postings.size(); j++){
				if (postings.get(i) != postings.get(j) && postings.get(i).getTermId() == postings.get(j).getTermId()){
					remove.add(postings.get(i));
				}
			}
		}
		if (remove.size() > 0){
			for(PostingList pl : remove){
				postings.remove(pl);
			}
		}
		while(postings.size() > 1) {
			Collections.sort(postings, new Comparator<PostingList>() {
						public int compare(PostingList a, PostingList b) {
							return Integer.compare(a.getList().size(), b.getList().size());
						}
					}
			);

			PostingList p1 = postings.removeFirst();
			PostingList p2 = postings.removeFirst();
			ArrayList<Integer> posts = new ArrayList<Integer>();
			Iterator<Integer> iter1 = p1.getList().iterator();
			Iterator<Integer> iter2 = p2.getList().iterator();
			int a = iter1.next();
			int b = iter2.next();
			while(true){
				if(a == b){
					posts.add(a);
					if(!iter1.hasNext() || !iter2.hasNext()){
						break;
					}
					a = iter1.next();
					b = iter2.next();
				} else if(a < b) {
					if (!iter1.hasNext()){
						break;
					}
					a = iter1.next();
				} else if (b < a) {
					if (!iter2.hasNext()){
						break;
					}
					b = iter2.next();
				}
			}
			postings.addLast(new PostingList(-1, posts));
		}

		return postings.get(0);

	}
}
