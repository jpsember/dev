use crate::app_error::*;
use crate::match_util::*;
use crate::math::FPoint;
use crate::node_set::*;
use crate::polyline::*;
use std::path::{Path, PathBuf};


// The .csv file did not have a column with the specified name
pub const ERROR_CSV_FIELD_NOT_FOUND: usize = 2003;

// A column with the match id's column name already exists in the .csv file
pub const ERROR_MATCH_ID_COLUMN_EXISTS: usize = 2004;



pub struct Scanr {
    text: Vec<u8>,
    cursor: usize,
    skip_text: Vec<u8>,
}

impl Scanr {
    pub fn new(text: &str) -> Scanr {
        Scanr {
            text: text.as_bytes().to_vec(),
            cursor: 0,
            skip_text: " ".as_bytes().to_vec(),
        }
    }

 fn do_panic(&self) {
        panic!(
            "{}",
            format!(
                "Unexpected text: ...{}",
                self.bytes_to_str(&self.text[self.cursor..])
            )
        );
    }
}
