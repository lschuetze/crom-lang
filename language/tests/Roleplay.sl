
function main() {
  obj1 = new();
  obj1.x = 42;
  obj1.z = 1;
  println(obj1.x);
  println(obj1.z);
  println("");

  // println(obj1.y); // Will print "Undefined property: y" and abort

  obj2 = new();
  obj2.x = 9001;
  obj2.y = "Hello, World!";
  println(obj2.x);
  println(obj2.y);
  println("");

  obj1 play obj2;

  println(obj1.x);
  println(newPlayer(obj1).x);
  println("");

  println(newPlayer(obj1).y);
  println(newPlayer(obj1).z);
  println("");

  // Reassigning properties of roles (is this actually necessary?)
  newPlayer(obj1).y = "That's all Folks!";
  println(obj2.y);

  println(obj2.z); // Will print "Undefined property: z" and abort
}