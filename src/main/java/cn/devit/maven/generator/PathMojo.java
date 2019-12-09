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
import java.io.FileWriter;
import java.io.IOException;
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
 * Goal which touches a timestamp file.
 * @author Alex Lei
 */
@Mojo(name = "path", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, inheritByDefault = true)
public class PathMojo extends AbstractMojo {

    private final class SimpleFileVisitorExtension
            extends SimpleFileVisitor<Path> {

        Path last = null;

        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) throws IOException {
            last = dir;
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            if (file.getFileName().toString().endsWith(".java")) {
                last = file.getParent();
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }

    }

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-test-sources/path")
    File outputDirectory;

    @Parameter(defaultValue = "${basedir}/src/test/resources")
    File resourceDirectory;

    /**
     * Packages for generated resources.
     * <p>
     * Default will be ${project.groupdId}
     * id else default package
     * 
     */
    @Parameter(defaultValue = "")
    String packages;

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
            packaged.mkdirs();
        }

        File f = new File(packaged, "R.java");
        this.getLog().info("Generating " + f.getPath());
        FileWriter writer;
        try {
            writer = new FileWriter(f);
            writer.write("package "+packageDir.replaceAll("/", ".")+";\n");
            writer.write(build.toString());
            writer.close();
            
            project.addTestCompileSourceRoot(outputDirectory.getPath());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    Pattern p = Pattern.compile("^\\d");

    public String fileNameToJava(String a) {
        if (p.matcher(a).find()) {
            a = "_" + a;
        }
        a = a.replaceAll("\\.", "_");
        a = a.replaceAll("-", "_");
        return a;
    }

    public String getPackageDir() {
        if (packages == null || packages.trim().isEmpty()) {
            return project.getGroupId().replaceAll("\\.", "/");
//            @SuppressWarnings("rawtypes")
//            List list = project.getTestCompileSourceRoots();
//            File testJava = new File((String) list.get(0));
//            if (testJava.exists()) {
//                String guess = firstFolderHasJava(testJava);
//                if (guess != null) {
//                    return guess;
//                }
//            }
//            return project.getGroupId().replaceAll("\\.", "/");
        }
        return packages.replaceAll("\\.", "/");
    }

    private String firstFolderHasJava(File testJava) {
        Path path = testJava.toPath();
        SimpleFileVisitorExtension visitor = new SimpleFileVisitorExtension();
        try {
            Files.walkFileTree(path, visitor);
        } catch (IOException e) {
            return null;
        }
        if (visitor.last != null) {
            String p = path.relativize(visitor.last).toString();
            if (!p.isEmpty()) {
                return p;
            }
        }
        return null;
    }

    public StringBuilder build(final Path root, final Path start) {
        final StringBuilder str = new StringBuilder("import java.io.File;\n"
                + "public final class R {\n" + "/**\n"
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
