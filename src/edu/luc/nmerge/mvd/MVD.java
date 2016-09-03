/*
 *  NMerge is Copyright 2009 Desmond Schmidt
 * 
 *  This file is part of NMerge. NMerge is a Java library for merging 
 *  multiple versions into multi-version documents (MVDs), and for 
 *  reading, searching and comparing them.
 *
 *  NMerge is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  NMerge is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.luc.nmerge.mvd;
import edu.luc.nmerge.mvd.navigator.TextNavigator;
import edu.luc.nmerge.mvd.diff.Diff;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.io.Serializable;
import java.io.File;

import edu.luc.nmerge.graph.Graph;
import edu.luc.nmerge.graph.Arc;
import edu.luc.nmerge.graph.MUM;
import edu.luc.nmerge.graph.SimpleQueue;
import edu.luc.nmerge.graph.SpecialArc;
import edu.luc.nmerge.graph.SpecialComparator;
import edu.luc.nmerge.graph.suffixtree.SuffixTree;
import edu.luc.nmerge.graph.Converter;
import edu.luc.nmerge.graph.Node;
import edu.luc.nmerge.exception.MVDException;
import edu.luc.nmerge.mvd.diff.Matrix;
import edu.luc.nmerge.mvd.table.TableView;
import edu.luc.nmerge.mvd.table.FragKind;
import edu.luc.nmerge.mvd.table.Options;
import edu.luc.nmerge.mvd.navigator.TextNavigator;

/**
 * Represent a multi-version document.
 * @author Desmond Schmidt &copy; 2009
 */
public class MVD extends Serialiser implements Serializable
{
	// new options
	boolean timing;
	boolean directAlignOnly;
	static final int DUFF_PID = -1;
	static final int NULL_PID = 0;
	public static String UNTITLED_NAME = "untitled";
	ArrayList<Group> groups;		// id = position in table+1
	ArrayList<Version> versions;	// id = position in table+1
	ArrayList<Pair> pairs;
	String description;
	int headerSize,groupTableSize,versionTableSize,pairsTableSize,
	dataTableSize,versionSetSize;
	int bestScore;
	long startTime;
	// used for checking
	HashSet<Pair> parents;
	BitSet partialVersions;
	String encoding;
	public MVD()
	{
		setDefaults();
	}
	public MVD( String description )
	{
		setDefaults();
		this.description = description;
	}
	/**
	 * Create an empty MVD - add versions and data later
	 * @param description about this MVD
	 * @param encoding the encoding of the data in this MVD
	 */
	public MVD( String description, String encoding )
	{
		setDefaults();
		this.description = description;
		this.encoding = encoding;
	}
	/**
	 * Set default values for the MVD
	 */
	private void setDefaults()
	{
		this.description = "";
		this.groups = new ArrayList<Group>();
		this.versions = new ArrayList<Version>();
		this.pairs = new ArrayList<Pair>();
		this.encoding = "UTF-8";
	}
	/**
	 * Set the encoding, which defaults to UTF-8
	 * @param encoding the new encoding
	 */
	public void setEncoding( String encoding )
	{
		this.encoding = encoding;
	}
	/**
	 * Set the MVD's ability to direct align only (avoiding transpositions)
	 * @param directAlignOnly a boolean descibing the correct state
 	 */
	public void setDirectAlign( boolean directAlignOnly )
	{
		this.directAlignOnly = directAlignOnly;
	}
	/**
	 * Set the version set size for all version sets
	 * @param setSize the size of the version set in bytes
	 */
	void setVersionSetSize( int setSize )
	{
		versionSetSize = setSize;
	}
	/**
	 * Get the description defined for this MVD
	 * @return the description as a String
	 */
	public String getDescription()
	{
		return description;
	}
	/**
	 * Get the encoding of the data in this MVD
	 * @return the encoding as a String
	 */
	public String getEncoding()
	{
		return encoding;
	}
	/** 
	 * Add a version after we've built the MVD. 
	 * @param version the version to add
	 * @param group its group id
	 * @throws MVDException
	 */
	public void addVersion( int version, short group ) throws MVDException
	{
		if ( version == versions.size()+1 )
		{
			Version v = new Version( group, Version.NO_BACKUP, "Z", 
				UNTITLED_NAME );
			versions.add( version-1, v );
		}
		else
			throw new MVDException("Invalid version "+version+" ignored");
	}
	/**
	 * Add a version to the MVD. 
	 * @param v the version definition to add
	 */
	void addVersion( Version v ) throws MVDException
	{
		if ( versions == null )
			versions = new ArrayList<Version>();
		if ( v.group > groups.size() )
			throw new MVDException( "invalid group id="+v.group );
		versions.add( v );
	}
	/**
	 * Get the number of versions
	 * @return the number of elements in the versions array
	 */
	public int numVersions()
	{
		return versions.size();
	}
	/**
	 * Get the number of groups
	 * @return the number of elements in the groups array
	 */
	public int numGroups()
	{
		return groups.size();
	}
	/**
	 * Add an anonymous group to a finished MVD by shifting higher 
	 * group ids up.
	 * @param groupId the id of the desired group
	 * @param parent the id of the parent group or 0 if top level
	 */
	public void addGroup( short groupId, short parent )
	{
		Group g = new Group( parent, UNTITLED_NAME );
		groups.add( groupId-1, g );
		// adjust groups referenced by versions
		for ( int i=0;i<versions.size();i++ )
		{
			Version v = versions.get( i );
			if ( v.group >= groupId )
				v.group++;
		}
	}
	/**
	 * Add a group to the MVD
	 * @param group the group to add
	 */
	void addGroup( Group group )
	{
		if ( groups == null )
			groups = new ArrayList<Group>();
		groups.add( group );
	}
	/**
	 * Get any sub-groups of the specified group
	 * @return an array of sub-groups or an empty array if none
	 */
	public String[] getSubGroups( short groupId )
	{
		ArrayList<String> subGroups = new ArrayList<String>();
		for ( int id=1,i=0;i<groups.size();i++,id++ )
		{
			Group g = groups.get( i );
			if ( g.getParent() == groupId )
				subGroups.add( g.toString()+";id:"+id );
		}
		String[] array = new String[subGroups.size()];
		return subGroups.toArray( array );
	}
	/**
	 * Get any immediate (not nested) sub-versions of the specified group
	 * @return an array of sub-versions or an empty array if none
	 */
	public String[] getSubVersions( short groupId )
	{
		ArrayList<String> subVersions = new ArrayList<String>();
		for ( int id=1,i=0;i<versions.size();i++,id++ )
		{
			Version v = versions.get( i );
			if ( v.group == groupId )
				subVersions.add( v.toString()+";id:"+id );
		}
		String[] array = new String[subVersions.size()];
		return subVersions.toArray( array );
	}
	/**
	 * Add a pair to the MVD
	 * @param pair the pair to add
	 */
	void addPair( Pair pair ) throws Exception
	{
		pairs.add( pair );
	}
	/**
	 * Get the pairs list for converting to a Graph
	 * @return the pairs - read only!
	 */
	public ArrayList<Pair> getPairs()
	{
		return pairs;
	}
	/**
	 * Get a pair from the MVD
	 * @param pairIndex the index of the pair
	 */
	Pair getPair( int pairIndex ) throws Exception
	{
		return pairs.get( pairIndex );
	}
	/**
	 * Make a bitset of all the partial versions for quick lookup
	 */
	void initPartialVersions()
	{
		partialVersions = new BitSet();
		for ( int i=1;i<=versions.size();i++ ) 
		{ 
			Version v = versions.get( i-1 );
			if ( v.isPartial() )
				partialVersions.set( i );
		}
	}
	/**
	 * Compare two versions u and v. If it is in u but not in v 
	 * then turn that pair and any subsequent pairs with the 
	 * same characteristic into a match. We also generate merged 
	 * Matches for the gaps between the state matches (added or 
	 * deleted). This way we can link up the merged matches in 
	 * the GUI.
	 * @param u the first version to compare
	 * @param v the second version to compare
	 * @param state the state of text belonging only to u
	 * @return an array of chunks for special display
	 */
	public Chunk[] compare( short u, short v, ChunkState state ) 
		throws MVDException
	{
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		short backup = versions.get(u-1).getBackup();
		Chunk current = new Chunk( encoding, backup );
		current.setVersion( u );
		TransposeState oldTS = null;
		TransposeState ts = new TransposeState();
		ChunkStateSet cs = new ChunkStateSet( backup );
		ChunkStateSet oldCS = null;
		Pair p = null;
		Chunk.chunkId = 0;
		TransposeState.transposeId = Integer.MAX_VALUE;
		int i = next( 0, u );
		while ( i < pairs.size() )
		{
			p = pairs.get( i );
			oldTS = ts;
			oldCS = cs;
			ts = ts.next( p, u, v );
			// transposed is not deleted, inserted or merged
			if ( !ts.isTransposed() )
				cs = cs.next( p, state, v );
			if ( ts != oldTS || cs != oldCS )
			{
				// then we have to write out current
				ChunkStateSet cs1 = current.getStates();
				if ( current.getLength()>0 )
				{
					if ( cs1.isMerged() )
						current.setId( ++Chunk.chunkId );
					chunks.add( current );
				}
				// set up a new current chunk
				ChunkState[] newStates;
				if ( ts.getId() != 0 )
				{
					newStates = new ChunkState[1];
					newStates[0] = ts.getChunkState();
				}
				else
					newStates = cs.getStates();
				current = new Chunk( encoding, ts.getId(), 
					newStates, p.getChars(), backup );
				current.setVersion( u );
			}
			else
				current.addData( p.getChars() );
			if ( i < pairs.size()-1 )
				i = next( i+1, u );
			else
				break;
		}
		// add any lingering chunks
		if ( current.getStates().isMerged() )
			current.setId( ++Chunk.chunkId );
		if ( chunks.isEmpty() || current != chunks.get(chunks.size()-1) )
			chunks.add( current );
		Chunk[] result = new Chunk[chunks.size()];
		chunks.toArray( result );
		return result;
	}
	/**
	 * Update the chunk's state list given a new pair and the version 
	 * we are following through
	 * @param chunk the chunk to update
	 * @param p the new pair
	 * @param version
	 */
	public void nextChunkState( Chunk chunk, Pair p, short version )
	{
	}
	/**
	 * Get the index of the next pair intersecting with a version
	 * @param pairIndex the index to start looking from
	 * @param u the version to look for
	 * @return the index of the next pair or Integer.MAX_VALUE if not found
	 */
	int next( int pairIndex, short u )
	{
		int i=pairIndex;
		while ( i < pairs.size() )
		{
			Pair p = pairs.get( i );
			if ( p.contains(u) )
				return i;
			else
				i++;
		}
		return Integer.MAX_VALUE;
	}
	/**
	 * Get the index of the previous pair intersecting with a 
	 * version
	 * @param pairIndex the index to start looking from
	 * @param u the version to look for
	 * @return the index of the previous pair or -1 if not found
	 */
	int previous( int pairIndex, short u )
	{
		int i=pairIndex-1;
		while ( i > 0 )
		{
			Pair p = pairs.get( i );
			if ( p.contains(u) )
				return i;
			else
				i--;
		}
		return -1;
	}
    /**
     * Look forward from a position for a string in a given version
     * @param str the string to seek
     * @param index the first index of the pair 
     * @param offset the offset into that pair's data
     * @param v the version to seek
     * @return true if it matched
     */
    private boolean findForward( String str, int index, int offset, int v )
    {
        TextNavigator tn = new TextNavigator(this,index,offset,v);
        int i;
        for ( i=0;i<str.length();i++ )
        {
            char tnChar = tn.next();
            if ( tnChar != str.charAt(i) )
            {
                char lcTnChar = Character.toLowerCase(tnChar);
                char sLcChar = Character.toLowerCase(str.charAt(i));
                if ( lcTnChar != sLcChar )
                    break;
            }
        }
        return i==str.length();
    }
    /**
     * Find a string backwards in a given version
     * @param str the string to find
     * @param index the index of the pair starting from
     * @param offset the offset in the pair to search first (may be -1)
     * @param v the version to follow
     * @return true if it was found
     */
    private boolean findBackward( String str, int index, int offset, int v )
    {
        TextNavigator tn = new TextNavigator(this,index,offset,v);
        int i;
        for ( i=str.length()-1;i>=0;i-- )
        {
            if ( tn.prev() == str.charAt(i) )
                break;
        }
        return i==-1;
    }
    /**
     * Find a literal query using index search
     * @param query the literal text to find
     * @param mvdPos the start-position of the first term in the mvd
     * @param firstTerm the first term of the query 
     * @return the set of versions it was found in (may be empty)
     */
	public BitSet find( String query, int mvdPos, String firstTerm )
    {
        int pos = 0;
        String rhs = query;
        String lhs = "";
        boolean found = false;
        BitSet bs = new BitSet();
        int index = query.indexOf(firstTerm);
        if ( index > 0 )
        {
            rhs = query.substring(index);
            lhs = query.substring(0,index);
        }
        for ( int i=0;i<pairs.size();i++ )
        {
            Pair p = pairs.get(i);
            if ( p.length()+pos > mvdPos )
            {
                for ( int v=p.versions.nextSetBit(0);v>=0;v=p.versions.nextSetBit(v+1) )
                {
                    found = findForward(rhs,i,mvdPos-pos,v);
                    if ( found && lhs.length()>0 )
                        found = findBackward(lhs,i,(mvdPos-pos)-1,v);
                    if ( found ) 
                        bs.set(v);
                }
                break;
            }
            pos += p.length();
        }
        return bs;
    }
    /**
	 * Search for a pattern. Return multiple matches if requested 
	 * as an array of Match objects
	 * @param pattern the pattern to search for
	 * @param bs the set of versions to search through
	 * @param multiple if true return all hits; otherwise only the first 
	 * @return an array of matches
	 */
	public Match[] search( char[] pattern, BitSet bs, boolean multiple ) 
		throws Exception
	{
		KMPSearchState inactive = null;
		KMPSearchState active = null;
		Match[] matches = new Match[0];
		if ( !versions.isEmpty() )
		{
			inactive = new KMPSearchState( pattern, bs );
			for ( int i=0;i<pairs.size();i++ )
			{
				Pair temp = pairs.get( i );
				// move all elements from active to inactive
				if ( inactive == null )
					inactive = active;
				else
					inactive.append( active );
				active = null;
				// move matching SearchStates into active
				KMPSearchState s = inactive;
				while ( s != null )
				{
					KMPSearchState sequential = s.following;
					if ( s.v.intersects(temp.versions) )
					{
						KMPSearchState child = s.split(temp.versions);
						if ( active == null )
							active = child;
						else
							active.append( child );
						if ( s.v.isEmpty() )
							inactive = inactive.remove( s );
					}
					s = sequential;
				}
				// now process each char of the pair
				if ( active != null )
				{
					char[] data = temp.getChars();
					for ( int j=0;j<data.length;j++ )
					{
						KMPSearchState ss = active;
						while ( ss != null )
						{
							if ( ss.update(data[j]) )
							{
								Match[] m = Match.makeMatches( 
									pattern.length,ss.v,
									this,i,j,multiple,ChunkState.found);
								if ( matches == null )
									matches = m;
								else
									matches = Match.merge( matches, m );
								if ( !multiple )
									break;
							}
							ss = ss.following;
						}
						// now prune the active list
						KMPSearchState s1 = active;
						if ( s1.next != null )
						{
							while ( s1 != null )
							{
								KMPSearchState s2 = s1.following;
								while ( s2 != null )
								{
									KMPSearchState sequential = s2.following;
									if ( s1.equals(s2) )
									{
										s1.merge( s2 );
										active.remove( s2 );
									}
									s2 = sequential;
								}
								s1 = s1.following;
							}
						}
					}
				}
			}
		}
		return matches;
	}
	/**
	 * Create a new empty version.
     * @param group if empty or null convert to TOP_LEVEL
	 * @return the id of the new version
	 */
	public int newVersion( String shortName, String longName, String group, 
		short backup, boolean partial ) 
	{
		short gId = findGroup( group );
		if ( gId == 0 && group != null && group.length()> 0 )
		{
			Group g = new Group( (short)0, group );
			groups.add( g );
			gId = (short)groups.size();
		}
		versions.add( new Version(gId, (partial)?backup:Version.NO_BACKUP, 
			shortName, longName) );
		int vId = versions.size();
		// now go through the graph, looking for any pair 
		// containing the backup version and adding to it 
		// the new version. Q: does that apply also to hints?
		if ( partial )
		{
			for ( int i=0;i<pairs.size();i++ )
			{
				Pair p = pairs.get( i );
				if ( p.versions.nextSetBit(backup)==backup )
					p.versions.set(vId);
			}
		}
		return vId;
	}
	/**
	 * Get the group id corresponding to the name
	 * @param groupName
	 * @return the group id
	 */
	private short findGroup( String groupName ) 
	{
		short id = 0;
		for ( int i=0;i<groups.size();i++ )
		{
			Group g = groups.get( i );
			if ( g.name.equals(groupName) )
			{
				id = (short) (i + 1);
				break;
			}
		}
		return id;
	}
	/**
	 * Get the group parent id
	 * @param groupId the group id
	 * @return the corresponding group name
	 */
	public short getGroupParent( short groupId )
	{
		Group g = groups.get( groupId-1 );
		return g.parent;
	}
	/**
	 * Get the group name given its id
	 * @param groupId the group id
	 * @return the corresponding group name
	 */
	public String getGroupName( short groupId )
	{
		Group g = groups.get( groupId-1 );
		return g.name;
	}
	/**
	 * Get the backup for the given version
	 * @param vId the version to get the backup of
	 * @return the backup version or 0 for NO_BACKUP
	 */
	public short getBackupForVersion( int vId )
	{
		Version v = versions.get(vId-1);
		return v.backup;
	}
	/**
	 * Get the group id for the given version
	 * @param vId the version to get the group of
	 * @return the group id
	 */
	public short getGroupForVersion( int vId )
	{
		Version v = versions.get(vId-1);
		return v.group;
	}
	/**
	 * Get the current index of the given group + 1
	 * @param groupName the unique group name to search for
	 * @return the index +1 of the group in the groups ArrayList or 0
	 */
	public short getGroupId( String groupName )
	{
		for ( short i=0;i<groups.size();i++ )
            if ( groups.get(i).name.equals(groupName) )
                return (short)(i+1);
        return 0;
	}
	/**
	 * Get the current index of the given version + 1
	 * @param v the version to search for
	 * @return the index +1 of the version in the versions ArrayList or 0
	 */
	public int getVersionId( Version v )
	{
		return versions.indexOf(v) + 1;
	}
	/**
	 * Change the description of this MVD
	 * @param description
	 */
	public void setDescription( String description )
	{
		this.description = description;
	}
	/**
	 * Rename a group. If the groupId == 0 do nothing
	 * @param groupId the id of the group to rename
	 * @param groupName the new name for the group
	 */
	public void setGroupName( short groupId, String groupName )
	{
		if ( groupId > 0 )
		{
			Group g = groups.get( groupId-1 );
			g.setName( groupName);
		}
	}
    /**
     * Get the full group path leading to a version
     * @param vId the version id
     * @return a String
     */
    public String getGroupPath( short vId )
    {
        StringBuilder sb = new StringBuilder();
        do
        {
            Version v = versions.get(vId-1);
            if ( v.group != Group.TOP_LEVEL )
            {
                Group g = groups.get( v.group-1 );
                sb.insert( 0, g.name );
                sb.insert( 0, "/" );
                vId = g.parent;
            }
            else
                break;
        }
        while ( vId > 0 );
        return sb.toString();
    }
	/**
	 * Set the parent of the given group
	 * @param groupId the id of the group to change
	 * @param parentId the new parent
	 */
	public void setGroupParent( short groupId, short parentId )
	{
		if ( groupId > 0 )
		{
			Group g = groups.get( groupId-1 );
			g.setParent( parentId );
		}
	}
	/**
	 * Set the short name of a given version
	 * @param versionId the id of the affected version
	 * @param shortName the new short name
	 */
	public void setVersionShortName( int versionId, String shortName )
	{
		Version v = versions.get( versionId-1 );
		v.shortName = shortName;
	}
	/**
	 * Set the long name of a given version
	 * @param versionId the id of the affected version
	 * @param longName the new long name
	 */
	public void setVersionLongName( int versionId, String longName )
	{
		Version v = versions.get( versionId-1 );
		v.longName = longName;
	}
	/**
	 * Set the backup version of a given version
	 * @param versionId the id of the affected version
	 * @param backup the new backup or NO_BACKUP
	 */
	public void setVersionBackup( int versionId, short backup )
	{
		Version v = versions.get( versionId-1 );
		v.backup = backup;
	}
	/**
	 * Set the group membership of a version
	 * @param versionId id of the affected version 
	 * @param groupId the new groupId
	 */
	public void setVersionGroup( int versionId, short groupId )
	{
		Version v = versions.get( versionId-1 );
		v.group = groupId;
	}
	/**
	 * Set a group's open status (a transient property)
	 * @param groupId the group id that is affected by the change
	 * @param open the new open value
	 */
	public void setOpen( short groupId, boolean open )
	{
		Group g = groups.get( groupId-1 );
		g.setOpen( open );
	}
	/**
	 * Get the default group for this MVD
	 * @return null if no versions or groups defined, otherwise the 
	 * first group in the list
	 */
	public String getDefaultGroup()
	{
		String groupName = null;
		if ( groups.size() > 0 )
		{
			Group g = groups.get( 0 );
			groupName = g.name;
		}
		return groupName;
	}
	/**
	 * Add a totally new version to a graph
	 * @param original the graph to add it to
	 * @param version the version id of the new version
	 * @param data the new version's data
	 * @throws Exception if something went wrong
	 */
    private void add( Graph original, short version, char[] data )
        throws Exception
    {
        Graph g = original;

		SpecialArc special = g.addSpecialArc( data, version, 0 );
		if ( timing )
			startTime = System.currentTimeMillis();
		if ( g.getStart().cardinality() > 1 )
		{
			SuffixTree st = makeSuffixTree( special );
			MUM bestMUM = MUM.findDirectMUM( special, st, g );
			mergeSpecial( g, bestMUM );
		}
		original.adopt( version );
    }
	/**
	 * Given an initial best MUM merge it into the given graph or subgraph
	 * @param g the initial graph or subgraph
	 * @param bestMUM the best MUM at the start. may be a transposition
	 * @throws Exception
	 */
	void mergeSpecial( Graph g, MUM bestMUM ) throws Exception
	{
		TreeMap<SpecialArc,Graph> specials =
		new TreeMap<SpecialArc,Graph>(new SpecialComparator());
		while ( bestMUM != null )
		{
			if ( bestMUM.verify() )
			{
				bestMUM.merge();
				SimpleQueue<SpecialArc> leftSpecials =
					bestMUM.getLeftSpecialArcs();
				SimpleQueue<SpecialArc> rightSpecials =
					bestMUM.getRightSpecialArcs();
				while ( leftSpecials != null && !leftSpecials.isEmpty() )
					installSpecial( specials, leftSpecials.poll(),
						bestMUM.getLeftSubgraph(), true );
				while ( rightSpecials != null && !rightSpecials.isEmpty() )
					installSpecial( specials, rightSpecials.poll(),
						bestMUM.getRightSubgraph(), false );
			}
			else // try again
			{
				bestMUM = recomputeMUM( bestMUM );
				if ( bestMUM != null )
					specials.put( bestMUM.getArc(), bestMUM.getGraph() );
			}
			// POP topmost entry, if possible
			bestMUM = null;
			if ( specials.size() > 0 )
			{
				SpecialArc key = specials.firstKey();
				if ( key != null )
				{
					g = specials.remove( key );
					bestMUM = key.getBest();
				}
			}
		}
	}
    /**
     * Update one version of an MVD using a diff algorithm to identify
     * changed parts of the version, and merge those as individual sub-graphs.
     * @param original the original Graph already deserialised
     * @param version the version to be revised
     * @param data the new data for that version
     * @param mergeSharedVersions if true merge all versions sharing the
     * same text in the original to the replacement text
     */
    private void revise( Graph original, short version, char[] data,
        boolean mergeSharedVersions ) throws Exception
    {
        char[] base = getVersion( version );
        Diff[] diffs = Matrix.computeBasicDiffs( data, base );
        TreeMap<SpecialArc,Graph> specials =
			new TreeMap<SpecialArc,Graph>(new SpecialComparator());
        Graph[] miniGraphs = new Graph[diffs.length];
        Node n = original.getStart();
        for ( int pos=0,i=0;i<diffs.length;i++ )
        {
            miniGraphs[i] = original.getMiniGraph( diffs[i], version, pos, n );
            pos = diffs[i].oldOff()+diffs[i].oldLen();
            n = miniGraphs[i].getEnd();
        }
        for ( int i=0;i<miniGraphs.length;i++ )
        {
            Graph g = miniGraphs[i];
            BitSet shared = g.getSharedVersions(version);
            g.removeVersions( shared );
			char[] diffData = new char[diffs[i].newLen()];
			int offset = diffs[i].newOff();
			for ( int j=0;j<diffData.length;j++ )
				diffData[j] = data[offset+j];
            SpecialArc special = g.addSpecialArc( diffData, shared, offset );
			if ( g.getStart().cardinality() > 1 )
			{
				MUM bestMUM = computeBestMUM( g, special );
				if ( bestMUM != null )
					mergeSpecial( g, bestMUM );
			}
			g.adopt( version );
        }
    }
	/**
	 * Update an existing version or add a new one.
	 * @param version the id of the version to add. 
	 * @param data the data to merge
     * @param mergeSharedVersions apply to all versions sharing the same text
	 * @return percentage of the new version that was unique, or 0 
	 * if this was the first version
	 */
	public float update( short version, char[] data,
        boolean mergeSharedVersions ) throws Exception
	{
		// to do: if version already exists, remove it first
		Converter con = new Converter();
		Graph original = con.create( pairs, versions.size() );
		if ( version < versions.size() )
        {
            //System.out.println("version="+version+" num-versions="+versions.size());
            revise( original, version, data, mergeSharedVersions );
        }
        else
            add( original, version, data );
        pairs = con.serialise();
		if ( timing )
		{
			String finishTime = new Long(System.currentTimeMillis()
				-startTime).toString();
			System.out.println( "Time taken to merge version "
				+version+"="+finishTime );
		}
		if ( numVersions()==1 )
			return 0.0f;
		else
			return getPercentUnique( version );
    }
    /**
	 * Get the percentage of the given version that is unique
	 * @param version the version to compute uniqueness for
	 * @return the percent as a float
	 */
	public float getUniquePercentage( short version )
	{
		int totalLen = 0;
		int uniqueLen = 0;
		if ( numVersions()==1 )
			return 0.0f;
		else
		{
			for ( int i=0;i<pairs.size();i++ )
			{
				Pair p = pairs.get( i );
				if ( p.versions.nextSetBit(version)==version )
				{
					if ( p.versions.size()==1 )
						uniqueLen+= p.length();
					totalLen += p.length();
				}
			}
			return (float)uniqueLen/(float)totalLen;
		}
	}
	/**
	 * Get the percentage of the given version that is unique
	 * @param version the version to test
	 * @return float fraction of version that is unique
	 */
	private float getPercentUnique( short version )
	{
		float unique=0.0f,shared=0.0f;
		for ( int i=0;i<pairs.size();i++ )
		{
			Pair p = pairs.get( i );
			if ( p.versions.nextSetBit(version)==version )
			{
				if ( p.versions.size()==1 )
					unique += p.length();
				else
					shared += p.length();
			}
		}
		return unique/shared;
	}
	/**
	 * The MUM is invalid. We have to find a valid one.
	 * @param old the old invalid MUM
	 * @return a new valid MUM or null
	 */
	MUM recomputeMUM( MUM old ) throws MVDException
	{
		Graph g = old.getGraph();
		SpecialArc special = old.getArc();
		return computeBestMUM( g, special );
	}
	/**
	 * Compute the best MUM
	 * @param g a graph
	 * @param special a special arc aligned with g
	 * @return the new MUM or null
	 * @throws an MVDException
	 */
	private MUM computeBestMUM( Graph g, SpecialArc special ) 
		throws MVDException
	{
		SuffixTree st = makeSuffixTree( special );
		MUM directMUM = MUM.findDirectMUM( special, st, g );
		MUM best = directMUM;
		if ( !directAlignOnly )
		{
			MUM leftTransposeMUM = MUM.findLeftTransposeMUM( 
				special, st, g );
			MUM rightTransposeMUM = MUM.findRightTransposeMUM( 
				special, st, g );
			best = getBest( directMUM, leftTransposeMUM, 
				rightTransposeMUM );
		}
		if ( best != null )
			special.setBest( best );
		return best;
	}
	/**
	 * Create a new suffix tree based on the data in the special arc. 
	 * @param special the special arc
	 * @return the suffix tree
	 * @throws MVDException
	 */
	private SuffixTree makeSuffixTree( SpecialArc special ) 
		throws MVDException
	{
		char[] specialData;
		specialData = special.getData();
		return new SuffixTree( specialData, false );
	}
	/**
	 * Install a subarc into specials
	 * @param specials the specials TreeMap (red-black tree)
	 * @param special the special subarc to add
	 * @param subGraph the directly opposite subgraph
	 * @param left true if we are doing the left subarc, otherwise the 
	 * right
	 */
	private void installSpecial( TreeMap<SpecialArc,Graph> specials, 
		SpecialArc special, Graph subGraph, boolean left ) throws MVDException
	{
		assert special.getFrom() != null && special.to != null;
		// this is necessary BEFORE you recalculate the MUM
		// because it will invalidate the special's location 
		// in the treemap and make it unfindable
		if ( specials.containsKey(special) )
			specials.remove( special );
		MUM best = computeBestMUM( subGraph, special );
		if ( best != null )
			specials.put( special, subGraph );
        //System.out.println("special="+special.toString());
	}
	/**
	 * Find the better of three MUMs or null if they are all null.
	 * @param direct a direct align MUM possibly null
	 * @param leftTransposed the left transpose MUM possibly null
	 * @param rightTransposed the right transpose MUM possibly null
	 * @return null or the best MUM
	 */
	private MUM getBest( MUM direct, MUM leftTransposed, 
		MUM rightTransposed )
	{
		MUM best = null;
		// decide which transpose MUM to use
		MUM transposed;
		if ( leftTransposed == null )
			transposed = rightTransposed;
		else if ( rightTransposed == null )
			transposed = leftTransposed;
		else if ( leftTransposed.compareTo(rightTransposed) > 0 )
			transposed = leftTransposed;
		else
			transposed = rightTransposed;
		// decide between direct and transpose MUM
		if ( direct != null && transposed != null )
		{
			int result = direct.compareTo( transposed );
			// remember, we nobbled the compareTo method
			// to produce reverse ordering in the specials
			// treemap, so "less than" is actually longer
			if ( result == 0 || result < 0 )
				best = direct;
			else
				best = transposed;
		}
		else if ( direct == null )
			best = transposed;
		else 
			best = direct;
		return best;
	}
	/**
	 * The only way to remove a version from an MVD is to construct 
	 * a graph and then delete the version from it. Then we serialise 
	 * it out into pairs again and call the other removeVersion method 
	 * on EACH and EVERY pair.
	 * @param version the version to be removed
	 */
	public void removeVersion( int version ) throws Exception
	{
		Converter con = new Converter();
		Graph original = con.create( pairs, versions.size() );
		original.removeVersion( version );
		original.verify();
		versions.remove( version-1 );
		pairs = con.serialise();
		for ( int i=0;i<pairs.size();i++ )
		{
			Pair p = pairs.get( i );
			p.versions = removeVersion( p.versions, version );
		}
	}
	/**
	 * Remove a version from a BitSet and shift all subsequent 
	 * versions down by 1
	 * @param versions the bitset containing the versions
	 * @param version the version id to remove
	 * @erturn a modified bitset 
	 */
	private BitSet removeVersion( BitSet versions, int version )
	{
		BitSet bs = new BitSet();
		for ( int i=versions.nextSetBit(0);i>=0;
			i=versions.nextSetBit(i+1) ) 
		{
			if ( i < version )
				bs.set( i );
			else if ( i > version )
				bs.set( i-1 );
			// and if equal we of course skip it
		}
		return bs;
	}
	/**
	 * Remove a group from the group table. Check that the parent 
	 * group now has at least one member. If not, remove it also. 
	 * Update all the group ids in all the versions too. 
	 * @param group the group id to remove
	 */
	public void removeGroup( short group ) throws Exception
	{
		// remove the actual group
		groups.remove( (short)(group-1) );
		// update all the versions to reflect the change
		HashSet<Integer> delenda = new HashSet<Integer>();
		for ( int i=0;i<versions.size();i++ )
		{
			Version v = versions.get( i );
			if ( v.group > group )
				v.group--;
			else if ( v.group == group )
				delenda.add( new Integer(i+1) );
		}
		// now remove any child versions of the group 
		if ( delenda.size() > 0 )
		{
			// delete the versions in reverse order
			Integer[] array = new Integer[delenda.size()];
			delenda.toArray( array );
			Arrays.sort( array );
			for ( int i=array.length-1;i>=0;i-- )
				removeVersion( array[i].intValue() );
		}
	}
     /**
     * For comparison we need the next version after one we specify
     * @param v1 the first version
     * @return the next version as a default comparison
     */
    public int getNextVersionId( short v1 )
    {
        if ( v1 < versions.size() )
            return v1+1;
        else if ( versions.size()>1 )
            return 1;
        else
            return v1;
    }
	/**
	 * Get the long name for the given version
	 * @param versionId the id of the version
	 * @return its long name
	 */
	public String getLongNameForVersion( int versionId )
	{
		Version v = versions.get( versionId-1 );
		if ( v != null )
			return v.longName;
		else
			return "";
	}
	/**
	 * Get the version contents by its short name
	 * @param shortName the shortName identifying the version
	 * @return the id of that version
	 */
	int getVersionByShortName( String shortName )
	{
		int version = -1;
		for ( int i=0;i<versions.size();i++ )
		{
			Version v = versions.get( i );
			if ( v.shortName.equals(shortName) )
			{
				version = i;
				break;
			}
		}
		return version+1;
	}
	/**
	 * Get an array of Version ids of a given group. If request is 
	 * for a group that contains other groups, get the versions for 
	 * that group recursively.
	 * @param group the group or TOP_LEVEL - get all the versions of 
	 * this group and its descendants
	 * @return an array of version ids
	 */
	public int[] getVersionsForGroup( short group )
	{
		HashSet<Short> descendants = new HashSet<Short>();
		if ( group != Group.TOP_LEVEL )
			descendants.add( group );
		getDescendantsOfGroup( group, descendants );
		ArrayList<Integer> chosen = new ArrayList<Integer>();
		for ( int i=0;i<versions.size();i++ )
		{
			Version v = versions.get( i );
			Short vGroup = new Short( v.group );
			if ( descendants.contains(vGroup) )
				chosen.add( i+1 );
		}
		int[] selectedVersions = new int[chosen.size()];
		for ( int i=0;i<chosen.size();i++ )
			selectedVersions[i] = chosen.get(i).intValue();
		return selectedVersions;
	}
	/**
	 * Get all the direct descendants of a group 
	 * @param group the parent group to check for descendants
	 * @param descendants a set containing the ids of the descendants 
	 * to be filled in
	 */
	private void getDescendantsOfGroup( short group, 
		HashSet<Short> descendants )
	{
		for ( int i=0;i<groups.size();i++ )
		{
			Group g = groups.get( i );
			if ( group == g.parent )
			{
				short localGroup = (short)(i+1);
				descendants.add( localGroup );
				getDescendantsOfGroup( localGroup, descendants );
			}
		}
	}
	/**
	 * Get the id of the highest version in the MVD
	 * @return a version ID
	 */
	int getHighestVersion()
	{
		return versions.size();
	}
    /**
     * Build a prefix for a particular group
     * @param groupId the id of the group
     * @return a string representing the nested group
     */
    private String composeGroupPrefix( short groupId )
    {
        StringBuilder sb = new StringBuilder();
        short gID = groupId;
        while ( gID != Group.TOP_LEVEL )
        {
            Group g = groups.get( groupId-1 );
            if ( gID == groupId )
                sb.insert( 0, g.name );
            else
                sb.insert( 0, "\t" );
            gID = g.getParent();
        }
        return sb.toString();
    }
    /**
     * How many tabs are there in a string?
     * @param key the key string to search
     * @return the number of tabs you found
     */
    int countTabs( String key )
	{
	    int nTabs = 0;
	    int index = 0;
	    index = key.indexOf("\t",index);
	    while ( index != -1 && index < key.length() )
	    {
	        nTabs++;
	        index = key.indexOf("\t",index+1);
	    }
	    return nTabs;
	}
    /**
     * Get a plain text representation of the groups and versions.
     * Each version is on a separate line, short names separated from 
     * long names via a tab. If not top-level each version is preceded 
     * by a tab or its group name or names, Each group-name is written 
     * exactly once
     * @return the version table including group names and description
     */
    public String getVersionTable()
    {
        HashMap<String,ArrayList<String>> map 
            = new HashMap<String,ArrayList<String>>();
        // compose group keys and store versions in map
        for ( int i=0;i<versions.size();i++ )
        {
            Version v = versions.get( i );
            String prefix = composeGroupPrefix( v.group );
            ArrayList<String> vList;
            if ( !map.containsKey(prefix) )
            {
                vList = new ArrayList<String>();
                map.put( prefix, vList );
            }
            else
                vList = map.get( prefix );
            StringBuilder sb = new StringBuilder();
            sb.append( v.shortName );
            sb.append( "\t" );
            sb.append( v.longName );
            vList.add( sb.toString() );
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append( description );
        sb2.append( "\n" );
        Set<String> keys = map.keySet();
        // add in the top-level names even though not in map
        ArrayList<String> topLevel = map.get("");
        int nTopLevel = 0;
        if ( topLevel != null )
            nTopLevel = topLevel.size();
        String[] groupNames = new String[keys.size()+nTopLevel];
        keys.toArray(groupNames);
        if ( topLevel != null )
            for ( int i=0;i<topLevel.size();i++ )
                groupNames[keys.size()+i] = topLevel.get(i);
        Arrays.sort(groupNames);
        for ( int k=0;k<groupNames.length;k++ )
        {
            String key = groupNames[k];
            if ( key.length()==0 )
                continue;
            ArrayList<String> list = map.get( key );
            // if the list is null it's a top level name
            if ( list == null )
            {
                sb2.append("top");
                sb2.append( "\t" );
                sb2.append( key );
                sb2.append( "\n" );
            }
            else
            {
                sb2.append( key );
                sb2.append( "\t" );
                sb2.append( list.get(0) );
                sb2.append( "\n" );
                for ( int i=1;i<list.size();i++ )
                {
                    int nTabs = countTabs(key) + 1;
                    for ( int j=0;j<nTabs;j++ )
                        sb2.append( "\t" );
                    sb2.append( list.get(i) );
                    sb2.append( "\n" );
                }
            }
        }
        return sb2.toString();
    }
	/**
	 * Return a readable printout of all the versions in the MVD.
	 * @param indent the amount to indent the outermost group
	 * @param gId the id of the group whose contents are desired
	 * @return the contents of the group in XML
	 * @throws MVDException if the group was not found
	 */
	public String getContentsForGroup( int indent, short gId ) 
		throws MVDException
	{
		StringBuffer sb = new StringBuffer();
		// write group start tag
		for ( int i=0;i<indent;i++ )
			sb.append( " " );
		Group g1 = (gId != 0)?groups.get(gId-1):new Group((short)-1, 
			"top level" );
		if ( g1 == null )
			throw new MVDException("group id "+gId+" not found!");
		sb.append("<group name=\""+g1.name+"\" id=\""+gId+"\"");
		if ( gId != 0 )
			sb.append(" parent=\""+g1.parent+"\"");
		sb.append( ">\n" );
		// check for sub-groups
		for ( short i=0;i<groups.size();i++ )
		{
			Group g = groups.get( i );
			if ( g.parent == gId )
				sb.append( getContentsForGroup(indent+2,
					(short)(i+1)) );
		}
		// get sub-versions
		for ( short i=0;i<versions.size();i++ )
		{
			Version v = versions.get( i );
			if ( v.group == gId )
				sb.append( v.toXML(indent+2,i+1) );
		}
		// write group end tag
		for ( int i=0;i<indent;i++ )
			sb.append( " " );
		sb.append("</group>");
		sb.append("\n");
		return sb.toString();
	}
	/**
	 * Return a printout of all the versions in the MVD.
	 * @return the descriptions as a String array
	 */
	public String[] getVersionDescriptions()
	{
		String[] descriptions = new String[versions.size()];
		for ( int id=1,i=0;i<versions.size();i++,id++ )
		{
			Version v = versions.get(i);
			descriptions[i] = v.toString()+";id:"+id;
		}
		return descriptions;
	}
	/**
	 * Return a printout of all the groups in the MVD.
	 * @return the descriptions as a String array
	 */
	public String[] getGroupDescriptions()
	{
		String[] descriptions = new String[groups.size()];
		for ( int id=1,i=0;i<groups.size();i++,id++ )
		{
			Group g = groups.get(i);
			descriptions[i] = g.toString()+";id:"+id;
		}
		return descriptions;
	}
	/**
	 * Retrieve a version, copying it from the MVD
	 * @param version the version to retrieve
	 * @return a char array containing all the data of that version
	 */
	public char[] getVersion( int version )
	{
		int length = 0;
		// measure the length
		for ( int i=0;i<pairs.size();i++ )
		{
			Pair p = pairs.get( i );
			if ( p.versions.nextSetBit(version)==version )
			{
				length += p.length();
			}
		}
		char[] result = new char[length];
		// now copy it
		int k,i;
		for ( k=0,i=0;i<pairs.size();i++ )
		{
			Pair p = pairs.get( i );
			if ( p.versions.nextSetBit(version)==version )
			{
				for ( int j=0;j<p.length();j++ )
					result[k++] = p.getChars()[j];
			}
		}
		return result;
	}
    /**
     * Get the version id of the named shortname and group-path
     * @param shortName the possibly ambiguous short name of the version
     * @param path to the group containing that version
     * @return the version id, 1-based or 0 if not found
     */
    public int getVersionByNameAndGroup( String shortName, String path )
    {
        StringTokenizer st = new StringTokenizer( path, "/" );
        short groupId = 0;
        int vId = 0;
        while ( st.hasMoreTokens() )
        {
            String groupName = st.nextToken();
            for ( short i=0;i<groups.size();i++ )
            {
                Group g = groups.get( i );
                if ( g.name.equals(groupName) && g.parent == groupId )
                {
                    groupId = (short)(i+1);
                    break;
                }
            }
        }
        // there must be at least one group
//        if ( groupId == 0 )
//            groupId = 1;
        for ( int i=0;i<versions.size();i++ )
        {
            Version v = versions.get(i);
            if ( v.shortName.endsWith(shortName) && v.group == groupId )
            {
                vId = i+1;
                break;
            }
        }
        return vId;
    }
	/*
	 * Find out the version id from the version's long name
	 * @param longName the long name of the desired version
	 * @return the version id or -1
	 */
	public short getVersionByLongName( String longName )
	{
		for ( int i=0;i<versions.size();i++ )
		{
			Version vi = versions.get( i );
			if ( longName.equals(vi.longName) )
				return (short)(i+1);
		}
		return -1;
	}
	/**
	 * Get a version's long name
	 * @param id the id of the version
	 * @return the long name of the version
	 */
	public String getVersionLongName( int id )
	{
		Version v = versions.get( id-1 );
		return v.longName;
	}
	/**
	 * Get a version's short name
	 * @param id the id of the version
	 * @return the short name of the version
	 */
	public String getVersionShortName( int id )
	{
		Version v = versions.get( id-1 );
		return v.shortName;
	}
	/**
	 * For each version in the MVD calculate its length in chars
	 * @return an array of lengths where each index represents 
	 * one version id-1 and the values are the lengths of that 
	 * version
	 */
	public int[] getVersionLengths()
	{
		int[] lengths = new int[versions.size()];
		for ( int i=0;i<pairs.size();i++ )
		{
			Pair p = pairs.get( i );
			BitSet bs = p.versions;
			for ( int j=bs.nextSetBit(1);j>=0;j=bs.nextSetBit(j+1) ) 
			{
				lengths[j-1] += p.length();
			}
		}
		return lengths;
	}
	/**
	 * Get the size of the data required in bytes to store this MVD
	 * @return the byte-size of the serialised mvd
	 * @throws UnsupportedEncodingException
	 */
	int dataSize() throws UnsupportedEncodingException
	{
		headerSize = groupTableSize = versionTableSize = 
			pairsTableSize = dataTableSize = 0;
		// header
		headerSize = MVDFile.MVD_MAGIC.length; // magic
		headerSize += 5 * 4; // table offsets etc
		/*try
		{
			MVDError.log( this.toString() );
		}
		catch ( Exception e )
		{
		}*/
		headerSize += measureUtf8String( description );
		headerSize += measureUtf8String( encoding );
		groupTableSize = 2; // number of groups
		for ( int i=0;i<groups.size();i++ )
		{
			Group g = groups.get( i );
			groupTableSize += g.dataSize();
		}
		versionTableSize = 2 + 2; // number of versions + setSize
		for ( int i=0;i<versions.size();i++ )
		{
			Version v = versions.get( i );
			versionTableSize += v.dataSize();
		}
		pairsTableSize = 4;	// number of pairs
		versionSetSize = (versions.size()+8)/8;
		for ( int i=0;i<pairs.size();i++ )
		{
			Pair p = pairs.get( i );
			pairsTableSize += p.pairSize(versionSetSize);
			dataTableSize += p.dataSize();
		}
		return headerSize + groupTableSize + versionTableSize 
			+ pairsTableSize + dataTableSize;
	}
	/**
	 * Serialise the entire mvd into the given byte array. Must 
	 * be preceded by a call to dataSize (otherwise no way to 
	 * calculate size of data).
	 * @param data a byte array of exactly the right size
	 * @return the number of serialised bytes
	 * @throws an Exception if data was the wrong size
	 */
	int serialise( byte[] data ) throws Exception
	{
		int nBytes = serialiseHeader( data );
		int p = headerSize;
		nBytes += serialiseGroups( data, p );
		p += groupTableSize;
		nBytes += serialiseVersions( data, p );
		p += versionTableSize;
		nBytes += serialisePairs( data, p, p+pairsTableSize );
		return nBytes;
	}
	/**
	 * Serialise the header starting at offset 0 in the data byte 
	 * array
	 * @param data the byte array to write to
	 * @return the number of serialised bytes
	 */
	private int serialiseHeader( byte[] data ) throws Exception
	{
		int nBytes = 0;
		if ( data.length >= headerSize )
		{
			for ( int i=0;i<MVDFile.MVD_MAGIC.length;i++ )
				data[i] = MVDFile.MVD_MAGIC[i];
			int p = 0;
			nBytes = MVDFile.MVD_MAGIC.length;
			// mask type - redundant
			writeInt( data, p+nBytes, 0 );
			nBytes += 4;
			// groupTableOffset
			writeInt( data, p+nBytes, headerSize );
			nBytes += 4;
			// versionTableOffset
			writeInt( data, p+nBytes, headerSize+groupTableSize );
			nBytes += 4;
			// pairsTableOffset
			writeInt( data, p+nBytes, headerSize
				+groupTableSize+versionTableSize );
			nBytes += 4;
			// dataTableOffset
			writeInt( data, p+nBytes, headerSize
				+groupTableSize+versionTableSize
				+pairsTableSize );
			nBytes += 4;
			nBytes += writeUtf8String( data, p+nBytes, description );
			nBytes += writeUtf8String( data, p+nBytes, encoding );
		}
		else
			throw new MVDException("No room for MVD header");
		return nBytes;
	}
	/**
	 * Serialise the pairs table starting at offset p in the data byte 
	 * array. Don't serialise the data they refer to yet. Since parents 
	 * and children may come in any order we have to keep track of orphaned 
	 * children or parents without children, and then join them up when we 
	 * can.
	 * @param data the byte array to write to
	 * @param p the offset within data to start writing
	 * @param dataTableOffset the offset to the start of the dataTable 
	 * within data
	 * @return the number of serialised bytes
	 */
	private int serialisePairs( byte[] data, int p, int dataTableOffset ) 
		throws Exception
	{
		int nBytes = 0;
		HashMap<Pair,Integer> ancestors = new HashMap<Pair,Integer>();
		HashMap<Pair,Integer> orphans = new HashMap<Pair,Integer>();
		if ( p + pairsTableSize <= data.length )
		{
			writeInt( data, p, pairs.size() );
			p += 4;
			nBytes += 4;
			// where we're writing the actual data
			int dataOffset = 0;
			int parentDataOffset = 0;
			int parentId = 1;
			for ( int i=0;i<pairs.size();i++ )
			{
				// this is set if known
				int tempPId = NULL_PID;
				Pair t = pairs.get( i );
				if ( t.isChild() )
				{
					// Do we have a registered parent?
					Integer value = ancestors.get( t.parent );
					// value is the parent's data offset
					if ( value != null )
					{
						parentDataOffset = value.intValue();
						tempPId = t.parent.id;
					}
					else
					{
						// the value in orphans is the offset 
						// pointing to the orphan pair entry
						orphans.put( t, new Integer(p) );
						// clearly duff value: fill this in later
						tempPId = DUFF_PID;
					}
				}
				else if ( t.isParent() )
				{
					// first assign this parent an id
					tempPId = t.id = parentId++;
					// then put ourselves in the ancestors list
					ancestors.put( t, dataOffset );
					// now check if we have any registered orphans
					ListIterator<Pair> iter = t.getChildIterator();
					while ( iter.hasNext() )
					{
						Pair child = iter.next();
						Integer value = orphans.get( child );
						if ( value != null )
						{
							// copy the parent's data offset 
							// into that of the child
							Pair.fixDataOffset( data, value.intValue(), 
								dataOffset, versionSetSize, t.id );
							// remove the child from the orphan list
							orphans.remove( child );
						}
					}
				}
				// if we set the parent data offset use that
				// otherwise use the current pair's data offset
				nBytes += t.serialisePair( data, p, versionSetSize, 
					(parentDataOffset!=0)?parentDataOffset:dataOffset, 
					dataTableOffset, tempPId );
				p += t.pairSize( versionSetSize );
				dataOffset += t.dataSize();
				parentDataOffset = 0;
			}
			if ( !orphans.isEmpty() )
			{
				Set<Pair> keys = orphans.keySet();
				Iterator<Pair> iter = keys.iterator();
				while ( iter.hasNext() )
				{
					Pair q = iter.next();
					if ( !ancestors.containsKey(q) )
						System.out.println("No matching key for pair");
				}
				throw new MVDException("Unmatched orphans after serialisation");
			}
		}
		else
			throw new MVDException( "No room for pairs table" );
		return nBytes;
	}
	/**
	 * Serialise the groups table starting at offset p in the data byte 
	 * array
	 * @param data the byte array to write to
	 * @param p the offset within data to start writing
	 * @return the number of serialised bytes
	 */
	private int serialiseGroups( byte[] data, int p ) throws Exception
	{
		int oldP = p;
		if ( p + groupTableSize < data.length )
		{
			writeShort( data, p, (short)groups.size() );
			p += 2;
			for ( int i=0;i<groups.size();i++ )
			{
				Group g = groups.get( i );
				g.serialise( data, p );
				p += g.dataSize();
			}
		}
		else
			throw new MVDException( "No room for group table" );
		return p - oldP;
	}
	/**
	 * Serialise the versions table starting at offset p in the data 
	 * byte array
	 * @param data the byte array to write to
	 * @param p the offset within data to start writing
	 * @return the number of serialised bytes
	 */
	private int serialiseVersions( byte[] data, int p ) throws Exception
	{
		int oldP = p;
		if ( p + versionTableSize < data.length )
		{
			if ( versions.size() < 0 )
				throw new MVDException( "at least one version needed" );
			writeShort( data, p, (short)versions.size() );
			p += 2;
			writeShort( data, p, (short)versionSetSize );
			p += 2;
			for ( int i=0;i<versions.size();i++ )
			{
				Version v = versions.get( i );
				v.serialise( data, p );
				p += v.dataSize();
			}
		}
		else
			throw new MVDException( "No room for group table" );
		return p - oldP;
	}
	/**
	 * Get the variants as in an apparatus. The technique is to reconstruct 
	 * just enough of the variant graph for a given range of pairs to determine 
	 * what the variants of a given base text are.
	 * @param base the base version
	 * @param offset the starting offset in that version
	 * @param len the length of the range to compute variants for
	 * @return an array of Variants
	 * @throws MVDException
	 */
	public Variant[] getApparatus( short base, int offset, int len ) 
		throws MVDException
	{
        int index = next( 0, base );
        if ( index != Integer.MAX_VALUE )
        {
            PairPos sPos = new PairPos( index, 0, base );
            PairPos first = getPairPos( sPos, offset );
            PairPos last = getPairPos( sPos, len );
            int firstIndex = extendFirst( first.getIndex() );
            int lastIndex = extendLast( last.getIndex() );
            return getVariants( firstIndex, lastIndex, base );
        }
        else 
            return new Variant[0];
    }
    /**
     * Get the new-style version id full path
     * @param id the numeric id starting at 1
     * @return a path starting with a slash
     */
    public String getVersionId( short id )
    {
        return getGroupPath(id)+"/"+getVersionShortName(id);
    }
    /**
     * Get the versions of a range 
     * @param base the version to look in
     * @param offset the start offset of the range
     * @param len the lenght of the range
     * @return a bitset of version ids
     */
    public BitSet getVersionSetOfRange( short base, int offset, int len )
    {
        try
        {
            PairPos start = new PairPos(next(0,base),0,base);
            PairPos sPos = getPairPos(start,offset);
            PairPos ePos = getPairPos(sPos,offset+len);
            BitSet bs = new BitSet();
            bs.or( getPair(sPos.index).versions );
            for ( int i=sPos.index;i<=ePos.index;i+=next(i+1,base) )
            {
                bs.and(getPair(i).versions);
            }
            return bs;
        }
        catch ( Exception e )
        {
            return null;
        }
    }
    /**
     * Get the versions that share a given range
     * @param base the base version of the range
     * @param offset the offset into base
     * @param len the length of the range
     * @return an array of vids that share all the chars of the range
     */
    public String[] getVersionsOfRange( short base, int offset, int len )
    {
        try
        {
            BitSet bs = getVersionSetOfRange( base, offset, len );
            if ( bs != null )
            {
                String[] vids = new String[bs.cardinality()];
                for ( int j=0,i=bs.nextSetBit(1);i>=0;i=bs.nextSetBit(i+1) ) 
                {
                    vids[j++] = getGroupPath((short)i)+"/"+getVersionShortName(i);
                }
                return vids;
            }
            else
                return new String[0];
        }
        catch ( Exception e )
        {
            return new String[0];
        }
    }
    /**
     * Get the variants of the base version between two endpoints
     * Filter the list of pairs between and including START..END.
	 * a) compute the FIRST set of versions leaving the node that precedes 
     * START. This will be the versions of all pairs up until the next node, 
     * plus the versions of any hint in the node. If FIRST is the first pair, 
     * set FIRST to the set of all versions.
	 * b) compute the SECOND set of versions of all pairs following END up 
     * until the next node, plus any versions of the hint included in the 
     * node defined by END. If END is the last pair then set SECOND to the 
     * set of all versions.
	 * c) compute the intersection VARIANTS of FIRST and SECOND.
	 * d) for each pair between and including START...END:
	 *	1. clear the base version from the pair. If it is now empty, drop it.
	 *	2. If the list of variants is empty, create a variant out of the pair 
     * and add it to the list
	 *	3. else go though the list of variants
	 *		a) if the pair's versions are equal to the variant, append it to 
     * that Variant.
	 *		b) if the pair's versions are a superset of the variant's versions, 
     * append the pair to the variant and clear those versions from the pair.
	 *		c) if the pair's versions are a subset of the variants's versions, 
     * split the variant into two. To the half that contains the versions of 
     * the new pair, append the pair. break.
	 * e) Compare each variant against each other variant. If two match, merge 
     * them.
	 * f) extend each variant to the next word-boundary (white space or 
     * punctuation)
     * @param first the first index into pairs
     * @param last the last index into pairs
     * @param base the base version to make variants of
     * @return an array of variants
     */
    Variant[] getVariants( int first, int last, short base )
    {
        BitSet bs1 = getVersionsTrailing( first );
        BitSet bs2 = getVersionsLeading( last );
        bs1.and( bs2 );
        ArrayList<Variant> variants = new ArrayList<Variant>();
        // now bs1 holds the intersection
        for ( int i=first;i<=last;i++ )
        {
            Pair p = pairs.get( i );
            BitSet bs = new BitSet();
            bs.or( p.versions );
            bs.clear( base );
            if ( bs.isEmpty() )
                continue;
            else if ( variants.isEmpty() )
            {
                Variant v = new Variant( 0, i, i, p.length(), bs, this );
                variants.add( v );
            }
            else
            {
                BitSet pVersions = (BitSet)p.versions.clone();
                for ( int j=0;j<variants.size();j++ )
                {
                    Variant v = variants.get(j);
                    if ( v.versions.equals(pVersions) )
                    {
                        v.endIndex = i;
                        v.length += p.length();
                        break;
                    }
                    else if ( isSubset(v.versions,pVersions) )
                    {
                        pVersions.andNot( v.versions );
                        v.endIndex = i;
                        v.length+= p.length();
                    }
                    else if ( isSubset(pVersions,v.versions) )
                    {
                        Variant v2 = v.split( pVersions );
                        v2.endIndex = i;
                        v2.length += p.length();
                        variants.add( v2 );
                        break;
                    }
                }
            }
        }
        // compare each variant to each other variant
        int i=0;
        while ( i<variants.size() )
        {
            Variant v1 = variants.get( i );
            v1.findContent();
            int j = 0;
            while ( j<variants.size() )
            {
                Variant v2 = variants.get( j );
                v2.findContent();
                if ( v1.equalsContent(v2) )
                    v1.merge( v2 );
                variants.remove( j );
                j++;
            }
            i++;
        }
        // extend them to the next word boundary
        for ( i=0;i<variants.size();i++ )
        {
            Variant v = variants.get( i );
            wordExtendBackwards( v );
            wordExtendForwards( v );
        }
        Variant[] varray = new Variant[variants.size()];
        variants.toArray( varray );
        return varray;
    }
    /**
     * In utf-8 general punctuation is represented by 8-bit characters.
     * Determine if the offered code is of that type
     * @param datum
     * @return true if it is general punctuation, else false
     */
    boolean isPunctuation( char datum )
    {
        int type = Character.getType(datum);
        return type == Character.END_PUNCTUATION
                || type==Character.DASH_PUNCTUATION
                || type==Character.FINAL_QUOTE_PUNCTUATION
                || type==Character.INITIAL_QUOTE_PUNCTUATION
                || type==Character.START_PUNCTUATION;
    }
    /**
     * Extend a variant to the next word boundary to the left and right
     * @param v the variant to extend
     */
    void wordExtendBackwards( Variant v )
    {
        char pc;
        int index = v.startIndex;
        int offset = v.startOffset;
        Pair p = pairs.get( index );
        do
        {
            offset--;
            while ( offset < 0 || p.length() == 0 )
            {
                index = previous( index,(short)v.versions.nextSetBit(1) );
                if ( index >= 0 )
                {
                    p = pairs.get( index );
                    offset = p.length()-1;
                }
                else
                    break;
            }
            // offset points to the previous character
            pc = p.getChars()[offset];
            if ( Character.isWhitespace((char)pc)||isPunctuation(pc) )
                break;
            else
            {
                v.startIndex = index;
                v.startOffset = offset;
                v.length++;
            }
        }
        while ( index >= 0 );
    }
    /**
     * Extend a variant to the next word boundary to the left and right
     * @param v the variant to extend
     */
    void wordExtendForwards( Variant v )
    {
        char pc;
        Pair p;
        int index = v.endIndex;
        int offset = v.length;
        for ( int i=v.startIndex;i<=v.endIndex;i++ )
        {
            p = pairs.get( i );
            if ( offset >= p.length() )
                offset -= p.length();
        }
        p = pairs.get( index );
        do
        {
            offset++;
            while ( offset >= p.length() )
            {
                index = next( index,(short)v.versions.nextSetBit(1) );
                if ( index >= 0 )
                {
                    p = pairs.get( index );
                    offset = 0;
                }
                else
                    break;
            }
            pc = p.getChars()[offset];
            if ( Character.isWhitespace((char)pc)||isPunctuation(pc) )
                break;
            else
            {
                v.endIndex = index;
                v.length++;
            }
        }
        while ( index >= 0 );
    }
    boolean isSubset( BitSet bs1, BitSet bs2 )
    {
        for ( int i = bs1.nextSetBit(0);i>=0;i=bs1.nextSetBit(i+1) ) 
            if ( bs2.nextSetBit(i)!=i )
                return false;
        return true && bs2.cardinality()>bs1.cardinality();
    }
    BitSet getVersionsLeading( int index )
    {
        BitSet bs = new BitSet();
        if ( index == pairs.size()-1 )
        {
            for ( int i=0;i<versions.size();i++ )
                bs.set( i+1 );
        }
        else
        {
            Pair p1 = pairs.get( index );
            Pair p2 = pairs.get( index+1 );
            bs.or( p1.versions );
            bs.or( p2.versions );
            for ( int i=index+1;i<pairs.size();i++ )
            {
                Pair q2 = pairs.get( i );
                Pair q1 = pairs.get( i-1 );
                if ( q2.isHint() || q2.versions.intersects(q1.versions) )
                    break;
                bs.or( q2.versions );
            }
        }
        return bs;
    }
    /**
     * Get the versions of a node defined by its trailing arc
     * @param index the index of that arc
     * @return a bitset of the node's full versions
     */
    BitSet getVersionsTrailing( int index )
    {
        BitSet bs = new BitSet();
        if ( index == 0 )
        {
            for ( int i=0;i<versions.size();i++ )
                bs.set( i+1 );
        }
        else
        {
            Pair p1 = pairs.get( index );
            Pair p2 = pairs.get( index-1 );
            bs.or( p1.versions );
            bs.or( p2.versions );
            if ( p2.isHint() )
            {
                Pair p3 = pairs.get( index-2 );
                bs.or( p3.versions );
            }
            for ( int i=index+1;i<pairs.size();i++ )
            {
                Pair q2 = pairs.get( i );
                Pair q1 = pairs.get( i-1 );
                if ( q2.isHint() || q2.versions.intersects(q1.versions) )
                    break;
                bs.or( q2.versions );
            }
        }
        return bs;
    }
    /**
     * Move the given first pair backwards to first outgoing arc of 
     * the node it attaches to
     * Algorithm:
     * go backwards one pair at a time from the first pair:
	 * a) if you reach pair 0 then set START to 0 and break
	 * b) if you encounter a hint 
	 *	1. if it intersects with the first-pair then set START to the index 
     * of the pair after the hint and break
	 *  2. else ignore it
	 * c) if you encounter a pair that intersects with the first-pair 
	 *	1. if it's part of a node, set START to the index of the following 
     * pair and break
	 *	2. else set START to the index of the DEFAULT_NODE and break (must 
     * be set)
	 * d) if you encounter a pair that intersects with the previous pair or 
     * precedes a hint then compute the union of versions of pairs immediately 
     * attached to it. 
	 *	1. If first-pair intersects with that set then set START to the index 
     * of the first outgoing pair of the node and break
	 *	2. Else set the DEFAULT_NODE to the index of the first outgoing 
     * pair and continue
	 * e) else ignore the pair
     * @param first the initial index of the first pair
     * @return the final index of the pair
     */
    int extendFirst( int first )
    {
        int start = first;
        int defaultNode = -1;
        Pair p1 = pairs.get( first );
        for ( int i=first-1;i>=0;i-- )
        {
            if ( i == 0 )
                start = 0;
            else
            {
                Pair p = pairs.get( i );
                if ( p.isHint() )
                {
                    if ( p.versions.intersects(p1.versions) )
                    {
                        start = i+1;
                        break;
                    }
                }
                else if ( p.versions.intersects(p1.versions) )
                {
                    Pair p2 = pairs.get(i+1);
                    if ( p2.isHint() )
                    {
                        start = i+2;
                        break;
                    }
                    else if ( p2.versions.intersects(p.versions) )
                    {
                        start = i+1;
                        break;
                    }
                    else if ( defaultNode != -1 )
                    {
                        start = defaultNode;
                        break;
                    }
                    else
                    {
                        // can't happen unless error
                        System.out.println("Default node not found.");
                    }
                }
                else 
                {
                    Pair p2 = pairs.get( i+1 );
                    if ( p2.versions.intersects(p.versions)||p2.isHint() )
                    {
                        BitSet bs = new BitSet();
                        int outgoing = i+1;
                        bs.or( p.versions );
                        bs.or( p2.versions );
                        if ( p2.isHint() )
                        {
                            Pair p3 = pairs.get( i+2 );
                            bs.or( p3.versions );
                            outgoing = i+2;
                        }
                        if ( bs.intersects(p1.versions) )
                        {
                            start = outgoing;
                            break;
                        }
                        else
                            defaultNode = outgoing;
                    }
                }
            }
        }
        return start;
    }
    /**
     * Move the last index to the incoming arc of the node to which
     * the current last arc attaches as ingoing.
     * Algorithm:
     * go forwards one pair at a time from the pair following the second-pair:
	 * a) if you reach the last pair set END to its index and break
	 * b) if you reach a pair that intersects with a previous ordinary pair
	 *	1. if the union of the versions of the two pairs intersects with that 
     * of the second-pair then set END to the index of the first of those two 
     * pairs and break
	 *	2. if second-pair doesn't intersect, create a node, add the leading 
     * and trailing pairs to it. Add the node to a list of NODES.
	 * c) if you reach a hint, compute the set of versions defined by the 
     * hinted node. 
	 *	1. If that intersects with second-pair, then set END to the leading 
     * non-hint pair's index and break
	 *	2. If not, then define a node including the hint and add it to the 
     * NODES list. 
	 * d) if the pair intersects with second-pair find the node in NODES that 
     * it attaches to. If none, attach it to the last node in NODES. Set END 
     * to the index of the incoming pair of that node and break.
	 * e) else ignore the pair
     * @param last initial last index of the range
     * @return updated last index
     */
    int extendLast( int last )
    {
        int end = last;
        ArrayList<CompactNode> nodes = new ArrayList<CompactNode>();
        Pair p1 = pairs.get( last );
        for ( int i=last+1;i<pairs.size()-1;i++ )
        {
            if ( i == pairs.size()-1 )
                end = pairs.size()-1;
            else
            {
                Pair p = pairs.get( i );
                Pair p3=null,p2 = pairs.get( i-1 );
                if ( p.isHint()||p1.versions.intersects(p2.versions) )
                {
                    BitSet bs = new BitSet();
                    bs.or( p.versions );
                    bs.or( p2.versions );
                    if ( p.isHint() )
                    {
                        p3 = pairs.get( i+1 );
                        bs.or( p3.versions );
                    }
                    if ( p1.versions.intersects(bs) )
                    {
                        end = i-1;
                        break;
                    }
                    else
                    {
                        CompactNode cn = new CompactNode(i-1);
                        cn.addOutgoing( p );
                        cn.addIncoming( p2 );
                        if ( p.isHint() )
                            cn.addOutgoing( p3 );
                        nodes.add( cn );
                    }
                }
                else if ( p.versions.intersects(p1.versions) )
                {
                    boolean found = false;
                    for ( int j=nodes.size()-1;j>=0;j-- )
                    {
                        CompactNode cn = nodes.get( j );
                        if ( cn.getWantsOutgoing().intersects(p.versions) )
                        {
                            end = cn.getIndex();
                            found = true;
                        }
                    }
                    if ( !found )
                        end = nodes.get(nodes.size()-1).getIndex();
                }
            }
        }
        return end;
    }
    /**
	 * Get the end-position of a range given a start position
	 * @param sPos the start position
	 * @param distance the distance from sPos within its base
	 * @return the the pair pos for sPos.
	 */
	PairPos getPairPos( PairPos sPos, int distance )
    {
        int i = sPos.getIndex();
        // position with distance
        int pos = -sPos.getPosition();
        PairPos ePos = null;
        Pair p;
        do
        {
            p = pairs.get( i );
            if ( pos+p.length() < distance )
            {
                pos += p.length();
                int oldi = i;
                i = next( i+1, sPos.getBase() );
                if ( i == Integer.MAX_VALUE )
                {
                    ePos = new PairPos( oldi, p.length(), sPos.getBase() );
                    break;
                }
            }
            else
            {
                ePos = new PairPos( i, distance-pos, sPos.getBase() );
                break;
            }
        } while ( pos < distance );
        return ePos;
    }
	/**
	 * Write MVD to a string
	 */
    @Override
	public String toString()
	{
		try
        {
            return MVDFile.externalise( this );
        }
        catch ( Exception e )
        {
            return "";
        }
	}
	/**
	 * Compute a difference matrix, suitable for inputting into 
	 * fitch, kitsch or neighbor programs in Phylip. Compute a simple
	 * sum of squares between all possible pairs of versions in the MVD
	 * such that equal characters are scored as 0, variants as 1 for 
	 * each character of the longest of the two variants, 1 for each 
	 * entire transposition, thus scaling them for length (a 10-char 
	 * transposition costs half that of a 5-char one). Having calculated 
	 * that in a matrix of nVersions x nVersions, divide by the length
	 * of the longest version in each case - 1.
	 * @return a 2-D matrix of differences.
	 */
	public double[][] computeDiffMatrix( )
	{
		// ignore 0th element to simplify indexing
		int s = versions.size()+1;
		// keep track of the length of each version
		int[] lengths = new int[s];
		// the length of j last time j and k were joined
		int[][] lastJoinJ = new int[s][s];
		// the length of k last time j and k were joined
		int[][] lastJoinK = new int[s][s];
		// the cost is the longest distance between any two 
		// versions since they were last joined
		int[][] costs = new int[s][s];
		for ( int i=0;i<pairs.size();i++ )
		{
			Pair p = pairs.get( i );
			// consider each combination of j and k, including j=k
			for ( int j=p.versions.nextSetBit(1);j>=1;j=p.versions.nextSetBit(j+1) )
			{
				for ( int k=p.versions.nextSetBit(j);k>=1;k=p.versions.nextSetBit(k+1) )
				{
					costs[j][k] += Math.max(
						lengths[j]-lastJoinJ[j][k],
						lengths[k]-lastJoinK[j][k]);
					costs[k][j] = costs[j][k];
					lastJoinJ[j][k] = lengths[j] + p.length();
					lastJoinK[j][k] = lengths[k] + p.length();
				}
				lengths[j] += p.length();
			}
		}
		double[][] diffs = new double[s-1][s-1];
		for ( int i=1;i<s;i++ )
		{
			for ( int j=1;j<s;j++ )
			{
				// normalise by the longer of the two lengths -1
				double denominator = Math.max(lengths[i],lengths[j])-1;
				diffs[i-1][j-1] = ((double)costs[i][j]) / denominator;
			}
		}
		return diffs;
	}
    String getShortNames( BitSet bs )
    {
        StringBuilder sb = new StringBuilder();
        for (int i=bs.nextSetBit(0);i>=0;i=bs.nextSetBit(i+1) )
        {
            Version v = versions.get(i-1);
            sb.append( v.shortName );
            if ( i<bs.cardinality() )
                sb.append(" ");
        }
        return sb.toString();  
    }
    /**
     * Convert a version specification, which is a set of version short names, 
     * optionally qualified by their group path, e.g. MS10/layer1, OR the word 
     * "all", which means all versions.
     * @param spec the version spec
     * @return a bitset with all specified versions set
     */
    public BitSet convertVersions( String spec )
    {
        BitSet set = new BitSet();
        if ( spec.toLowerCase().equals("all") )
        {
            for ( int i=1;i<=versions.size();i++ )
                set.set(i);
        }
        else
        {
            HashMap<String,Short> map = new HashMap<String,Short>();
            for ( int i=0;i<versions.size();i++ )
            {
                Version v = versions.get( i );
                String vid = v.versionID(groups);
                map.put( vid, (short)(i+1) );
            }
            StringTokenizer st = new StringTokenizer(spec,",");
            while ( st.hasMoreTokens() )
            {
                String token = st.nextToken();
                if ( map.containsKey(token) )
                    set.set( map.get(token).shortValue() );
                else
                    System.out.println("couldn't find version "+token
                        +" (ignored)");
            }
        }
        return set;
    }
    /**
     * Reduce the versions of a Pair
     * @param original the original pair's versions
     * @param constraint a set of versions they must be within
     * @return a new BitSet, the logical AND of original AND constraint
     */
    BitSet constrainVersions( BitSet original, BitSet constraint )
    {
        BitSet bs = new BitSet();
        bs.or( original );
        bs.and( constraint );
        return bs;
    }
    /**
     * Get a set of versions corresponding to a named set
     * @param spec a comma-separated list of full version names
     * @return a set of actual versions in bitset form
     */
    BitSet getSelectedVersions( String spec )
    {
        BitSet bs;
        if ( spec != null && spec.length()>0 )
            bs = convertVersions( spec );
        else
        {
            bs = new BitSet();
            for ( int i=1;i<=versions.size();i++ )
                bs.set( i );
        }
        return bs; 
    }
    /**
     * Measure a table without turning it into HTML or JSON
     * @param base the base version
     * @return an array of section starts in base
     */
    public int[] measureTable( short base )
    {
        int[] lengths = getVersionLengths();
        int[] stats = new int[2];
        stats[0] = 0;
        stats[1] = lengths[base-1];
        return stats;
    }
    public static String versionsToString( BitSet bs )
    {
        StringBuilder sb = new StringBuilder();
        int start = -1;
        int end = -1;
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
        {
            if ( i==end+1 )
                end = i;
            else if ( start != -1 && end != -1 )
            {
                if ( sb.length()> 0 )
                    sb.append(",");
                if ( end > start )
                {
                    sb.append(start );
                    sb.append("-");
                    sb.append(end);
                }
                else
                    sb.append(end);
                start = end = i;
            }
            else
            {
                start = end = i;
            }
        }
        // coda
        if ( start != -1 && end != -1 )
        {
            if ( sb.length()> 0 )
                sb.append(",");
            if ( end > start )
            {
                sb.append(start );
                sb.append("-");
                sb.append(end);
            }
            else
                sb.append(end);
        }
        return sb.toString();
    }
    /**
     * FInd out if this set is almost aligned with the reference
     * @param set the set to test
     * @param curr the set to align against, typically all versions
     * @return true if set is nearly equal to curr, depending on its size
     */
    boolean isAlmost( BitSet set, BitSet curr )
    {
        BitSet bs = (BitSet) set.clone();
        bs.xor(curr);
        int card = curr.cardinality();
        // for 8 versions tolerance is one errant version
        // for 16: 2, for 32: 3...
        int res = card>>3;
        int tolerance = 0;
        while ( res != 0 )
        {
            tolerance++;
            res >>= 1;
        }
        return bs.cardinality()<= tolerance;
    }
    /**
     * Build a table with default options
     * @param base the base version
     * @parma bs the set of versions we are interested in
     * @param start the start offset within base
     * @param len the length of the base string to include in table
     * @return a complete table view, not yet rendered
     */
    TableView buildDefaultTableView( short base, BitSet bs, int start, int len )
    {
        EnumMap<Options,Object> options 
            = new EnumMap<Options,Object>(Options.class);
        options.put(Options.COMPACT,false);
        options.put(Options.HIDE_MERGED,false);
        options.put(Options.WHOLE_WORDS,true);
        options.put(Options.FIRST_MERGEID,0);
        options.put(Options.TABLE_ID,"json_table");
        int[] lengths = getVersionLengths();
        int[] totals = new int[lengths.length];
        TableView view = new TableView( this.versions, this.groups, base, bs, 
            options );
        int offset = 0;
        int end = start+len;
        for ( int i=0;i<=pairs.size()-1;i++ )
        {
            Pair p = pairs.get( i );
            BitSet pv = p.versions;
            if ( p.length()>0 )
            {
                if ( !pv.intersects(bs) )
                    continue;
                else 
                {
                    boolean isBase = pv.nextSetBit(base) == base;
                    if ( !isBase || offset < end )
                    {
                        BitSet set = constrainVersions(pv, bs);
                        char[] data = p.getChars();
                        // trim data
                        if ( isBase && offset+p.length()>start&& offset<end )
                        {
                            if ( offset < start )
                            {
                                int dataLen = data.length-(start-offset);
                                char[] data2 = new char[dataLen];
                                System.arraycopy( data, start-offset, data2, 0, dataLen);
                                data = data2;
                            }
                            else if ( end < offset+data.length )
                            {
                                int dataLen = (offset+data.length)-end;
                                char[] data2 = new char[dataLen];
                                System.arraycopy(data,0,data2,0,dataLen);
                                data = data2;
                            }
                        }
                        String frag = new String(data);
                        BitSet curr = view.getCurrentVersions();
                        if ( set.equals(curr) )
                        {
                            view.addFragment( FragKind.merged, offset, set, frag );
                        }
                        else if ( isAlmost(set,curr) )
                            view.addFragment( FragKind.almost, offset, set, frag );
                        else if ( !isBase )
                        {
                            view.addFragment( FragKind.inserted, offset, set, frag );
                        }
                        else // aligned with base
                        {
                            view.addFragment( FragKind.aligned, offset, set, frag );
                        }
                        if ( isBase )
                            offset += data.length;
                    }
                }
                // update totals and current versions
                for (int j=pv.nextSetBit(1);j>=0;j=pv.nextSetBit(j+1)) 
                {
                    totals[j-1] += p.length();
                    if ( totals[j-1] >= lengths[j-1] )
                    {
                        view.clearCurrent((short)j);
                    }
                }
            }
            if ( offset > end )
                break;
        }
//        System.out.println("totals:");
//        for ( int i=0;i<totals.length;i++ )
//            System.out.print(totals[i]+" ");
//        System.out.println("");
//        System.out.println("length:");
//        for ( int i=0;i<lengths.length;i++ )
//            System.out.print(lengths[i]+" ");
//        System.out.println("");
        return view; 
    }
    /**
     * Get a JSON representation of the entire MVD as a table
     * @param base the version to regard as the base
     * @param start the offset into base to start from
     * @param len the length from start to return
     * @param spec a specification of a comma-separated set of versions
     * @return a JSON document
     */
    public String getTable( short base, int start, int len, String spec )
    {
        try
        {
            BitSet bs = getSelectedVersions(spec);
            TableView view = buildDefaultTableView(base,bs,start,len);
            HashMap<String,String> map = new HashMap<String,String>();
            if ( spec != null )
                map.put("selected",spec );
            return view.toJSONString(map);
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
            return "";
        }
    }
    /**
     * Compute a table view containing all the variants of a given base range
     * @param base the version to regard as the base
     * @param start the start offset within base of the range
     * @param len the length of the range within base
     * @param compact compact the table by merging nearly equal versions
     * @param hideMerged display only base version in merged sections
     * @param wholeWords expand differences to whole words
     * @param spec a specification of a comma-separated set of versions
     * @param firstID ID of the first merged text ID
     * @return a HTML fragment 
     */
    public String getTableView( short base, int start, int len, 
        boolean compact, boolean hideMerged, boolean wholeWords,
        String spec, int firstID, String tableId )
    {
        try
        {
            int index = next( 0, base );
            if ( index == Integer.MAX_VALUE )
                throw new Exception( "MVD has no pair for version "+base);
            PairPos sPos = new PairPos( index, 0, base );
            sPos = getPairPos( sPos, start );
            PairPos ePos = getPairPos( sPos, len );
            BitSet bs = convertVersions( spec );
            BitSet found = new BitSet();
            for ( int i=sPos.getIndex();i<=ePos.getIndex();i++ )
            {
                Pair p = pairs.get( i );
                if ( p.versions.intersects(bs) )
                    found.or( p.versions );
            }
            found.and( bs );
            // so now we have a range of pair-positions
            // and ALL its versions in found
            EnumMap<Options,Object> options 
                = new EnumMap<Options,Object>(Options.class);
            options.put(Options.COMPACT,compact);
            options.put(Options.HIDE_MERGED,hideMerged);
            options.put(Options.WHOLE_WORDS,wholeWords);
            options.put(Options.FIRST_MERGEID,firstID);
            options.put(Options.TABLE_ID,tableId);
            TableView view = new TableView( this.versions, this.groups, 
                base, found, options );
            int offset = 0;
            for ( int i=sPos.getIndex();i<=ePos.getIndex();i++ )
            {
                Pair p = pairs.get( i );
                if ( p.length()>0 )
                {
                    int deleted = 0;
                    boolean isBase = p.versions.nextSetBit(base) == base;
                    char[] data = p.getChars();
                    if ( i == sPos.getIndex() )
                    {
                        int size = data.length-sPos.getPosition();
                        char[] dCopy = new char[size];
                        System.arraycopy( data, sPos.getPosition(), 
                            dCopy, 0, size ); 
                        data = dCopy;
                        deleted = sPos.getPosition();
                    }
                    if ( i == ePos.getIndex() )
                    {
                        int del = (deleted+data.length)-ePos.getPosition();
                        int remaining = data.length-del;
                        if ( remaining < 0 )
                            throw new Exception("size of range was < 0");
                        char[] eCopy = new char[remaining];
                        System.arraycopy( data, 0, eCopy, 0, remaining );
                        data = eCopy;
                    }
                    String frag = new String(data);
                    BitSet set = constrainVersions(p.versions,found);
                    if ( !p.versions.intersects(found) )
                        continue;
                    else if ( set.equals(found) )
                        view.addFragment( FragKind.merged, offset, found, frag );
                    else if ( !isBase )
                        view.addFragment( FragKind.inserted, offset, set, frag );
                    else // aligned with base
                        view.addFragment( FragKind.aligned, offset, set, frag );
                    if ( isBase )
                        offset += data.length;
                }
            }
            return view.toString();
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
            return "";
        }
    }
    /**
     * Create a substring around a given offset
     * @param text the text source
     * @param i the offset
     * @return the substring
     */
    private static String around( String text, int i )
    {
        if ( i-20 > 0 )
            return text.substring( i-20, i+20 );
        else
            return text.substring( 0, 40-i );
    }
    private static int parseError( char token, String text, int offset )
    {
        System.out.println("Unexpected "+token
                            +" around "+around(text,offset));
        return 0;
    }
    /**
     * Check that a table has valid HTML
     * @param table the HTML to check
     * @param maxId the maximum id value
     * @return 1 if it worked
     */
    private static int checkTableOutput( String table, int maxId )
    {
        int state = 0;
        int maxIdValue=0;
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        for ( int i=0;i<table.length();i++ )
        {
            char token = table.charAt(i);
            switch ( state )
            {
                case 0: // looking for '<'
                    if ( token=='<' )
                        state = 1;
                    else if ( token=='>' )
                        return parseError(token,table,i);
                    break;
                case 1: // looking for '/'
                    if ( token!='/'&&!Character.isLetter(token) )
                        return parseError(token,table,i);
                    state = 2;
                    break;
                case 2: // reading tag-name
                    if ( Character.isWhitespace(token) )
                    {
                        key.delete(0,key.length());
                        value.delete(0,value.length());
                        state = 3;
                    }
                    else if ( token=='>' )
                        state = 0;
                    else if (!Character.isLetter(token) )
                        return parseError(token,table,i);
                    break;
                case 3: // looking for attribute key
                    if ( Character.isLetter(token) )
                    {
                        key.append(token);
                        state = 4;
                    }
                    else if ( !Character.isWhitespace(token) )
                        return parseError(token,table,i);
                    break;
                case 4: // looking for '='
                    if ( token == '=' )
                        state = 5;
                    else if ( !Character.isLetter(token) )
                        return parseError(token,table,i);
                    else
                        key.append(token);
                    break;
                case 5: // looking for opening quote
                    if ( token == '"' )
                        state = 6;
                    else
                        return parseError(token,table,i);
                    break;
                case 6: // reading attribute value
                    if ( token == '"' )
                    {
                        if ( key.toString().equals("id") )
                        {
                            String val = value.substring(1);
                            try
                            {
                                maxIdValue = Integer.parseInt(val);
                            }
                            catch ( Exception e )
                            {
                                // ignore invalid numbers
                            }
                        }
                        state = 2;
                    }
                    else if ( !Character.isLetter(token)&&!Character.isDigit(token) )
                        return parseError(token,table,i);
                    else
                        value.append( token );
                    break;
            }
        }
        if ( maxIdValue != maxId )
            System.out.println("expected maximum id to be "+maxId
                +" but it was "+maxIdValue);
        System.out.println("The html was correct");
        return 1;
    }
    /**
     * Test table building
     * @param args one: the name of an MVD file
     */
    public static void main( String[] args )
    {
        try
        {
            if ( args.length==1 )
            {
                File src = new File( args[0] );
                MVD mvd = MVDFile.internalise( src, null );
                if ( mvd != null )
                {
                    long start = System.currentTimeMillis();
                    String tView = mvd.getTableView( (short)6, 0, 22720, 
                    true, true, true, "all",1,"apparatus");
                    System.out.println( tView );
                    checkTableOutput( tView, 568 );
                    System.out.println("time taken="
                        +(System.currentTimeMillis()-start)
                        +" milliseconds. length="+tView.length());   
                }
                else
                    System.out.println("failed to load mvd");
            }
            else
                System.out.println("specify an MVD file name");
        }
        catch ( Exception e )
        {
        }    
    }
}
