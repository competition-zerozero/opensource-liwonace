CREATE TABLE IF NOT EXISTS graph_nodes (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    name TEXT NOT NULL,
    properties JSONB DEFAULT '{}'::jsonb
);

CREATE TABLE IF NOT EXISTS graph_edges (
    id BIGSERIAL PRIMARY KEY,
    source_id TEXT NOT NULL REFERENCES graph_nodes(id),
    target_id TEXT NOT NULL REFERENCES graph_nodes(id),
    relation TEXT NOT NULL,
    properties JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_graph_nodes_name ON graph_nodes(name);
CREATE INDEX IF NOT EXISTS idx_graph_edges_source ON graph_edges(source_id);
CREATE INDEX IF NOT EXISTS idx_graph_edges_target ON graph_edges(target_id);
CREATE INDEX IF NOT EXISTS idx_graph_edges_relation ON graph_edges(relation);
