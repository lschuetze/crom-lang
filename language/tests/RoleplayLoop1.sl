
function test1() {
  obj1 = new();
  obj1.x = 42;

  obj2 = new();
  obj2.x = 0;

  obj3 = new();
  obj3.x = 1000;

  play(obj1, obj2);
  play(obj1, obj3);

  result = obj1!x;
  if (result != 1000) {
    println(result);
    println(obj1!x);
    println("---");
  }
}

function main() {
  i = 0;
  while (i < 1000) {
    test1();
    i = i + 1;
  }
  println("END");
}
