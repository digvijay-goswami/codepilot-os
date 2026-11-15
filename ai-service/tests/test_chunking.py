from main import chunk_file


def make_lines(n):
    """Lines like 'line00', 'line01'... each exactly 6 characters long."""
    return [f"line{i:02d}" for i in range(n)]


def test_short_file_is_one_chunk():
    chunks = chunk_file("a\nb\nc")
    assert chunks == [(1, 3, "a\nb\nc")]


def test_empty_content_gives_no_chunks():
    assert chunk_file("") == []


def test_long_file_is_split_with_overlap():
    # max_chars=21 fits 3 lines of 6 chars (+1 for each newline) per chunk.
    # overlap_lines=1 means each chunk starts on the previous chunk's last line.
    chunks = chunk_file("\n".join(make_lines(10)), max_chars=21, overlap_lines=1)
    ranges = [(start, end) for start, end, _ in chunks]
    assert ranges == [(1, 3), (3, 5), (5, 7), (7, 9), (9, 10)]


def test_chunk_text_matches_reported_line_numbers():
    lines = make_lines(10)
    chunks = chunk_file("\n".join(lines), max_chars=21, overlap_lines=1)
    for start, end, text in chunks:
        assert text == "\n".join(lines[start - 1:end])


def test_chunks_respect_max_chars_and_cover_every_line():
    chunks = chunk_file("\n".join(make_lines(10)), max_chars=21, overlap_lines=1)
    covered = set()
    for start, end, text in chunks:
        assert len(text) <= 21
        covered.update(range(start, end + 1))
    assert covered == set(range(1, 11))


def test_single_oversized_line_still_makes_progress():
    # A line longer than max_chars must get its own chunk, not loop forever.
    chunks = chunk_file("x" * 50 + "\nshort", max_chars=10)
    ranges = [(start, end) for start, end, _ in chunks]
    assert ranges == [(1, 1), (2, 2)]