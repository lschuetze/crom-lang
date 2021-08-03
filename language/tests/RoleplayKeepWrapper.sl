
function main() {
  obj1 = new();
  obj1.x = 42;
  obj2 = new();
  obj2.y = 9000;
  play(obj1, obj2);
  objWrapper = newPlayer(obj1);
  obj1.z = 0;
  println(objWrapper.z);
  println(objWrapper.y);
}