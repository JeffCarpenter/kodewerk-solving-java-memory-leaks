// ========================================================================
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
package org.mortbay.jetty.handler.rewrite;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.rewrite.PatternRule;

import junit.framework.TestCase;

public class PatternRuleTest extends TestCase
{
    private PatternRule _rule;

    public void setUp()
    {
        _rule = new TestPatternRule();
    }
    
    public void tearDown()
    {
        _rule = null;
    }
    
    public void testTrueMatch() throws IOException
    {
        String[][] matchCases = {
                // index 0 - pattern
                // index 1 - URI to match
                
                {"/abc", "/abc"},
                {"/abc/", "/abc/"},
                
                {"/abc/path/longer", "/abc/path/longer"},
                {"/abc/path/longer/", "/abc/path/longer/"},
                
                {"/abc/*", "/abc/hello.jsp"},
                {"/abc/*", "/abc/a"},
                {"/abc/*", "/abc/a/hello.jsp"},
                {"/abc/*", "/abc/a/b"},
                {"/abc/*", "/abc/a/b/hello.jsp"},
                {"/abc/*", "/abc/a/b/c"},
                {"/abc/*", "/abc/a/b/c/hello.jsp"},
                
                {"/abc/def/*", "/abc/def/gf"},
                {"/abc/def/*", "/abc/def/gf.html"},
                {"/abc/def/*", "/abc/def/ghi"},
                {"/abc/def/*", "/abc/def/ghi/"},
                {"/abc/def/*", "/abc/def/ghi/hello.html"},
                
                {"*.do", "/abc.do"},
                {"*.do", "/abc/hello.do"},
                {"*.do", "/abc/def/hello.do"},
                {"*.do", "/abc/def/ghi/hello.do"},
                
                {"*.jsp", "/abc.jsp"},
                {"*.jsp", "/abc/hello.jsp"},
                {"*.jsp", "/abc/def/hello.jsp"},
                {"*.jsp", "/abc/def/ghi/hello.jsp"},
                
                {"/", "/Other"},
                {"/", "/Other/hello.do"},
                {"/", "/Other/path"},
                {"/", "/Other/path/hello.do"},
                {"/", "/abc/def"},
                
                {"/abc:/def", "/abc:/def"}
        };
        
        for (int i = 0; i < matchCases.length; i++)
        {
            String[] matchCase = matchCases[i];
            assertMatch(true, matchCase);
        }
    }
    
    public void testFalseMatch() throws IOException
    {
        String[][] matchCases = {
                
                {"/abc", "/abcd"},
                {"/abc/", "/abcd/"},
                
                {"/abc/path/longer", "/abc/path/longer/"},
                {"/abc/path/longer", "/abc/path/longer1"},
                {"/abc/path/longer/", "/abc/path/longer"},
                {"/abc/path/longer/", "/abc/path/longer1/"},
                
                {"/*.jsp", "/hello.jsp"},
                {"/abc/*.jsp", "/abc/hello.jsp"},
                
                {"*.jsp", "/hello.1jsp"},
                {"*.jsp", "/hello.jsp1"},
                {"*.jsp", "/hello.do"},
                
                {"*.jsp", "/abc/hello.do"},
                {"*.jsp", "/abc/def/hello.do"},
                {"*.jsp", "/abc.do"}
        };
        
        for (int i = 0; i < matchCases.length; i++)
        {
            String[] matchCase = matchCases[i];
            assertMatch(false, matchCase);
        }
    }
    
    private void assertMatch(boolean flag, String[] matchCase) throws IOException
    {
        _rule.setPattern(matchCase[0]);
        final String uri=matchCase[1];
        String result = _rule.matchAndApply(uri,
        new Request()
        {
            {
                setRequestURI(uri);
            }
        }, null
        );
        
        assertEquals("pattern: " + matchCase[0] + " uri: " + matchCase[1], flag, result!=null);
    }
    
    
    private class TestPatternRule extends PatternRule
    {
        @Override
        public String apply(String target,
                HttpServletRequest request, HttpServletResponse response) throws IOException
        {
            return target;
        }
        
    }
}
