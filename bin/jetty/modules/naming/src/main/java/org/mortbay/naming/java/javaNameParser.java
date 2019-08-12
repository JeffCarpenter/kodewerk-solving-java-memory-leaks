// ========================================================================
// $Id: javaNameParser.java 231 2006-02-19 15:09:58Z janb $
// Copyright 2006 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.naming.java;

import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;


/**
 * javaNameParser
 *
 */
public class javaNameParser implements NameParser
{

    static Properties syntax = new Properties();

    static 
    {
      syntax.put("jndi.syntax.direction", "left_to_right");
      syntax.put("jndi.syntax.separator", "/");
      syntax.put("jndi.syntax.ignorecase", "false");
    }

   /**
    * Parse a name into its components.
    * @param  name The non-null string name to parse.
    * @return A non-null parsed form of the name using the naming convention
    * of this parser.
    * @exception NamingException If a naming exception was encountered.
    */
   public Name parse(String name) throws NamingException
   {
      return new CompoundName(name, syntax);
   }

}
