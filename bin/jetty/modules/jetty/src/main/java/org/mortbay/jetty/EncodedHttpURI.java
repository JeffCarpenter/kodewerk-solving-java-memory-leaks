package org.mortbay.jetty;

import java.io.UnsupportedEncodingException;

import org.mortbay.util.MultiMap;
import org.mortbay.util.StringUtil;
import org.mortbay.util.TypeUtil;
import org.mortbay.util.URIUtil;
import org.mortbay.util.UrlEncoded;
import org.mortbay.util.Utf8StringBuffer;

public class EncodedHttpURI extends HttpURI
{
    private String _encoding;
    
    public EncodedHttpURI(String encoding)
    {
        super();
        _encoding = encoding;
    }
    
    
    public String getScheme()
    {
        if (_scheme==_authority)
            return null;
        int l=_authority-_scheme;
        if (l==5 && 
            _raw[_scheme]=='h' && 
            _raw[_scheme+1]=='t' && 
            _raw[_scheme+2]=='t' && 
            _raw[_scheme+3]=='p' )
            return HttpSchemes.HTTP;
        if (l==6 && 
            _raw[_scheme]=='h' && 
            _raw[_scheme+1]=='t' && 
            _raw[_scheme+2]=='t' && 
            _raw[_scheme+3]=='p' && 
            _raw[_scheme+4]=='s' )
            return HttpSchemes.HTTPS;
        
        return StringUtil.toString(_raw,_scheme,_authority-_scheme-1,_encoding);
    }
    
    public String getAuthority()
    {
        if (_authority==_path)
            return null;
        return StringUtil.toString(_raw,_authority,_path-_authority,_encoding);
    }
    
    public String getHost()
    {
        if (_host==_port)
            return null;
        return StringUtil.toString(_raw,_host,_port-_host,_encoding);
    }
    
    public int getPort()
    {
        if (_port==_path)
            return -1;
        return TypeUtil.parseInt(_raw, _port+1, _path-_port-1,10);
    }
    
    public String getPath()
    {
        if (_path==_param)
            return null;
        return StringUtil.toString(_raw,_path,_param-_path,_encoding);
    }
    
    public String getDecodedPath()
    {
        if (_path==_param)
            return null;
        return URIUtil.decodePath(_raw,_path,_param-_path);
    }
    
    public String getPathAndParam()
    {
        if (_path==_query)
            return null;
        return StringUtil.toString(_raw,_path,_query-_path,_encoding);
    }
    
    public String getCompletePath()
    {
        if (_path==_end)
            return null;
        return StringUtil.toString(_raw,_path,_end-_path,_encoding);
    }
    
    public String getParam()
    {
        if (_param==_query)
            return null;
        return StringUtil.toString(_raw,_param+1,_query-_param-1,_encoding);
    }
    
    public String getQuery()
    {
        if (_query==_fragment)
            return null;
        return StringUtil.toString(_raw,_query+1,_fragment-_query-1,_encoding);
    }
    
    public boolean hasQuery()
    {
        return (_fragment>_query);
    }
    
    public String getFragment()
    {
        if (_fragment==_end)
            return null;
        return StringUtil.toString(_raw,_fragment+1,_end-_fragment-1,_encoding);
    }

    public void decodeQueryTo(MultiMap parameters) 
    {
        if (_query==_fragment)
            return;
        UrlEncoded.decodeTo(StringUtil.toString(_raw,_query+1,_fragment-_query-1,_encoding),parameters,_encoding);
    }

    public void decodeQueryTo(MultiMap parameters, String encoding) 
        throws UnsupportedEncodingException
    {
        if (_query==_fragment)
            return;
       
        if (encoding==null)
            encoding=_encoding;
        UrlEncoded.decodeTo(StringUtil.toString(_raw,_query+1,_fragment-_query-1,encoding),parameters,encoding);
    }
    
    public String toString()
    {
        if (_rawString==null)
            _rawString= StringUtil.toString(_raw,_scheme,_end-_scheme,_encoding);
        return _rawString;
    }
    
    public void writeTo(Utf8StringBuffer buf)
    {
        buf.getStringBuffer().append(toString());
    }
    
}
