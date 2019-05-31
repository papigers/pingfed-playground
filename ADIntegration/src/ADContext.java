import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 * @author Ilusha
 * Singleton for working with AD
 */
public class ADContext {

	private String Domain;
	private String LDAPURL;
	private String password;
	private String userName;
	private Hashtable<String,String> env;
	private LdapContext ldapCtx;
	
	/**
	 * @return The singleton for the AD context. InitializeADContext must be run before use
	 */
	public static LdapContext getContext() {		
		if (adContext != null)
			return adContext.ldapCtx;
		else
			return null;
	}
	
	private static ADContext adContext;
	
	/**
	 * @param domain The domain for the context
	 * @param userName The user name for the login
	 * @param password The password for the login
	 * @throws NamingException
	 */
	private ADContext(String domain, String userName, String password) throws NamingException {
		this.Domain = domain;
		this.LDAPURL = String.format("ldap://" + domain);
		this.userName = userName;
		this.password = password;
		
		this.env = new Hashtable<String,String>();
		this.env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		this.env.put(Context.SECURITY_AUTHENTICATION, "simple");
		this.env.put(Context.SECURITY_PRINCIPAL, this.userName + "@" + this.Domain);
		this.env.put(Context.SECURITY_CREDENTIALS, this.password);
		this.env.put(Context.PROVIDER_URL, this.LDAPURL);
	    
	    // To get groups in proper format
		this.env.put("java.naming.ldap.attributes.binary","tokenGroups");
	    
		this.ldapCtx = new InitialLdapContext(this.env,null);		
	}
	
	/**
	 * @param domain The domain for the context
	 * @param username The user name for the login
	 * @param password  The password for the login
	 * @throws NamingException
	 */
	public static void InitializeADContext(String domain, String username, String password) throws NamingException	{
		if (getContext() == null) {
			adContext = new ADContext(domain, username, password);
		}
	}
}
