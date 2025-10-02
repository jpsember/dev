// -------remove-----------------------------------------------
//   removed --->
// {~  this should disappear   ~}
//   <--- removed
// --------replace with alternative ----------------------------------------------
pub const FOO : usize = // {~
                       5 // ~|~ 6 ~}
                        ;
// -------turn "(a) and not (b)" with "(b)"------------------------------------
// {~
    if true {
        pr("do a");
    } else // ~|~
    {
        pr("do b");
        }  // ~}
// -------turn "(a) and not (b)" with "(b)", with comment stripping ---------------
// {~
    if true {
        pr("alternative with comments");
    } else // ~|~
    //    {
    //    pr("this is the alternative");
    //    // here are some comments
    //    }
    // ~}
// ------------------------------------------------------

// {~
// ------------------------------------------------------------------
// DirWalk
// ------------------------------------------------------------------

pub struct DirWalk {
  root_directory: JFile,
      recurse: bool,
      files: Vec<JFile>,
  include_directories: bool,
      prepared: bool,
      patterns_to_omit: HashSet<String>,
  patterns_to_include: HashSet<String>,
}

pub fn new_dirwalk(directory: &JFile) -> DirWalk {
  DirWalk {
    root_directory: directory.clone(),
        recurse: false,
        files: Vec::new(),
        include_directories: false,
        prepared: false,
        patterns_to_omit: HashSet::new(),
        patterns_to_include: HashSet::new(),
  }
}

impl DirWalk {
pub fn with_recurse(&mut self, f: bool) -> &mut Self {
    self.assert_mutable();
self.recurse = f;
self
    }

fn assert_mutable(&self) {
  if self.prepared {
    panic!("prepare() already called");
  }
}


fn assert_prepared(&self) {
  if !self.prepared {
    panic!("prepare() hasn't been called");
  }
}

pub fn prepare(&mut self) -> &Self {
  self.assert_mutable();

  // Add default omit patterns
  let user_count = self.patterns_to_omit.len();

  self.omit_prefix(&"_SKIP_");
  self.omit_prefix(&"_OLD_");

  let default_omit_count = self.patterns_to_omit.len() - user_count;

  let mut omit_regex = Vec::new();
  for exp in &self.patterns_to_omit {
    omit_regex.push(Regex::new(exp).unwrap());
  }

  let mut incl_regex = Vec::new();
  for exp in &self.patterns_to_include {
    incl_regex.push(Regex::new(exp).unwrap());
  }

  if omit_regex.len() > default_omit_count && !incl_regex.is_empty() {
    panic!("DirWalk error: include vs omit are exclusive options");
  }

  let mut files_list = Vec::<JFile>::new();
  let mut stack = Vec::<JFile>::new();
  stack.push(self.root_directory.clone());
  while !stack.is_empty() {
    let dir = stack.pop().unwrap();
    let was_start = dir.eq(&self.root_directory);
    if self.include_directories && !was_start {
      files_list.push(dir.clone());
    }
    if !self.recurse && !was_start {
      continue;
    }
    {
      use std::fs;

      'outer: for r in fs::read_dir(dir.0).unwrap() {
      let dir_entry = r.unwrap();
      let name = dir_entry.file_name().into_string().unwrap();

      let path = dir_entry.path();

----------------------------------------------???
      if !incl_regex.is_empty() {
      let mut include = false;
          include = true;

































     // ~}   if path.is_dir() {
                }
      } else {
        for p in &incl_regex {
          if p.is_match(&name) {
            include = true;
            break;
-----------------!!!
        }
      }
      if !include {
        continue 'outer;
      }
    } else {
      for r in &omit_regex {
        if r.is_match(&name) {
          continue 'outer;
        }
      }
    }
---------------------------!!!
      let jf = new_file_from(&path);
      if path.is_dir() {
      stack.push(jf);
    } else {
      files_list.push(jf);
    }
    }
  }
}
        files_list.sort_by(compare_files); //|a, b| a.0.cmp(&b.0));
self.files = files_list;
self.prepared = true;
self
    }

pub fn root_directory(&self) -> &JFile {
        &self.root_directory
}
// -----------------------------------------!
pub fn omit_name(&mut self, name: &str) -> &mut Self {
    self.assert_mutable();
        self.patterns_to_omit.insert(format!("^{name}$"));
self
    }

pub fn accept_extension(&mut self, ext: &str) -> &mut Self {
    self.assert_mutable();
        self.patterns_to_include.insert(format!("\\.{ext}$"));
self
    }

pub fn omit_prefix(&mut self, prefix: &str) -> &mut Self {
    self.assert_mutable();
        self.patterns_to_omit.insert(format!("^{prefix}"));
self
    }

pub fn files(&self) -> &Vec<JFile> {
  self.assert_prepared();
        &self.files
}

pub fn rel_file(&self, file: &JFile) -> Result<JFile, JFileError> {
  file_relative_to(file, &self.root_directory)
}
}

pub fn compare_files(a: &JFile, b: &JFile) -> cmp::Ordering {
  a.0.cmp(&b.0)
}

// ------------------------------------------------------------------
// File extensions
// ------------------------------------------------------------------

pub const EXT_JSON: &str = "json";

// Shouldn't affect these:
use crate::util::math::{
    angle_sep, distance_between, polar_angle, pt_distance_to_segment, FPoint, M_DEG,
};

