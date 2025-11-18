use crate::match_alg::apply_match_alg;

// {~
use crate::util::tools::*;
// ~}


let database = {
    let csv_path = &self.config.databasepath;
    if !csv_path.is_empty() {
        self.db_manager.parse_csv(csv_path)?
    } else {
        check_arg(!self.config.stream_id.is_empty(), "stream_id is EMPTY");
        let stream_id = &self.config.stream_id;
        self.db_manager.get(stream_id).await?
    }
};

// Alternate example


    let matches =
        // {~
         if file_args.is_some() {
        matches.get_matches_from(file_args.unwrap()) } else
         // ~|~
         {
             matches.get_matches()
         };
    // ~}

