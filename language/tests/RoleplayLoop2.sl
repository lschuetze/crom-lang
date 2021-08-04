
function test1(obj1) {
  if (obj1!x != 0) {
    println(obj1!x);
  }
}

function main() {
  obj1 = new();
  obj1.x = 42;

  obj2 = new();
  obj2.x = 0;

  play(obj1, obj2);

  i = 0;
  while (i < 1000) {
    test1(obj1);
    i = i + 1;
  }
  println("END");
}
