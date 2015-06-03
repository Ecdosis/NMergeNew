/**
 * Store the difference between one version of text and another
 */

package edu.luc.nmerge.mvd.diff;

/**
 *
 * @author desmond
 */
public class Diff
{
	/** offset in the OLD (B) version */
    int oldOffset;
	/** offset in the NEW (A) version */
    int newOffset;
	/** its old version length */
    int oldLen;
	/** its new version length */
    int newLen;
    /** kind of Diff */
    DiffKind kind;
	/**
	 * Create a Diff
	 * @param oldOffset its offset in the old (B) version
	 * @param newOffset its offset in the new (A) version
	 * @param oldLen the length of the old text in this diff
	 * @param newLen the length of the new text in this diff
	 */
    Diff( int oldOffset, int newOffset, int oldLen, int newLen, DiffKind kind )
    {
        this.oldOffset = oldOffset;
        this.newOffset = newOffset;
        this.oldLen = oldLen;
        this.newLen = newLen;
        this.kind = kind;
    }
    /**
    * Get the end of the diff's old version
    * @return an integer
    */
    public int oldEnd()
    {
            return oldOffset+ oldLen;
    }
    /**
     * Get the end of the diff's new version
     * @return an integer
     */
    public int newEnd()
    {
            return newOffset+newLen;
    }

    /**
     * Get the length of the diff's old version
     * @return an integer
     */
    public int oldLen()
    {
            return oldLen;
    }
    /**
     * Get the length of the diff's new version
     * @return an integer
     */
    public int newLen()
    {
            return newLen;
    }
    /**
     * Get the old offset of the Diff
     * @return an integer
     */
    public int oldOff()
    {
            return oldOffset;
    }
    /**
     * Get the new offset of the Diff
     * @return an integer
     */
    public int newOff()
    {
            return newOffset;
    }
   /**
    * Get the Diff's kind
    * @return a DiffKind
    */
    public DiffKind getKind()
    {
        return this.kind;
    }
}
