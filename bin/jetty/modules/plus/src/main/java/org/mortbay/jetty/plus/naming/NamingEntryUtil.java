package org.mortbay.jetty.plus.naming;




import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.mortbay.log.Log;


public class NamingEntryUtil
{
 
    
    /**
     * Link a name in a webapp's java:/comp/evn namespace to a pre-existing
     * resource. The pre-existing resource can be either in the webapp's
     * naming environment, or in the container's naming environment. Webapp's 
     * environment takes precedence over the server's namespace.
     * 
     * @param asName the name to bind as
     * @param mappedName the name from the environment to link to asName
     * @param namingEntryType
     * @throws NamingException
     */
    public static boolean bindToENC (Object scope, String asName, String mappedName)
    throws NamingException
    {  
        if (asName==null||asName.trim().equals(""))
            throw new NamingException ("No name for NamingEntry");

        if (mappedName==null || "".equals(mappedName))
            mappedName=asName;
        
        NamingEntry entry = lookupNamingEntry (scope, mappedName);
        if (entry == null)
            return false;
        
        entry.bindToENC(asName);
        return true;
     }

    
    
 
    
    /**
     * Find a NamingEntry instance. 
     * 
     * First the webapp's naming space is searched, and then
     * the container's.
     * 
     * @param jndiName name to lookup
     * @return
     * @throws NamingException
     */
    public static NamingEntry lookupNamingEntry (Object scope, String jndiName)
    throws NamingException
    {
        NamingEntry entry = null;
        try
        {         
            Name scopeName = getNameForScope(scope);
            InitialContext ic = new InitialContext();   
            NameParser parser = ic.getNameParser("");
            Name namingEntryName = makeNamingEntryName(parser, jndiName);  
            scopeName.addAll(namingEntryName);           
            entry =  (NamingEntry)ic.lookup(scopeName);
        }
        catch (NameNotFoundException ee)
        {
        }

        return entry;
    }

    
    /** 
     * Get all NameEntries of a certain type in the given naming
     * environment scope (server-wide names or context-specific names)
     * 
     * @param scope 
     * @param clazz the type of the entry
     * @return
     * @throws NamingException
     */
    public static List lookupNamingEntries (Object scope, Class clazz)
    throws NamingException
    { 
        try
        {
            Context scopeContext = getContextForScope(scope);
            Context namingEntriesContext = (Context)scopeContext.lookup(NamingEntry.__contextName);
            ArrayList list = new ArrayList();
            lookupNamingEntries(list, namingEntriesContext, clazz);
            return list;
        }
        catch (NameNotFoundException e)
        {
            return Collections.EMPTY_LIST;
        }
    }
    
    
    public static Name makeNamingEntryName (NameParser parser, NamingEntry namingEntry)
    throws NamingException
    {
        return makeNamingEntryName(parser, (namingEntry==null?null:namingEntry.getJndiName()));
    }
    
    public static Name makeNamingEntryName (NameParser parser, String jndiName)
    throws NamingException
    {
        if (jndiName==null)
            return null;
        
        if (parser==null)
        {
            InitialContext ic = new InitialContext();
            parser = ic.getNameParser("");
        }
        
        Name name = parser.parse("");
        name.add(NamingEntry.__contextName);
        name.addAll(parser.parse(jndiName));
        return name;
    }
    

    public static Name getNameForScope (Object scope)
    {
        try
        {
            InitialContext ic = new InitialContext();
            NameParser parser = ic.getNameParser("");
            Name name = parser.parse("");
            if (scope != null)
            {
                name.add(canonicalizeScope(scope));
            }  
            return name;
        }
        catch (NamingException e)
        {
            Log.warn(e);
            return null;
        }
    }

    public static Context getContextForScope(Object scope)
    throws NamingException
    {

        InitialContext ic = new InitialContext();
        NameParser parser = ic.getNameParser("");
        Name name = parser.parse("");
        if (scope != null)
        {
            name.add(canonicalizeScope(scope));
        }  
        return (Context)ic.lookup(name);
    }
    
    public static Context getContextForNamingEntries (Object scope)
    throws NamingException
    {
        Context scopeContext = getContextForScope(scope);
        return (Context)scopeContext.lookup(NamingEntry.__contextName);
    }

    /**
     * Build up a list of NamingEntry objects that are of a specific type.
     * 
     * @param list
     * @param context
     * @param clazz
     * @return
     * @throws NamingException
     */
    private static List lookupNamingEntries (List list, Context context, Class clazz)
    throws NamingException
    {
        try
        {
            NamingEnumeration nenum = context.listBindings("");
            while (nenum.hasMoreElements())
            {
                Binding binding = (Binding)nenum.next();
                if (binding.getObject() instanceof Context)
                    lookupNamingEntries (list, (Context)binding.getObject(), clazz);
                else if (clazz.isInstance(binding.getObject()))
                  list.add(binding.getObject());
            }
        }
        catch (NameNotFoundException e)
        {
            Log.debug("No entries of type "+clazz.getName()+" in context="+context);
        }

        return list;
    }

    private static String canonicalizeScope(Object scope)
    {
        if (scope==null)
            return "";

        String str = scope.toString();
        str=str.replace('/', '_').replace(' ', '_');
        return str;
    }
}
