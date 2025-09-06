-- 1. Kreiranje osnovnog indexa
CREATE INDEX IF NOT EXISTS vector_cosine_idx 
ON vector_store USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);

-- 2. Index za metadata pretragu
CREATE INDEX IF NOT EXISTS metadata_source_idx 
ON vector_store USING GIN ((metadata::jsonb));

-- 3. Optimizacije tabele
ALTER TABLE vector_store SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_analyze_scale_factor = 0.02
);

-- 4. Update statistika
ANALYZE vector_store;