
File touchFile = new File( basedir, "target/generated-test-sources/path/cn/devit/maven/it/Z.java" );
assert touchFile.isFile()

touchFile = new File( basedir, "target/test-classes/cn/devit/maven/it/Z.class" );
try{
  assert touchFile.isFile()
}catch(Exception e){
  println touchFile.text
  throw e
}
