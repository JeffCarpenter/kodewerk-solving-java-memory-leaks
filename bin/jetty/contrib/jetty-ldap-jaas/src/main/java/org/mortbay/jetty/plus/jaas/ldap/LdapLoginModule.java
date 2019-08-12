package org.mortbay.jetty.plus.jaas.ldap;

// ========================================================================
// Copyright 2007 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.mortbay.jetty.plus.jaas.callback.ObjectCallback;
import org.mortbay.jetty.plus.jaas.spi.AbstractLoginModule;
import org.mortbay.jetty.plus.jaas.spi.UserInfo;
import org.mortbay.jetty.security.Credential;
import org.mortbay.log.Log;

/**
 *
 * A LdapLoginModule for use with JAAS setups
 *
 * The jvm should be started with the following parameter:
 * <br><br>
 * <code>
 * -Djava.security.auth.login.config=etc/ldap-loginModule.conf
 * </code>
 * <br><br>
 * and an example of the ldap-loginModule.conf would be:
 * <br><br>
 * <pre>
 * ldaploginmodule {
 *    org.mortbay.jetty.plus.jaas.spi.LdapLoginModule required
 *    debug="true"
 *    useLdaps="false"
 *    contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
 *    hostname="ldap.example.com"
 *    port="389"
 *    bindDn="cn=Directory Manager"
 *    bindPassword="directory"
 *    authenticationMethod="simple"
 *    forceBindingLogin="false"
 *    userBaseDn="ou=people,dc=alcatel"
 *    userRdnAttribute="uid"
 *    userIdAttribute="uid"
 *    userPasswordAttribute="userPassword"
 *    userObjectClass="inetOrgPerson"
 *    roleBaseDn="ou=groups,dc=example,dc=com"
 *    roleNameAttribute="cn"
 *    roleMemberAttribute="uniqueMember"
 *    roleObjectClass="groupOfUniqueNames";
 *    };
 *  </pre>
 *
 * @author Jesse McConnell <jesse@codehaus.org>
 * @author Frederic Nizery <frederic.nizery@alcatel-lucent.fr>
 * @author Trygve Laugstol <trygvis@codehaus.org>
 */
public class LdapLoginModule extends AbstractLoginModule
{
    /**
     * hostname of the ldap server
     */
    private String _hostname;

    /**
     * port of the ldap server
     */
    private int _port;

    /**
     * Context.SECURITY_AUTHENTICATION
     */
    private String _authenticationMethod;

    /**
     * Context.INITIAL_CONTEXT_FACTORY
     */
    private String _contextFactory;

    /**
     * root DN used to connect to
     */
    private String _bindDn;

    /**
     * password used to connect to the root ldap context
     */
    private String _bindPassword;

    /**
     * object class of a user
     */
    private String _userObjectClass = "inetOrgPerson";

    /**
     * attribute that the principal is located
     */
    private String _userRdnAttribute = "uid";

    /**
     * attribute that the principal is located
     */
    private String _userIdAttribute = "cn";

    /**
     * name of the attribute that a users password is stored under
     * <p/>
     * NOTE: not always accessible, see force binding login
     */
    private String _userPasswordAttribute = "userPassword";

    /**
     * base DN where users are to be searched from
     */
    private String _userBaseDn;

    /**
     * base DN where role membership is to be searched from
     */
    private String _roleBaseDn;

    /**
     * object class of roles
     */
    private String _roleObjectClass = "groupOfUniqueNames";

    /**
     * name of the attribute that a username would be under a role class
     */
    private String _roleMemberAttribute = "uniqueMember";

    /**
     * the name of the attribute that a role would be stored under
     */
    private String _roleNameAttribute = "roleName";

    private boolean _debug;

    /**
     * if the getUserInfo can pull a password off of the user then
     * password comparison is an option for authn, to force binding
     * login checks, set this to true
     */
    private boolean _forceBindingLogin = false;
    
    /**
     * When true changes the protocol to ldaps
     */
    private boolean _useLdaps = false;

    private DirContext _rootContext;

    /**
     * get the available information about the user
     * <p/>
     * for this LoginModule, the credential can be null which will result in a
     * binding ldap authentication scenario
     * <p/>
     * roles are also an optional concept if required
     *
     * @param username
     * @return
     * @throws Exception
     */
    public UserInfo getUserInfo(String username) throws Exception
    {
        String pwdCredential = getUserCredentials(username);

        if (pwdCredential == null)
        {
            return null;
        }

        pwdCredential = convertCredentialLdapToJetty(pwdCredential);
        Credential credential = Credential.getCredential(pwdCredential);
        List roles = getUserRoles(_rootContext, username);

        return new UserInfo(username, credential, roles);
    }

    protected String doRFC2254Encoding(String inputString)
    {
        StringBuffer buf = new StringBuffer(inputString.length());
        for (int i = 0; i < inputString.length(); i++)
        {
            char c = inputString.charAt(i);
            switch (c)
            {
                case '\\':
                    buf.append("\\5c");
                    break;
                case '*':
                    buf.append("\\2a");
                    break;
                case '(':
                    buf.append("\\28");
                    break;
                case ')':
                    buf.append("\\29");
                    break;
                case '\0':
                    buf.append("\\00");
                    break;
                default:
                    buf.append(c);
                    break;
            }
        }
        return buf.toString();
    }

    /**
     * attempts to get the users credentials from the users context
     * <p/>
     * NOTE: this is not an user authenticated operation
     *
     * @param username
     * @return
     * @throws LoginException
     */
    private String getUserCredentials(String username) throws LoginException
    {
        String ldapCredential = null;

        SearchControls ctls = new SearchControls();
        ctls.setCountLimit(1);
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = "(&(objectClass={0})({1}={2}))";

        Log.debug("Searching for users with filter: \'" + filter + "\'" + " from base dn: " + _userBaseDn);

        try
        {
            Object[] filterArguments = {_userObjectClass, _userIdAttribute, username};
            NamingEnumeration results = _rootContext.search(_userBaseDn, filter, filterArguments, ctls);

            Log.debug("Found user?: " + results.hasMoreElements());

            if (!results.hasMoreElements())
            {
                throw new LoginException("User not found.");
            }

            SearchResult result = findUser(username);

            Attributes attributes = result.getAttributes();

            Attribute attribute = attributes.get(_userPasswordAttribute);
            if (attribute != null)
            {
                try
                {
                    byte[] value = (byte[]) attribute.get();

                    ldapCredential = new String(value);
                }
                catch (NamingException e)
                {
                    Log.debug("no password available under attribute: " + _userPasswordAttribute);
                }
            }
        }
        catch (NamingException e)
        {
            throw new LoginException("Root context binding failure.");
        }

        Log.debug("user cred is: " + ldapCredential);

        return ldapCredential;
    }

    /**
     * attempts to get the users roles from the root context
     * <p/>
     * NOTE: this is not an user authenticated operation
     *
     * @param dirContext
     * @param username
     * @return
     * @throws LoginException
     */
    private List getUserRoles(DirContext dirContext, String username) throws LoginException, NamingException
    {
        String userDn = _userRdnAttribute + "=" + username + "," + _userBaseDn;

        return getUserRolesByDn(dirContext, userDn);
    }

    private List getUserRolesByDn(DirContext dirContext, String userDn) throws LoginException, NamingException
    {
        ArrayList roleList = new ArrayList();

        if (dirContext == null || _roleBaseDn == null || _roleMemberAttribute == null || _roleObjectClass == null)
        {
            return roleList;
        }

        SearchControls ctls = new SearchControls();
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = "(&(objectClass={0})({1}={2}))";
        Object[] filterArguments = {_roleObjectClass, _roleMemberAttribute, userDn};
        NamingEnumeration results = dirContext.search(_roleBaseDn, filter, filterArguments, ctls);

        Log.debug("Found user roles?: " + results.hasMoreElements());

        while (results.hasMoreElements())
        {
            SearchResult result = (SearchResult)results.nextElement();

            Attributes attributes = result.getAttributes();

            if (attributes == null)
            {
                continue;
            }

            Attribute roleAttribute = attributes.get(_roleNameAttribute);

            if (roleAttribute == null)
            {
                continue;
            }

            NamingEnumeration roles = roleAttribute.getAll();
            while (roles.hasMore())
            {
                roleList.add(roles.next());
            }
        }

        return roleList;
    }

    /**
     * since ldap uses a context bind for valid authentication checking, we override login()
     * <p/>
     * if credentials are not available from the users context or if we are forcing the binding check
     * then we try a binding authentication check, otherwise if we have the users encoded password then
     * we can try authentication via that mechanic
     *
     * @return
     * @throws LoginException
     */
    public boolean login() throws LoginException
    {
        try
        {
            if (getCallbackHandler() == null)
            {
                throw new LoginException("No callback handler");
            }

            Callback[] callbacks = configureCallbacks();
            getCallbackHandler().handle(callbacks);

            String webUserName = ((NameCallback) callbacks[0]).getName();
            Object webCredential = ((ObjectCallback) callbacks[1]).getObject();

            if (webUserName == null || webCredential == null)
            {
                setAuthenticated(false);
                return isAuthenticated();
            }

            if (_forceBindingLogin)
            {
                return bindingLogin(webUserName, webCredential);
            }

            // This sets read and the credential
            UserInfo userInfo = getUserInfo(webUserName);

            if( userInfo == null) {
                setAuthenticated(false);
                return false;
            }

            setCurrentUser(new JAASUserInfo(userInfo));

            if (webCredential instanceof String)
            {
                return credentialLogin(Credential.getCredential((String) webCredential));
            }

            return credentialLogin(webCredential);
        }
        catch (UnsupportedCallbackException e)
        {
            throw new LoginException("Error obtaining callback information.");
        }
        catch (IOException e)
        {
            if (_debug)
            {
                e.printStackTrace();
            }
            throw new LoginException("IO Error performing login.");
        }
        catch (Exception e)
        {
            if (_debug)
            {
                e.printStackTrace();
            }
            throw new LoginException("Error obtaining user info.");
        }
    }

    /**
     * password supplied authentication check
     *
     * @param webCredential
     * @return
     * @throws LoginException
     */
    protected boolean credentialLogin(Object webCredential) throws LoginException
    {
        setAuthenticated(getCurrentUser().checkCredential(webCredential));
        return isAuthenticated();
    }

    /**
     * binding authentication check
     * This methode of authentication works only if the user branch of the DIT (ldap tree)
     * has an ACI (acces control instruction) that allow the access to any user or at least
     * for the user that logs in.
     *
     * @param username
     * @param password
     * @return
     * @throws LoginException
     */
    protected boolean bindingLogin(String username, Object password) throws LoginException, NamingException
    {
        SearchResult searchResult = findUser(username);

        DirContext usrsContext = (DirContext)_rootContext.lookup(_userBaseDn);
        DirContext usrContext = (DirContext)usrsContext.lookup(searchResult.getName());
        String userDn = usrContext.getNameInNamespace();

        Log.info("Attempting authentication: " + userDn);

        Hashtable environment = getEnvironment();
        environment.put(Context.SECURITY_PRINCIPAL, userDn);
        environment.put(Context.SECURITY_CREDENTIALS, password);

        DirContext dirContext = new InitialDirContext(environment);

        List roles = getUserRolesByDn(dirContext, userDn);

        UserInfo userInfo = new UserInfo(username, null, roles);
        setCurrentUser(new JAASUserInfo(userInfo));
        setAuthenticated(true);

        return true;
    }

    private SearchResult findUser(String username) throws NamingException, LoginException
    {
        SearchControls ctls = new SearchControls();
        ctls.setCountLimit(1);
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = "(&(objectClass={0})({1}={2}))";

        Log.info("Searching for users with filter: \'" + filter + "\'" + " from base dn: " + _userBaseDn);

        Object[] filterArguments = new Object[]{
            _userObjectClass,
            _userIdAttribute,
            username
        };
        NamingEnumeration results = _rootContext.search(_userBaseDn, filter, filterArguments, ctls);

        Log.info("Found user?: " + results.hasMoreElements());

        if (!results.hasMoreElements())
        {
            throw new LoginException("User not found.");
        }

        return (SearchResult)results.nextElement();
    }

    public void initialize(Subject subject,
                           CallbackHandler callbackHandler,
                           Map sharedState,
                           Map options)
    {
        super.initialize(subject, callbackHandler, sharedState, options);

        _hostname = (String) options.get("hostname");
        _port = Integer.parseInt((String) options.get("port"));
        _contextFactory = (String) options.get("contextFactory");
        _bindDn = (String) options.get("bindDn");
        _bindPassword = (String) options.get("bindPassword");
        _authenticationMethod = (String) options.get("authenticationMethod");

        _userBaseDn = (String) options.get("userBaseDn");

        _roleBaseDn = (String) options.get("roleBaseDn");

        if (options.containsKey("forceBindingLogin"))
        {
            _forceBindingLogin = Boolean.valueOf((String) options.get("forceBindingLogin")).booleanValue();
        }
        
        if (options.containsKey("useLdaps"))
        {
            _useLdaps = Boolean.parseBoolean((String) options.get("useLdaps"));
        }     
        
        _userObjectClass = getOption(options, "userObjectClass", _userObjectClass);
        _userRdnAttribute = getOption(options, "userRdnAttribute", _userRdnAttribute);
        _userIdAttribute = getOption(options, "userIdAttribute", _userIdAttribute);
        _userPasswordAttribute = getOption(options, "userPasswordAttribute", _userPasswordAttribute);
        _roleObjectClass = getOption(options, "roleObjectClass", _roleObjectClass);
        _roleMemberAttribute = getOption(options, "roleMemberAttribute", _roleMemberAttribute);
        _roleNameAttribute = getOption(options, "roleNameAttribute", _roleNameAttribute);
        _debug = Boolean.valueOf(String.valueOf(getOption(options, "debug", Boolean.toString(_debug)))).booleanValue();

        try
        {
            _rootContext = new InitialDirContext(getEnvironment());
        }
        catch (NamingException ex)
        {
            throw new RuntimeException("Unable to establish root context", ex);
        }
    }

    public boolean commit() throws LoginException 
    {
        try 
        {
            _rootContext.close();
        } 
        catch (NamingException e) 
        {
            throw new LoginException("error closing root context: " + e.getMessage());
        }

        return super.commit();
    }

    public boolean abort() throws LoginException 
    {
        try 
        {
            _rootContext.close();
        } 
        catch (NamingException e) 
        {
            throw new LoginException("error closing root context: " + e.getMessage());
        }

        return super.abort();
    }

    private String getOption(Map options, String key, String defaultValue)
    {
        Object value = options.get(key);

        if (value == null) {
            return defaultValue;
        }

        return (String) value;
    }

    /**
     * get the context for connection
     *
     * @return
     */
    public Hashtable getEnvironment()
    {
        Properties env = new Properties();

        env.put(Context.INITIAL_CONTEXT_FACTORY, _contextFactory);

        if (_hostname != null)
        {
            env.put(Context.PROVIDER_URL, (_useLdaps?"ldaps://":"ldap://") + _hostname + (_port==0?"":":"+_port) +"/");
        }

        if (_authenticationMethod != null)
        {
            env.put(Context.SECURITY_AUTHENTICATION, _authenticationMethod);
        }

        if (_bindDn != null)
        {
            env.put(Context.SECURITY_PRINCIPAL, _bindDn);
        }

        if (_bindPassword != null)
        {
            env.put(Context.SECURITY_CREDENTIALS, _bindPassword);
        }

        return env;
    }

    public static String convertCredentialJettyToLdap( String encryptedPassword )
    {
        if ("MD5:".startsWith(encryptedPassword.toUpperCase()))
        {
            return "{MD5}" + encryptedPassword.substring("MD5:".length(), encryptedPassword.length());
        }

        if ("CRYPT:".startsWith(encryptedPassword.toUpperCase()))
        {
            return "{CRYPT}" + encryptedPassword.substring("CRYPT:".length(), encryptedPassword.length());
        }

        return encryptedPassword;
    }

    public static String convertCredentialLdapToJetty( String encryptedPassword )
    {
        if (encryptedPassword == null)
        {
            return encryptedPassword;
        }

        if ("{MD5}".startsWith(encryptedPassword.toUpperCase()))
        {
            return "MD5:" + encryptedPassword.substring("{MD5}".length(), encryptedPassword.length());
        }

        if ("{CRYPT}".startsWith(encryptedPassword.toUpperCase()))
        {
            return "CRYPT:" + encryptedPassword.substring("{CRYPT}".length(), encryptedPassword.length());
        }

        return encryptedPassword;
    }
}
