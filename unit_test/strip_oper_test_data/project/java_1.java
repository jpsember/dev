this is an example
alpha bravo
       pr("hello", (1+2));
    ..  ^ this line is completely removed...
echo  pr("..");   this one is not
    foxtrot

start block 1
// {~
that gets
completely removed
//  ~}
end block 1, start block 2
// {~
This second one should NOT be part of the first one!
//  ~}
end block 2