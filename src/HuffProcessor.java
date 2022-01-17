import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out) {
		int[] freq = new int[ALPH_SIZE + 1];
		int bits = in.readBits(BITS_PER_WORD);
		while (bits != -1) {
			freq[bits]++;
			bits = in.readBits(BITS_PER_WORD);
		}
		freq[PSEUDO_EOF] = 1;

		PriorityQueue<HuffNode> pq = new PriorityQueue<>();


		for (int i = 0; i < freq.length; i++) {
			if (freq[i] > 0)
				pq.add(new HuffNode(i, freq[i], null, null));
		}

		while (pq.size() > 1) {
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode t = new HuffNode(0, left.myWeight + right.myWeight, left, right);
			pq.add(t);
		}
		HuffNode root = pq.remove();

		String[] encodings = new String[ALPH_SIZE + 1];
		codingHelper(root, "", encodings);

		out.writeBits(BITS_PER_INT, HUFF_TREE);
		in.reset();
		writeTree(root, out);

		while (true) {
			int bits2 = in.readBits(BITS_PER_WORD);
			if (bits2 == -1) break;

			String code = encodings[bits2];
			if (code != null)
				out.writeBits(code.length(), Integer.parseInt(code, 2));
		}

		String pseudo = encodings[PSEUDO_EOF];
		out.writeBits(pseudo.length(), Integer.parseInt(pseudo, 2));
		out.close();
	}
	private void writeTree(HuffNode root, BitOutputStream out) {
		if (root.myLeft != null || root.myRight != null) {
			out.writeBits(1, 0);
			writeTree(root.myLeft, out);
			writeTree(root.myRight, out);
		}
		else {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, root.myValue);
		}
	}

	private void codingHelper(HuffNode root, String path, String[] encodings) {
		if (root.myRight == null && root.myLeft == null) {
			encodings[root.myValue] = path;
			return;
		}
		codingHelper(root.myLeft, path + "0", encodings);
		codingHelper(root.myRight, path + "1", encodings);
	}


	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){

		int magic = in.readBits(BITS_PER_INT);
		if (magic == -1) throw new HuffException("invalid magic number2" + magic);
		if (magic != HUFF_TREE) {
			throw new HuffException("invalid magic number "+magic);
		}
		// remove all code below this point for P7
		HuffNode root = readTree(in);
		HuffNode current = root;
		while(true) {
			int compressedBit = in.readBits(1);
			if (compressedBit == -1) throw new HuffException("bad input, no PSEUDO_EOF");
			else{
				if(compressedBit == 0) current = current.myLeft;
				else current = current.myRight;
				// is leaf
				if(current.myRight == null && current.myLeft == null){
					if(current.myValue == PSEUDO_EOF) break;
					else{
						out.writeBits(BITS_PER_WORD, current.myValue);
						current = root;
					}
				}
			}
		}
		out.close();
		/*

		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}
		out.close();*/
	}
	private HuffNode readTree(BitInputStream in){
		int bit = in.readBits(1);
		if (bit == -1) throw new HuffException("bit reading failed");
		if (bit == 0) {
			HuffNode left = readTree(in);
			HuffNode right = readTree(in);
			return new HuffNode(0,0,left,right);
		}
		else{
			int value = in.readBits(BITS_PER_WORD+1);
			return new HuffNode(value, 0, null, null);
		}
	}
}