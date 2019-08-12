//========================================================================
//$Id: Injection.java 4027 2008-11-12 00:59:06Z janb $
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty.plus.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;



import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.mortbay.log.Log;
import org.mortbay.util.IntrospectionUtil;

/**
 * Injection
 *
 * Represents the injection of a resource into a target (method or field).
 * The injection is performed by doing an ENC lookup using the jndi
 * name provided, and setting the object obtained on the target.
 *
 */
public class Injection
{
    private Class _targetClass;
    private String _jndiName;
    private String _mappingName;
    private Member _target;
    
    
    public Injection ()
    {}
    

    /**
     * @return the _className
     */
    public Class getTargetClass()
    {
        return _targetClass;
    }


    /**
     * @param name the _className to set
     */
    public void setTargetClass(Class clazz)
    {
        _targetClass = clazz;
    }
    
    /**
     * @return the jndiName
     */
    public String getJndiName()
    {
        return _jndiName;
    }
    /**
     * @param jndiName the jndiName to set
     */
    public void setJndiName(String jndiName)
    {
        this._jndiName = jndiName;
    }
    /**
     * @return the mappingName
     */
    public String getMappingName()
    {
        return _mappingName;
    }
    /**
     * @param mappingName the mappingName to set
     */
    public void setMappingName(String mappingName)
    {
        this._mappingName = mappingName;
    }
    
    /**
     * @return the target
     */
    public Member getTarget()
    {
        return _target;
    }
    
    /**
     * @param target the target to set
     */
    public void setTarget(Member target)
    {
        this._target = target;
    }

    //TODO: define an equals method
    
    public void setTarget (Class clazz, String targetName, Class targetType)
    {
        //first look for a javabeans style setter matching the targetName
        String setter = "set"+targetName.substring(0,1).toUpperCase()+targetName.substring(1);
        try
        {
            Log.debug("Looking for method for setter: "+setter+" with arg "+targetType);
            _target = IntrospectionUtil.findMethod(clazz, setter, new Class[] {targetType}, true, false); 
            _targetClass = clazz;
        }
        catch (NoSuchMethodException me)
        {
            //try as a field
            try
            {
                _target = IntrospectionUtil.findField(clazz, targetName, targetType, true, false);
                _targetClass = clazz;
            }
            catch (NoSuchFieldException fe)
            {
                throw new IllegalArgumentException("No such field or method "+targetName+" on class "+_targetClass);
            }
        }
    }

    
    /**
     * Inject a value for a Resource from JNDI into an object
     * @param injectable
     * @throws Exception
     */
    public void inject (Object injectable)
    {
        Member theTarget = getTarget(); 
        if (theTarget instanceof Field)
        {
            injectField((Field)theTarget, injectable);
        }
        else if (theTarget instanceof Method)
        {
            injectMethod((Method)theTarget, injectable);
        }
    }

    
    /**
     * The Resource must already exist in the ENC of this webapp.
     * @return
     * @throws Exception
     */
    public Object lookupInjectedValue ()
    throws NamingException
    {
        InitialContext context = new InitialContext();
        return context.lookup("java:comp/env/"+getJndiName());
    }
    
    

    /**
     * Inject value from jndi into a field of an instance
     * @param field
     * @param injectable
     */
    public void injectField (Field field, Object injectable)
    {           
        try
        {
            //validateInjection(field, injectable);
            boolean accessibility = field.isAccessible();
            field.setAccessible(true);
            field.set(injectable, lookupInjectedValue());
            field.setAccessible(accessibility);
        }
        catch (Exception e)
        {
            Log.warn(e);
            throw new IllegalStateException("Inject failed for field "+field.getName());
        }
    }
    
    /**
     * Inject value from jndi into a setter method of an instance
     * @param method
     * @param injectable
     */
    public void injectMethod (Method method, Object injectable)
    {
        //validateInjection(method, injectable);
        try
        {
            boolean accessibility = method.isAccessible();
            method.setAccessible(true);
            method.invoke(injectable, new Object[] {lookupInjectedValue()});
            method.setAccessible(accessibility);
        }
        catch (Exception e)
        {
            Log.warn(e);
            throw new IllegalStateException("Inject failed for method "+method.getName());
        }
    }
    
  

    
    private void validateInjection (Method method, Object injectable)
    throws NoSuchMethodException
    {
        if ((injectable==null) || (method==null))
            return;
        //check the injection target actually has a matching method
        //TODO: think about this, they have to be assignable
        injectable.getClass().getMethod(method.getName(), method.getParameterTypes());    
    }
    
    private void validateInjection (Field field, Object injectable) 
    throws NoSuchFieldException
    {
        if ((field==null) || (injectable==null))
            return;

        Field f = injectable.getClass().getField(field.getName());
        if (!f.getType().isAssignableFrom(field.getType()))
            throw new NoSuchFieldException("Mismatching type of field: "+f.getType().getName()+" v "+field.getType().getName());
    }   
}
