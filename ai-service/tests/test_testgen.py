from main import framework_for_file, suggested_test_name


def test_detects_framework_from_extension():
    assert framework_for_file("src/main/java/com/x/Owner.java")[1].startswith("JUnit 5")
    assert framework_for_file("app/utils.py") == ("Python", "pytest")
    assert framework_for_file("src/App.TSX")[0] == "TypeScript React"


def test_unsupported_files_return_none():
    assert framework_for_file("README.md") is None
    assert framework_for_file("Makefile") is None


def test_suggested_test_file_names():
    assert suggested_test_name("src/main/java/com/x/Owner.java") == "OwnerTest.java"
    assert suggested_test_name("app/utils.py") == "test_utils.py"
    assert suggested_test_name("src/api.ts") == "api.test.ts"
    assert suggested_test_name("pkg/handler.go") == "handler_test.go"