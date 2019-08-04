package cn.devit.maven.generator;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class MyMojoTest {

    public MyMojo bean = new MyMojo();

    @Test
    public void walk() throws Exception {

        final Path root = new File("").toPath();
        System.out.println(root);
        final Path start = Paths.get("src/it/simple-it/src/test/resources");
        System.out.println((root.relativize(start)));
        
        System.out.println(root.resolve(start));

        final String str = bean.build(root, start).toString();

        System.out.println(str.toString());

        //ignore hidden file
        assertThat(str, not(containsString(".keep")));
        //escape
        assertThat(str, (containsString("_101")));
        assertThat(str, (containsString("sample_1")));
        //unicode
        assertThat(str, (containsString("汉字")));
    }
}
