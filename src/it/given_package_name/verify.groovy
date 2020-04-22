
File touchFile = new File( basedir, "target/generated-test-sources/path/com/foo/bar/R.java" );
assert touchFile.isFile()

touchFile = new File( basedir, "target/test-classes/com/foo/bar/R.class" );
try{
  assert touchFile.isFile()
}catch(Exception e){
  println touchFile.text
  throw e
}
