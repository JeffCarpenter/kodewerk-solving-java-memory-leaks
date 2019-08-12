package org.mortbay.jetty.handler.rewrite;


public class VirtualHostRuleContainerTest extends AbstractRuleTestCase
{
    RewriteHandler _handler;

    RewritePatternRule _rule;
    RewritePatternRule _fooRule;
    VirtualHostRuleContainer _fooContainerRule;
    
    public void setUp() throws Exception
    {
        _handler = new RewriteHandler();
        _handler.setRewriteRequestURI(true);

        _rule = new RewritePatternRule();
        _rule.setPattern("/cheese/*");
        _rule.setReplacement("/rule");

        _fooRule = new RewritePatternRule();
        _fooRule.setPattern("/cheese/bar/*");
        _fooRule.setReplacement("/cheese/fooRule");
        
        _fooContainerRule = new VirtualHostRuleContainer();
        _fooContainerRule.setVirtualHosts(new String[] {"foo.com"});
        _fooContainerRule.setRules(new Rule[] { _fooRule });
        
        _server.setHandler(_handler);

        super.setUp();
        _request.setRequestURI("/cheese/bar");
    }

    public void testArbitraryHost() throws Exception
    {
        _request.setServerName("cheese.com");
        _handler.setRules(new Rule[] { _rule, _fooContainerRule });
        handleRequest();
        assertEquals("{_rule, _fooContainerRule, Host: cheese.com}: applied _rule", "/rule/bar", _request.getRequestURI());
    }

    public void testVirtualHost() throws Exception
    {
        _request.setServerName("foo.com");
        _handler.setRules(new Rule[] { _fooContainerRule });
        handleRequest();
        assertEquals("{_fooContainerRule, Host: foo.com}: applied _fooRule", "/cheese/fooRule", _request.getRequestURI());
    }

    public void testCascadingRules() throws Exception
    {
        _request.setServerName("foo.com");
        _request.setRequestURI("/cheese/bar");
                
        _rule.setTerminating(false);
        _fooRule.setTerminating(false);
        _fooContainerRule.setTerminating(false);
        
        _handler.setRules(new Rule[]{_rule, _fooContainerRule});
        handleRequest();
        assertEquals("{_rule, _fooContainerRule}: applied _rule, didn't match _fooRule", "/rule/bar", _request.getRequestURI());
        
        _request.setRequestURI("/cheese/bar");
        _handler.setRules(new Rule[] { _fooContainerRule, _rule });
        handleRequest();
        assertEquals("{_fooContainerRule, _rule}: applied _fooRule, _rule","/rule/fooRule", _request.getRequestURI());
        
        _request.setRequestURI("/cheese/bar");
        _fooRule.setTerminating(true);
        handleRequest();
        assertEquals("{_fooContainerRule, _rule}: (_fooRule is terminating); applied _fooRule, _rule", "/rule/fooRule", _request.getRequestURI());
        
        _request.setRequestURI("/cheese/bar");
        _fooRule.setTerminating(false);
        _fooContainerRule.setTerminating(true);
        handleRequest();
        assertEquals("{_fooContainerRule, _rule}: (_fooContainerRule is terminating); applied _fooRule, terminated before _rule", "/cheese/fooRule", _request.getRequestURI());
    }
    
    public void testCaseInsensitiveHostname() throws Exception 
    {
        _request.setServerName("Foo.com");
        _fooContainerRule.setVirtualHosts(new String[] {"foo.com"} );
       
        _handler.setRules(new Rule[]{ _fooContainerRule });
        handleRequest();
        assertEquals("Foo.com and foo.com are equivalent", "/cheese/fooRule", _request.getRequestURI());
    }
    
    public void testEmptyVirtualHost() throws Exception 
    {
        _request.setServerName("cheese.com");
        
        _handler.setRules(new Rule[] { _fooContainerRule });
        _fooContainerRule.setVirtualHosts(null);
        handleRequest();
        assertEquals("{_fooContainerRule: virtual hosts array is null, Host: cheese.com}: apply _fooRule", "/cheese/fooRule", _request.getRequestURI());
        
        _request.setRequestURI("/cheese/bar");
        _request.setRequestURI("/cheese/bar");
        _fooContainerRule.setVirtualHosts(new String[] {});
        handleRequest();
        assertEquals("{_fooContainerRule: virtual hosts array is empty, Host: cheese.com}: apply _fooRule", "/cheese/fooRule", _request.getRequestURI());
        
        _request.setRequestURI("/cheese/bar");
        _request.setRequestURI("/cheese/bar");
        _fooContainerRule.setVirtualHosts(new String[] {null});
        handleRequest();
        assertEquals("{_fooContainerRule: virtual host is null, Host: cheese.com}: apply _fooRule", "/cheese/fooRule", _request.getRequestURI());
        
    }
        
    public void testMultipleVirtualHosts() throws Exception 
    {
        _request.setServerName("foo.com");
        _handler.setRules(new Rule[] {_fooContainerRule });
        
        _fooContainerRule.setVirtualHosts(new String[]{ "cheese.com" });
        handleRequest();
        assertEquals("{_fooContainerRule: vhosts[cheese.com], Host: foo.com}: no effect", "/cheese/bar", _request.getRequestURI());
        
        _request.setRequestURI("/cheese/bar");
        _fooContainerRule.addVirtualHost( "foo.com" );
        handleRequest();
        assertEquals("{_fooContainerRule: vhosts[cheese.com, foo.com], Host: foo.com}: apply _fooRule", "/cheese/fooRule", _request.getRequestURI());
    }
    
    public void testWildcardVirtualHosts() throws Exception
    {
        checkWildcardHost(true,null,new String[] {"foo.com", ".foo.com", "vhost.foo.com"});
        checkWildcardHost(true,new String[] {null},new String[] {"foo.com", ".foo.com", "vhost.foo.com"});

        checkWildcardHost(true,new String[] {"foo.com", "*.foo.com"}, new String[] {"foo.com", ".foo.com", "vhost.foo.com"});
        checkWildcardHost(false,new String[] {"foo.com", "*.foo.com"}, new String[] {"badfoo.com", ".badfoo.com", "vhost.badfoo.com"});
        
        checkWildcardHost(false,new String[] {"*."}, new String[] {"anything.anything"});
        
        checkWildcardHost(true,new String[] {"*.foo.com"}, new String[] {"vhost.foo.com", ".foo.com"});
        checkWildcardHost(false,new String[] {"*.foo.com"}, new String[] {"vhost.www.foo.com", "foo.com", "www.vhost.foo.com"});

        checkWildcardHost(true,new String[] {"*.sub.foo.com"}, new String[] {"vhost.sub.foo.com", ".sub.foo.com"});
        checkWildcardHost(false,new String[] {"*.sub.foo.com"}, new String[] {".foo.com", "sub.foo.com", "vhost.foo.com"});
        
        checkWildcardHost(false,new String[] {"foo.*.com","foo.com.*"}, new String[] {"foo.vhost.com", "foo.com.vhost", "foo.com"});                    
    }
    
    private void checkWildcardHost(boolean succeed, String[] ruleHosts, String[] requestHosts) throws Exception
    {
        _fooContainerRule.setVirtualHosts(ruleHosts);
        _handler.setRules(new Rule[] { _fooContainerRule });

        for(String host: requestHosts)
        {
            _request.setServerName(host);
            _request.setRequestURI("/cheese/bar");
            handleRequest();
            if(succeed)
                assertEquals("{_fooContainerRule, Host: "+host+"}: should apply _fooRule", "/cheese/fooRule", _request.getRequestURI());
            else
                assertEquals("{_fooContainerRule, Host: "+host+"}: should not apply _fooRule", "/cheese/bar", _request.getRequestURI());
        }
    }

    private void handleRequest() throws Exception
    {
        _server.handle("/cheese/bar", _request, _response, 0);
    }
}
   