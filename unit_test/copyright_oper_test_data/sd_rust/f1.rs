// Input data (e.g. a csv file) had no records, or
// had invalid fields
pub const ERROR_NO_RECORDS: usize = 1005;

// Multiple columns in a .csv file look like they contain geometry (e.g. LINESTRINGs)
pub const ERROR_MULTIPLE_GEOMETRY_COLUMNS: usize = 1002;




use crate::app_error::*;
use crate::match_util::*;
use crate::math::FPoint;
use crate::node_set::*;
use crate::polyline::*;
use std::path::{Path, PathBuf};


impl Scanr {

    pub fn do_panic(&self) {
        panic!(
            "{}",
            format!(
                "Unexpected text: ...{}",
                self.bytes_to_str(&self.text[self.cursor..])
            )
        );
    }
}
