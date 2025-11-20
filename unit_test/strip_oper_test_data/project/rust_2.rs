// -------remove-----------------------------------------------
//   removed --->
// {~
    this should disappear
//    ~}
//   <--- removed

// --------replace with alternative ----------------------------------------------
pub const FOO : usize =
// {~
                       5
                       /* ~|~
                       6
                        ~} */
                       ;


// -------turn "(a) and not (b)" with "(b)"------------------------------------
// {~
    if true {
        pr("do a");
    } else
    // ~|~
    {
        pr("do b");
        }
// ~}

// -------turn "(a) and not (b)" with "(b)"---------------
// {~
    if true {
        pr("alternative with comments");
    } else
    /* ~|~
    {
        pr("this is the alternative");
       // here are some comments
    }
 ~} */
// ------------------------------------------------------

