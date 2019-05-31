import java.util.ArrayList;


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

/**
 * @author Ilusha
 *
 */
public class ADAuthenticator {

	/**
	 * @param ctx An initialized LDAP context to work with
	 * @param groupsSearchFilter A filter for retrieving the groups from AD
	 * @param searchBase The location of the groups within the domain
	 * @return A list of groups
	 * @throws NamingException
	 */
	public static final ArrayList<String> getGroupsByFilter(LdapContext ctx, String groupsSearchFilter, String searchBase) throws NamingException
	{
		ArrayList<String> groupList = new ArrayList<String>();
		// Search for groups the user belongs to in order to get their names
		
		//Create the search controls   
		
		SearchControls groupsSearchCtls = new SearchControls();
		
		//Specify the search scope
		groupsSearchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		 
		//Specify the attributes to return
		String groupsReturnedAtts[]={"sAMAccountName"};
		groupsSearchCtls.setReturningAttributes(groupsReturnedAtts);
		
		//Search for objects using the filter
		NamingEnumeration<SearchResult> groupsAnswer = ctx.search(searchBase, groupsSearchFilter.toString(), groupsSearchCtls);
		                        
		//Loop through the search results
		while (groupsAnswer.hasMoreElements()) {
			SearchResult sr = (SearchResult)groupsAnswer.next();
			Attributes attrs = sr.getAttributes();
			
			if (attrs != null) {
				groupList.add(attrs.get("sAMAccountName").get().toString());
				}
			}
		
		return groupList;
	}
	
	/**
	 * @param groupsFromAD A result containing our group SIDs in forms of byte arrays 
	 * @return A filter for retrieving the groups from AD
	 * @throws NamingException
	 */
	public static final StringBuffer getUserTokenGroupsFilter(NamingEnumeration<SearchResult> groupsFromAD) throws NamingException
	{
		//placeholder for an LDAP filter that will store SIDs of the groups the user belongs to
		StringBuffer groupsSearchFilter = new StringBuffer();
		groupsSearchFilter.append("(|");
		
		//Loop through the search results
		while (groupsFromAD.hasMoreElements()) {

			SearchResult sr = (SearchResult)groupsFromAD.next();
			Attributes attrs = sr.getAttributes();

			if (attrs != null) {
				try {
					for (NamingEnumeration<? extends Attribute> ae = attrs.getAll();ae.hasMore();) {
						Attribute attr = (Attribute)ae.next();
						for (NamingEnumeration<?> e = attr.getAll();e.hasMore();) {
							byte[] sid = (byte[])e.next();
							groupsSearchFilter.append("(objectSid=" + binarySidToStringSid(sid) + ")");
							}
						groupsSearchFilter.append(")");
					   }
				   }
			   catch (NamingException e) {
				   e.printStackTrace();
				   }
			   }
		   }
		
		return groupsSearchFilter; 
	}
	
	/**
	 * @param ctx An initialized LDAP context to work with
	 * @param distinguishedName The distinguished name of the user
	 * @return A result containing our group SIDs in forms of byte arrays 
	 * @throws NamingException
	 */
	public static final NamingEnumeration<SearchResult> getUserTokenGroups(LdapContext ctx, String distinguishedName) throws NamingException
	{

		//Create the search controls   
		SearchControls userSearchCtls = new SearchControls();
		
		//Specify the search scope (can enumerate groups only with an OBJECT_SCOPE) any other scope will fail
		userSearchCtls.setSearchScope(SearchControls.OBJECT_SCOPE);
		
		//Specify the attributes to return
		String userReturnedAtts[]={"tokenGroups"};
		userSearchCtls.setReturningAttributes(userReturnedAtts);
		
		//specify the LDAP search filter to find the user in question
		//String userSearchFilter = "(&(objectCategory=user)(sAMAccountName=" + targetUser + "))";
		String userSearchFilter = "(objectCategory=user)";
		
		
		//Search for objects using the filter
		return ctx.search(distinguishedName, userSearchFilter, userSearchCtls);
		
	}
	
	/**
	 * @param ctx An initialized LDAP context to work with
	 * @param sAMAccountName The sAMAccountName of the user
	 * @param domainName The domain name of the user
	 * @param searchBase The location of the user within the domain
	 * @return The distinguished name of the user
	 * @throws NamingException
	 */
	public static final String getUserDistinguishedName(LdapContext ctx, String sAMAccountName, String domainName, String searchBase) throws NamingException
	{
		
		// Create the search controls   
		SearchControls userSearchCtls = new SearchControls();
		userSearchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		String userReturnedAtts[]={"distinguishedName"};
		userSearchCtls.setReturningAttributes(userReturnedAtts);
		
		// Initialize the filter
		String userSearchFilter = "(&(objectCategory=user)(userPrincipalName=" + sAMAccountName + "@" + domainName + "))";
		
		NamingEnumeration<SearchResult> userAnswer = ctx.search(searchBase, userSearchFilter, userSearchCtls);
		
		String distinguishedName = ""; 
		
		//Loop through the search results
		while (userAnswer.hasMoreElements()) {	   
			   SearchResult sr = (SearchResult)userAnswer.next();
			   Attributes attrs = sr.getAttributes();
		                                                             
			   if (attrs != null) {
				   distinguishedName = (String)attrs.get("distinguishedname").get();
			   }
		}
		return distinguishedName;
		
	}
    /**
     * @param SID a SID from a directory
     * @return The SID in the form of a string
     */
    private static final String binarySidToStringSid( byte[] SID ) {
    	
		String strSID = "";
	
	    //convert the SID into string format
	
	    long version;
	    long authority;
	    long count;
	    long rid;
	
	    strSID = "S";
	    version = SID[0];
	    strSID = strSID + "-" + Long.toString(version);
	    authority = SID[4];
	
	    for (int i = 0;i<4;i++) {
	            authority <<= 8;
	            authority += SID[4+i] & 0xFF;
	    }
	
	    strSID = strSID + "-" + Long.toString(authority);
	    count = SID[2];
	    count <<= 8;
	    count += SID[1] & 0xFF;
	
	    for (int j=0;j<count;j++) {
	
	            rid = SID[11 + (j*4)] & 0xFF;
	
	            for (int k=1;k<4;k++) {
	
	                   rid <<= 8;
	
	                   rid += SID[11-k + (j*4)] & 0xFF;
	
	            }
	
	            strSID = strSID + "-" + Long.toString(rid);
	
	    }
	
		return strSID;
    }

}
