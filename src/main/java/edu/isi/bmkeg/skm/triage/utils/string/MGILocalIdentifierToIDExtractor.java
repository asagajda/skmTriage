package edu.isi.bmkeg.skm.triage.utils.string;

import java.util.Scanner;
import java.util.regex.Pattern;

import edu.isi.bmkeg.skm.core.exceptions.StringCleanerException;
import edu.isi.bmkeg.skm.core.utils.string.LocalIdentifierToIDExtractor;

public class MGILocalIdentifierToIDExtractor extends LocalIdentifierToIDExtractor
{
	public static final String JID = "J:";
	public static final String MGI_ID="MGI:";

	public MGILocalIdentifierToIDExtractor(boolean useJID, String pattern)
	{	
		super(false);
		if(!useJID)
			this.idType = MGI_ID;
		else
			this.idType = JID;
		this.pattern = pattern;
	}

	public String cleanItUp(String dirtyString) throws StringCleanerException
	{
		Scanner s= new Scanner(dirtyString);
		String match = s.findInLine(Pattern.compile(pattern));
		if(match!=null){
			return match;
		}
		return null;
	}


}
