# Utilities for creating and working with software projects


## Secrets

This operation allows confidential files to be encrypted and stored within a git repo.

It is assumed there is a directory `<repo_root>/secrets`, whose confidential files are omitted from git by use of its `.gitignore` file.

### encrypt mode (encrypt = true)

All the files within `<repo_root>/secrets` (except `entity_info.json`) are zipped up, and the zip file is AES encrypted, using the passphrase provided by the user. The resulting file is stored in  `<repo root>/project_config/encrypted_secrets.bin`, and this file *is* tracked by git.

The special handling of `entity_info.json` is because this file serves to identify the current machine's device, its id, os, etcetera.  This file is *not* included in the encrypted secrets file, as it will be created by the decrypt operation.

### decrypt mode (encrypt = false)

The file `<repo root>/project_config/encrypted_secrets.bin` is decrypted (using the passphrase), and the resulting zip file is unzipped into `<repo_root>/secrets` (deleting this directory beforehand if necessary!).

The appropriate entity info is read from the entity map (using the supplied entity id as a key) and is written to `<repo_root>/secrets/entity_info.json`.

