/*
 *  NMerge is Copyright 2015 Desmond Schmidt
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

package edu.luc.nmerge.mvd.navigator;

import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.Pair;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Navigate through the plain text of an MVD assuming that it contains  
 * hyphens at line-end and page-numbers on a line on their own. 
 * Page-numbers may be pure Arabic, or Arabic followed by one lowercase 
 * letter. Roman page numbers must be lowercase.
 * @author desmond
 */
public class TextNavigator 
{
    static HashSet<Character>  roman;
    static {
        roman = new HashSet<Character>();
        roman.add('i');
        roman.add('x');
        roman.add('v');
        roman.add('l');
        roman.add('c');
        roman.add('m');
    };
    int offset;
    int index;
    int v;
    String text;
    boolean lastWasLetter;
    MVD mvd;
    ArrayList<Pair> pairs;
    ArrayList<Character> undo;
    public TextNavigator( MVD mvd, int index, int offset, int v )
    {
        this.mvd = mvd;
        this.offset = offset;
        this.index = index;
        this.v = v;
        this.pairs = mvd.getPairs();
    }
    public TextNavigator()
    {
    }
    /**
     * Push a just-read token onto the unget queue
     * @param token the token to unget
     */
    private void push( char token )
    {
        if ( undo == null )
            undo = new ArrayList<Character>();
        undo.add(token);
    }
    /**
     * When going backwards we need to "push" to the front of the queue
     * @param token the token to unpush
     */
    private void unpush( char token )
    {
        if ( undo == null )
            undo = new ArrayList<Character>();
        undo.add(0,token);
    }
    /*
     * Debug version of nextChar
     */
//    private char nextChar()
//    {
//        if ( undo != null && undo.size() > 0 )
//            return undo.remove(0);
//        else if ( offset == text.length() )
//            return (char)-1;
//        else if ( offset < text.length() )
//            return text.charAt(offset++);
//        else
//            return (char)-1;
//    }
    /**
     * Get the next character without worrying what it is
     * @return a char or -1
     */
    private char nextChar()
    {
        // dequeue
        if ( undo != null && undo.size() > 0 )
            return undo.remove(0);
        else if ( index == pairs.size() )
            return (char)-1;
        else
        {
            Pair p = pairs.get(index);
            char[] data = p.getChars();
            if ( offset < 0 )
                System.out.println("offset < 0" );
            char token = data[offset++];
            // prepare for next get
            if ( offset == data.length )
            {
                offset = -1;
                while ( index < pairs.size()-1 )
                {
                    p = pairs.get(++index);
                    if ( p.versions.nextSetBit(v)==v && p.length()>0 )
                    {
                        offset = 0;
                        break;
                    }
                }
                if ( offset == -1 )
                    token = (char)-1;
            }
            return token;
        }
    }
    /*
     * Debug version of prevchar
     * @return a char or -1
     */

//    private char prevChar()
//    {
//        if ( undo != null && undo.size() > 0 )
//            return undo.remove(0);
//        else if ( offset == -1 )
//            return (char)-1;
//        else if ( offset >= 0 )
//            return text.charAt(offset--);
//        else
//            return (char)-1;
//    }
    /**
     * Get the previous character without worrying what it is
     * @return a char or -1
     */
    private char prevChar()
    {
        if ( undo.size() > 0 )
            return undo.remove(0);
        else if ( index < 0 )
            return (char)-1;
        else
        {
            Pair p = pairs.get(index);
            char[] data = p.getChars();
            // preload previous pair if needed
            offset--;
            while ( index > 0 && offset < 0 )
            {
                p = pairs.get(--index);
                if ( p.versions.nextSetBit(v)==v && p.length()>0 )
                {
                    data = p.getChars();
                    offset = data.length-1;
                }
            }
            if ( offset == -1 )
                return (char)-1;
            else
                return data[offset];
        }
    }
    /**
     * Read a Roman page number if it is there, else do no damage
     * @return true if it was bona fide Roman page no ending in CR/LF 
     */
    private boolean readRomanPage()
    {
        ArrayList<Character> saved = new ArrayList<Character>();
        char token = nextChar();
        int len = 0;
        // saved leading newline chars
        while ( token=='\n'||token=='\r' )
        {
            saved.add(token);
            token = nextChar();
        }
        while ( roman.contains(token) )
        {
            saved.add(token);
            token = nextChar();
            len++;
        }
        // ensure we have read all LFs or CRs (may be DOS)
        char mismatched = token;
        while ( token=='\r' || token=='\n' )
        {
            saved.add(token);
            token = nextChar();
        }
        // save last char - probably a letter
        saved.add(token);
        while (saved.size()>0 )
            push( saved.remove(0) );
        return len>0&&(mismatched=='\n'||mismatched=='\r');
    }
    /**
     * Read an Arabic page number if it is there, else do no damage
     * @return true if it was bona fide Arabic page no ending in CR/LF 
     */
    private boolean readArabicPage()
    {
        boolean leadingDigit = false;
        int len = 0;
        ArrayList<Character> saved = new ArrayList<Character>();
        char token = nextChar();
        while ( token=='\n'||token=='\r' )
        {
            saved.add(token);
            token = nextChar();
        }
        while ( Character.isDigit(token) 
            || (leadingDigit && Character.isLowerCase(token)) )
        {
            if ( Character.isDigit(token) )
                leadingDigit = true;
            // after one letter cancel leadingDigit
            if ( Character.isLowerCase(token) )
                leadingDigit = false;
            saved.add(token);
            token = nextChar();
            len++;
        }
        // ensure we have read all LFs or CRs (may be DOS)
        char mismatched = token;
        while ( token=='\r' || token=='\n' )
        {
            saved.add(token);
            token = nextChar();
        }
        // save last char - probably a letter
        saved.add(token);
        while (saved.size()>0 )
            push( saved.remove(0) );
        return len>0 && (mismatched=='\n'||mismatched=='\r');
    }
    /**
     * Read a hyphenated word - do no harm
     * @return true if it was hyphenated at line-end
     */
    private boolean readHyphenatedWord()
    {
        ArrayList<Character> saved = new ArrayList<Character>();
        // check for page numbers
        char token;
        if ( readArabicPage()||readRomanPage() )
        {
            // remove page number
            char last = undo.get(undo.size()-1);
            //debug
//            for ( int i=0;i<undo.size()-1;i++ )
//            {
//                char tok = undo.get(i);
//                System.out.print(tok);
//            }
            // end debug
            undo.clear();
            saved.add('-');
            token = last;
        }
        else
            token = nextChar();
        while ( token=='\n'||token=='\r' )
        {
            saved.add(token);
            token = nextChar();
        }
        saved.add(token);
        while ( undo.size()>0 )
            saved.add(undo.remove(0));
        while (saved.size()>0 )
            push( saved.remove(0) );
        // token is definitely not a newline
        return Character.isLetter(token);
    }
    /**
     * Get the next character or -1, skipping line-ends and page-numbers
     * @return a char or -1 if no more
     */
    public char next()
    {
        char token = nextChar();
        if ( token== (char)-1 )
        {
            return token;
        }
        else
        {
            if ( lastWasLetter && token == '-' )
            {
                if ( readHyphenatedWord() )
                {
                    undo.add(0,'-');
                    // remove CR from undo and replace hyphen
                    ArrayList<Character> saved = new ArrayList<Character>();
                    // debug
                    for ( int i=0;i<undo.size();i++ )
                    {
                        char tok = undo.get(i);
                        if ( tok == '-' )
                            token = (char)0xAD;
                        else if ( tok != '\r'&&tok!='\n' )
                            saved.add(tok);
                    }
                    // end debug
                    undo = saved;
                }
            }
            else if ( token=='\n'||token=='\r' )
            {
                if ( readRomanPage()||readArabicPage() )
                {
                    char last = undo.get(undo.size()-1);
//                    for ( int i=0;i<undo.size();i++ )
//                    {
//                        char tok = undo.get(i);
//                        System.out.print(tok);
//                        if ( tok=='\n' )
//                            break;
//                    }
                    undo.clear();
                    undo.add(last);
                }
            }
            lastWasLetter = Character.isLetter(token);
            return token;
        }
    }
    /**
     * See if there is a hyphenated word maybe split over a page-break 
     * when reading backwards.
     * @return true if you found it else false
     */
    private boolean readHyphenatedWordBackwards()
    {
        boolean hadHyphen = false;
        ArrayList<Character> saved = new ArrayList<Character>();
        // check for page numbers
        char token;
        if ( readArabicPageBackwards()||readRomanPageBackwards() )
        {
            // remove page number
            char last = undo.get(undo.size()-1);
            undo.clear();
            token = last;
        }
        else
            token = prevChar();
        saved.add(token);
        if ( token=='-' )
        {
            token = prevChar();
            saved.add(token);
            hadHyphen = true;
        }
        while ( undo.size()>0 )
            saved.add(undo.remove(0));
        while (saved.size()>0 )
            unpush( saved.remove(saved.size()-1) );
        // token is definitely not a newline
        return hadHyphen && Character.isLetter(token);
    }
    /**
     * Read an Roman page-number backwards on a line by itself
     * If not present, saved the read characters in undo
     * @return true if it was there
     */
    private boolean readRomanPageBackwards()
    {
        ArrayList<Character> saved = new ArrayList<Character>();
        char token = prevChar();
//        if ( token=='p')
//            System.out.println("p");
        int len = 0;
        // saved leading newline chars
        while ( token=='\n'||token=='\r' )
        {
            saved.add(token);
            token = prevChar();
        }
        while ( roman.contains(token) )
        {
            saved.add(token);
            token = prevChar();
            len++;
        }
        // ensure we have read all LFs or CRs (may be DOS)
        char mismatched = token;
        while ( token=='\r' || token=='\n' )
        {
            saved.add(token);
            token = prevChar();
        }
        // save last char - probably a letter
        saved.add(token);
        while (saved.size()>0 )
            unpush( saved.remove(saved.size()-1) );
        return len>0&&(mismatched=='\n'||mismatched=='\r');
    }
    /**
     * Read an Arabic page-number possibly ending in a lowercases letter
     * If not present, saved the read characters in undo
     * @return true if it was there
     */
    boolean readArabicPageBackwards()
    {
        boolean leadingLetter = false;
        int len = 0;
        ArrayList<Character> saved = new ArrayList<Character>();
        char token = prevChar();
//        if ( token=='p' )
//            System.out.println("p");
        while ( token=='\n'||token=='\r' )
        {
            saved.add(token);
            token = prevChar();
        }
        while ( Character.isDigit(token) 
            || (!leadingLetter && Character.isLowerCase(token)) )
        {
            if ( Character.isLowerCase(token) )
                leadingLetter = true;
            saved.add(token);
            token = prevChar();
            len++;
        }
        // ensure we have read all LFs or CRs (may be DOS)
        char mismatched = token;
        while ( token=='\r' || token=='\n' )
        {
            saved.add(token);
            token = prevChar();
        }
        // save last char - probably a letter
        saved.add(token);
        while (saved.size()>0 )
            push( saved.remove(0) );
        return len>0 && (mismatched=='\n'||mismatched=='\r');
    }
    /**
     * Read the previous character, skipping CRs if word is hyphenated and 
     * skipping over page numbers likewise
     * @return the previous char or -1 if there was none
     */
    public char prev()
    {
        char token = prevChar();
        if ( token == (char)-1 )
        {
            return token;
        }
        else if (token == '\r'||token=='\n')
        {
            if ( lastWasLetter && readHyphenatedWordBackwards() )
            {
                char last = undo.get(undo.size()-1);
                undo.clear();
                token = (char)0xAD;
                undo.add(last);
            }
            else if ( readRomanPageBackwards()||readArabicPageBackwards() )
            {
                char last = undo.get(undo.size()-1);
                undo.clear();
                undo.add(last);
            }
            lastWasLetter = false;
            return token;
        }
        else
        {
            lastWasLetter = Character.isLetter(token);
            return token;
        }
    }
    /**
     * This test requires the nextChar or prevChar debug versions
     * @param args 
     */
    public static void main(String[] args )
    {
        TextNavigator tn = new TextNavigator();
        tn.text = "forbidden treasures. The common folk of the neigh-\n" +
        "borhood, peons of the estancias, vaqueros of the sea-\n" +
        "board plains, tame Indians coming miles to market\n" +
        "with a bundle of sugar-cane or a basket of maize worth\n" +
        "about threepence, are well aware that heaps of shin-\n" +
        "ing gold lie in the gloom of the deep precipices cleav-\n" +
        "ing the stony levels of Azuera. Tradition has it that\n" +
        "many adventurers of olden time had perished in the\n" +
        "search. The story goes also that within men's memory\n" +
        "two wandering sailors--Americanos, perhaps, but\n" +
        "gringos of some sort for certain--talked over a gam-\n" +
        "bling, good-for-nothing mozo, and the three stole a\n" +
        "234a\n" +
        "donkey to carry for them a bundle of dry sticks, a\n" +
        "water-skin, and provisions enough to last a few days.\n" +
        "Thus accompanied, and with revolvers at their belts,\n" +
        "they had started to chop their way with machetes";
        StringBuilder sb = new StringBuilder();
        char t = 0;
        tn.offset = 0;
        //tn.text.length()-1;   // use this for backwards
        while ( t != (char)-1)
        {
            t = tn.next();  // forwards
            // t = tn.prev(); // backwards
            if ( t != (char)-1 )
                sb.append(t);
        }
        // write it out backwards
//        for ( int i=sb.length()-1;i>=0;i-- )
//            System.out.print(sb.charAt(i));
        // use this for forwards
        for ( int i=0;i<sb.length();i++ )
            System.out.print(sb.charAt(i));
    }
}
