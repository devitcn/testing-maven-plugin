# 给src/test/resource中的文件生成一个静态类，类似android中的R

Generate a static class for all files in src/test/resource(like android R.java)

* 方便引用，可以直接代码提示
* 提前发现FileNotFound异常
* 不能成为JAVA变量名的字符都转换成了下划线，例如首字符是数字，字符，各类标点符号。

# Example

There are two files resource in `src/test/resources`:
    
    test-data.txt
    features/1.txt
    
Then will Generate src/test/com/boo/R.java

    package com.foo
    
    import java.io.File;
    
    public final class R
    {
      public static final File dir = new File("src/test/resources/");
      
      public static class features
      {
        public static final File dir = new File("src/test/resources/features");
        public static final File _1_txt = new File("src/test/resources/features/1.txt");
      }
      
      public static final File test_data_txt = new File("src/test/resources/sample-1.txt");
    }
