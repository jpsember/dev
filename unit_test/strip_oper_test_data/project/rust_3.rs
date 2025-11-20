let desk = file_home_dir()?.join("Desktop");
            check_state(desk.exists(), "Desktop for saving backups");

// Copyright (c), CommunityLogiq Software
//
use crate::dev_util::app_error::*;
use crate::dev_util::file_util::*;
use crate::dev_util::source_location_info::{new_source_location_info, SourceLocationInfo};
use crate::dev_util::tools::*;
use std::path::{Path, PathBuf};

// ------------------------------------------------------------------
// Error codes
// ------------------------------------------------------------------

pub const ERROR_NO_SUCH_SOURCE_LINE: usize = 20_000;

// ------------------------------------------------------------------

#[allow(unused)]
pub fn validate_source_info(info: &SourceLocationInfo) -> Result<(), AppError> {
    let msg: &str;
    if info.origin.is_empty() {
        msg = "No origin string";
    } else if file_as_string(&info.source_file)?.starts_with("?") || !info.source_file.exists() {
        msg = "Source file is undefined or doesn't exist";
    } else if info.line_number <= 0 || info.column_number <= 0 {
        msg = "Line or column number missing";
    } else if info.function_name.is_empty()
        || info.function_name.starts_with("?")
        || info.function_name.contains("{")
    {
        msg = "Function name missing";
    } else {
        return Ok(());
    }

    Err(build_error(
        ERROR_SOURCE_INFO,
        &format!("{}; {}", msg, info),
    ))
}

#[allow(unused)]
pub fn caller() -> String {
    caller_n_str(1).to_string()
}

pub fn caller_n_str(skip_count: usize) -> SourceLocationInfo {
    let mut b = new_source_location_info(&file_new("?UNKNOWN?"));

    let trace = std::backtrace::Backtrace::force_capture();
    let s = trace.to_string();

    // Split byte array by newline ('\n'), and strip trailing '\r' as well
    //
    let lines = s
        .as_bytes()
        .split(|&b| b == b'\n')
        .map(|line| line.strip_suffix(b"\r").unwrap_or(line))
        .collect::<Vec<&[u8]>>();

    // Example returned by backtrace
    // line:    3: rust_core::caller_n_str
    // line:              at ./src/main.rs:27:17
    //

    let j: usize = (skip_count + 4) * 2;
    if j + 2 <= lines.len() {
        let stmnt = lines[j];

        {
            // The function will appear in the stmnt, e.g.
            //  "6: rust_core::show_caller"
            //
            let double_colon = b"::";
            let curs = last_subsequence(stmnt, double_colon);
            if curs >= 0 {
                b.function_name =
                    utf8_to_str(&stmnt[(curs as usize + double_colon.len())..]).unwrap();
            }
        }
        let loc_bytes = lines[j + 1];

        {
            // Look for location; it will be the suffix after " at "
            let loc_prefix = b" at ";
            let (found, _pos, end) = find_bytes(loc_bytes, loc_prefix);
            if found {
                let origin = expect_or_fail(utf8_to_str(&loc_bytes[end..]));
                b.origin = origin.clone();
                let parts = origin.split(':').collect::<Vec<_>>();
                if parts.len() == 3 {
                    let source_info = parts[0];
                    b.source_file = file_new(source_info);
                    b.line_number = try_parse_positive_int(parts[1]);
                    b.column_number = try_parse_positive_int(parts[2]);
                }
            }
        }
    }
    b
}

// Attempt to find a contiguous sequence of bytes within bytes.
// There exist fancy algorithms to do this quickly, but this is not one of them.
//
// Returns (found flag, start of found sequence (or 0), end of found sequence (or 0))
//
fn find_bytes(source: &[u8], seq: &[u8]) -> (bool, usize, usize) {
    let seq_len = seq.len();

    let mut cursor_start: usize = 0;
    loop {
        let cursor_end = cursor_start + seq_len;
        if cursor_end > source.len() {
            return (false, 0, 0);
        }

        if source[cursor_start..cursor_end] == seq[..] {
            return (true, cursor_start, cursor_end);
        }
        cursor_start += 1;
    }
}

fn last_subsequence(source: &[u8], seq: &[u8]) -> i32 {
    let source_len = source.len();
    let seq_len = seq.len();

    if seq_len > source_len {
        return -1;
    }

    let mut c = source_len - seq_len;

    loop {
        if source[c..c + seq_len] == seq[..seq_len] {
            return c as i32;
        }
        if c == 0 {
            return -1;
        }
        c -= 1;
    }
}

fn try_parse_positive_int(arg: &str) -> i32 {
    let x = arg.parse::<i32>();
    if let Ok(n) = x
        && n > 0
    {
        return n;
    }
    0
}

#[allow(unused)]
pub struct SourceFile {
    file: PathBuf,
    lines: Vec<String>,
    modified: bool,
    original: String,
}

impl SourceFile {
    pub fn new(file: &Path) -> Result<Self, AppError> {
        file_assert_exists(file, "source_file")?;

        let content = file_read_string(file)?;

        let lines = content
            .lines()
            .map(std::string::ToString::to_string)
            .collect::<Vec<_>>();

        Ok(SourceFile {
            file: file.to_path_buf(),
            lines,
            modified: false,
            original: content,
        })
    }

    pub fn assert_line_exists(&self, line_number: usize) -> Result<usize, AppError> {
        if line_number == 0 || line_number > self.lines.len() {
            Err(build_error(
                ERROR_NO_SUCH_SOURCE_LINE,
                &format!(
                    "no such line number {} in 1...{}",
                    line_number,
                    self.lines.len()
                ),
            ))
        } else {
            Ok(line_number)
        }
    }

    pub fn neighbor_line_numbers(
        &self,
        line_number: usize,
        delta_min: isize,
        delta_max: isize,
    ) -> Vec<usize> {
        let mut out = Vec::new();
        let mut k0 = line_number as isize + delta_min;
        let mut k1 = line_number as isize + delta_max;
        k0 = k0.max(1);
        k1 = k1.min(self.lines.len() as isize);
        while k0 <= k1 {
            out.push(k0 as usize);
            k0 += 1;
        }
        out
    }

    pub fn line(&self, number: usize) -> Result<&str, AppError> {
        self.assert_line_exists(number)?;
        Ok(&self.lines[number - 1])
    }

    pub fn set_line(&mut self, number: usize, line: &str) -> Result<(), AppError> {
        let ln = line.to_string();
        if self.lines.len() + 1 == number {
            self.lines.push(ln);
        } else {
            self.assert_line_exists(number)?;
            self.lines[number - 1] = ln;
        }
        self.modified = true;
        Ok(())
    }

    pub fn modified(&self) -> bool {
        self.modified
    }

    pub fn flush(&mut self) -> Result<bool, AppError> {
        let was_mod = self.modified;
        if self.modified {
            todo("have policy about NOT saving backup if very recent backup exists");
            // Save a copy of the original source file, if we haven't already done so very recently
            let p = self.file.clone();
            let p = file_absolute(&p)?;
            let s = p.to_str().unwrap();
            if !s.starts_with("/") {
                return Err(build_error(
                    ERROR_FILENAME_FORMAT,
                    &format!("unexpected source file name: {}", &s),
                ));
            }
            let desk = file_home_dir()?.join("Desktop");
            check_state(desk.exists(), "Desktop for saving backups");
            let backup_dir = desk.join("_rust_source_backups_");
            let backup_file = backup_dir.join(&s[1..]); // Trim the '/'

            // Don't make a backup if there is already a recent one
            if !backup_file.exists()
                || file_modification_time(&backup_file)? + 60_000 < current_time_ms()
            {
                pr!("...saving backup of source file to:", &backup_file);
                let par = &backup_file.parent().unwrap().to_owned();
                file_mkdirs(par)?;

                file_write_string(&backup_file, &self.original)?;
            }
            let modified_source = self.lines.join("\n") + "\n";
            file_write_string(&self.file, &modified_source)?;
            self.modified = false;
        }
        Ok(was_mod)
    }
}
