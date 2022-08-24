package org.opentripplanner.util.index;

import java.util.Arrays;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;

public class FBitSet {

  /*
   * BitSets are packed into arrays of "words."  Currently a word is
   * a long, which consists of 64 bits, requiring 6 address bits.
   * The choice of word size is determined purely by performance concerns.
   */
  private static final int ADDRESS_BITS_PER_WORD = 6;
  private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
  private static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;

  /* Used to shift left or right for a partial word mask */
  private static final long WORD_MASK = 0xffffffffffffffffL;

  /**
   * The internal field corresponding to the serialField "bits".
   */
  private final long[] words;

  private boolean empty = true;

  /**
   * Creates a bit set whose initial size is large enough to explicitly
   * represent bits with indices in the range {@code 0} through
   * {@code nbits-1}. All bits are initially {@code false}.
   *
   * @param  nbits the initial size of the bit set
   * @throws NegativeArraySizeException if the specified initial size
   *         is negative
   */
  public FBitSet(int nbits) {
    // nbits can't be negative; size 0 is OK
    if (nbits < 0) throw new NegativeArraySizeException("nbits < 0: " + nbits);

    this.words = new long[wordIndex(nbits - 1) + 1];
  }

  /**
   * Given a bit index, return word index containing it.
   */
  private static int wordIndex(int bitIndex) {
    return bitIndex >> ADDRESS_BITS_PER_WORD;
  }

  /**
   * Sets the bit at the specified index to {@code true}.
   */
  public void set(int bitIndex) {
    this.empty = false;
    int wordIndex = wordIndex(bitIndex);
    words[wordIndex] |= (1L << bitIndex);
  }

  /**
   * Sets all of the bits in this BitSet to {@code false}.
   *
   * @since 1.4
   */
  public void clear() {
    this.empty = true;
    Arrays.fill(words, 0);
  }

  /**
   * Returns the value of the bit with the specified index. The value
   * is {@code true} if the bit with the index {@code bitIndex}
   * is currently set in this {@code BitSet}; otherwise, the result
   * is {@code false}.
   *
   * @param  bitIndex   the bit index
   * @return the value of the bit with the specified index
   * @throws IndexOutOfBoundsException if the specified index is negative
   */
  public boolean get(int bitIndex) {
    int wordIndex = wordIndex(bitIndex);
    return (words[wordIndex] & (1L << bitIndex)) != 0;
  }

  /**
   * Returns true if this {@code BitSet} contains no bits that are set
   * to {@code true}.
   *
   * @return boolean indicating whether this {@code BitSet} is empty
   * @since  1.4
   */
  public boolean isEmpty() {
    return empty;
  }

  /**
   * Performs a logical <b>AND</b> of this target bit set with the
   * argument bit set. This bit set is modified so that each bit in it
   * has the value {@code true} if and only if it both initially
   * had the value {@code true} and the corresponding bit in the
   * bit set argument also had the value {@code true}.
   */
  public void and(FBitSet other) {
    // Perform logical AND on words in common
    for (int i = 0; i < words.length; i++) {
      words[i] &= other.words[i];
    }
  }

  /**
   * Performs a logical <b>OR</b> of this bit set with the bit set
   * argument. This bit set is modified so that a bit in it has the
   * value {@code true} if and only if it either already had the
   * value {@code true} or the corresponding bit in the bit set
   * argument has the value {@code true}.
   */
  public void or(FBitSet other) {
    // Perform logical OR on words in common
    for (int i = 0; i < words.length; i++) {
      words[i] |= other.words[i];
    }
  }

  /**
   * Performs a logical <b>XOR</b> of this bit set with the bit set
   * argument. This bit set is modified so that a bit in it has the
   * value {@code true} if and only if one of the following
   * statements holds:
   * <ul>
   * <li>The bit initially has the value {@code true}, and the
   *     corresponding bit in the argument has the value {@code false}.
   * <li>The bit initially has the value {@code false}, and the
   *     corresponding bit in the argument has the value {@code true}.
   * </ul>
   */
  public void xor(FBitSet other) {
    // Perform logical XOR on words in common
    for (int i = 0; i < words.length; i++) {
      words[i] ^= other.words[i];
    }
  }

  /**
   * Clears all of the bits in this {@code BitSet} whose corresponding
   * bit is set in the specified {@code BitSet}.
   */
  public void andNot(FBitSet other) {
    // Perform logical (a & !b) on words in common
    for (int i = 0; i < words.length; i++) {
      words[i] &= ~other.words[i];
    }
  }

  /**
   * Returns the hash code value for this bit set. The hash code depends
   * only on which bits are set within this {@code BitSet}.
   *
   * <p>The hash code is defined to be the result of the following
   * calculation:
   *  <pre> {@code
   * public int hashCode() {
   *     long h = 1234;
   *     long[] words = toLongArray();
   *     for (int i = words.length; --i >= 0; )
   *         h ^= words[i] * (i + 1);
   *     return (int)((h >> 32) ^ h);
   * }}</pre>
   * Note that the hash code changes if the set of bits is altered.
   *
   * @return the hash code value for this bit set
   */
  public int hashCode() {
    long h = 1234;
    for (int i = words.length; --i >= 0;) h ^= words[i] * (i + 1);

    return (int) ((h >> 32) ^ h);
  }

  /**
   * Returns the number of bits of space actually in use by this
   * {@code BitSet} to represent bit values.
   * The maximum element in the set is the size - 1st element.
   *
   * @return the number of bits currently in this bit set
   */
  public int size() {
    return words.length * BITS_PER_WORD;
  }

  /**
   * Compares this object against the specified object.
   * The result is {@code true} if and only if the argument is
   * not {@code null} and is a {@code BitSet} object that has
   * exactly the same set of bits set to {@code true} as this bit
   * set. That is, for every nonnegative {@code int} index {@code k},
   * <pre>((BitSet)obj).get(k) == this.get(k)</pre>
   * must be true. The current sizes of the two bit sets are not compared.
   *
   * @param  obj the object to compare with
   * @return {@code true} if the objects are the same;
   *         {@code false} otherwise
   * @see    #size()
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof FBitSet set)) return false;
    if (this == obj) return true;

    // Check words in use by both BitSets
    for (int i = 0; i < words.length; i++) if (words[i] != set.words[i]) return false;

    return true;
  }

  public String toString() {
    return "FBitSet{" + size() + "}";
  }

  public IntIterator iterator() {
    return new IntIterator() {
      private int iw;
      private int ib;
      private long im;

      @Override
      public int next() {
        return (iw * 64) | ib - 1;
      }

      @Override
      public boolean hasNext() {
        while (iw < words.length) {
          while (ib < 64) {
            boolean found = (words[iw] & im) != 0;
            im = im << 1;
            ++ib;
            if (found) {
              return true;
            }
          }
          ib = 0;
          im = 1;
          ++iw;
        }
        return false;
      }
    };
  }

  public static void main(String[] args) {
    var bs = new FBitSet(65);
    print("Empty", bs);

    bs.set(3);
    print("Exp: 3", bs);

    bs.set(7);
    print("Exp: 3 7", bs);

    bs.set(63);
    print("Exp: 3 7 63", bs);

    bs.set(64);
    print("Exp: 3 7 63 64", bs);

    bs.clear();
    print("Clear", bs);
  }

  static void print(String label, FBitSet bs) {
    var it = bs.iterator();
    System.out.print(label + ":");
    while (it.hasNext()) {
      System.out.print(" " + it.next());
    }
    System.out.println();
  }
}
