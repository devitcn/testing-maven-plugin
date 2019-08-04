
File touchFile = new File( basedir, "target/generated-test-sources/path/cn/devit/maven/it/R.java" );
assert touchFile.isFile()

touchFile = new File( basedir, "target/test-classes/cn/devit/maven/it/R.class" );
assert touchFile.isFile()
