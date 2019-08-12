/*
 * **************************************************
 *  Copyright (C) 2017 Ping Identity Corporation
 *  All rights reserved.
 *
 *  The contents of this file are subject to the terms of the
 *  Ping Identity Corporation SDK Developer Guide.
 *
 *  Ping Identity Corporation
 *  1001 17th St Suite 100
 *  Denver, CO 80202
 *  303.468.2900
 *  http://www.pingidentity.com
 * ****************************************************
 */

package com.pingidentity.adapter.idp;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.core.config.plugins.validation.validators.RequiredValidator;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.X509Util;
import org.jose4j.keys.resolvers.X509VerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.sourceid.saml20.adapter.AuthnAdapterException;
import org.sourceid.saml20.adapter.attribute.AttributeValue;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.conf.Field;
import org.sourceid.saml20.adapter.gui.ActionDescriptor;
import org.sourceid.saml20.adapter.gui.AdapterConfigurationGuiDescriptor;
import org.sourceid.saml20.adapter.gui.CheckBoxFieldDescriptor;
import org.sourceid.saml20.adapter.gui.FieldDescriptor;
import org.sourceid.saml20.adapter.gui.TableDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.saml20.adapter.gui.event.ConfigurationListener;
import org.sourceid.saml20.adapter.gui.event.EventException;
import org.sourceid.saml20.adapter.gui.event.PreRenderCallback;
import org.sourceid.saml20.adapter.gui.TextAreaFieldDescriptor;
import org.sourceid.saml20.adapter.gui.validation.ConfigurationValidator;
import org.sourceid.saml20.adapter.gui.validation.FieldValidator;
import org.sourceid.saml20.adapter.gui.validation.ValidationException;
import org.sourceid.saml20.adapter.gui.validation.impl.RequiredFieldValidator;
import org.sourceid.saml20.adapter.idp.authn.AuthnPolicy;
import org.sourceid.saml20.adapter.idp.authn.IdpAuthenticationAdapter;
import org.sourceid.saml20.adapter.idp.authn.IdpAuthnAdapterDescriptor;
import org.sourceid.saml20.domain.ConditionType;
import java.security.spec.X509EncodedKeySpec;

import com.amazonaws.util.Base64;
//import com.pingidentity.admin.api.model.plugin.FieldDescriptor;
//import com.pingidentity.admin.api.model.plugin.FieldDescriptor;
//import com.pingidentity.admin.api.model.plugin.FieldDescriptorType;
import com.pingidentity.pingcommons.crypto.CertificateUtil;
import com.pingidentity.sdk.AuthnAdapterResponse;
import com.pingidentity.sdk.AuthnAdapterResponse.AUTHN_STATUS;

import jcifs.dcerpc.msrpc.netdfs;

import com.pingidentity.sdk.IdpAuthenticationAdapterV2;

/**
 * <p>
 * This class is an example of an IdP authentication adapter that uses the client's (or last proxy that sent the
 * request) IPv4 address to identify the user. If authenticated, the user will be assigned a guest role by default. In
 * order to be have a corporate role, this adapter needs to be chained to another adapter via a Composite Adapter.
 * </p>
 * <p>
 * For simplicity sake, if the client has a non-loopback IPv6 address the authentication will fail.
 * </p>
 * This adapter is simply a sample, and in production (at a minimum) would likely be chained with another adapter to
 * further identify and authenticate the end user.
 */
public class JWTAdapter implements IdpAuthenticationAdapterV2
{
    
    private class PEMFieldValidator implements FieldValidator
    {
    	private static final long serialVersionUID = 1L;
        private static final String ERROR_MESSAGE = "Not a valid public key (PEM format)";
        
        @Override
        public void validate(Field field) throws ValidationException
        {
            String pem = field.getValue();
            try
            {
            	parsePublicKey(pem);
            }
            catch (Exception e)
            {
            	System.out.println(e.getMessage());
            	System.out.println(e.getStackTrace());
                throw new ValidationException(ERROR_MESSAGE);
            }
        }
    }
    
//    private static final String CONFIG_SUBJECT_CLAIM = "Subject Claim Name";
//    private static final String CONFIG_DOMAIN_CLAIM = "Domain Claim Name";
    private static final String CONFIG_CHECK_TOKEN_SIGNATURE = "Verify Token Signature";
    private static final String CONFIG_PUBLIC_KEY = "Token Signing Public Key";
    private static final String ATTR_SUB = "sub";
    private static final String ATTR_DOMAIN = "domain";

    private final IdpAuthnAdapterDescriptor descriptor;
    private byte[] baseAddress = null;
    private byte[] subnetMask = null;
    
    private String subjectClaim = null;
    private String domainClaim = null;
    private boolean checkSignature;
    private boolean useCertificate;
    private Set<String> fields = null;
    private GenericPublicKey publickKey = null;

    /**
     * Constructor for the Sample Subnet Adapter. Initializes the authentication adapter descriptor so PingFederate can
     * generate the proper configuration GUI
     */
    public JWTAdapter()
    {
//    	TextFieldDescriptor subjectClaimNameField = new TextFieldDescriptor(CONFIG_SUBJECT_CLAIM,
//    			"Enter the claim name that will identify the user");
//    	subjectClaimNameField.addValidator(new RequiredFieldValidator());
//    	subjectClaimNameField.setDefaultValue("sub");
//    	
//    	TextFieldDescriptor subjectClaimDomainField = new TextFieldDescriptor(CONFIG_DOMAIN_CLAIM,
//    			"Enter the claim name that will identify the user's domain");
//    	subjectClaimDomainField.addValidator(new RequiredFieldValidator());
//    	subjectClaimDomainField.setDefaultValue("domain");
    	
    	CheckBoxFieldDescriptor checkTokenSignatureField = new CheckBoxFieldDescriptor(CONFIG_CHECK_TOKEN_SIGNATURE,
    			"Uncheck if you don't want the adapter to verify the token's signature");
    	checkTokenSignatureField.setDefaultValue(true);
    	
    	
    	TextAreaFieldDescriptor tokenPublicKeyField = new TextAreaFieldDescriptor(CONFIG_PUBLIC_KEY,
    			"Enter the public certificate or key (in PEM format) that will be used to verify the received token", 6, 26);
    	tokenPublicKeyField.addValidator(new RequiredFieldValidator());
    	tokenPublicKeyField.addValidator(new PEMFieldValidator());
    	

        // Create a GUI descriptor
        AdapterConfigurationGuiDescriptor guiDescriptor = new AdapterConfigurationGuiDescriptor(
                "Set the details of the JWT to identify your SSO clients");
//        guiDescriptor.addField(subjectClaimNameField);
//        guiDescriptor.addField(subjectClaimDomainField);
    	checkTokenSignatureField.addValidator(new FieldValidator() {
			
			@Override
			public void validate(Field field) throws ValidationException {
				boolean verifySig = field.getValueAsBoolean();
				List<FieldDescriptor> fields = guiDescriptor.getFields();
				boolean hasPubKeyField = false;
				for (int i = 0; i < fields.size() && !hasPubKeyField; i++) {
					if (fields.get(i).getName().equals(CONFIG_PUBLIC_KEY)) {
						hasPubKeyField = true;
					}
				}
				if (verifySig && !hasPubKeyField) {
					throw new ValidationException("You must add a public key");
				}
			}
		});
        guiDescriptor.addField(checkTokenSignatureField);
        guiDescriptor.addField(tokenPublicKeyField);
        
        guiDescriptor.addPreRenderCallback(new PreRenderCallback() {
			
			@Override
			public void callback(List<FieldDescriptor> fields,
					List<FieldDescriptor> advancedFields, List<TableDescriptor> tables,
					Configuration config) {
				boolean checkSig = config.getBooleanFieldValue(CONFIG_CHECK_TOKEN_SIGNATURE);
				int tokenIndex = -1;
				int index = 0;
				for (FieldDescriptor field : fields) {
					if (field.getName().equals(CONFIG_PUBLIC_KEY)) {
						tokenIndex = index;
					}
					index++;
				}
				if (tokenIndex != -1 && !checkSig) {
					fields.remove(tokenIndex);
				}
				else if (tokenIndex == -1 && checkSig) {
					fields.add(tokenPublicKeyField);
				}
			}
		});

        // Create the Idp authentication adapter descriptor
        Set<String> contract = new HashSet<String>();
        contract.add(ATTR_SUB);
        contract.add(ATTR_DOMAIN);
        descriptor = new IdpAuthnAdapterDescriptor(this, "JWT Adapter", contract, true, guiDescriptor, false);
        descriptor.setSupportsExtendedContract(true);
    }

    /**
     * The PingFederate server will invoke this method on your adapter implementation to discover metadata about the
     * implementation. This included the adapter's attribute contract and a description of what configuration fields to
     * render in the GUI. <br/>
     * <br/>
     * Your implementation of this method should return the same IdpAuthnAdapterDescriptor object from call to call -
     * behaviour of the system is undefined if this convention is not followed.
     * 
     * @return an IdpAuthnAdapterDescriptor object that describes this IdP adapter implementation.
     */
    public IdpAuthnAdapterDescriptor getAdapterDescriptor()
    {
        return descriptor;
    }

    /**
     * This is the method that the PingFederate server will invoke during processing of a single logout to terminate a
     * security context for a user at the external application or authentication provider service.
     * <p>
     * If your implementation of this method needs to operate asynchronously, it just needs to write to the
     * HttpServletResponse as appropriate and commit it. Right after invoking this method the PingFederate server checks
     * to see if the response has been committed. If the response has been committed, PingFederate saves the state it
     * needs and discontinues processing for the current transaction. Processing of the transaction is continued when
     * the user agent returns to the <code>resumePath</code> at the PingFederate server at which point the server
     * invokes this method again. This series of events will be repeated until this method returns without committing
     * the response. When that happens (which could be the first invocation) PingFederate will complete the protocol
     * transaction processing with the return result of this method.
     * </p>
     * <p>
     * Note that if the response is committed, then PingFederate ignores the return value. Only the return value of an
     * invocation that does not commit the response will be used. Accessing the HttpSession from the request is not
     * recommended and doing so is deprecated. Use {@link org.sourceid.saml20.adapter.state.SessionStateSupport} as an
     * alternative.
     * </p>
     * <p>
     * 
     * <b>Note on SOAP logout:</b> If this logout is being invoked as the result of a back channel protocol request, the
     * request, response and resumePath parameters will all be null as they have no meaning in such a context where the
     * user agent is inaccessible.
     * </p>
     * <p>
     * In this example, no extra action is needed to logout so simply return true.
     * </p>
     * 
     * @param authnIdentifiers
     *            the map of authentication identifiers originally returned to the PingFederate server by the
     *            {@link #lookupAuthN} method. This enables the adapter to associate a security context or session
     *            returned by lookupAuthN with the invocation of this logout method.
     * @param req
     *            the HttpServletRequest can be used to read cookies, parameters, headers, etc. It can also be used to
     *            find out more about the request like the full URL the request was made to.
     * @param resp
     *            the HttpServletResponse. The response can be used to facilitate an asynchronous interaction. Sending a
     *            client side redirect or writing (and flushing) custom content to the response are two ways that an
     *            invocation of this method allows for the adapter to take control of the user agent. Note that if
     *            control of the user agent is taken in this way, then the agent must eventually be returned to the
     *            <code>resumePath</code> endpoint at the PingFederate server to complete the protocol transaction.
     * @param resumePath
     *            the relative URL that the user agent needs to return to, if the implementation of this method
     *            invocation needs to operate asynchronously. If this method operates synchronously, this parameter can
     *            be ignored. The resumePath is the full path portion of the URL - everything after hostname and port.
     *            If the hostname, port, or protocol are needed, they can be derived using the HttpServletRequest.
     * @return a boolean indicating if the logout was successful.
     * @throws AuthnAdapterException
     *             for any unexpected runtime problem that the implementation cannot handle.
     * @throws IOException
     *             for any problem with I/O (typically any operation that writes to the HttpServletResponse will throw
     *             an IOException.
     * 
     * @see IdpAuthenticationAdapter#logoutAuthN(Map, HttpServletRequest, HttpServletResponse, String)
     */
    @SuppressWarnings("rawtypes")
    public boolean logoutAuthN(Map authnIdentifiers, HttpServletRequest req, HttpServletResponse resp, String resumePath)
            throws AuthnAdapterException, IOException
    {
        return true;
    }

    /**
     * This method is called by the PingFederate server to push configuration values entered by the administrator via
     * the dynamically rendered GUI configuration screen in the PingFederate administration console. Your implementation
     * should use the {@link Configuration} parameter to configure its own internal state as needed. The tables and
     * fields available in the Configuration object will correspond to the tables and fields defined on the
     * {@link org.sourceid.saml20.adapter.gui.AdapterConfigurationGuiDescriptor} on the AuthnAdapterDescriptor returned
     * by the {@link #getAdapterDescriptor()} method of this class. <br/>
     * <br/>
     * Each time the PingFederate server creates a new instance of your adapter implementation this method will be
     * invoked with the proper configuration. All concurrency issues are handled in the server so you don't need to
     * worry about them here. The server doesn't allow access to your adapter implementation instance until after
     * creation and configuration is completed.
     * 
     * @param configuration
     *            the Configuration object constructed from the values entered by the user via the GUI.
     */
    public void configure(Configuration configuration)
    {
//    	subjectClaim = configuration.getFieldValue(CONFIG_SUBJECT_CLAIM);
//    	domainClaim = configuration.getFieldValue(CONFIG_DOMAIN_CLAIM);
    	checkSignature = configuration.getBooleanFieldValue(CONFIG_CHECK_TOKEN_SIGNATURE);
    	if (checkSignature) {
	    	try {
	    		publickKey = parsePublicKey(configuration.getFieldValue(CONFIG_PUBLIC_KEY));
	    	}
	    	catch (Exception e) {
				publickKey = null;
	    	}
    	}
    	else {
    		publickKey = null;
    	}
    	fields = configuration.getAdditionalAttrNames();
    }

    /**
     * This method is used to retrieve information about the adapter (e.g. AuthnContext).
     * <p>
     * In this example the method not used, return null
     * </p>
     * 
     * @return a map
     */
    public Map<String, Object> getAdapterInfo()
    {
        return null;
    }

    /**
     * This is an extended method that the PingFederate server will invoke during processing of a single sign-on
     * transaction to lookup information about an authenticated security context or session for a user at the external
     * application or authentication provider service.
     * <p>
     * If your implementation of this method needs to operate asynchronously, it just needs to write to the
     * HttpServletResponse as appropriate and commit it. Right after invoking this method the PingFederate server checks
     * to see if the response has been committed. If the response has been committed, PingFederate saves the state it
     * needs and discontinues processing for the current transaction. Processing of the transaction is continued when
     * the user agent returns to the <code>resumePath</code> at the PingFederate server at which point the server
     * invokes this method again. This series of events will be repeated until this method returns without committing
     * the response. When that happens (which could be the first invocation) PingFederate will complete the protocol
     * transaction processing with the return result of this method.
     * </p>
     * <p>
     * Note that if the response is committed, then PingFederate ignores the return value. Only the return value of an
     * invocation that does not commit the response will be used.
     * </p>
     * <p>
     * If this adapter is implemented asynchronously, it's recommended that the user agent always returns to the <code>
     * resumePath</code> in order to be compatible with Composite Adapter's "Sufficent" adapter chaining policy. The
     * Composite Adapter allows an Administrator to "chain" a selection of available adapter instances for a connection.
     * At runtime, adapter chaining means that SSO requests are passed sequentially through each adapter instance
     * specified until one or more authentication results are found for the user. If the user agent does not return
     * control to PingFederate for failed authentication scenarios, then the authentication chain will break and should
     * not be used with Composite Adapter's "Sufficient" chaining policy.
     * </p>
     * <p>
     * In this example, we determine if the client (or the last proxy) is on the configured subnet. If the client has an
     * IPv6 address that's not ::1, fail immediately. If the user was previously authenticated by another adapter assign
     * it a corporate role, otherwise use the guest role.
     * </p>
     * 
     * @param req
     *            the HttpServletRequest can be used to read cookies, parameters, headers, etc. It can also be used to
     *            find out more about the request like the full URL the request was made to. Accessing the HttpSession
     *            from the request is not recommended and doing so is deprecated. Use
     *            {@link org.sourceid.saml20.adapter.state.SessionStateSupport} as an alternative.
     * @param resp
     *            the HttpServletResponse. The response can be used to facilitate an asynchronous interaction. Sending a
     *            client side redirect or writing (and flushing) custom content to the response are two ways that an
     *            invocation of this method allows for the adapter to take control of the user agent. Note that if
     *            control of the user agent is taken in this way, then the agent must eventually be returned to the
     *            <code>resumePath</code> endpoint at the PingFederate server to complete the protocol transaction.
     * @param inParameters
     *            A map that contains a set of input parameters. The input parameters provided are detailed in
     *            {@link IdpAuthenticationAdapterV2}, prefixed with <code>IN_PARAMETER_NAME_*</code> i.e.
     *            {@link IdpAuthenticationAdapterV2#IN_PARAMETER_NAME_RESUME_PATH}.
     * @return {@link AuthnAdapterResponse} The return value should not be null.
     * @throws AuthnAdapterException
     *             for any unexpected runtime problem that the implementation cannot handle.
     * @throws IOException
     *             for any problem with I/O (typically any operation that writes to the HttpServletResponse).
     */
    @SuppressWarnings("unchecked")
    public AuthnAdapterResponse lookupAuthN(HttpServletRequest req, HttpServletResponse resp,
            Map<String, Object> inParameters) throws AuthnAdapterException, IOException
    {
//        String spEntityId = (String) inParameters.get(IN_PARAMETER_NAME_PARTNER_ENTITYID);
//        Map<String, AttributeValue> chainedAttributes = (Map<String, AttributeValue>) inParameters.get(
//                IN_PARAMETER_NAME_CHAINED_ATTRIBUTES);

        AuthnAdapterResponse authnAdapterResponse = new AuthnAdapterResponse();

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
        	authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.FAILURE);
        	authnAdapterResponse.setErrorMessage("Authorization header is empty");
        }
        if (!authHeader.toLowerCase().startsWith("bearer")) {
        	authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.FAILURE);
        	authnAdapterResponse.setErrorMessage("Authorization header should contain bearer token");
        }
        String authTokenEncoded = authHeader.substring("bearer".length()).trim();
        System.out.println("Encoded JWT received: " + authTokenEncoded);
        
        if (publickKey == null && checkSignature) {
        	authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.FAILURE);
        	authnAdapterResponse.setErrorMessage("Failed to parse public key");
        }
        JwtConsumerBuilder jwtConsumerBuilder = new JwtConsumerBuilder()
        		.setRequireExpirationTime()
        		.setAllowedClockSkewInSeconds(30)
        		.setJwsAlgorithmConstraints(
    				new AlgorithmConstraints(ConstraintType.WHITELIST,
    						AlgorithmIdentifiers.RSA_USING_SHA256));
        if (checkSignature) {
	        if (publickKey.getType().equals("RSA")) {
	        	jwtConsumerBuilder.setVerificationKey(publickKey.getPublicKeyRSA().getKey());
	        }
	        else if (publickKey.getType().equals("X509")) {
	        	X509VerificationKeyResolver resolver = new X509VerificationKeyResolver(publickKey.getPublicKeyX509());
	        	resolver.setTryAllOnNoThumbHeader(true);
	        	jwtConsumerBuilder.setVerificationKeyResolver(resolver);
	        }
        }
        else {
        	jwtConsumerBuilder.setSkipSignatureVerification();
        }
        JwtConsumer jwtConsumer = jwtConsumerBuilder.build();
        
        try {
        	JwtClaims jwtClaims = jwtConsumer.processToClaims(authTokenEncoded);
        	System.out.println("JWT validation succeeded! " + jwtClaims);
        	
        	HashMap<String, Object> attributes = new HashMap<String, Object>();
        	System.out.println("Writing " + jwtClaims.getClaimValue(ATTR_SUB) + " into: " + ATTR_SUB);
            attributes.put(ATTR_SUB, jwtClaims.getClaimValue(ATTR_SUB));
            System.out.println("Writing " + jwtClaims.getClaimValue(ATTR_DOMAIN) + " into: " + ATTR_DOMAIN);
            attributes.put(ATTR_DOMAIN, jwtClaims.getClaimValue(ATTR_DOMAIN));
            System.out.println(fields.size());
            System.out.println(fields);
            for (String field : fields) {
                System.out.println("Writing " + jwtClaims.getClaimValue(field) + " into: " + field);
				attributes.put(field, jwtClaims.getClaimValue(field));
			}

            authnAdapterResponse.setAttributeMap(attributes);
            authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.SUCCESS);
        }
        catch (InvalidJwtException e){
        	System.out.println("Invalid JWT: " + e);
        	if (e.hasExpired()) {
        		authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.FAILURE);
            	authnAdapterResponse.setErrorMessage("JWT token expired");
        	}
        	else {
	        	authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.FAILURE);
	        	authnAdapterResponse.setErrorMessage("Invalid JWT " + e.getMessage());
        	}
        }

        return authnAdapterResponse;
    }

    /**
     * This method is deprecated. It is not called when IdpAuthenticationAdapterV2 is implemented. It is replaced by
     * {@link #lookupAuthN(HttpServletRequest, HttpServletResponse, Map)}
     * 
     * @deprecated
     */
    @SuppressWarnings(value = { "rawtypes" })
    public Map lookupAuthN(HttpServletRequest req, HttpServletResponse resp, String partnerSpEntityId,
            AuthnPolicy authnPolicy, String resumePath) throws AuthnAdapterException, IOException
    {
        throw new UnsupportedOperationException();
    }
    
    private RsaJsonWebKey parsePublicKeyRSA(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        pem = pem.replace("-----BEGIN PUBLIC KEY-----", "");
        pem = pem.replace("-----END PUBLIC KEY-----", "");
        pem = pem.trim();
    	
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.decode(pem));
    	KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    	RSAPublicKey pubKey = (RSAPublicKey)keyFactory.generatePublic(pubKeySpec);
    	RsaJsonWebKey webKey = new RsaJsonWebKey(pubKey);
    	
    	return webKey;
    }
    
    private X509Certificate parsePublicKeyX509(String pem) throws JoseException {
    	pem = pem.replace("-----BEGIN CERTIFICATE-----", "");
    	pem = pem.replace("-----END CERTIFICATE-----", "");
    	pem = pem.trim();
    	
    	X509Util x509util = new X509Util();
    	X509Certificate cer = x509util.fromBase64Der(pem);
    	
    	return cer;
    }
    
    private class GenericPublicKey {
    	private final Object publicKey;
    	private final String type;
    	
    	public GenericPublicKey(X509Certificate cer) {
    		publicKey = cer;
    		type = "X509";
    	}
    	
    	public GenericPublicKey(RsaJsonWebKey key) {
    		publicKey = key;
    		type = "RSA";
    	}
    	
    	public X509Certificate getPublicKeyX509() {
    		if (type.equals("X509")) {
    			return (X509Certificate)publicKey;
    		}
    		return null;
    	}
    	
    	public RsaJsonWebKey getPublicKeyRSA() {
    		if (type.equals("RSA")) {
    			return (RsaJsonWebKey)publicKey;
    		}
    		return null;
    	}
    	
    	public String getType() {
    		return type;
    	}
    }
    
    private GenericPublicKey parsePublicKey(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException, JoseException {
    	if (pem.indexOf("BEGIN PUBLIC KEY") != -1) {
    		RsaJsonWebKey key = parsePublicKeyRSA(pem);
    		return new GenericPublicKey(key);
    	}
    	X509Certificate cer = parsePublicKeyX509(pem);
    	return new GenericPublicKey(cer);
    }
}