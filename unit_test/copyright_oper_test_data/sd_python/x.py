from pathlib import Path

# A bad argument was supplied
ERROR_BAD_ARGUMENT = 5000
# Argument was None
ERROR_NONE_ARGUMENT = 5001
# An illegal program state was encountered
ERROR_ILLEGAL_STATE = 5002
# The program was intentionally halted
ERROR_INTENTIONAL_HALT = 5999
# Failed for an unspecified cause
ERROR_UNKNOWN_FAILURE = 5998

# The feature has not been implemented yet
ERROR_NOT_IMPLEMENTED = 9999

# An unexpected situation occurred within a unit test
ERROR_FAILED_UNIT_TEST = 8000

# An import failed
ERROR_FAILED_IMPORT = 8001


def try_import(pkg_name, advice=None):
    import importlib

    try:
        imp