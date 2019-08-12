// ========================================================================
// $Id: DefList.java,v 1.3 2004/05/09 20:31:28 gregwilkins Exp $
// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.html;
import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

// =======================================================================
public class DefList extends Element
{

    // ------------------------------------------------------------
    public DefList()
    {
        terms = new Vector();
        defs = new Vector();
    }

    // ------------------------------------------------------------
    public void add(Element term, Element def)
    {
        terms.addElement(term);
        defs.addElement(def);
    }

    // ------------------------------------------------------------
    public void write(Writer out)
         throws IOException
    {
        out.write("<dl"+attributes()+">");

        if (terms.size() != defs.size())
            throw new Error("mismatched Vector sizes");

        for (int i=0; i <terms.size() ; i++)
        {
            out.write("<dt>");
            ((Element)terms.elementAt(i)).write(out);
            out.write("</dt><dd>");
            ((Element)defs.elementAt(i)).write(out);
            out.write("</dd>");
        }

        out.write("</dl>");
    }

    // ------------------------------------------------------------
    private Vector terms;
    private Vector defs;
}

