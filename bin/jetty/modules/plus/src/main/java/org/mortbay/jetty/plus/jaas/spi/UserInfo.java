// ========================================================================
// $Id: UserInfo.java 305 2006-03-07 10:32:14Z janb $
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus.jaas.spi;

import java.util.ArrayList;
import java.util.List;

import org.mortbay.jetty.security.Credential;

/**
 * UserInfo
 *
 * This is the information read from the external source
 * about a user.
 * 
 * Can be cached by a UserInfoCache implementation
 */
public class UserInfo
{
    
    private String userName;
    private Credential credential;
    private List roleNames;
    
    
    public UserInfo (String userName, Credential credential, List roleNames)
    {
        this.userName = userName;
        this.credential = credential;
        this.roleNames = new ArrayList();
        if (roleNames != null)
            this.roleNames.addAll(roleNames);
    }
    
    public String getUserName()
    {
        return this.userName;
    }
    
    public List getRoleNames ()
    {
        return new ArrayList(this.roleNames);
    }
    
    public boolean checkCredential (Object suppliedCredential)
    {
        return this.credential.check(suppliedCredential);
    }
    
    protected Credential getCredential ()
    {
        return this.credential;
    }
    
}
