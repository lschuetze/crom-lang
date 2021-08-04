
function test1() {
  obj1 = new();
  obj1.x = 42;

  obj2 = new();
  obj2.x = 0;

  println(obj1!x);
  // This should invalidate the SLAsPlayerBuiltin.newPlayer cache for the following newPlayer(obj1)
  play(obj1, obj2);
  println(obj1!x);
}

function main() {
  test1();
}
