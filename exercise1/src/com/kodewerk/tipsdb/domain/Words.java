package com.kodewerk.tipsdb.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class Words {

    public Words() {}

    public boolean isPriorityKeyword(String word) {
        return true;
    }

    public boolean isIgnoredKeyword( String word) {
        return true;
    }

    public boolean isKeyword() {
     return true;
    }


    private final static Pattern WordPattern = Pattern.compile("\\w+");

    private final static HashMap ALL_KEYWORDS = new HashMap();
    private final static ArrayList AllKeywordsList = new ArrayList();
    private final static HashMap AllKeywordsBackList = new HashMap();
    static int AllKeywordsCount = 0;

    public static void completeInit() {
        for (int i = 0; i < AllKeywordsList.size(); i++)
            AllKeywordsBackList.put(AllKeywordsList.get(i), new Integer(i));
    }

    static int MaxKSize = 0;

    public static Keyword[] extractKeywordsFrom(Tip tip, String s) {
//    if (s.length() > MaxKSize)
//      System.out.println(MaxKSize = s.length());
        Matcher matcher = WordPattern.matcher(s);
        HashMap keywords = new HashMap();
        //Run through each word in the tip
        while (matcher.find()) {
            String upword = matcher.group().toUpperCase();
            //Get the canonical form
            addToAllWords(upword);
            //Only interested if we don't ignore the word
            if (IgnoreWords.get(upword) == null) {
                //Get the canonical form
                String pupword = (String) PrioritizeWords.get(upword);
                if (pupword != null)
                    upword = pupword;
                if (upword.length() > 30)
                    upword = upword.substring(0, 30);

                //Only interested if we haven't found this already in this tip
                if (keywords.get(upword) == null) {
                    keywords.put(upword, Boolean.TRUE);
                    ArrayList arr = (ArrayList) ALL_KEYWORDS.get(upword);
                    if (arr == null) {
                        arr = new ArrayList();
                        AllKeywordsList.add(upword);
                    }
                    arr.add(tip);
                   ALL_KEYWORDS.put(upword, arr);
                    AllKeywordsCount++;
                }
            }
        }
        return null;
    }

    public static void printAllWords() {
/*
    System.out.println(AllKeywordsCount);
    System.out.println();
    Iterator iterator = AllKeywords.entrySet().iterator();
    while(iterator.hasNext())
    {
      Map.Entry e = (Map.Entry) iterator.next();
      int i = ((Integer) e.getValue()).intValue();
      if ( i > 0)
        System.out.println(i + "\t" + e.getKey());
    }
*/
/*
    Iterator iterator = AllWords.entrySet().iterator();
    while(iterator.hasNext())
    {
      Map.Entry e = (Map.Entry) iterator.next();
      int i = ((Integer) e.getValue()).intValue();
      if ( i > 20)
        System.out.println(i + "\t" + e.getKey());
    }
*/
    }

    public static void addToAllWords(String word) {
        String upword = word.toUpperCase();
        if ((IgnoreWords.get(upword) == null) && (PrioritizeWords.get(upword) == null)) {
            Integer i = (Integer) AllWords.get(upword);
            if (i == null)
                i = new Integer(1);
            else
                i = new Integer(i.intValue() + 1);
            AllWords.put(upword, i);
        }
    }

    public static boolean isPrioritizedWord( String word) {
        return PrioritizeWords.get(word.toUpperCase()) == null;
    }

    static HashMap AllWords = new HashMap();
    public static final HashMap IgnoreWords = new HashMap();
    public static final HashMap PrioritizeWords = new HashMap();

    public static void init() {
        for (int i = 0; i < PrioritizeTheseWords.length; i += 2) {
            PrioritizeWords.put(PrioritizeTheseWords[i].toUpperCase(), PrioritizeTheseWords[i + 1].toUpperCase());
        }

        for (int i = 0; i < IgnoreTheseWords.length; i++) {
            IgnoreWords.put(IgnoreTheseWords[i].toUpperCase(), Boolean.TRUE);
            if (PrioritizeWords.get(IgnoreTheseWords[i]) != null)
                throw new RuntimeException("PrioritizeTheseWords and IgnoreTheseWords both contain " + IgnoreTheseWords[i]);
        }
    }

    static String[] PrioritizeTheseWords = {
        "THREADS", "THREAD",
        "THREAD", "THREAD",
        "BEANS", "BEAN",
        "BEAN", "BEAN",
        "CACHE", "CACHE",
        "CACHES", "CACHE",
        "CACHING", "CACHE",
        "APPLICATION", "APPLICATION",
        "APPLICATIONS", "APPLICATION",
        "DATABASE", "DATABASE",
        "DATABASES", "DATABASE",
        "HEAP", "HEAP",
        "HEAPS", "HEAP",
        "METHOD", "METHOD",
        "METHODS", "METHOD",
        "CALLS", "METHOD",
        "CALL", "METHOD",
        "CALLED", "METHOD",
        "GC", "GC",
        "COLLECTOR", "GC",
        "GARBAGE", "GC",
        "COLLECTION", "COLLECTION",
        "COLLECTIONS", "COLLECTION",
        "OBJECT", "OBJECT",
        "OBJECTS", "OBJECT",
        "NETWORK", "NETWORK",
        "NETWORKS", "NETWORK",
        "MEMORY", "MEMORY",
        "USER", "USER",
        "USERS", "USER",
        "MULTIPLE", "MULTIPLE",
        "SIZE", "SIZE",
        "SIZES", "SIZE",
        "NUMBER", "NUMBER",
        "NUMBERS", "NUMBER",
        "REMOTE", "REMOTE",
        "ACCESS", "ACCESS",
        "TIME", "TIME",
        "TIMES", "TIME",
        "SYSTEM", "SYSTEM",
        "SYSTEMS", "SYSTEM",
        "O", "O",
        "SERVER", "SERVER",
        "SERVERS", "SERVER",
        "CONNECTION", "CONNECTION",
        "CONNECTIONS", "CONNECTION",
        "CODE", "CODE",
        "CODEBASE", "CODE",
        "CLASS", "CLASS",
        "CLASSES", "CLASS",
        "LARGE", "LARGE",
        "SESSION", "SESSION",
        "SESSIONS", "SESSION",
        "DATA", "DATA",
        "LOAD", "LOAD",
        "LOADS", "LOAD",
        "JVM", "JVM",
        "JVMS", "JVM",
        "AVOID", "AVOID",
        "AVOIDS", "AVOID",
        "AVOIDING", "AVOID",
        "TEST", "TEST",
        "TESTS", "TEST",
        "TESTING", "TEST",
        "TESTED", "TEST",
        "RESPONSE", "RESPONSE",
        "OVERHEAD", "OVERHEAD",
        "OVERHEADS", "OVERHEAD",
        "CLIENT", "CLIENT",
        "CLIENTS", "CLIENT",
        "REQUEST", "REQUEST",
        "REQUESTS", "REQUEST",
        "REQUESTED", "REQUEST",
        "WEB", "WEB",
        "FILE", "FILE",
        "FILES", "FILE",
        "STRING", "STRING",
        "STRINGS", "STRING",
        "THROUGHPUT", "THROUGHPUT",
        "NEW", "NEW",
        "OPERATIONS", "OPERATION",
        "OPERATION", "OPERATION",
        "LOOP", "LOOP",
        "LOOPS", "LOOP",
        "LOOPED", "LOOP",
        "LOOPING", "LOOP",
        "SMALL", "SMALL",
        "EJB", "EJB",
        "EJBS", "EJB",
        "RESOURCE", "RESOURCE",
        "RESOURCES", "RESOURCE",
        "BOTTLENECK", "BOTTLENECK",
        "BOTTLENECKS", "BOTTLENECK",
        "BOTTLENECKED", "BOTTLENECK",
        "BOTTLENECKING", "BOTTLENECK",
        "PAGE", "PAGE",
        "PAGES", "PAGE",
        "SYNCHRONIZE", "SYNCHRONIZE",
        "SYNCHRONIZES", "SYNCHRONIZE",
        "SYNCHRONIZED", "SYNCHRONIZE",
        "SYNCHRONIZING", "SYNCHRONIZE",
        "SYNCHRONIZABLE", "SYNCHRONIZE",
        "SYNC", "SYNCHRONIZE",
        "SYNCHRONIZATION", "SYNCHRONIZE",
        "PROFILING", "PROFILING",
        "PROFILE", "PROFILING",
        "PROFILES", "PROFILING",
        "PROFILED", "PROFILING",
        "TESTING_PROFILING_MEASURING_BENCHMARKING", "PROFILING",
        "CPU", "CPU",
        "CPUS", "CPU",
        "PATTERN", "PATTERN",
        "PATTERNS", "PATTERN",
        "JDBC", "JDBC",
        "SINGLE", "SINGLE",
        "SERVICE", "SERVICE",
        "MINIMIZE", "MINIMIZE",
        "MINIMIZES", "MINIMIZE",
        "MINIMIZED", "MINIMIZE",
        "MINIMIZING", "MINIMIZE",
        "READ", "READ",
        "READS", "READ",
        "POOL", "POOL",
        "POOLS", "POOL",
        "POOLED", "POOL",
        "POOLING", "POOL",
        "CONN_POOL", "POOL",
        "VARIABLE", "VARIABLE",
        "VARIABLES", "VARIABLE",
        "LONG", "LONG",
        "TRY", "TRY",
        "LOCAL", "LOCAL",
        "LOCALS", "LOCAL",
        "XX", "XX",
        "HIGH", "HIGH",
        "CREATE", "CREATE",
        "CREATES", "CREATE",
        "CREATED", "CREATE",
        "CREATING", "CREATE",
        "CREATION", "CREATE",
        "OBJECT_CREATION", "CREATE",
        "VALUE", "VALUE",
        "PROCESS", "PROCESS",
        "PROCESSES", "PROCESS",
        "PROCESSED", "PROCESS",
        "PROCESSING", "PROCESS",
        "GENERATION", "GENERATION",
        "OUT", "OUT",
        "MONITOR", "MONITOR",
        "COMPONENT", "COMPONENT",
        "COMPONENTS", "COMPONENT",
        "INCLUDE", "INCLUDE",
        "TRANSACTION", "TRANSACTION",
        "TRANSACTIONS", "TRANSACTION",
        "TYPE", "TYPE",
        "TYPES", "TYPE",
        "SCREEN", "SCREEN",
        "SCREENS", "SCREEN",
        "IMAGE", "IMAGE",
        "IMAGES", "IMAGE",
        "IMAGING", "IMAGE",
        "BUFFER", "BUFFER",
        "BUFFERS", "BUFFER",
        "BUFFERED", "BUFFER",
        "BUFFERING", "BUFFER",
        "HEAP_AND_MEMORY_AND_GARBAGE_COLLECTION", "GC",
        "DISK", "DISK",
        "A_D", "DESIGN",
        "DESIGN", "DESIGN",
        "DESIGNS", "DESIGN",
        "DESIGNED", "DESIGN",
        "DESIGNING", "DESIGN",
        "ANALYSIS_AND_DESIGN", "DESIGN",
        "ARRAY", "ARRAY",
        "ARRAYS", "ARRAY",
        "LEVEL", "LEVEL",
        "SCALABILITY", "SCALING",
        "SCALE", "SCALING",
        "SCALES", "SCALING",
        "SCALED", "SCALING",
        "SCALING", "SCALING",
        "SECOND", "SECOND",
        "DISTRIBUTED", "DISTRIBUTED",
        "DISTRIBUTES", "DISTRIBUTED",
        "DISTRIBUTE", "DISTRIBUTED",
        "OLD", "OLD",
        "AVERAGE", "AVERAGE",
        "AVERAGES", "AVERAGE",
        "AVERAGED", "AVERAGE",
        "AVERAGING", "AVERAGE",
        "CONCURRENT", "CONCURRENT",
        "CONCURRENCY", "CONCURRENT",
        "ENTITY", "ENTITY",
        "IMPORTANT", "IMPORTANT",
        "HTTP", "HTTP",
        "ELEMENT", "ELEMENT",
        "ELEMENTS", "ELEMENT",
        "STATIC", "STATIC",
        "STATICS", "STATIC",
        "SHORT", "SHORT",
        "DOUBLE", "DOUBLE",
        "STATEMENT", "STATEMENT",
        "STATEMENTS", "STATEMENT",
        "SERVLET", "SERVLET",
        "SERVLETS", "SERVLET",
        "REUSE", "REUSE",
        "REUSES", "REUSE",
        "REUSED", "REUSE",
        "REUSING", "REUSE",
        "REUSEABLE", "REUSE",
        "REUSABLE", "REUSE",
        "INTERFACE", "INTERFACE",
        "INTERFACES", "INTERFACE",
        "INTERFACED", "INTERFACE",
        "INTERFACING", "INTERFACE",
        "PARAMETER", "PARAMETER",
        "PARAMETERS", "PARAMETER",
        "DEFAULT", "DEFAULT",
        "DEFAULTS", "DEFAULT",
        "ALGORITHM", "ALGORITHMS",
        "ALGORITHMS", "ALGORITHMS",
        "SQL", "SQL",
        "HARDWARE", "HARDWARE",
        "CAPACITY", "CAPACITY",
        "IO", "IO",
        "MANAGEMENT_AND_STRATEGY_AND_TACTICS", "STRATEGY",
        "PLANNED", "STRATEGY",
        "PLAN", "STRATEGY",
        "PLANNING", "STRATEGY",
        "PLANS", "STRATEGY",
        "REQUIREMENTS", "DESIGN",
        "LIST", "LIST",
        "RMI", "RMI",
        "XML", "XML",
        "STATE", "STATE",
        "LOG", "LOG",
        "LOGGING", "LOG",
        "LOGS", "LOG",
        "STORE", "STORE",
        "J2EE", "J2EE",
        "INFORMATION", "INFORMATION",
        "BALANCE", "BALANCE",
        "BALANCING", "BALANCE",
        "BALANCES", "BALANCE",
        "BALANCED", "BALANCE",
        "MESSAGE", "MESSAGE",
        "MESSAGES", "MESSAGE",
        "MESSAGED", "MESSAGE",
        "MESSAGING", "MESSAGE",
        "MEASURE", "MEASURE",
        "MEASURED", "MEASURE",
        "MEASURES", "MEASURE",
        "MEASURING", "MEASURE",
        "MEASUREABLE", "MEASURE",
        "MEASUREMENT", "MEASURE",
        "APP_SERVER", "SERVER",
        "GRAINED", "GRAINED",
        "ORDER", "ORDER",
        "CHANGE", "CHANGE",
        "RUNTIME", "RUNTIME",
        "OPTION", "OPTION",
        "GRAPHIC", "GRAPHICS",
        "GRAPHICS", "GRAPHICS",
        "INSTANCE", "INSTANCE",
        "INSTANCES", "INSTANCE",
        "RESULT", "RESULT",
        "RESULTS", "RESULT",
        "UPDATE", "UPDATE",
        "UPDATES", "UPDATE",
        "UPDATED", "UPDATE",
        "FINAL", "FINAL",
        "FINALIZE", "FINAL",
        "FINALIZER", "FINAL",
        "FINALIZES", "FINAL",
        "FINALIZATION", "FINAL",
        "FINALIZING", "FINAL",
        "LIFE", "LIFE",
        "LIFETIME", "LIFE",
        "LIFETIMES", "LIFE",
        "LIFES", "LIFE",
        "LIVE", "LIFE",
        "LIVES", "LIFE",
        "LIVED", "LIFE",
        "DURATION", "LIFE",
        "COMPILER", "COMPILER",
        "COMPILERS", "COMPILER",
        "COMPILED", "COMPILER",
        "COMPILE", "COMPILER",
        "COMPILES", "COMPILER",
        "ANIMATION", "ANIMATION",
        "ANIMATE", "ANIMATION",
        "ANIMATES", "ANIMATION",
        "ANIMATED", "ANIMATION",
        "WRITE", "WRITE",
        "WRITES", "WRITE",
        "WRITTEN", "WRITE",
        "WROTE", "WRITE",
        "DIRECT", "DIRECT",
        "DIRECTLY", "DIRECT",
        "FULL", "FULL",
        "STRINGBUFFER", "STRINGBUFFER",
        "STRINGBUFFERS", "STRINGBUFFER",
        "COPY", "COPY",
        "COPIES", "COPY",
        "DRIVER", "DRIVER",
        "DRIVERS", "DRIVER",
        "WHILE", "WHILE",
        "BEST", "BEST",
        "MAXIMUM", "MAXIMUM",
        "MINIMUM", "MINIMUM",
        "IDENTIFY", "IDENTIFY",
        "IDENTIFIES", "IDENTIFY",
        "IDENTIFIED", "IDENTIFY",
        "REGISTER", "REGISTER",
        "REGISTERS", "REGISTER",
        "REGISTERED", "REGISTER",
        "REGISTERING", "REGISTER",
        "REGISTRATION", "REGISTER",
        "CONCATENTATE", "CONCATENTATE",
        "CONCATENTATES", "CONCATENTATE",
        "CONCATENTATED", "CONCATENTATE",
        "CONCATENTATING", "CONCATENTATE",
        "CONCATENTATION", "CONCATENTATE",
        "FAILURE", "FAILURE",
        "FAILURES", "FAILURE",
        "FAILING", "FAILURE",
        "FAILS", "FAILURE",
        "FAILED", "FAILURE",
        "ROUTE", "ROUTE",
        "ROUTES", "ROUTE",
        "ROUTED", "ROUTE",
        "ROUTING", "ROUTE",
        "TIMESTAMP", "TIMESTAMP",
        "TIMESTAMPS", "TIMESTAMP",
        "TIMESTAMPING", "TIMESTAMP",
        "TIMESTAMPED", "TIMESTAMP",
        "REFLECT", "REFLECTION",
        "REFLECTS", "REFLECTION",
        "REFLECTED", "REFLECTION",
        "REFLECTION", "REFLECTION",
        "REFLECTING", "REFLECTION",
    };

    static String[] IgnoreTheseWords = {
        "from", "to", "and", "your", "than", "is", "java", "will", "it", "on",
        "have", "all", "by", "performance", "a", "may", "in", "can", "i", "or",
        "article", "at", "where", "using", "as", "rather", "one", "this", "so",
        "an", "that", "when", "you", "need", "into", "li", "not", "with", "use",
        "1", "but", "for", "if", "used", "are", "should", "be", "of", "faster",
        "s", "E", "T", "more", "which", "the", "possible", "ctgry", "only", "some",
        "reduce", "they", "has", "don", "no", "make", "them", "efficient", "then",
        "very", "g", "same", "slow", "other", "space", "much", "each", "4", "many",
        "require", "required", "requires", "available", "2", "do", "does", "did",
        "better", "provide", "provides", "provided", "up", "how", "such", "instead",
        "most", "non", "there", "improve", "improves", "improved", "any", "too", "also",
        "two", "em", "0", "3", "per", "show", "shows", "other", "others", "these",
        "worthwhile", "optimize", "optimizing", "optimized", "optimization", "optimizes",
        "obvious", "vastly", "look", "looks", "allow", "allows", "slower", "pre",
        "greater", "own", "impact", "tune", "tuning", "tunes", "tuned", "ultimately",
        "DIFFERENT", "ALWAYS", "MUST", "BEING", "COST", "OFTEN", "SPEED", "PROBLEMS",
        "BETWEEN", "BASED", "EXPENSIVE", "RUN", "DETERMINE", "FAST", "EVERY", "OFF",
        "NEEDED", "SUPPORT", "WORK", "GET", "SET", "BECAUSE", "UNTIL", "ITS", "EVEN",
        "CONSIDER", "INCREASE", "EXAMPLE", "END", "amount", "BEFORE", "WAY", "EXECUTE",
        "OPTIMAL", "USAGE", "UTILIZATION", "EXECUTION", "HANDLE", "PERFORM", "AFTER",
        "NEEDS", "LONGER", "OVER", "LESS", "USUALLY", "ONCE", "COMMON", "RUNNING",
        "DISCUSSES", "SEPARATE", "SIMPLE", "ENSURE", "keep", "ADD", "additional",
        "THROUGH", "FIRST", "UNNECESSARY", "LOW", "SIGNIFICANT", "LIKE", "ABOUT",
        "GOOD", "THEIR", "WITHOUT", "TAKE", "BUILT", "BUILD", "BUILDS", "LOWEST",
        "lowest", "500", "wide", "widely", "enhance", "enhancing", "COME"
    };

    static {
        init();
    }

}

