/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.devit.maven.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * To generate a static class which reference all file resource in
 * src/test/resources
 * 
 * @author Alex Lei
 */
@Mojo(name = "path", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, inheritByDefault = true)
public class PathMojo extends AbstractMojo {

  /**
   * Location of the file.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-test-sources/path")
  File outputDirectory;

  @Parameter(defaultValue = "${basedir}/src/test/resources")
  File resourceDirectory;

  @Parameter(defaultValue = "${project.build.sourceEncoding}")
  String encoding;

  /**
   * simple name or full qualified class name.
   * <br/>
   * If use full qualified class name, packageName parameter will be ignored.
   * 
   */
  @Parameter(defaultValue = "R")
  String className;

  /**
   * Package name for generated class.
   * <p>
   * Default will be ${project.groupdId}
   * <br/>
   * If className is full qualified ,this parameter will be ignored.
   * 
   */
  @Parameter(defaultValue = "")
  String packageName;

  /**
   * The current Maven project.
   */
  @Parameter(property = "project", required = true, readonly = true)
  protected MavenProject project;

  public void execute() throws MojoExecutionException {
    if (!resourceDirectory.exists()) {
      this.getLog().info("No test resources found, skipped.");
      return;
    }

    StringBuilder build = build(project.getBasedir().toPath(),
        resourceDirectory.toPath());

    String packageDir = getPackageDir();
    File packaged = new File(outputDirectory, packageDir);
    if (!packaged.exists()) {
      this.getLog().info("Create package folder " + packageDir);
      packaged.mkdirs();
    }

    File f = new File(packaged, simpleClassName() + ".java");
    this.getLog().info("Generating " + f.getPath() + ", encoding: " + encoding);
    Writer writer;
    try {
      if (encoding != null) {
        writer = new OutputStreamWriter(new FileOutputStream(f),
            encoding);
      } else {
        writer = new FileWriter(f);
      }
      writer.write("package " + packageName() + ";\n");
      writer.write(build.toString());
      writer.close();

      project.addTestCompileSourceRoot(outputDirectory.getPath());

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  Pattern startWithDigit = Pattern.compile("^\\d");

  public String packageName() {
    if (this.className.contains(".")) {
      return this.className.substring(0, this.className.lastIndexOf("."));
    } else if (packageName == null || packageName.trim().isEmpty()) {
      return project.getGroupId();
    }
    return packageName;
  }

  public String fileNameToJava(String a) {
    if (startWithDigit.matcher(a).find()) {
      a = "_" + a;
    }
    a = a.replaceAll("\\.", "_");
    a = a.replaceAll("-", "_");
    return a;
  }

  public String getPackageDir() {
    return packageName().replaceAll("\\.", "/");
  }

  public String simpleClassName() {
    if(this.className==null) {
      return "R";
    }
    if (this.className.contains(".")) {
      String[] parts = this.className.split("\\.");
      String filename = parts[parts.length - 1];
      mustBeValidJavaIdentifier(filename);
      return filename;
    }
    mustBeValidJavaIdentifier(className);
    return className;
  }

  /** 合格的类名 */
  void mustBeValidJavaIdentifier(String name) {
    if (name.isEmpty()) {
      throw new IllegalArgumentException(name + "不符合java语法规则");
    }
    if (!Character.isJavaIdentifierStart(name.charAt(0))) {
      throw new IllegalArgumentException(name + "不符合java语法规则");
    }
    for (int i = 1; i < name.length(); i++) {
      if (!Character.isJavaIdentifierPart(name.charAt(i))) {
        throw new IllegalArgumentException(name + "不符合java语法规则");
      }
    }
  }

  public StringBuilder build(final Path root, final Path start) {
    final StringBuilder str = new StringBuilder("import java.io.File;\n"
        + "public final class " + simpleClassName() + " {\n" + "/**\n"
        + " * src/test/resources\n" + " */"
        + " public final static File dir = new File(\"src/test/resources/\");\n");
    try {
      Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir,
            BasicFileAttributes attrs) throws IOException {
          if (dir.getFileName().toString().startsWith(".")) {
            return FileVisitResult.SKIP_SUBTREE;
          }
          if (dir.equals(start)) {
            return FileVisitResult.CONTINUE;
          }
          if (attrs.isSymbolicLink()) {
            return FileVisitResult.SKIP_SUBTREE;
          }
          Path fileName = dir.getFileName();
          int count = fileName.getNameCount();
          String p = root.relativize(dir)
              .toString().replaceAll("\\\\", "/");
          str.append("  public static class "
              + fileNameToJava(fileName.toString()) + " {\n");
          str.append("/**\n").append(" * " + p + "\n")
              .append(" */\n");

          str.append("  public static final File dir = new File(\""
              + p + "\");\n");
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir,
            IOException exc) throws IOException {
          str.append("}\n");
          return super.postVisitDirectory(dir, exc);
        }

        @Override
        public FileVisitResult visitFile(Path file,
            BasicFileAttributes attrs) throws IOException {
          if (file.getFileName().toString().startsWith(".")) {
            return FileVisitResult.CONTINUE;
          }
          if (attrs.isRegularFile()) {
            String p = root.relativize(file)
                .toString().replaceAll("\\\\", "/");
            str.append("/**\n").append(" * " + p + "\n")
                .append(" */\n");
            str.append("  public static final File "
                + fileNameToJava(file.getFileName().toString())
                + " ").append(" = new File(\"" + p + "\");\n");
            return FileVisitResult.CONTINUE;
          }
          return super.visitFile(file, attrs);
        }
      });
    } catch (IOException e1) {
      throw new RuntimeException(e1);
    }
    return str;
  }
}
