# 给src/test/resource中的文件生成一个静态类，类似android中的R

Generate a static class for all files in src/test/resource(like android R.java)

* 方便引用，可以直接代码提示
* 用编译错误推动测试代码维护，提前发现FileNotFound异常
* 不能成为JAVA变量名的字符都转换成了下划线，例如首字符是数字，字符，各类标点符号。

## Example

There are two files resource in `src/test/resources`:
    
    test-data.txt
    features/1.txt
    
Then will Generate com/boo/R.java at `target/generated-test-sources/path/`

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
## Usage

在pom.xml中增加如下build配置：

```xml
<plugin>
    <groupId>cn.devit.maven</groupId>
    <artifactId>testing-maven-plugin</artifactId>
    <version>0.3.0</version>
    <executions>
      <execution>
        <goals><goal>path</goal></goals>
        <phase>generate-test-sources</phase>
      <configuration>
        <packageName>com.foo.bar</packageName>
      </configuration>
      </execution>
    </executions>
</plugin>
```


支持两个参数

packageName :指定生成的类的包名，默认会取groupdId
className：指定生成的类名，默认会取R，也可以指定类全名，这时packageName参数会忽略