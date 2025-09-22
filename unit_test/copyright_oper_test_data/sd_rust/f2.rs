use crate::app_error::*;
use crate::match_util::*;
use crate::math::FPoint;
use crate::node_set::*;
use crate::polyline::*;
use std::path::{Path, PathBuf};

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
