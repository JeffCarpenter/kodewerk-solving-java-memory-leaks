// ========================================================================
// Copyright 2003-2005 Mort Bay Consulting Pty. Ltd.
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
package org.mortbay.start;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

/*-------------------------------------------*/
/**
 * Main start class. This class is intended to be the main class listed in the MANIFEST.MF of the
 * start.jar archive. It allows an application to be started with the command "java -jar
 * start.jar". The behaviour of Main is controlled by the "org/mortbay/start/start.config" file
 * obtained as a resource or file. This can be overridden with the START system property. The
 * format of each line in this file is:
 * 
 * <PRE>
 * 
 * SUBJECT [ [!] CONDITION [AND|OR] ]*
 * 
 * </PRE>
 * 
 * where SUBJECT:
 * 
 * <PRE>
 * ends with ".class" is the Main class to run.
 * ends with ".xml" is a configuration file for the command line
 * ends with "/" is a directory from which add all jar and zip files from.
 * ends with "/*" is a directory from which add all unconsidered jar and zip files from.
 * Containing = are used to assign system properties.
 * all other subjects are treated as files to be added to the classpath.
 * </PRE>
 * 
 * Subjects may include system properties with $(propertyname) syntax. File subjects starting with
 * "/" are considered absolute, all others are relative to the home directory.
 * <P>
 * CONDITION is one of:
 * 
 * <PRE>
 * 
 * always
 * never
 * available package.class 
 * java OPERATOR n.n 
 * nargs OPERATOR n
 * OPERATOR := one of "<",">"," <=",">=","==","!="
 * 
 * </PRE>
 * 
 * CONTITIONS can be combined with AND OR or !, with AND being the assume operator for a list of
 * CONDITIONS. Classpath operations are evaluated on the fly, so once a class or jar is added to
 * the classpath, subsequent available conditions will see that class. The system parameter
 * CLASSPATH, if set is given to the start classloader before any paths from the configuration
 * file. Programs started with start.jar may be stopped with the stop.jar, which connects via a
 * local port to stop the server. The default port can be set with the STOP.PORT system property (a
 * port of < 0 disables the stop mechanism). If the STOP.KEY system property is set, then a random
 * key is generated and written to stdout. This key must be passed to the stop.jar.
 * 
 * @author Jan Hlavaty (hlavac@code.cz)
 * @author Greg Wilkins
 */
public class Main
{
    static boolean _debug=System.getProperty("DEBUG",null)!=null;
    private String _classname=null;
    private Classpath _classpath=new Classpath();
    private String _config=System.getProperty("START","org/mortbay/start/start.config");
    private ArrayList _xml=new ArrayList();
    private boolean _version=false;

    public static void main(String[] args)
    {
        try
        {
            if (args.length>0&&args[0].equalsIgnoreCase("--help"))
            {
                System.err
                        .println("Usage: java [-DDEBUG] [-DSTART=start.config] [-Dmain.class=org.MyMain] -jar start.jar [--help|--stop|--version] [config ...]");
                System.exit(1);
            }
            else if (args.length>0&&args[0].equalsIgnoreCase("--stop"))
            {
                new Main().stop();
            }
            else if (args.length>0&&args[0].equalsIgnoreCase("--version"))
            {
                String[] nargs=new String[args.length-1];
                System.arraycopy(args,1,nargs,0,nargs.length);
                Main main=new Main();
                main._version=true;
                main.start(nargs);
            }
            else
            {
                new Main().start(args);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static File getDirectory(String name)
    {
        try
        {
            if (name!=null)
            {
                File dir=new File(name).getCanonicalFile();
                if (dir.isDirectory())
                {
                    return dir;
                }
            }
        }
        catch (IOException e)
        {
        }
        return null;
    }

    boolean isAvailable(String classname)
    {
        try
        {
            Class.forName(classname);
            return true;
        }
        catch (NoClassDefFoundError e)
        {
        }
        catch (ClassNotFoundException e)
        {
        }
        ClassLoader loader=_classpath.getClassLoader();
        try
        {
            loader.loadClass(classname);
            return true;
        }
        catch (NoClassDefFoundError e)
        {
        }
        catch (ClassNotFoundException e)
        {
        }
        return false;
    }

    public void invokeMain(ClassLoader classloader, String classname, String[] args) throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException, ClassNotFoundException
    {
        Class invoked_class=null;
        invoked_class=classloader.loadClass(classname);

        if (_version)
        {
            System.err.println(invoked_class.getPackage().getImplementationTitle()+" "+invoked_class.getPackage().getImplementationVersion());
            System.exit(0);
        }

        Class[] method_param_types=new Class[1];
        method_param_types[0]=args.getClass();
        Method main=null;
        main=invoked_class.getDeclaredMethod("main",method_param_types);
        Object[] method_params=new Object[1];
        method_params[0]=args;

        main.invoke(null,method_params);
    }

    /* ------------------------------------------------------------ */
    String expand(String s)
    {
        int i1=0;
        int i2=0;
        while (s!=null)
        {
            i1=s.indexOf("$(",i2);
            if (i1<0)
                break;
            i2=s.indexOf(")",i1+2);
            if (i2<0)
                break;
            String property=System.getProperty(s.substring(i1+2,i2),"");
            s=s.substring(0,i1)+property+s.substring(i2+1);
        }
        return s;
    }

    /* ------------------------------------------------------------ */
    void configure(InputStream config, int nargs) throws Exception
    {
        BufferedReader cfg=new BufferedReader(new InputStreamReader(config,"ISO-8859-1"));
        Version java_version=new Version(System.getProperty("java.version"));
        Version ver=new Version();
        // JAR's already processed
        java.util.Hashtable done=new Hashtable();
        // Initial classpath
        String classpath=System.getProperty("CLASSPATH");
        if (classpath!=null)
        {
            StringTokenizer tok=new StringTokenizer(classpath,File.pathSeparator);
            while (tok.hasMoreTokens())
                _classpath.addComponent(tok.nextToken());
        }
        // Handle line by line
        String line=null;
        while (true)
        {
            line=cfg.readLine();
            if (line==null)
                break;
            if (line.length()==0||line.startsWith("#"))
                continue;
            try
            {
                StringTokenizer st=new StringTokenizer(line);
                String subject=st.nextToken();
                boolean expression=true;
                boolean not=false;
                String condition=null;
                // Evaluate all conditions
                while (st.hasMoreTokens())
                {
                    condition=st.nextToken();
                    if (condition.equalsIgnoreCase("!"))
                    {
                        not=true;
                        continue;
                    }
                    if (condition.equalsIgnoreCase("OR"))
                    {
                        if (expression)
                            break;
                        expression=true;
                        continue;
                    }
                    if (condition.equalsIgnoreCase("AND"))
                    {
                        if (!expression)
                            break;
                        continue;
                    }
                    boolean eval=true;
                    if (condition.equals("true")||condition.equals("always"))
                    {
                        eval=true;
                    }
                    else if (condition.equals("false")||condition.equals("never"))
                    {
                        eval=false;
                    }
                    else if (condition.equals("available"))
                    {
                        String class_to_check=st.nextToken();
                        eval=isAvailable(class_to_check);
                    }
                    else if (condition.equals("exists"))
                    {
                        try
                        {
                            eval=false;
                            File file=new File(expand(st.nextToken()));
                            eval=file.exists();
                        }
                        catch (Exception e)
                        {
                            if (_debug)
                                e.printStackTrace();
                        }
                    }
                    else if (condition.equals("property"))
                    {
                        String property=System.getProperty(st.nextToken());
                        eval=property!=null&&property.length()>0;
                    }
                    else if (condition.equals("java"))
                    {
                        String operator=st.nextToken();
                        String version=st.nextToken();
                        ver.parse(version);
                        eval=(operator.equals("<")&&java_version.compare(ver)<0)||(operator.equals(">")&&java_version.compare(ver)>0)
                                ||(operator.equals("<=")&&java_version.compare(ver)<=0)||(operator.equals("=<")&&java_version.compare(ver)<=0)
                                ||(operator.equals("=>")&&java_version.compare(ver)>=0)||(operator.equals(">=")&&java_version.compare(ver)>=0)
                                ||(operator.equals("==")&&java_version.compare(ver)==0)||(operator.equals("!=")&&java_version.compare(ver)!=0);
                    }
                    else if (condition.equals("nargs"))
                    {
                        String operator=st.nextToken();
                        int number=Integer.parseInt(st.nextToken());
                        eval=(operator.equals("<")&&nargs<number)||(operator.equals(">")&&nargs>number)||(operator.equals("<=")&&nargs<=number)
                                ||(operator.equals("=<")&&nargs<=number)||(operator.equals("=>")&&nargs>=number)||(operator.equals(">=")&&nargs>=number)
                                ||(operator.equals("==")&&nargs==number)||(operator.equals("!=")&&nargs!=number);
                    }
                    else
                    {
                        System.err.println("ERROR: Unknown condition: "+condition);
                        eval=false;
                    }
                    expression&=not?!eval:eval;
                    not=false;
                }
                String file=expand(subject).replace('/',File.separatorChar);
                if (_debug)
                    System.err.println((expression?"T ":"F ")+line);
                if (!expression)
                {
                    done.put(file,file);
                    continue;
                }
                // Handle the subject
                if (subject.indexOf("=")>0)
                {
                    int i=file.indexOf("=");
                    String property=file.substring(0,i);
                    String value=file.substring(i+1);
                    if (_debug)
                        System.err.println("  "+property+"="+value);
                    System.setProperty(property,value);
                }
                else if (subject.endsWith("/*"))
                {
                    // directory of JAR files - only add jars and zips
                    // within the directory
                    File dir=new File(file.substring(0,file.length()-1));
                    addJars(dir,done,false);
                }
                else if (subject.endsWith("/**"))
                {
                    //directory hierarchy of jar files - recursively add all
                    //jars and zips in the hierarchy
                    File dir=new File(file.substring(0,file.length()-2));
                    addJars(dir,done,true);
                }
                else if (subject.endsWith("/"))
                {
                    // class directory
                    File cd=new File(file);
                    String d=cd.getCanonicalPath();
                    if (!done.containsKey(d))
                    {
                        done.put(d,d);
                        boolean added=_classpath.addComponent(d);
                        if (_debug)
                            System.err.println((added?"  CLASSPATH+=":"  !")+d);
                    }
                }
                else if (subject.toLowerCase().endsWith(".xml"))
                {
                    // Config file
                    File f=new File(file);
                    if (f.exists())
                        _xml.add(f.getCanonicalPath());
                    if (_debug)
                        System.err.println("  ARGS+="+f);
                }
                else if (subject.toLowerCase().endsWith(".class"))
                {
                    // Class
                    String cn=expand(subject.substring(0,subject.length()-6));
                    if (cn!=null&&cn.length()>0)
                    {
                        if (_debug)
                            System.err.println("  CLASS="+cn);
                        _classname=cn;
                    }
                }
                else if (subject.toLowerCase().endsWith(".path"))
                {
                    //classpath (jetty.class.path?) to add to runtime classpath
                    String cn=expand(subject.substring(0,subject.length()-5));
                    if (cn!=null&&cn.length()>0)
                    {
                        if (_debug)
                            System.err.println("  PATH="+cn);
                        _classpath.addClasspath(cn);
                    }                  
                }
                else
                {
                    // single JAR file
                    File f=new File(file);
		    if(f.exists())
		    {
			String d=f.getCanonicalPath();
			if (!done.containsKey(d))
			{
			    done.put(d,d);
			    boolean added=_classpath.addComponent(d);
			    if (!added)
			    {
				added=_classpath.addClasspath(expand(subject));
				if (_debug)
				    System.err.println((added?"  CLASSPATH+=":"  !")+d);
			    }
			    else if (_debug)
				System.err.println((added?"  CLASSPATH+=":"  !")+d);
		        }
                    }
                }
            }
            catch (Exception e)
            {
                System.err.println("on line: '"+line+"'");
                e.printStackTrace();
            }
        }
    }

    /* ------------------------------------------------------------ */
    public void start(String[] args)
    {
        init(args);
        Monitor.monitor();
        start();
    }

    public void init(String[] args)
    {
        ArrayList al=new ArrayList();
        for (int i=0; i<args.length; i++)
        {
            if (args[i]==null)
                continue;
            else
                al.add(args[i]);
        }
        args=(String[])al.toArray(new String[al.size()]);
        // set up classpath:
        InputStream cpcfg=null;
        try
        {
            cpcfg=getClass().getClassLoader().getResourceAsStream(_config);
            if (_debug)
                System.err.println("config="+_config);
            if (cpcfg==null)
                cpcfg=new FileInputStream(_config);
            configure(cpcfg,args.length);
            File file=new File(System.getProperty("jetty.home"));
            String canonical=file.getCanonicalPath();
            System.setProperty("jetty.home",canonical);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        finally
        {
            try
            {
                cpcfg.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        // okay, classpath complete.
        System.setProperty("java.class.path",_classpath.toString());
        ClassLoader cl=_classpath.getClassLoader();
        if (_debug)
        {
            System.err.println("java.class.path="+System.getProperty("java.class.path"));
            System.err.println("jetty.home="+System.getProperty("jetty.home"));
            System.err.println("java.io.tmpdir="+System.getProperty("java.io.tmpdir"));
            System.err.println("java.class.path="+_classpath);
            System.err.println("classloader="+cl);
            System.err.println("classloader.parent="+cl.getParent());
        }

        for (int i=0; i<args.length; i++)
        {
            if (args[i]==null)
                continue;
            _xml.add(args[i]);
        }
     }

     public void start()
     {
        ClassLoader cl=_classpath.getClassLoader();
        // Invoke main(args) using new classloader.
        Thread.currentThread().setContextClassLoader(cl);
        // re-eval the policy now that env is set
        try
        {
            Policy policy=Policy.getPolicy();
            if (policy!=null)
                policy.refresh();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            String[] args=(String[])_xml.toArray(new String[_xml.size()]);
            //check for override of start class
            String mainClass=System.getProperty("jetty.server");
            if (mainClass!=null)
                _classname=mainClass;
            mainClass=System.getProperty("main.class");
            if (mainClass!=null)
                _classname=mainClass;
            if (_debug)
                System.err.println("main.class="+_classname);
            invokeMain(cl,_classname,args);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Stop a running jetty instance.
     */
    public void stop()
    {
        int _port=Integer.getInteger("STOP.PORT",-1).intValue();
        String _key=System.getProperty("STOP.KEY",null);

        try
        {
            if (_port<=0)
                System.err.println("STOP.PORT system property must be specified");
            if (_key==null)
            {
                _key="";
                System.err.println("STOP.KEY system property must be specified");
                System.err.println("Using empty key");
            }

            Socket s=new Socket(InetAddress.getByName("127.0.0.1"),_port);
            OutputStream out=s.getOutputStream();
            out.write((_key+"\r\nstop\r\n").getBytes());
            out.flush();
            s.close();
        }
        catch (ConnectException e)
        {
            System.err.println("ERROR: Not running!");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void addJars(File dir, Hashtable table, boolean recurse) throws IOException
    {
        File[] entries=dir.listFiles();

        for (int i=0; entries!=null&&i<entries.length; i++)
        {
            File entry=entries[i];

            if (entry.isDirectory()&&recurse)
                addJars(entry,table,recurse);
            else
            {
                String name=entry.getName().toLowerCase();
                if (name.endsWith(".jar")||name.endsWith(".zip"))
                {
                    String jar=entry.getCanonicalPath();
                    if (!table.containsKey(jar))
                    {
                        table.put(jar,jar);
                        boolean added=_classpath.addComponent(jar);
                        if (_debug)
                            System.err.println((added?"  CLASSPATH+=":"  !")+jar);
                    }
                }
            }
        }
    }
}
