
File touchFile = new File( basedir, "target/generated-test-sources/path/com/foo/bar/Zen.java" );
assert touchFile.isFile()

touchFile = new File( basedir, "target/test-classes/com/foo/bar/Zen.class" );
try{
  assert touchFile.isFile()
}catch(Exception e){
  println touchFile.text
  throw e
}
