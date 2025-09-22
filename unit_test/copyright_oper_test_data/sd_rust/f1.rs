
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
