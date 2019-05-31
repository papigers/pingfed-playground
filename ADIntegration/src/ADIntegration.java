import java.util.ArrayList;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
/**
 * @author Ilusha
 *
 */
public class ADIntegration {

	public static String Domain = "SPing.test";
	public static String LDAPServer = "ldap://SPing.test";
	//public static String userSearchBase = "CN=Dude,OU=Units,DC=SPing,DC=test";
	//public static String userSearchBase = "OU=Units,DC=SPing,DC=test";
	public static String userSearchBase = "DC=SPing,DC=test";
	public static String groupSearchBase = "DC=SPing,DC=test";
	public static String userName = "Ilush";
	public static String password = "Aa123123123123";
	public static String targetUser = "Dude";
	
	public static void main(String[] args) {
		
		try {
			ADContext.InitializeADContext(Domain, userName, password);

			LdapContext ctx = ADContext.getContext();
			String distinguishedName = ADAuthenticator.getUserDistinguishedName(ctx, targetUser, Domain, userSearchBase);
			
			NamingEnumeration<SearchResult> groupsResult = ADAuthenticator.getUserTokenGroups(ctx, distinguishedName);
			
			StringBuffer groupsSearchFilter = ADAuthenticator.getUserTokenGroupsFilter(groupsResult);
			
			ArrayList<String> groupList = ADAuthenticator.getGroupsByFilter(ctx, groupsSearchFilter.toString(), groupSearchBase);
			
			for (String group : groupList) {
				System.out.println(group);
			}
			ctx.close();
		}
		catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
