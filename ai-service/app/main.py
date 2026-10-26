from contextlib import asynccontextmanager
import os
import time
from typing import List, Optional

import psycopg2  # type: ignore
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from google import genai
from google.genai import types
from pydantic import BaseModel

load_dotenv(override=True)

API_KEY = os.getenv("GEMINI_API_KEY") or os.getenv("Gemini_API")
MODEL = os.getenv("GEMINI_MODEL") or os.getenv("CHAT_MODEL", "gemini-3.8-flash")
FALLBACK_MODEL = os.getenv("GEMINI_FALLBACK_MODEL", "")
EMBED_MODEL = os.getenv("GEMINI_EMBED_MODEL") or os.getenv("EMBED_MODEL", "gemini-embedding-001")
EMBED_DIM = int(os.getenv("EMBED_DIM", "768"))
DATABASE_URL = os.getenv(
    "DATABASE_URL", "postgresql://admin:admin@127.0.0.1:5432/codepilot"
)
MAX_CHARS = 60000  # max code sent to Gemini for analysis

if not API_KEY:
    raise RuntimeError("GEMINI_API_KEY is missing. Add it to ai-service/.env")

client = genai.Client(api_key=API_KEY.strip())


@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        init_db()
        print("Database ready (pgvector table OK)")
    except Exception as e:
        print(f"WARNING: database not ready, chat will not work: {e}")
    yield


app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------- Models for incoming requests ----------
class FileItem(BaseModel):
    path: str
    content: Optional[str] = None


class AnalyzeRequest(BaseModel):
    repo_id: int
    files: List[FileItem]


class ChatRequest(BaseModel):
    repo_id: int
    question: str


# ---------- Database ----------
def get_db():
    return psycopg2.connect(DATABASE_URL)


def init_db():
    conn = get_db()
    try:
        with conn, conn.cursor() as cur:
            cur.execute("CREATE EXTENSION IF NOT EXISTS vector")
            cur.execute(
                f"""CREATE TABLE IF NOT EXISTS embeddings (
                    id SERIAL PRIMARY KEY,
                    repo_id INT NOT NULL,
                    file_path TEXT NOT NULL,
                    start_line INT,
                    end_line INT,
                    chunk_text TEXT NOT NULL,
                    embedding vector({EMBED_DIM})
                )"""
            )
            cur.execute(
                "CREATE INDEX IF NOT EXISTS idx_embeddings_repo ON embeddings(repo_id)"
            )
    finally:
        conn.close()





# ---------- Gemini helpers ----------
def with_retry(fn, tries=3):
    """Retry temporary Google errors (429, 500, 503, 504)."""
    for attempt in range(tries):
        try:
            return fn()
        except Exception as e:
            code = getattr(e, "code", None)
            if code in (429, 500, 503, 504) and attempt < tries - 1:
                time.sleep(2 * (attempt + 1))
                continue
            raise


def call_gemini(contents, config=None):
    models = [MODEL]
    if FALLBACK_MODEL and FALLBACK_MODEL != MODEL:
        models.append(FALLBACK_MODEL)
    last_error = None
    for name in models:
        try:
            return with_retry(
                lambda: client.models.generate_content(
                    model=name, contents=contents, config=config
                )
            )
        except Exception as e:
            last_error = e
            if getattr(e, "code", None) not in (429, 500, 503, 504):
                raise
    raise last_error


def embed(texts, task_type):
    """Turn a list of texts into a list of number-lists (embeddings)."""
    config = types.EmbedContentConfig(
        task_type=task_type, output_dimensionality=EMBED_DIM
    )
    vectors = []
    for i in range(0, len(texts), 50):
        batch = texts[i : i + 50]
        res = with_retry(
            lambda: client.models.embed_content(
                model=EMBED_MODEL, contents=batch, config=config
            )
        )
        got = [e.values for e in res.embeddings]
        if len(got) != len(batch):
            # The model returned fewer vectors than inputs: embed one by one instead
            print(f"Batch returned {len(got)} vectors for {len(batch)} texts, embedding one by one")
            got = []
            for text in batch:
                r = with_retry(
                    lambda: client.models.embed_content(
                        model=EMBED_MODEL, contents=text, config=config
                    )
                )
                got.append(r.embeddings[0].values)
        vectors.extend(got)
    return vectors


def to_pgvector(vec):
    return "[" + ",".join(str(x) for x in vec) + "]"


# ---------- Chunking and ingestion ----------
def chunk_file(content, max_chars=1500, overlap_lines=3):
    """Split code into chunks of whole lines, remembering line numbers."""
    lines = content.splitlines()
    chunks = []
    start = 0
    while start < len(lines):
        size = 0
        end = start
        while end < len(lines) and (size + len(lines[end]) + 1 <= max_chars or end == start):
            size += len(lines[end]) + 1
            end += 1
        chunks.append((start + 1, end, "\n".join(lines[start:end])))
        if end >= len(lines):
            break
        start = max(end - overlap_lines, start + 1)
    return chunks


def ingest_files(repo_id, files):
    meta = []
    texts = []
    for f in files:
        if not f.content:
            continue
        for start_line, end_line, text in chunk_file(f.content):
            meta.append((f.path, start_line, end_line, text))
            texts.append(f"File: {f.path} (lines {start_line}-{end_line})\n{text}")
    if not texts:
        return 0

    vectors = embed(texts, "RETRIEVAL_DOCUMENT")
    print(f"Embedded {len(vectors)} vectors for {len(texts)} chunks")
    if len(vectors) != len(texts):
        raise RuntimeError(f"Embedding mismatch: {len(texts)} chunks but {len(vectors)} vectors")

    conn = get_db()
    try:
        with conn, conn.cursor() as cur:
            cur.execute("DELETE FROM embeddings WHERE repo_id = %s", (repo_id,))
            for (path, s, e, text), vec in zip(meta, vectors):
                cur.execute(
                    """INSERT INTO embeddings
                       (repo_id, file_path, start_line, end_line, chunk_text, embedding)
                       VALUES (%s, %s, %s, %s, %s, %s::vector)""",
                    (repo_id, path, s, e, text, to_pgvector(vec)),
                )
    finally:
        conn.close()
    return len(texts)


# ---------- Endpoints ----------
@app.get("/health")
def health():
    return {"status": "AI Service UP", "model": MODEL, "embed_model": EMBED_MODEL}


@app.get("/api/ai/ping")
def ping():
    try:
        response = call_gemini("Say hello in one short sentence.")
        return {"gemini_says": response.text}
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"{type(e).__name__}: {e}")


@app.post("/api/ai/analyze")
def analyze(req: AnalyzeRequest):
    # 1. Build one text block of code, with line numbers
    code_blob = ""
    for f in req.files:
        if not f.content:
            continue
        numbered = "\n".join(
            f"{i + 1}: {line}" for i, line in enumerate(f.content.splitlines())
        )
        piece = f"\n=== FILE: {f.path} ===\n{numbered}\n"
        if len(code_blob) + len(piece) > MAX_CHARS:
            break
        code_blob += piece

    if not code_blob:
        raise HTTPException(status_code=400, detail="No readable code received")

    prompt = f"""You are a senior software engineer reviewing a code repository.
Each line of code starts with its line number.

Find real bugs and real security vulnerabilities. Only report issues you can
point to in the code. Do not invent problems. Report at most 8 bugs and 8 security issues.

Score the repository from 0 to 100. Judge the actual code. Use varied, specific
numbers (like 63 or 82), and never copy numbers from the format below.
- quality_score: readability, structure, code smells
- security_score: 100 means no vulnerabilities, lower means more or worse ones
- maintainability_score: how easy the code is to change and understand

Return JSON in exactly this shape:
{{
  "quality_score": <integer 0-100>,
  "security_score": <integer 0-100>,
  "maintainability_score": <integer 0-100>,
  "bugs": [
    {{"file": "<path>", "line": <number>, "severity": "<HIGH|MEDIUM|LOW>", "description": "<what is wrong and why>"}}
  ],
  "security": [
    {{"file": "<path>", "line": <number>, "severity": "<HIGH|MEDIUM|LOW>", "issue": "<short title>", "description": "<what is wrong and how to fix>"}}
  ]
}}

CODE:
{code_blob}
"""

    try:
        response = call_gemini(
            prompt,
            types.GenerateContentConfig(
                response_mime_type="application/json",
                temperature=0.2,
            ),
        )
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Gemini error: {e}")

    # 2. Index the code for "Chat with repo". If this fails, analysis still succeeds.
    chunks_stored = 0
    try:
        chunks_stored = ingest_files(req.repo_id, req.files)
        print(f"Indexed {chunks_stored} chunks for repo {req.repo_id}")
    except Exception as e:
        print(f"WARNING: could not store embeddings: {type(e).__name__}: {e}")

    print(f"Analyzed {len(req.files)} files for repo {req.repo_id}")
    return {"analysis": response.text, "chunks_stored": chunks_stored}


@app.post("/api/ai/chat")
def chat(req: ChatRequest):
    # 1. Turn the question into numbers and find the closest code chunks
    try:
        qvec = embed([req.question], "RETRIEVAL_QUERY")[0]
        conn = get_db()
        try:
            with conn, conn.cursor() as cur:
                cur.execute(
                    """SELECT file_path, start_line, end_line, chunk_text
                       FROM embeddings
                       WHERE repo_id = %s
                       ORDER BY embedding <=> %s::vector
                       LIMIT 6""",
                    (req.repo_id, to_pgvector(qvec)),
                )
                rows = cur.fetchall()
        finally:
            conn.close()
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Search error: {type(e).__name__}: {e}")

    if not rows:
        raise HTTPException(
            status_code=404,
            detail="No code indexed for this repo_id. Run analyze first and use the repositoryId it returned.",
        )

    # 2. Ask Gemini to answer using only those chunks
    context = "\n\n".join(
        f"--- {path} (lines {start}-{end}) ---\n{text}"
        for path, start, end, text in rows
    )
    prompt = f"""You are an assistant that answers questions about a code repository.
Use ONLY the code excerpts below. Mention the file name and line numbers you used.
If the excerpts do not contain the answer, say you could not find it in the indexed code.
Keep the answer clear and short.

CODE EXCERPTS:
{context}

QUESTION: {req.question}
"""
    try:
        response = call_gemini(prompt)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Gemini error: {e}")

    return {
        "answer": response.text,
        "sources": [
            {"file": path, "start_line": start, "end_line": end}
            for path, start, end, _ in rows
        ],
    }