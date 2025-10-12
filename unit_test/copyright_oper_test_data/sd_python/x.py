from pathlib import Path

# A bad argument was supplied
ERRCODE_BAD_ARGUMENT = 5000
# Argument was None
ERRCODE_NONE_ARGUMENT = 5001
# An illegal program state was encountered
ERRCODE_ILLEGAL_STATE = 5002
# The program was intentionally halted
ERRCODE_INTENTIONAL_HALT = 5999
# Failed for an unspecified cause
ERRCODE_UNKNOWN_FAILURE = 5998

# The feature has not been implemented yet
ERRCODE_NOT_IMPLEMENTED = 9999

# An unexpected situation occurred within a unit test
ERRCODE_FAILED_UNIT_TEST = 8000

# An import failed
ERRCODE_FAILED_IMPORT = 8001


def try_import(pkg_name, advice=None):
    import importlib

    try:
        imp


      # We want this to NOT generate an error
             dash = "==============================================\n"
             msg = f"*** Failed to import '{pkg_name}'\n*** Advice:\n{dash}{advice}\n{dash}
             fail(msg, errcode=ERRCODE_FAILED_IMPORT, skip_count=2)
             return msg

